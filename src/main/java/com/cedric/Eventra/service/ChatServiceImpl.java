package com.cedric.Eventra.service;

import com.cedric.Eventra.dto.ChatMessageDTO;
import com.cedric.Eventra.dto.ChatRoomDTO;
import com.cedric.Eventra.dto.Response;
import com.cedric.Eventra.dto.UserDTO; // Assuming this is your standard UserDTO
import com.cedric.Eventra.dto.CreateChatRoomRequestDTO;
import com.cedric.Eventra.dto.SendMessageRequestDTO;
import com.cedric.Eventra.entity.Booking;
import com.cedric.Eventra.entity.ChatMessage;
import com.cedric.Eventra.entity.ChatRoom;
import com.cedric.Eventra.entity.User;
import com.cedric.Eventra.exception.BadRequestException;
import com.cedric.Eventra.exception.ResourceNotFoundException;
import com.cedric.Eventra.exception.UnauthorizedException;
import com.cedric.Eventra.repository.BookingRepository;
import com.cedric.Eventra.repository.ChatMessageRepository;
import com.cedric.Eventra.repository.ChatRoomRepository;
import com.cedric.Eventra.repository.UserRepository;
import com.cedric.Eventra.service.ChatService;
import com.cedric.Eventra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final BookingRepository bookingRepository; // Optional, if linking chat to booking
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public Response getOrCreateChatRoom(CreateChatRoomRequestDTO requestDTO) {
        User currentUser;
        try {
            currentUser = userService.getCurrentLoggedInUser();
        } catch (Exception e) {
            return Response.builder().status(HttpStatus.UNAUTHORIZED.value()).message("User not authenticated.").build();
        }

        User otherUser = userRepository.findById(requestDTO.getOtherUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User to chat with not found with ID: " + requestDTO.getOtherUserId()));

        if (currentUser.getId().equals(otherUser.getId())) {
            return Response.builder().status(HttpStatus.BAD_REQUEST.value()).message("Cannot create or get a chat room with yourself.").build();
        }

        // Ensure consistent participant order for querying by sorting IDs
        User participant1 = currentUser.getId() < otherUser.getId() ? currentUser : otherUser;
        User participant2 = currentUser.getId() < otherUser.getId() ? otherUser : currentUser;

        Optional<ChatRoom> existingRoomOpt = chatRoomRepository.findChatRoomByParticipants(participant1, participant2);

        ChatRoom chatRoom;
        String message;
        HttpStatus httpStatus;

        if (existingRoomOpt.isPresent()) {
            chatRoom = existingRoomOpt.get();
            // Optionally update booking link if a new bookingId is provided and chat room already exists
            if (requestDTO.getBookingId() != null && (chatRoom.getBooking() == null || !chatRoom.getBooking().getId().equals(requestDTO.getBookingId()))) {
                Booking bookingContext = bookingRepository.findById(requestDTO.getBookingId()).orElse(null);
                if (bookingContext != null && areParticipantsRelatedToBooking(currentUser, otherUser, bookingContext)) {
                    chatRoom.setBooking(bookingContext);
                    chatRoom = chatRoomRepository.save(chatRoom); // Save if booking context updated
                }
            }
            message = "Chat room retrieved successfully.";
            httpStatus = HttpStatus.OK;
            log.info("Retrieved existing chat room ID {} between user {} and {}", chatRoom.getId(), participant1.getId(), participant2.getId());
        } else {
            Booking bookingContext = null;
            if (requestDTO.getBookingId() != null) {
                bookingContext = bookingRepository.findById(requestDTO.getBookingId())
                        .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + requestDTO.getBookingId()));
                if (!areParticipantsRelatedToBooking(currentUser, otherUser, bookingContext)) {
                    return Response.builder().status(HttpStatus.BAD_REQUEST.value()).message("Provided booking ID is not related to these participants.").build();
                }
            }

            chatRoom = ChatRoom.builder()
                    .participant1(participant1)
                    .participant2(participant2)
                    .booking(bookingContext)
                    .lastMessageAt(LocalDateTime.now()) // Initialize lastMessageAt
                    .build();
            // createdAt is set by @CreationTimestamp
            chatRoom = chatRoomRepository.save(chatRoom);
            message = "Chat room created successfully.";
            httpStatus = HttpStatus.CREATED;
            log.info("Created new chat room ID {} between user {} and {}", chatRoom.getId(), participant1.getId(), participant2.getId());
        }

        return Response.builder()
                .status(httpStatus.value())
                .message(message)
                .chatRoom(mapToChatRoomDTO(chatRoom)) // mapToChatRoomDTO will handle participant mapping
                .build();
    }

    private boolean areParticipantsRelatedToBooking(User userA, User userB, Booking booking) {
        User bookingCustomer = booking.getUser();
        User bookingProvider = booking.getOfferedService().getProvider();
        return (bookingCustomer.getId().equals(userA.getId()) && bookingProvider.getId().equals(userB.getId())) ||
                (bookingCustomer.getId().equals(userB.getId()) && bookingProvider.getId().equals(userA.getId()));
    }


    @Override
    @Transactional(readOnly = true)
    public Response getMyChatRooms() {
        User currentUser;
        try {
            currentUser = userService.getCurrentLoggedInUser();
        } catch (Exception e) {
            return Response.builder().status(HttpStatus.UNAUTHORIZED.value()).message("User not authenticated.").build();
        }

        List<ChatRoom> chatRooms = chatRoomRepository.findByParticipant1OrParticipant2OrderByLastMessageAtDesc(currentUser, currentUser);
        List<ChatRoomDTO> chatRoomDTOs = chatRooms.stream()
                .map(this::mapToChatRoomDTO)
                .collect(Collectors.toList());

        return Response.builder()
                .status(HttpStatus.OK.value())
                .message(chatRoomDTOs.isEmpty() ? "You have no active chat rooms." : "Chat rooms retrieved successfully.")
                .chatRooms(chatRoomDTOs.isEmpty() ? Collections.emptyList() : chatRoomDTOs)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Response getChatMessagesForRoom(Long chatRoomId) {
        User currentUser;
        try {
            currentUser = userService.getCurrentLoggedInUser();
        } catch (Exception e) {
            return Response.builder().status(HttpStatus.UNAUTHORIZED.value()).message("User not authenticated.").build();
        }

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat room not found with ID: " + chatRoomId));

        if (!chatRoom.getParticipant1().getId().equals(currentUser.getId()) &&
                !chatRoom.getParticipant2().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You are not authorized to view messages for this chat room.");
        }

        // Fetching all messages, ordered by timestamp (oldest first for typical chat display)
        List<ChatMessage> messages = chatMessageRepository.findByChatRoomOrderByTimestampAsc(chatRoom);
        List<ChatMessageDTO> messageDTOs = messages.stream()
                .map(this::mapToChatMessageDTO)
                .collect(Collectors.toList());

        return Response.builder()
                .status(HttpStatus.OK.value())
                .message("Chat messages retrieved successfully.")
                .chatMessages(messageDTOs)
                .build();
    }

    @Override
    @Transactional
    public ChatMessageDTO saveAndPrepareMessage(SendMessageRequestDTO messageRequest, User sender) {
        ChatRoom chatRoom = chatRoomRepository.findById(messageRequest.getChatRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Chat room not found with ID: " + messageRequest.getChatRoomId()));

        if (sender == null) {
            throw new UnauthorizedException("Sender cannot be null to send a message.");
        }

        if (!chatRoom.getParticipant1().getId().equals(sender.getId()) &&
                !chatRoom.getParticipant2().getId().equals(sender.getId())) {
            log.warn("User {} attempted to send message to chat room {} they are not part of.", sender.getId(), chatRoom.getId());
            throw new UnauthorizedException("You are not authorized to send messages to this chat room.");
        }

        ChatMessage chatMessage = ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .content(messageRequest.getContent())
                // timestamp is set by @CreationTimestamp
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);

        // Update lastMessageAt for the chat room to the timestamp of the new message
        chatRoom.setLastMessageAt(savedMessage.getTimestamp());
        chatRoomRepository.save(chatRoom);

        log.info("Message sent by user {} in chat room {}: {}", sender.getId(), chatRoom.getId(), savedMessage.getContent());
        return mapToChatMessageDTO(savedMessage);
    }

    // --- Helper Mapping Methods ---

    private ChatRoomDTO mapToChatRoomDTO(ChatRoom chatRoom) {
        ChatRoomDTO dto = new ChatRoomDTO();
        dto.setId(chatRoom.getId());
        if (chatRoom.getParticipant1() != null) {
            dto.setParticipant1(modelMapper.map(chatRoom.getParticipant1(), UserDTO.class));
        }
        if (chatRoom.getParticipant2() != null) {
            dto.setParticipant2(modelMapper.map(chatRoom.getParticipant2(), UserDTO.class));
        }
        if (chatRoom.getBooking() != null) {
            dto.setBookingId(chatRoom.getBooking().getId());
        }
        dto.setCreatedAt(chatRoom.getCreatedAt());
        dto.setLastMessageAt(chatRoom.getLastMessageAt());

        // For simplified version, we are not fetching the last message object here.
        // dto.setLastMessage(null); // Or omit if JsonInclude.Include.NON_NULL is used on ChatRoomDTO

        return dto;
    }

    private ChatMessageDTO mapToChatMessageDTO(ChatMessage chatMessage) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setId(chatMessage.getId());
        dto.setChatRoomId(chatMessage.getChatRoom().getId());
        if (chatMessage.getSender() != null) {
            dto.setSender(modelMapper.map(chatMessage.getSender(), UserDTO.class));
        }
        dto.setContent(chatMessage.getContent());
        dto.setTimestamp(chatMessage.getTimestamp());
        return dto;
    }
}
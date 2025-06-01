package com.cedric.Eventra.service;

import com.cedric.Eventra.entity.BookingReference;
import com.cedric.Eventra.repository.BookingReferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class BookingCodeGenerator {

    private final BookingReferenceRepository bookingReferenceRepository;

    // auto generate unique reference number
    private String generateRandomAlphaNumericCode(int length){
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ123456789";
        Random random = new Random();
        StringBuilder stringBuilder = new StringBuilder(length);
        for(int i = 0; i < length; i++){
            int index = random.nextInt(characters.length());
            stringBuilder.append(characters.charAt(index));
        }

        return stringBuilder.toString();
    }

    public String generateBookingReference(){
        String bookingReference;
        // Keep generating until a unique code is found
        do{
            bookingReference = generateRandomAlphaNumericCode(10); // generate code of length 10
        } while(isBookingReferenceExist(bookingReference)); // check if the code already exists
        saveBookingReferenceToDatabase(bookingReference); // save to database

        return bookingReference;
    }

    private boolean isBookingReferenceExist(String bookingReference){
        return bookingReferenceRepository.findByReferenceNo(bookingReference).isPresent();
    }

    private void saveBookingReferenceToDatabase(String bookingReference){
        BookingReference newBookingReference = BookingReference.builder()
                .referenceNo(bookingReference).build();
        bookingReferenceRepository.save(newBookingReference);
    }
}

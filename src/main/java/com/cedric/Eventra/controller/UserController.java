package com.cedric.Eventra.controller;

import com.cedric.Eventra.dto.Response;
import com.cedric.Eventra.dto.UserDTO;
import com.cedric.Eventra.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response> getAllUsers(){
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/account")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'SERVICE_PROVIDER')")
    public ResponseEntity<Response> getOwnAccountDetails(){
        return ResponseEntity.ok(userService.getOwnAccountDetails());
    }

    @PutMapping("/account")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'SERVICE_PROVIDER')")
    public ResponseEntity<Response> updateOwnAccount(@RequestBody @Valid UserDTO userDTO){
        return ResponseEntity.ok(userService.updateOwnAccount(userDTO));
    }

    @DeleteMapping("/account")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'SERVICE_PROVIDER')")
    public ResponseEntity<Response> deleteOwnAccount(){
        return ResponseEntity.ok(userService.deleteOwnAccount());
    }

    @GetMapping("/bookings")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<Response> getMyBookingHistory(){
        return ResponseEntity.ok(userService.getMyBookingHistory());
    }

}

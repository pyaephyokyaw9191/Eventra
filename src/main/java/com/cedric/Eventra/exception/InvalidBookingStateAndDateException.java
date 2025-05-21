package com.cedric.Eventra.exception;

public class InvalidBookingStateAndDateException extends RuntimeException {

    public InvalidBookingStateAndDateException(String message){
        super(message); 
    }
}

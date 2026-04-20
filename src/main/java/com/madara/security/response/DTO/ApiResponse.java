package com.madara.security.response.DTO;

import lombok.*;
import org.springframework.http.HttpStatus;

@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private HttpStatus status;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(
         T data,
         String message,
         HttpStatus status
    ) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(status)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(
       String message,
       HttpStatus status
    ) {
        return ApiResponse.<T>builder()
                .success(false)
                .status(status)
                .message(message)
                .data(null)
                .build();
    }

    public static <T> ApiResponse<T> error(
            String message,
            HttpStatus status,
            T data
    ) {
        return ApiResponse.<T>builder()
                .success(false)
                .status(status)
                .message(message)
                .data(data)
                .build();
    }


}

package com.madara.security.controller;

import com.madara.security.service.GetUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hello")
@RequiredArgsConstructor
public class SayHelloController {

    private final GetUserDetails getUserDetails;

    @GetMapping
    public ResponseEntity<Long> sayHello(){
        long userId = getUserDetails.getUserId();
        return new ResponseEntity<>(userId, HttpStatus.OK);
    }
}

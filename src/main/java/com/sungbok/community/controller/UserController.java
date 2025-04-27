package com.sungbok.community.controller;


import com.sungbok.community.dto.AddUserRequestDTO;
import com.sungbok.community.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@Valid @RequestBody AddUserRequestDTO request) {
        userService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}

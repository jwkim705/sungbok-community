package com.sungbok.community.controller;


import com.sungbok.community.dto.AddUserRequestDTO;
import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.service.change.ChangeUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final ChangeUserService changeUserService;

    @PostMapping("/signup")
    public ResponseEntity<UserMemberDTO> signup(@RequestBody @Valid AddUserRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(changeUserService.signup(request));
    }

}

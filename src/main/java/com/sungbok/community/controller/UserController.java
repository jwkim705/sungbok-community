package com.sungbok.community.controller;


import com.sungbok.community.common.constant.UriConstant;
import com.sungbok.community.dto.AddUserRequestDTO;
import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.service.change.ChangeUserService;
import com.sungbok.community.service.get.GetUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(UriConstant.USERS)
public class UserController {

    private final GetUserService getUserService;
    private final ChangeUserService changeUserService;

//    @GetMapping("/{userId}")
//    public ResponseEntity<UserMemberDTO> getUserById(@PathVariable("userId") Long userId) {
//        return ResponseEntity.ok(getUserService.getUserById(userId));
//    }

//    @GetMapping
//    public ResponseEntity<Page<UserMemberDTO>> getUserList(Pageable pageable) {
//        return ResponseEntity.ok(getUserService.getUserList(pageable));
//    }

    @PostMapping("/signup")
    public ResponseEntity<UserMemberDTO> signup(@RequestBody @Valid AddUserRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(changeUserService.signup(request));
    }

//    @PatchMapping("/{userId}")
//    public ResponseEntity<UpdateUserResponseDTO> updateUser(@RequestBody @Valid UpdateUserRequestDTO updateUserRequest,
//                                                            @PathVariable("userId") Long userId) {
//        return ResponseEntity.ok(changeUserService.updateUser(userId, updateUserRequest));
//    }

//    @PatchMapping
//    public ResponseEntity<UpdateMeResponseDTO> updateMe(@RequestBody @Valid UpdateUserRequestDTO updateUserRequest, Authentication authentication) {
//        PrincipalDetails user =  (PrincipalDetails) authentication.getPrincipal();
//        return ResponseEntity.ok(changeUserService.updateMe(user.getUser().getUserId(), updateUserRequest));
//    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable("userId") Long userId) {
        changeUserService.deleteUser(userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}

package com.paymedia.administrations.controller;

import com.paymedia.administrations.entity.DualAuthData;
import com.paymedia.administrations.entity.User;
import com.paymedia.administrations.model.request.UserRequest;
import com.paymedia.administrations.model.response.CommonResponse;
import com.paymedia.administrations.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<User>>> getUsers(
            @RequestParam(value = "search", required = false) String searchTerm,
            Pageable pageable) {
        Page<User> users = userService.searchUsers(searchTerm, pageable);
        return ResponseEntity.ok(new CommonResponse<>(true, "Users fetched successfully", users));
    }

//    @PostMapping("/create")
//    public ResponseEntity<CommonResponse<String>> createUser(@RequestBody User user) {
//        // Get the current authentication object
//
//
//        // Call the service to create the user
//        userService.createUser(user);
//
//        return ResponseEntity.ok(new CommonResponse<>(true, "User creation request submitted successfully"));
//    }

//    @PostMapping("/create")
//    public ResponseEntity<CommonResponse<UserRequest>> createUser(@RequestBody User user, @RequestParam Long creatorId) {
//        UserDto createdUser = dualAuthSystemService.createPendingUser(user, creatorId);
//        return ResponseEntity.ok(new ApiResponse<>(true, "User created and pending approval", createdUser));
//    }

    @PostMapping("/create")
    public ResponseEntity<CommonResponse<DualAuthData>> createUser(@RequestBody UserRequest userRequest) {

        DualAuthData createdUser = userService.createUser(userRequest);


        return ResponseEntity.ok(new CommonResponse<>(true, "User creation request submitted successfully", createdUser));
    }


    @PostMapping("/approve/{id}")
    public ResponseEntity<CommonResponse<String>> approveUser(@PathVariable Integer id) {

        userService.approveUser(id);

        return ResponseEntity.ok(new CommonResponse<>(true, "User approved successfully", null));
    }

    @PostMapping("/reject/{id}")
    public ResponseEntity<CommonResponse<String>> rejectUser(@PathVariable Integer id) {
        userService.rejectUser(id);
        return ResponseEntity.ok(new CommonResponse<>(true, "User rejected successfully", null));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<CommonResponse<String>> updateUserRequest(
            @PathVariable Integer id,
            @RequestBody UserRequest newUserRequest) {

        String result = userService.updateUserRequest(id, newUserRequest);
        return ResponseEntity.ok(new CommonResponse<>(true, result, null));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<CommonResponse<String>> deleteDualAuthData(
            @PathVariable Integer id) {

        String result = userService.deleteDualAuthDataById(id);
        return ResponseEntity.ok(new CommonResponse<>(true, result, null));
    }


}

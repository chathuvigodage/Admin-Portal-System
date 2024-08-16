package com.paymedia.administrations.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymedia.administrations.entity.DualAuthData;
import com.paymedia.administrations.entity.Role;
import com.paymedia.administrations.entity.User;
import com.paymedia.administrations.model.UserSearchCriteria;
import com.paymedia.administrations.model.request.UserRequest;
import com.paymedia.administrations.repository.DualAuthDataRepository;
import com.paymedia.administrations.repository.RoleRepository;
import com.paymedia.administrations.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private DualAuthDataRepository dualAuthDataRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private PasswordEncoder passwordEncoder;



    public Page<User> searchUsers(String searchTerm, Pageable pageable) {
        return userRepository.searchByUsernameOrRoleName(searchTerm, pageable);
    }

//    public void createUser(UserRequest userRequest) {
//    log.info("================================================>####");
//        try {
//            Integer adminId = authenticationService.getLoggedInUserId();
//            log.info("*********************Logged-in user ID: {}", adminId);
//            if (adminId == null) {
//                throw new RuntimeException("Logged-in user ID is null");
//            }
//            // Convert UserRequest to JSON
//            log.info("========================================>before new data");
//            String newData = objectMapper.writeValueAsString(userRequest);
//            log.info("====================>UserRequest JSON: {}", newData);
//
//            // Create DualAuthData
//            DualAuthData dualAuthData = DualAuthData.builder()
//                    .entity("User")
//                    .newData(newData)
//                    .createdBy(adminId)
//                    .status("Pending")
//                    .build();
//
//            dualAuthDataRepository.save(dualAuthData);
//            log.info("=====================>DualAuthData saved successfully with ID: {}", dualAuthData.getId());
//
//        } catch (Exception e) {
//            log.error("Error creating DualAuthData", e);
//        }
//
//    }


    public DualAuthData createUser(UserRequest userRequest) {
        log.info("Starting to create user...");
        try {
            Integer adminId = authenticationService.getLoggedInUserId();
            log.info("*********************Logged-in user ID: {}", adminId);
            if (adminId == null) {
                throw new RuntimeException("Logged-in user ID is null");
            }

            String newData = objectMapper.writeValueAsString(userRequest);
            log.info("UserRequest JSON: {}", newData);


            DualAuthData dualAuthData = DualAuthData.builder()
                    .entity("User")
                    .newData(newData)
                    .createdBy(adminId)
                    .status("Pending")
                    .build();


            DualAuthData savedDualAuthData = dualAuthDataRepository.save(dualAuthData);
            log.info("DualAuthData saved successfully with ID: {}", savedDualAuthData.getId());
            return savedDualAuthData;
        } catch (Exception e) {
            log.error("Error creating DualAuthData", e);
            throw new RuntimeException("Failed to create user: " + e.getMessage());
        }
    }

    public void approveUser(Integer dualAuthDataId) {
        Optional<DualAuthData> optionalDualAuthData = dualAuthDataRepository.findById(dualAuthDataId);

        if (optionalDualAuthData.isPresent()) {
            DualAuthData dualAuthData = optionalDualAuthData.get();

            try {
                UserRequest userRequest = objectMapper.readValue(dualAuthData.getNewData(), UserRequest.class);
                Optional<Role> roleOptional = roleRepository.findById(userRequest.getRoleId());
                Integer adminId = authenticationService.getLoggedInUserId();
                if(roleOptional.isEmpty()) {
                    log.info("register -> role not found");
                }

                Role role = roleOptional.get();


                User user = User.builder()
                        .firstName(userRequest.getFirstName())
                        .lastName(userRequest.getLastName())
                        .username(userRequest.getUsername())
                        .password(passwordEncoder.encode(userRequest.getPassword())) // Remember to encode password before saving
                        .role(role) // Assign role based on role ID
                        .build();

                userRepository.save(user);

                // Update DualAuthData status and reviewedBy
                dualAuthData.setReviewedBy(adminId); // Assume a method to get the logged-in user's ID
                dualAuthData.setStatus("Approved");
//                dualAuthData.setReviewedAt(LocalDateTime.now());
                dualAuthDataRepository.save(dualAuthData);


            } catch (Exception e) {
                log.error("Error approving user", e);
            }
        } else {
            log.error("DualAuthData not found for id: {}", dualAuthDataId);
        }
    }

    public void rejectUser(Integer dualAuthDataId) {
        Optional<DualAuthData> optionalDualAuthData = dualAuthDataRepository.findById(dualAuthDataId);

        if (optionalDualAuthData.isPresent()) {
            DualAuthData dualAuthData = optionalDualAuthData.get();


            try {
                Integer adminId = authenticationService.getLoggedInUserId();

                dualAuthData.setReviewedBy(adminId); // Assume a method to get the logged-in user's ID
                dualAuthData.setStatus("Rejected");
//                dualAuthData.setReviewedAt(LocalDateTime.now());
                dualAuthDataRepository.save(dualAuthData);
            } catch (Exception e) {
                log.error("Error approving user", e);
            }
        }
    }

    public String updateUserRequest(Integer id, UserRequest newUserRequest) {
        Optional<DualAuthData> optionalDualAuthData = dualAuthDataRepository.findById(id);

        if (optionalDualAuthData.isPresent()) {
            DualAuthData dualAuthData = optionalDualAuthData.get();

            try {

                String updatedNewData = objectMapper.writeValueAsString(newUserRequest);


                String currentNewData = dualAuthData.getNewData();
                dualAuthData.setOldData(currentNewData);


                dualAuthData.setNewData(updatedNewData);


                dualAuthDataRepository.save(dualAuthData);

                return "User request updated successfully";

            } catch (Exception e) {
                log.error("Error updating DualAuthData", e);
                return "Error updating user request";
            }
        } else {
            log.error("DualAuthData not found for id: {}", id);
            return "DualAuthData not found";
        }
    }

    public String deleteDualAuthDataById(Integer id) {
        try {
            if (dualAuthDataRepository.existsById(id)) {
                dualAuthDataRepository.deleteById(id);
                return "DualAuthData record deleted successfully";
            } else {
                return "DualAuthData record not found";
            }
        } catch (Exception e) {
            log.error("Error deleting DualAuthData", e);
            return "Error deleting DualAuthData record";
        }
    }


}
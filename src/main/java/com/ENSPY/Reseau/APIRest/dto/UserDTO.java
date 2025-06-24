package com.ENSPY.Reseau.APIRest.dto;

import com.ENSPY.Reseau.APIRest.model.User;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class UserDTO {
    private UUID id;
    private String email;
    private String companyName;
    private String firstName;
    private String lastName;
    private String profilePicture;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UserDTO(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.companyName = user.getCompanyName();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.profilePicture = user.getProfilePicture();
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
    }
}

package com.elkassimi.monitoring_v2_0.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 128)
    private String id;

    @Email
    @Column(nullable = false,unique = true)
    private String email;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String password;

    @Enumerated(EnumType.STRING)
    private role role;

    private boolean isOnline;

    public enum role{
        ADMIN,VIEWER,AGENT
    }


}

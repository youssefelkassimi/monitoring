package com.elkassimi.monitoring_v2_0.repository;

import com.elkassimi.monitoring_v2_0.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User,String> {

    List<User> findByIsOnlineIsTrue();

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    List<User> findByRole(User.role role);
}

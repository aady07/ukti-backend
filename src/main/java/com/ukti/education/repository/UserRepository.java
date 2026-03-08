package com.ukti.education.repository;

import com.ukti.education.entity.User;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByCognitoSub(String cognitoSub);

    Optional<User> findByEmail(String email);

    boolean existsByCognitoSub(String cognitoSub);

    boolean existsByEmail(String email);
}

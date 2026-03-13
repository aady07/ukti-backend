package com.ukti.education.repository;

import com.ukti.education.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByCognitoSub(String cognitoSub);

    Optional<User> findByEmail(String email);

    boolean existsByCognitoSub(String cognitoSub);

    boolean existsByEmail(String email);

    Optional<User> findBySchoolUuidAndClassIdAndRollNumberAndUserType(
            UUID schoolUuid, UUID classId, String rollNumber, String userType);

    List<User> findBySchoolUuidAndRollNumberAndUserType(
            UUID schoolUuid, String rollNumber, String userType);

    List<User> findBySchoolUuidAndClassIdAndUserTypeOrderByRollNumber(
            UUID schoolUuid, UUID classId, String userType);

    boolean existsBySchoolUuidAndClassIdAndRollNumberAndUserType(
            UUID schoolUuid, UUID classId, String rollNumber, String userType);
}

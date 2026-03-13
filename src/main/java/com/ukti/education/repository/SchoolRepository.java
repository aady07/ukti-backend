package com.ukti.education.repository;

import com.ukti.education.entity.School;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolRepository extends JpaRepository<School, UUID> {
}

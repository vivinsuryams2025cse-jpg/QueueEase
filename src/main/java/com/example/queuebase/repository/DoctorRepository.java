package com.example.queuebase.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.queuebase.model.Doctor;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
}
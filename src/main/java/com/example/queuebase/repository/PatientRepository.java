package com.example.queuebase.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.queuebase.model.Patient;

public interface PatientRepository extends JpaRepository<Patient, Long> {
}
package com.example.queuebase.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.queuebase.model.Doctor;

import jakarta.persistence.LockModeType;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT doctor FROM Doctor doctor WHERE doctor.id = :doctorId")
	Optional<Doctor> findByIdForUpdate(@Param("doctorId") Long doctorId);
}
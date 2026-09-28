package com.example.queuebase.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.queuebase.exception.InvalidTokenOperationException;
import com.example.queuebase.model.Doctor;
import com.example.queuebase.repository.DoctorRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

@Service
public class DoctorService {

	private final DoctorRepository doctorRepository;
	private final Validator validator;

	public DoctorService(DoctorRepository doctorRepository, Validator validator) {
		this.doctorRepository = doctorRepository;
		this.validator = validator;
	}

	public Doctor createDoctor(Doctor doctor) {
		validateDoctor(doctor);
		return doctorRepository.save(doctor);
	}

	public List<Doctor> getAllDoctors() {
		return doctorRepository.findAll();
	}

	public Doctor getDoctorById(Long id) {
		return doctorRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Doctor not found with ID: " + id));
	}

	public Doctor updateDoctor(Long id, Doctor updatedDoctor) {
		Doctor doctor = getDoctorById(id);
		doctor.setName(updatedDoctor.getName());
		doctor.setSpecialization(updatedDoctor.getSpecialization());
		doctor.setAverageConsultationMinutes(updatedDoctor.getAverageConsultationMinutes());
		doctor.setActive(updatedDoctor.getActive());
		validateDoctor(doctor);
		return doctorRepository.save(doctor);
	}

	public void deleteDoctor(Long id) {
		doctorRepository.delete(getDoctorById(id));
	}

	public boolean doctorExists(Long id) {
		return doctorRepository.existsById(id);
	}

	@Transactional(readOnly = true)
	public Doctor getActiveDoctorById(Long id) {
		Doctor doctor = getDoctorById(id);
		if (!Boolean.TRUE.equals(doctor.getActive())) {
			throw new InvalidTokenOperationException("The selected doctor is not active.");
		}
		return doctor;
	}

	private void validateDoctor(Doctor doctor) {
		Set<ConstraintViolation<Doctor>> violations = validator.validate(doctor);
		if (!violations.isEmpty()) {
			throw new ConstraintViolationException(violations);
		}
	}
}
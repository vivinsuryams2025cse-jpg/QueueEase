package com.example.queuebase.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.example.queuebase.model.Patient;
import com.example.queuebase.repository.PatientRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

@Service
public class PatientService {

	private final PatientRepository patientRepository;
	private final Validator validator;

	public PatientService(PatientRepository patientRepository, Validator validator) {
		this.patientRepository = patientRepository;
		this.validator = validator;
	}

	public Patient registerPatient(Patient patient) {
		validatePatient(patient);
		return patientRepository.save(patient);
	}

	public List<Patient> getAllPatients() {
		return patientRepository.findAll();
	}

	public Patient getPatientById(Long id) {
		return patientRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Patient not found with ID: " + id));
	}

	public Patient updatePatient(Long id, Patient updatedPatient) {
		Patient patient = getPatientById(id);
		patient.setName(updatedPatient.getName());
		patient.setAge(updatedPatient.getAge());
		patient.setPhone(updatedPatient.getPhone());
		patient.setGender(updatedPatient.getGender());

		validatePatient(patient);
		return patientRepository.save(patient);
	}

	private void validatePatient(Patient patient) {
		Set<ConstraintViolation<Patient>> violations = validator.validate(patient);
		if (!violations.isEmpty()) {
			throw new ConstraintViolationException(violations);
		}
	}
}
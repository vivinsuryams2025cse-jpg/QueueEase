package com.example.queuebase.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.queuebase.model.Doctor;
import com.example.queuebase.model.Token;
import com.example.queuebase.service.DoctorService;
import com.example.queuebase.service.TokenService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/doctors")
public class DoctorController {

	private final DoctorService doctorService;
	private final TokenService tokenService;

	public DoctorController(DoctorService doctorService, TokenService tokenService) {
		this.doctorService = doctorService;
		this.tokenService = tokenService;
	}

	@GetMapping
	public List<Doctor> getAllDoctors() {
		return doctorService.getAllDoctors();
	}

	@GetMapping("/{id}")
	public Doctor getDoctorById(@PathVariable Long id) {
		return doctorService.getDoctorById(id);
	}

	@PostMapping
	public ResponseEntity<Doctor> createDoctor(@Valid @RequestBody Doctor doctor) {
		return ResponseEntity.status(HttpStatus.CREATED).body(doctorService.createDoctor(doctor));
	}

	@PutMapping("/{id}")
	public Doctor updateDoctor(@PathVariable Long id, @Valid @RequestBody Doctor doctor) {
		return doctorService.updateDoctor(id, doctor);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteDoctor(@PathVariable Long id) {
		doctorService.deleteDoctor(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{id}/queue")
	public List<Token> getQueue(@PathVariable Long id) {
		return tokenService.getWaitingTokensForDoctor(id);
	}

	@PostMapping("/{id}/next")
	public ResponseEntity<Token> serveNextPatient(@PathVariable Long id) {
		Optional<Token> nextToken = tokenService.serveNextPatient(id);
		return nextToken.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
	}

	@GetMapping("/{id}/current-token")
	public ResponseEntity<Token> getCurrentServingToken(@PathVariable Long id) {
		return tokenService.getCurrentServingToken(id)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.noContent().build());
	}

	@GetMapping("/{id}/history")
	public Page<Token> getTokenHistory(
			@PathVariable Long id,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@PageableDefault(size = 20) Pageable pageable) {
		return tokenService.getTokenHistory(id, date == null ? LocalDate.now() : date, pageable);
	}
}
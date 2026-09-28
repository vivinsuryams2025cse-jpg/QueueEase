package com.example.queuebase.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.queuebase.exception.InvalidTokenOperationException;
import com.example.queuebase.model.Doctor;
import com.example.queuebase.model.Patient;
import com.example.queuebase.model.Token;
import com.example.queuebase.model.TokenStatus;
import com.example.queuebase.repository.DoctorRepository;
import com.example.queuebase.repository.PatientRepository;
import com.example.queuebase.repository.TokenRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

@Service
public class TokenService {

	private static final List<TokenStatus> ACTIVE_PRIORITY_STATUSES =
			List.of(TokenStatus.WAITING, TokenStatus.SERVING);

	private final DoctorRepository doctorRepository;
	private final PatientRepository patientRepository;
	private final TokenRepository tokenRepository;
	private final Validator validator;

	public TokenService(
			DoctorRepository doctorRepository,
			PatientRepository patientRepository,
			TokenRepository tokenRepository,
			Validator validator) {
		this.doctorRepository = doctorRepository;
		this.patientRepository = patientRepository;
		this.tokenRepository = tokenRepository;
		this.validator = validator;
	}

	@Transactional
	public Token generateToken(Long doctorId, Long patientId, boolean priority) {
		Doctor doctor = lockActiveDoctor(doctorId);
		Patient patient = patientRepository.findById(patientId)
				.orElseThrow(() -> new EntityNotFoundException("Patient not found with ID: " + patientId));
		LocalDate today = LocalDate.now();

		if (priority && hasActivePriorityToken(doctorId, today)) {
			throw new InvalidTokenOperationException("A priority token is already active for this doctor today.");
		}

		int nextNumber = tokenRepository.findHighestTokenNumberByDoctorAndTokenDate(doctorId, today)
				.orElse(0) + 1;

		Token token = new Token();
		token.setDoctor(doctor);
		token.setPatient(patient);
		token.setTokenNumber(nextNumber);
		token.setTokenDate(today);
		token.setPriority(priority);
		token.setStatus(TokenStatus.WAITING);
		token.setCreatedAt(LocalDateTime.now());
		validateToken(token);

		Token savedToken = tokenRepository.save(token);
		updateWaitingTimes(doctor, today);
		return savedToken;
	}

	@Transactional(readOnly = true)
	public Page<Token> getAllTokens(Pageable pageable) {
		return tokenRepository.findAll(pageable);
	}

	@Transactional(readOnly = true)
	public Token getTokenById(Long id) {
		return tokenRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Token not found with ID: " + id));
	}

	@Transactional
	public void cancelToken(Long id) {
		Token token = getTokenById(id);
		if (token.getStatus() != TokenStatus.WAITING) {
			throw new InvalidTokenOperationException("Only waiting tokens can be cancelled.");
		}
		Doctor doctor = lockDoctor(token.getDoctor().getId());
		token.setStatus(TokenStatus.CANCELLED);
		tokenRepository.save(token);
		updateWaitingTimes(doctor, token.getTokenDate());
	}

	@Transactional(readOnly = true)
	public List<Token> getTodaysTokensForDoctor(Long doctorId) {
		return tokenRepository.findByDoctor_IdAndTokenDateOrderByPriorityDescTokenNumberAsc(
				doctorId, LocalDate.now());
	}

	@Transactional(readOnly = true)
	public List<Token> getWaitingTokensForDoctor(Long doctorId) {
		return tokenRepository.findByDoctor_IdAndTokenDateAndStatusOrderByPriorityDescTokenNumberAsc(
				doctorId, LocalDate.now(), TokenStatus.WAITING);
	}

	@Transactional(readOnly = true)
	public Page<Token> getTokenHistory(Long doctorId, LocalDate date, Pageable pageable) {
		return tokenRepository.findByDoctor_IdAndTokenDateOrderByTokenNumberAsc(doctorId, date, pageable);
	}

	@Transactional(readOnly = true)
	public Optional<Token> getCurrentServingToken(Long doctorId) {
		return tokenRepository.findFirstByDoctor_IdAndTokenDateAndStatusOrderByCreatedAtAsc(
				doctorId, LocalDate.now(), TokenStatus.SERVING);
	}

	@Transactional(readOnly = true)
	public int getEstimatedWaitingTime(Long tokenId) {
		Token token = getTokenById(tokenId);
		return token.getEstimatedWaitMinutes() == null ? 0 : token.getEstimatedWaitMinutes();
	}

	@Transactional
	public Token markPriority(Long tokenId) {
		Token token = getTokenById(tokenId);
		Doctor doctor = lockActiveDoctor(token.getDoctor().getId());
		LocalDate today = LocalDate.now();

		if (!today.equals(token.getTokenDate()) || token.getStatus() != TokenStatus.WAITING) {
			throw new InvalidTokenOperationException("Only today's waiting tokens can be marked as priority.");
		}
		if (Boolean.TRUE.equals(token.getPriority())) {
			return token;
		}
		if (hasActivePriorityToken(doctor.getId(), today)) {
			throw new InvalidTokenOperationException("A priority token is already active for this doctor today.");
		}

		token.setPriority(true);
		Token savedToken = tokenRepository.save(token);
		updateWaitingTimes(doctor, today);
		return savedToken;
	}

	@Transactional
	public Optional<Token> serveNextPatient(Long doctorId) {
		Doctor doctor = lockDoctor(doctorId);
		LocalDate today = LocalDate.now();

		tokenRepository.findFirstByDoctor_IdAndTokenDateAndStatusOrderByCreatedAtAsc(
				doctorId, today, TokenStatus.SERVING).ifPresent(currentToken -> {
			currentToken.setStatus(TokenStatus.COMPLETED);
			tokenRepository.save(currentToken);
		});

		List<Token> priorityTokens = tokenRepository
				.findByDoctor_IdAndTokenDateAndStatusAndPriorityTrueOrderByTokenNumberAsc(
						doctorId, today, TokenStatus.WAITING);
		List<Token> normalTokens = priorityTokens.isEmpty()
				? tokenRepository.findByDoctor_IdAndTokenDateAndStatusAndPriorityFalseOrderByTokenNumberAsc(
						doctorId, today, TokenStatus.WAITING)
				: List.of();

		Token nextToken = !priorityTokens.isEmpty()
				? priorityTokens.get(0)
				: normalTokens.isEmpty() ? null : normalTokens.get(0);
		if (nextToken == null) {
			updateWaitingTimes(doctor, today);
			return Optional.empty();
		}

		nextToken.setStatus(TokenStatus.SERVING);
		Token servingToken = tokenRepository.save(nextToken);
		updateWaitingTimes(doctor, today);
		return Optional.of(servingToken);
	}

	private Doctor lockActiveDoctor(Long doctorId) {
		Doctor doctor = lockDoctor(doctorId);
		if (!Boolean.TRUE.equals(doctor.getActive())) {
			throw new InvalidTokenOperationException("The selected doctor is not active.");
		}
		return doctor;
	}

	private Doctor lockDoctor(Long doctorId) {
		return doctorRepository.findByIdForUpdate(doctorId)
				.orElseThrow(() -> new EntityNotFoundException("Doctor not found with ID: " + doctorId));
	}

	private boolean hasActivePriorityToken(Long doctorId, LocalDate date) {
		return tokenRepository.existsByDoctor_IdAndTokenDateAndPriorityTrueAndStatusIn(
				doctorId, date, ACTIVE_PRIORITY_STATUSES);
	}

	private void updateWaitingTimes(Doctor doctor, LocalDate date) {
		List<Token> waitingTokens = tokenRepository
				.findByDoctor_IdAndTokenDateAndStatusOrderByPriorityDescTokenNumberAsc(
						doctor.getId(), date, TokenStatus.WAITING);
		boolean serving = tokenRepository.findFirstByDoctor_IdAndTokenDateAndStatusOrderByCreatedAtAsc(
				doctor.getId(), date, TokenStatus.SERVING).isPresent();
		int patientsAhead = serving ? 1 : 0;
		int consultationMinutes = doctor.getAverageConsultationMinutes();

		for (Token waitingToken : waitingTokens) {
			waitingToken.setEstimatedWaitMinutes(patientsAhead * consultationMinutes);
			patientsAhead++;
		}
		tokenRepository.saveAll(waitingTokens);
	}

	private void validateToken(Token token) {
		Set<ConstraintViolation<Token>> violations = validator.validate(token);
		if (!violations.isEmpty()) {
			throw new ConstraintViolationException(violations);
		}
	}
}
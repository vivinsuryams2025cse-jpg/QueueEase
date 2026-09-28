package com.example.queuebase.controller;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.queuebase.dto.CreateTokenRequest;
import com.example.queuebase.model.Token;
import com.example.queuebase.service.TokenService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/tokens")
public class TokenController {

	private final TokenService tokenService;

	public TokenController(TokenService tokenService) {
		this.tokenService = tokenService;
	}

	@GetMapping
	public Page<Token> getAllTokens(@PageableDefault(size = 20) Pageable pageable) {
		return tokenService.getAllTokens(pageable);
	}

	@GetMapping("/{id}")
	public Token getTokenById(@PathVariable Long id) {
		return tokenService.getTokenById(id);
	}

	@PostMapping
	public ResponseEntity<Token> createToken(@Valid @RequestBody CreateTokenRequest request) {
		Token token = tokenService.generateToken(
				request.getDoctorId(), request.getPatientId(), Boolean.TRUE.equals(request.getPriority()));
		return ResponseEntity.status(HttpStatus.CREATED).body(token);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> cancelToken(@PathVariable Long id) {
		tokenService.cancelToken(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/priority")
	public Token markPriority(@PathVariable Long id) {
		return tokenService.markPriority(id);
	}

	@GetMapping("/{id}/waiting-time")
	public Map<String, Object> getWaitingTime(@PathVariable Long id) {
		return Map.of("tokenId", id, "estimatedWaitMinutes", tokenService.getEstimatedWaitingTime(id));
	}
}
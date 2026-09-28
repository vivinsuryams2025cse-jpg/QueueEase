package com.example.queuebase.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.queuebase.model.Token;
import com.example.queuebase.model.TokenStatus;

public interface TokenRepository extends JpaRepository<Token, Long> {

	@Query("SELECT token FROM Token token WHERE token.doctor.id = :doctorId "
			+ "AND token.tokenDate = CURRENT_DATE ORDER BY token.tokenNumber ASC")
	List<Token> findTodaysTokensByDoctorId(@Param("doctorId") Long doctorId);

	List<Token> findAllByOrderByTokenNumberAsc();

	List<Token> findByStatus(TokenStatus status);

	List<Token> findByPriorityTrue();

	@Query("SELECT MAX(token.tokenNumber) FROM Token token "
			+ "WHERE token.doctor.id = :doctorId AND token.tokenDate = :tokenDate")
	Optional<Integer> findHighestTokenNumberByDoctorAndTokenDate(
			@Param("doctorId") Long doctorId,
			@Param("tokenDate") LocalDate tokenDate);
}
package com.example.queuebase.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

	List<Token> findByDoctor_IdAndTokenDateOrderByTokenNumberAsc(Long doctorId, LocalDate tokenDate);

	List<Token> findByDoctor_IdAndTokenDateOrderByPriorityDescTokenNumberAsc(Long doctorId, LocalDate tokenDate);

	Page<Token> findByDoctor_IdAndTokenDateOrderByTokenNumberAsc(
			Long doctorId, LocalDate tokenDate, Pageable pageable);

	List<Token> findByStatus(TokenStatus status);

	List<Token> findByDoctor_IdAndTokenDateAndStatus(Long doctorId, LocalDate tokenDate, TokenStatus status);

	List<Token> findByDoctor_IdAndTokenDateAndStatusOrderByPriorityDescTokenNumberAsc(
			Long doctorId, LocalDate tokenDate, TokenStatus status);

	List<Token> findByDoctor_IdAndTokenDateAndStatusAndPriorityTrueOrderByTokenNumberAsc(
			Long doctorId, LocalDate tokenDate, TokenStatus status);

	List<Token> findByDoctor_IdAndTokenDateAndStatusAndPriorityFalseOrderByTokenNumberAsc(
			Long doctorId, LocalDate tokenDate, TokenStatus status);

	Optional<Token> findFirstByDoctor_IdAndTokenDateAndStatusOrderByCreatedAtAsc(
			Long doctorId, LocalDate tokenDate, TokenStatus status);

	List<Token> findByPriorityTrue();

	List<Token> findByDoctor_IdAndTokenDateAndPriorityTrue(Long doctorId, LocalDate tokenDate);

	boolean existsByDoctor_IdAndTokenDateAndPriorityTrueAndStatusIn(
			Long doctorId, LocalDate tokenDate, Collection<TokenStatus> statuses);

	@Query("SELECT MAX(token.tokenNumber) FROM Token token "
			+ "WHERE token.doctor.id = :doctorId AND token.tokenDate = :tokenDate")
	Optional<Integer> findHighestTokenNumberByDoctorAndTokenDate(
			@Param("doctorId") Long doctorId,
			@Param("tokenDate") LocalDate tokenDate);
}
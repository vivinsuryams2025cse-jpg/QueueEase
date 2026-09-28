package com.example.queuebase;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.queuebase.exception.InvalidTokenOperationException;
import com.example.queuebase.model.Doctor;
import com.example.queuebase.model.Patient;
import com.example.queuebase.model.Token;
import com.example.queuebase.model.TokenStatus;
import com.example.queuebase.repository.DoctorRepository;
import com.example.queuebase.repository.PatientRepository;
import com.example.queuebase.repository.TokenRepository;
import com.example.queuebase.service.PatientService;
import com.example.queuebase.service.TokenService;

import jakarta.validation.ConstraintViolationException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class QueueBaseApplicationTests {

	@Autowired
	private DoctorRepository doctorRepository;

	@Autowired
	private PatientRepository patientRepository;

	@Autowired
	private TokenRepository tokenRepository;

	@Autowired
	private PatientService patientService;

	@Autowired
	private TokenService tokenService;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	void clearDatabase() {
		tokenRepository.deleteAll();
		patientRepository.deleteAll();
		doctorRepository.deleteAll();
	}

	@Test
	void contextLoads() {
		assertThat(doctorRepository).isNotNull();
	}

	@Test
	void tokenNumbersAreSequentialAndSeparateForEachDoctor() {
		Doctor firstDoctor = createDoctor("Dr. One");
		Doctor secondDoctor = createDoctor("Dr. Two");
		Patient patient = createPatient("Asha");

		Token first = tokenService.generateToken(firstDoctor.getId(), patient.getId(), false);
		Token second = tokenService.generateToken(firstDoctor.getId(), patient.getId(), false);
		Token otherDoctor = tokenService.generateToken(secondDoctor.getId(), patient.getId(), false);

		assertThat(first.getTokenNumber()).isEqualTo(1);
		assertThat(second.getTokenNumber()).isEqualTo(2);
		assertThat(otherDoctor.getTokenNumber()).isEqualTo(1);
		assertThat(second.getEstimatedWaitMinutes()).isEqualTo(10);
	}

	@Test
	void firstTokenForDoctorOnNewDateStartsAtOne() {
		Doctor doctor = createDoctor("Dr. Daily");
		Patient patient = createPatient("Nila");
		Token yesterdayToken = new Token();
		yesterdayToken.setDoctor(doctor);
		yesterdayToken.setPatient(patient);
		yesterdayToken.setTokenNumber(9);
		yesterdayToken.setTokenDate(LocalDate.now().minusDays(1));
		yesterdayToken.setPriority(false);
		yesterdayToken.setStatus(TokenStatus.COMPLETED);
		yesterdayToken.setCreatedAt(LocalDateTime.now().minusDays(1));
		tokenRepository.save(yesterdayToken);

		Token todayToken = tokenService.generateToken(doctor.getId(), patient.getId(), false);

		assertThat(todayToken.getTokenNumber()).isEqualTo(1);
	}

	@Test
	void priorityTokenIsServedBeforeNormalWaitingTokens() {
		Doctor doctor = createDoctor("Dr. Queue");
		Patient patient = createPatient("Mina");
		Token normalToken = tokenService.generateToken(doctor.getId(), patient.getId(), false);
		Token priorityCandidate = tokenService.generateToken(doctor.getId(), patient.getId(), false);
		tokenService.markPriority(priorityCandidate.getId());

		assertThatThrownBy(() -> tokenService.markPriority(normalToken.getId()))
				.isInstanceOf(InvalidTokenOperationException.class);

		Token firstServed = tokenService.serveNextPatient(doctor.getId()).orElseThrow();
		assertThat(firstServed.getId()).isEqualTo(priorityCandidate.getId());
		assertThat(firstServed.getStatus()).isEqualTo(TokenStatus.SERVING);

		Token secondServed = tokenService.serveNextPatient(doctor.getId()).orElseThrow();
		assertThat(secondServed.getId()).isEqualTo(normalToken.getId());
		assertThat(tokenService.getTokenById(priorityCandidate.getId()).getStatus())
				.isEqualTo(TokenStatus.COMPLETED);
	}

	@Test
	void patientInputIsValidatedBeforeSave() {
		assertThatThrownBy(() -> patientService.registerPatient(new Patient()))
				.isInstanceOf(ConstraintViolationException.class);
	}

	@Test
	void cancelledTokensAreNotReturnedOrServedFromTheQueue() {
		Doctor doctor = createDoctor("Dr. Cancel");
		Patient patient = createPatient("Hari");
		Token token = tokenService.generateToken(doctor.getId(), patient.getId(), false);

		tokenService.cancelToken(token.getId());

		assertThat(tokenService.getWaitingTokensForDoctor(doctor.getId())).isEmpty();
		assertThat(tokenService.serveNextPatient(doctor.getId())).isEmpty();
		assertThat(tokenService.getTokenById(token.getId()).getStatus()).isEqualTo(TokenStatus.CANCELLED);
	}

	@Test
	void allWebsitePagesRender() throws Exception {
		for (String path : List.of("/", "/dashboard", "/ui/doctors", "/ui/patients", "/ui/token", "/ui/queue", "/ui/history")) {
			mockMvc.perform(get(path)).andExpect(status().isOk());
		}
	}

	@Test
	void restApiCreatesAndServesAToken() throws Exception {
		String doctorBody = mockMvc.perform(post("/doctors")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\":\"Dr. API\",\"specialization\":\"Cardiology\",\"averageConsultationMinutes\":10,\"active\":true}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		JsonNode doctor = objectMapper.readTree(doctorBody);

		String patientBody = mockMvc.perform(post("/patients")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\":\"Ravi API\",\"age\":26,\"phone\":\"5551234567\",\"gender\":\"Male\"}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		JsonNode patient = objectMapper.readTree(patientBody);

		mockMvc.perform(post("/tokens")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"doctorId\":" + doctor.get("id").asLong()
							+ ",\"patientId\":" + patient.get("id").asLong() + "}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.tokenNumber").value(1))
				.andExpect(jsonPath("$.status").value("WAITING"));
		mockMvc.perform(get("/doctors/" + doctor.get("id").asLong() + "/history"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1));

		mockMvc.perform(post("/doctors/" + doctor.get("id").asLong() + "/next"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SERVING"));

		mockMvc.perform(get("/doctors/" + doctor.get("id").asLong() + "/current-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.patient.name").value("Ravi API"));
	}

	@Test
	void invalidPatientRequestReturnsValidationDetails() throws Exception {
		mockMvc.perform(post("/patients").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.details.name").exists());
	}

	private Doctor createDoctor(String name) {
		Doctor doctor = new Doctor();
		doctor.setName(name);
		doctor.setSpecialization("General Medicine");
		doctor.setAverageConsultationMinutes(10);
		doctor.setActive(true);
		return doctorRepository.save(doctor);
	}

	private Patient createPatient(String name) {
		Patient patient = new Patient();
		patient.setName(name);
		patient.setAge(30);
		patient.setPhone("5551234567");
		patient.setGender("Female");
		return patientRepository.save(patient);
	}
}
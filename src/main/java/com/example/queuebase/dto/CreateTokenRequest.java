package com.example.queuebase.dto;

import jakarta.validation.constraints.NotNull;

public class CreateTokenRequest {

	@NotNull
	private Long doctorId;

	@NotNull
	private Long patientId;

	private Boolean priority = false;

	public Long getDoctorId() {
		return doctorId;
	}

	public void setDoctorId(Long doctorId) {
		this.doctorId = doctorId;
	}

	public Long getPatientId() {
		return patientId;
	}

	public void setPatientId(Long patientId) {
		this.patientId = patientId;
	}

	public Boolean getPriority() {
		return priority;
	}

	public void setPriority(Boolean priority) {
		this.priority = priority;
	}
}
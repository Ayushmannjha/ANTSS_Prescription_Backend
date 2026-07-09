package com.antss_prescription.websocket;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.antss_prescription.entity.prescription.PatientRegistration;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PatientRegistrationWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    public void publishRegistrationCreated(PatientRegistration registration) {
        Long hospitalId = registration.getHospital() != null ? registration.getHospital().getId() : null;
        Long clinicId = registration.getClinic() != null ? registration.getClinic().getId() : null;

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "PATIENT_REGISTRATION_CREATED");
            payload.put("registrationId", registration.getRegistrationId());
            payload.put("registrationNumber", registration.getRegistrationNumber());
            payload.put("hospitalId", hospitalId);
            payload.put("clinicId", clinicId);
            payload.put("createdAt", registration.getCreatedAt());
            payload.put("updatedAt", registration.getUpdatedAt());

            if (registration.getPatient() != null) {
                Map<String, Object> patient = new LinkedHashMap<>();
                patient.put("patientId", registration.getPatient().getPatientId());
                patient.put("patientName", registration.getPatient().getPatientName());
                patient.put("mobileNumber", registration.getPatient().getMobileNumber());
                patient.put("gender", registration.getPatient().getGender());
                patient.put("age", registration.getPatient().getAge());
                patient.put("address", registration.getPatient().getAddress());
                patient.put("state", registration.getPatient().getState());
                patient.put("city", registration.getPatient().getCity());
                patient.put("pincode", registration.getPatient().getPincode());
                patient.put("dateOfBirth", registration.getPatient().getDateOfBirth());
                payload.put("patient", patient);
            }

            TextMessage message = new TextMessage(objectMapper.writeValueAsString(payload));
            sessions.removeIf(session -> !session.isOpen());
            for (WebSocketSession session : sessions) {
                try {
                    session.sendMessage(message);
                } catch (IOException ex) {
                    log.warn("Failed to publish patient registration websocket event", ex);
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to build patient registration websocket event", ex);
        }
    }
}

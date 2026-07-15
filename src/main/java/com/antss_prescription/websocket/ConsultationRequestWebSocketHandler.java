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

import com.antss_prescription.entity.prescription.Consultation;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConsultationRequestWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    public void publishConsultationRequestCreated(Consultation consultation) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "CONSULTATION_REQUEST_CREATED");
            payload.put("consultationId", consultation.getConsultationId());
            payload.put("consultationNumber", consultation.getConsultationNumber());
            payload.put("doctorId", consultation.getDoctor() != null ? consultation.getDoctor().getId() : null);
            payload.put("doctorName", consultation.getDoctor() != null ? consultation.getDoctor().getDoctorName() : null);
            payload.put("registrationId", consultation.getPatientRegistration() != null
                    ? consultation.getPatientRegistration().getRegistrationId() : null);
            payload.put("registrationNumber", consultation.getPatientRegistration() != null
                    ? consultation.getPatientRegistration().getRegistrationNumber() : null);
            payload.put("patientName", consultation.getPatientRegistration() != null
                    ? consultation.getPatientRegistration().getPatientName() : null);
            payload.put("mobileNumber", consultation.getPatientRegistration() != null
                    ? consultation.getPatientRegistration().getMobileNumber() : null);
            payload.put("gender", consultation.getPatientRegistration() != null
                    ? consultation.getPatientRegistration().getGender() : null);
            payload.put("age", consultation.getPatientRegistration() != null
                    ? consultation.getPatientRegistration().getAge() : null);
            payload.put("priority", consultation.getPriority());
            payload.put("status", consultation.getStatus());
            payload.put("consultReason", consultation.getConsultReason());
            payload.put("requestedAt", consultation.getRequestedAt());

            TextMessage message = new TextMessage(objectMapper.writeValueAsString(payload));
            sessions.removeIf(session -> !session.isOpen());
            for (WebSocketSession session : sessions) {
                try {
                    session.sendMessage(message);
                } catch (IOException ex) {
                    log.warn("Failed to publish consultation request websocket event", ex);
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to build consultation request websocket event", ex);
        }
    }
}

package com.antss_prescription.configuration;

import com.antss_prescription.websocket.ConsultationRequestWebSocketHandler;
import com.antss_prescription.websocket.PatientRegistrationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final PatientRegistrationWebSocketHandler patientRegistrationWebSocketHandler;
    private final ConsultationRequestWebSocketHandler consultationRequestWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(patientRegistrationWebSocketHandler, "/ws/patient-registrations")
                .setAllowedOriginPatterns("*");
        registry.addHandler(consultationRequestWebSocketHandler, "/ws/consultation-requests")
                .setAllowedOriginPatterns("*");
    }
}

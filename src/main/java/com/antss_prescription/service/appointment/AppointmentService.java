package com.antss_prescription.service.appointment;

import java.util.List;

import org.springframework.stereotype.Service;

import com.antss_prescription.dto.request.AppointmentRequestDto;
import com.antss_prescription.entity.appointment.Appointment;
import com.antss_prescription.repository.appointment.AppointmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;

    public Appointment requestAppointment(AppointmentRequestDto dto) {

        Appointment appointment = new Appointment();

        appointment.setHospitalId(dto.getHospitalId());
        appointment.setHospitalName(dto.getHospitalName());

        appointment.setDoctorId(dto.getDoctorId());
        appointment.setDoctorName(dto.getDoctorName());

        appointment.setPatientName(dto.getPatientName());
        appointment.setMobileNumber(dto.getMobileNumber());
        appointment.setAge(dto.getAge());
        appointment.setGender(dto.getGender());

        appointment.setAppointmentDate(dto.getAppointmentDate());
        appointment.setAppointmentTime(dto.getAppointmentTime());
        appointment.setReason(dto.getReason());

        appointment.setStatus("REQUESTED");

        return appointmentRepository.save(appointment);
    }

    public List<Appointment> getAppointmentsByHospital(Long hospitalId) {
        return appointmentRepository.findByHospitalIdOrderByCreatedAtDesc(hospitalId);
    }

    public List<Appointment> getRequestedAppointments(Long hospitalId) {
        return appointmentRepository.findByHospitalIdAndStatusOrderByCreatedAtDesc(
                hospitalId,
                "REQUESTED"
        );
    }

    public Appointment updateStatus(Long appointmentId, String status) {

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        appointment.setStatus(status);

        return appointmentRepository.save(appointment);
    }
}
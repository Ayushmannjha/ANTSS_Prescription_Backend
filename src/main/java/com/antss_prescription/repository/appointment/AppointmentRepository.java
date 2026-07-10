package com.antss_prescription.repository.appointment;

import com.antss_prescription.entity.appointment.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByHospitalIdOrderByCreatedAtDesc(Long hospitalId);

    List<Appointment> findByDoctorIdOrderByCreatedAtDesc(Long doctorId);

    List<Appointment> findByHospitalIdAndStatusOrderByCreatedAtDesc(Long hospitalId, String status);

    List<Appointment> findByHospitalIdAndAppointmentDateOrderByAppointmentTimeAsc(
            Long hospitalId,
            LocalDate appointmentDate
    );
}
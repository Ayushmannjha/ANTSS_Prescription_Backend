package com.antss_prescription.service;

import java.util.List;

import com.antss_prescription.dto.response.PatientRegistrationResponse;
import com.antss_prescription.entity.Clinic;
import com.antss_prescription.entity.Hospital;
import com.antss_prescription.entity.prescription.PatientRegistration;

public interface PatientRegistrationService {

    PatientRegistration saveRegistration(PatientRegistration registration);

    PatientRegistrationResponse getRegistrationById(Integer registrationId);

    List<PatientRegistrationResponse> getAllRegistrations();
    List<PatientRegistrationResponse> getAllRegistrationsByClinic(Clinic clinic);
    List<PatientRegistrationResponse> getAllRegistrationsByHospital(Hospital hospital);
    PatientRegistrationResponse updateRegistration(Integer registrationId,
                                           PatientRegistration registration);

    void deleteRegistration(Integer registrationId);
}
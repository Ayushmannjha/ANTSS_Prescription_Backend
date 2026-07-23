package com.antss_prescription.service.impl;

import com.antss_prescription.dto.request.ConsultationBillDiscountRequest;
import com.antss_prescription.dto.response.ConsultationBillResponse;
import com.antss_prescription.entity.Clinic;
import com.antss_prescription.entity.Doctor;
import com.antss_prescription.entity.Hospital;
import com.antss_prescription.entity.prescription.Consultation;
import com.antss_prescription.entity.prescription.ConsultationBill;
import com.antss_prescription.entity.prescription.PatientRegistration;
import com.antss_prescription.enums.DiscountPolicy;
import com.antss_prescription.exception.BusinessException;
import com.antss_prescription.exception.ResourceNotFoundException;
import com.antss_prescription.repository.prescription.ConsultationBillRepository;
import com.antss_prescription.security.AccessControlService;
import com.antss_prescription.service.ConsultationBillService;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ConsultationBillServiceImpl implements ConsultationBillService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final DateTimeFormatter BILL_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter PRINT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm", Locale.ENGLISH);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Color TEXT = new Color(17, 24, 39);
    private static final Color MUTED = new Color(83, 101, 127);
    private static final Color LINE = new Color(203, 213, 225);
    private static final Color ACCENT = new Color(20, 184, 166);

    private final ConsultationBillRepository billRepository;
    private final AccessControlService accessControl;

    @Override
    @Transactional
    public ConsultationBillResponse generateBill(
            Consultation consultation,
            DiscountPolicy discountPolicy,
            BigDecimal discountValue) {

        if (consultation == null || consultation.getConsultationId() == 0) {
            throw new BusinessException("Consultation must be saved before generating a bill");
        }
        return billRepository.findByConsultation(consultation)
                .map(this::mapToResponse)
                .orElseGet(() -> mapToResponse(createBill(consultation, discountPolicy, discountValue)));
    }

    @Override
    @Transactional
    public ConsultationBillResponse getBillById(Long billId) {
        ConsultationBill bill = findBill(billId);
        accessControl.requireConsultationAccess(bill.getConsultation());
        return mapToResponse(bill);
    }

    @Override
    @Transactional
    public ConsultationBillResponse getBillByConsultationId(Integer consultationId) {
        ConsultationBill bill = billRepository.findByConsultationConsultationId(consultationId)
                .orElseThrow(() -> new ResourceNotFoundException("ConsultationBill", consultationId));
        accessControl.requireConsultationAccess(bill.getConsultation());
        return mapToResponse(bill);
    }

    @Override
    @Transactional
    public ConsultationBillResponse updateDiscount(Long billId, ConsultationBillDiscountRequest request) {
        ConsultationBill bill = findBill(billId);
        accessControl.requireConsultationAccess(bill.getConsultation());
        applyDiscount(
                bill,
                request == null ? DiscountPolicy.NONE : request.getDiscountPolicy(),
                request == null ? BigDecimal.ZERO : request.getDiscountValue());
        return mapToResponse(billRepository.save(bill));
    }

    @Override
    @Transactional
    public void writeBillPdf(OutputStream outputStream, Long billId) throws IOException {
        if (outputStream == null) {
            throw new IllegalArgumentException("OutputStream is required");
        }
        ConsultationBill bill = findBill(billId);
        accessControl.requireConsultationAccess(bill.getConsultation());

        Document document = new Document(PageSize.A4, 42f, 42f, 42f, 42f);
        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();
            addHeader(document, bill);
            addPatientAndDoctor(document, bill);
            addBillTable(document, bill);
            addFooter(document);
        } catch (DocumentException e) {
            throw new IOException("Unable to generate consultation bill PDF", e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }

    private ConsultationBill createBill(
            Consultation consultation,
            DiscountPolicy discountPolicy,
            BigDecimal discountValue) {

        Doctor doctor = consultation.getDoctor();
        PatientRegistration registration = consultation.getPatientRegistration();
        if (doctor == null || registration == null) {
            throw new BusinessException("Consultation must have doctor and patient registration for billing");
        }

        ConsultationBill bill = new ConsultationBill();
        bill.setBillNumber(generateBillNumber());
        bill.setConsultation(consultation);
        bill.setDoctor(doctor);
        bill.setPatientRegistration(registration);
        bill.setConsultationFee(scale(moneyOrZero(doctor.getConsultationFee())));
        applyDiscount(bill, discountPolicy, discountValue);
        return billRepository.save(bill);
    }

    private void applyDiscount(
            ConsultationBill bill,
            DiscountPolicy discountPolicy,
            BigDecimal discountValue) {

        DiscountPolicy policy = discountPolicy == null ? DiscountPolicy.NONE : discountPolicy;
        BigDecimal fee = scale(moneyOrZero(bill.getConsultationFee()));
        BigDecimal value = scale(moneyOrZero(discountValue));

        if (policy == DiscountPolicy.NONE) {
            value = BigDecimal.ZERO;
        }
        if (policy == DiscountPolicy.PERCENTAGE && value.compareTo(ONE_HUNDRED) > 0) {
            throw new BusinessException("Percentage discount cannot be greater than 100");
        }

        BigDecimal discountAmount = switch (policy) {
            case FLAT -> value;
            case PERCENTAGE -> fee.multiply(value).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
            case NONE -> BigDecimal.ZERO;
        };

        if (discountAmount.compareTo(fee) > 0) {
            throw new BusinessException("Discount cannot be greater than consultation fee");
        }

        bill.setDiscountPolicy(policy);
        bill.setDiscountValue(value);
        bill.setDiscountAmount(scale(discountAmount));
        bill.setPayableAmount(scale(fee.subtract(discountAmount)));
    }

    private ConsultationBill findBill(Long billId) {
        return billRepository.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException("ConsultationBill", billId));
    }

    private String generateBillNumber() {
        String prefix = "CB-" + LocalDate.now().format(BILL_DATE_FORMAT) + "-";
        String billNumber;
        do {
            billNumber = prefix + String.format("%06d", RANDOM.nextInt(1_000_000));
        } while (billRepository.existsByBillNumber(billNumber));
        return billNumber;
    }

    private ConsultationBillResponse mapToResponse(ConsultationBill bill) {
        Consultation consultation = bill.getConsultation();
        Doctor doctor = bill.getDoctor();
        PatientRegistration registration = bill.getPatientRegistration();

        ConsultationBillResponse response = new ConsultationBillResponse();
        response.setBillId(bill.getBillId());
        response.setBillNumber(bill.getBillNumber());
        response.setConsultationId(consultation == null ? null : consultation.getConsultationId());
        response.setConsultationNumber(consultation == null ? null : consultation.getConsultationNumber());
        response.setDoctorId(doctor == null ? null : doctor.getId());
        response.setDoctorName(doctor == null ? null : doctor.getDoctorName());
        response.setRegistrationId(registration == null ? null : registration.getRegistrationId());
        response.setRegistrationNumber(registration == null ? null : registration.getRegistrationNumber());
        response.setPatientName(registration == null ? null : registration.getPatientName());
        response.setPatientMobileNumber(registration == null ? null : registration.getMobileNumber());
        response.setConsultationFee(bill.getConsultationFee());
        response.setDiscountPolicy(bill.getDiscountPolicy());
        response.setDiscountValue(bill.getDiscountValue());
        response.setDiscountAmount(bill.getDiscountAmount());
        response.setPayableAmount(bill.getPayableAmount());
        response.setPaymentStatus(bill.getPaymentStatus());
        response.setCreatedAt(bill.getCreatedAt());
        response.setUpdatedAt(bill.getUpdatedAt());
        return response;
    }

    private void addHeader(Document document, ConsultationBill bill) throws DocumentException {
        Doctor doctor = bill.getDoctor();
        Hospital hospital = doctor == null ? null : doctor.getHospital();
        Clinic clinic = doctor == null ? null : doctor.getClinic();

        Paragraph title = new Paragraph("Consultation Bill", text(20, Font.BOLD, ACCENT));
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(12f);
        document.add(title);

        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{1.2f, 1f});
        header.addCell(noBorder("Dr. " + value(doctor == null ? null : doctor.getDoctorName(), "-")
                + "\n" + value(doctor == null ? null : doctor.getQualification(), "")
                + "\n" + value(doctor == null ? null : doctor.getSpecialization(), ""), Element.ALIGN_LEFT));
        header.addCell(noBorder(value(facilityName(hospital, clinic), "-")
                + "\n" + value(facilityAddress(hospital, clinic), "")
                + "\nPhone: " + value(facilityPhone(hospital, clinic, doctor), "-"), Element.ALIGN_RIGHT));
        document.add(header);
        addSeparator(document);
    }

    private void addPatientAndDoctor(Document document, ConsultationBill bill) throws DocumentException {
        PatientRegistration registration = bill.getPatientRegistration();
        Consultation consultation = bill.getConsultation();

        PdfPTable details = new PdfPTable(2);
        details.setWidthPercentage(100);
        details.setWidths(new float[]{1f, 1f});
        details.addCell(labelValue("Bill No.", bill.getBillNumber()));
        details.addCell(labelValue("Bill Date", bill.getCreatedAt() == null ? "-" : bill.getCreatedAt().format(PRINT_DATE_FORMAT)));
        details.addCell(labelValue("Patient", value(registration == null ? null : registration.getPatientName(), "-")));
        details.addCell(labelValue("Mobile", value(registration == null ? null : registration.getMobileNumber(), "-")));
        details.addCell(labelValue("UHID", value(registration == null ? null : registration.getRegistrationNumber(), "-")));
        details.addCell(labelValue("Consultation ID", consultation == null ? "-" : String.valueOf(consultation.getConsultationId())));
        document.add(details);
        addSeparator(document);
    }

    private void addBillTable(Document document, ConsultationBill bill) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.3f, .7f});
        table.addCell(headerCell("Particulars"));
        table.addCell(headerCell("Amount"));
        table.addCell(bodyCell("Consultation Fee", Element.ALIGN_LEFT));
        table.addCell(bodyCell(formatMoney(bill.getConsultationFee()), Element.ALIGN_RIGHT));
        table.addCell(bodyCell("Discount (" + bill.getDiscountPolicy() + ")", Element.ALIGN_LEFT));
        table.addCell(bodyCell("- " + formatMoney(bill.getDiscountAmount()), Element.ALIGN_RIGHT));
        table.addCell(totalCell("Payable Amount", Element.ALIGN_LEFT));
        table.addCell(totalCell(formatMoney(bill.getPayableAmount()), Element.ALIGN_RIGHT));
        document.add(table);
    }

    private void addFooter(Document document) throws DocumentException {
        Paragraph note = new Paragraph("This is a computer generated consultation bill.", text(9, Font.NORMAL, MUTED));
        note.setAlignment(Element.ALIGN_CENTER);
        note.setSpacingBefore(24f);
        document.add(note);
    }

    private PdfPCell labelValue(String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(5f);
        cell.addElement(new Paragraph(label, text(8, Font.BOLD, MUTED)));
        cell.addElement(new Paragraph(value(value, "-"), text(10, Font.NORMAL, TEXT)));
        return cell;
    }

    private PdfPCell headerCell(String value) {
        PdfPCell cell = new PdfPCell(new Phrase(value, text(9, Font.BOLD, Color.WHITE)));
        cell.setBackgroundColor(ACCENT);
        cell.setPadding(8f);
        cell.setBorderColor(ACCENT);
        return cell;
    }

    private PdfPCell bodyCell(String value, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(value(value, "-"), text(9, Font.NORMAL, TEXT)));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(8f);
        cell.setBorderColor(LINE);
        return cell;
    }

    private PdfPCell totalCell(String value, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(value(value, "-"), text(10, Font.BOLD, TEXT)));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(9f);
        cell.setBorderColor(LINE);
        cell.setBackgroundColor(new Color(240, 253, 250));
        return cell;
    }

    private PdfPCell noBorder(String value, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(value(value, "-"), text(9, Font.NORMAL, TEXT)));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(4f);
        return cell;
    }

    private void addSeparator(Document document) throws DocumentException {
        PdfPTable separator = new PdfPTable(1);
        separator.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell(new Phrase(""));
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(LINE);
        cell.setPaddingTop(8f);
        cell.setPaddingBottom(8f);
        separator.addCell(cell);
        document.add(separator);
    }

    private Font text(int size, int style, Color color) {
        Font font = new Font(Font.HELVETICA, size, style, color);
        return font;
    }

    private BigDecimal moneyOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal scale(BigDecimal value) {
        return moneyOrZero(value).setScale(2, RoundingMode.HALF_UP);
    }

    private String formatMoney(BigDecimal value) {
        return "Rs. " + scale(value);
    }

    private String facilityName(Hospital hospital, Clinic clinic) {
        if (hospital != null) return hospital.getHospitalName();
        return clinic == null ? null : clinic.getClinicName();
    }

    private String facilityAddress(Hospital hospital, Clinic clinic) {
        if (hospital != null) {
            return joinParts(hospital.getAddressLine1(), hospital.getCity(), hospital.getState(), hospital.getPincode());
        }
        if (clinic != null) {
            return joinParts(clinic.getAddressLine1(), clinic.getCity(), clinic.getState(), clinic.getPincode());
        }
        return null;
    }

    private String facilityPhone(Hospital hospital, Clinic clinic, Doctor doctor) {
        if (hospital != null) return hospital.getMobileNumber();
        if (clinic != null) return clinic.getMobileNumber();
        return doctor == null ? null : doctor.getMobileNumber();
    }

    private String joinParts(String... parts) {
        return java.util.stream.Stream.of(parts)
                .filter(part -> part != null && !part.isBlank())
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private String value(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}

package com.antss_prescription.service;

import com.antss_prescription.dto.response.ConsultationResponse;
import com.antss_prescription.dto.response.DetailedPrescriptionResponse;
import com.antss_prescription.entity.Clinic;
import com.antss_prescription.entity.Doctor;
import com.antss_prescription.entity.Hospital;
import com.antss_prescription.entity.prescription.PrintHeaders;
import com.antss_prescription.exception.ResourceNotFoundException;
import com.antss_prescription.repository.DoctorRepository;
import com.antss_prescription.repository.prescription.PrintHeadersRepo;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Chunk;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrescriptionPrintService {

    private final PrintHeadersRepo headerRepo;
    private final DoctorRepository doctorRepository;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
    private static final float PDF_SIDE_MARGIN = 42f;
    private static final float PDF_HEADER_HEIGHT = 112f;
    private static final float PDF_TOP_MARGIN = PDF_HEADER_HEIGHT + 24f;
    private static final float PDF_BOTTOM_MARGIN = 128f;
    private static final Color PDF_TEXT = new Color(17, 24, 39);
    private static final Color PDF_MUTED = new Color(83, 101, 127);
    private static final Color PDF_LINE = new Color(203, 213, 225);
    private static final Color PDF_LIGHT_LINE = new Color(226, 232, 240);

    @Value("${app.pdf.unicode-font-path:}")
    private String configuredUnicodeFontPath;

    private BaseFont unicodeBaseFont;

    @PostConstruct
    void initializePdfFont() {
        String fontPath = resolveUnicodeFontPath();
        if (fontPath == null) {
            return;
        }
        try {
            unicodeBaseFont = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        } catch (Exception ignored) {
            unicodeBaseFont = null;
        }
    }

    public void writePrescriptionPrint(
            PrintWriter writer,
            long headersId,
            DetailedPrescriptionResponse prescription) {

        if (writer == null) {
            throw new IllegalArgumentException("PrintWriter is required");
        }
        if (prescription == null) {
            throw new IllegalArgumentException("Prescription details are required");
        }
        PrintHeaders headers = headerRepo.findById(headersId)
                .orElseThrow(() -> new ResourceNotFoundException("PrintHeaders", headersId));
        ConsultationResponse consultation = prescription.getConsultation();
        String headerUrl = headers == null ? null : headers.getHeaderUrl();

        writer.println("<!doctype html>");
        writer.println("<html lang=\"en\">");
        writer.println("<head>");
        writer.println("<meta charset=\"UTF-8\">");
        writer.println("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        writer.println("<title>Prescription Print</title>");
        writeStyles(writer);
        writer.println("</head>");
        writer.println("<body>");
        writer.println("<div class=\"print-toolbar\">");
        writer.println("<button class=\"print-button\" onclick=\"window.print()\">Print Prescription</button>");
        writer.println("</div>");
        writer.println("<main class=\"page\">");

        writeHeader(writer, headerUrl, consultation);
        writer.println("<div class=\"watermark\">Rx</div>");
        writer.println("<section class=\"content\">");
        writePatientRow(writer, consultation, prescription);
        writeClinicalSummary(writer, consultation);
        writePastHistory(writer, consultation);
        writeDiagnosis(writer, consultation);
        writeInvestigations(writer, prescription);
        writeMedicines(writer, prescription.getMedicines());
        writeFollowUp(writer, consultation);
        writer.println("</section>");

        writer.println("<footer class=\"footer\">");
        writer.println("<div>SAVE<br>PRESCRIPTION TO<br>GENERATE QR</div>");
        writer.println("<div class=\"signature\">DOCTOR SIGNATURE / STAMP</div>");
        writer.println("<div class=\"note\">Substitute with equivalent generics as required. This is a digitally signed prescription.</div>");
        writer.println("</footer>");
        writer.println("</main>");
        writer.println("</body>");
        writer.println("</html>");
        writer.flush();
    }

    public String buildPrescriptionPrint(long headersId, DetailedPrescriptionResponse prescription) {
        StringWriter stringWriter = new StringWriter();
        writePrescriptionPrint(new PrintWriter(stringWriter), headersId, prescription);
        return stringWriter.toString();
    }

    public void writePrescriptionPdf(
            OutputStream outputStream,
            Long headersId,
            DetailedPrescriptionResponse prescription,
            String downloadUrl) throws IOException {

        if (outputStream == null) {
            throw new IllegalArgumentException("OutputStream is required");
        }
        if (prescription == null) {
            throw new IllegalArgumentException("Prescription details are required");
        }

        PrintHeaders headers = headersId == null
                ? null
                : headerRepo.findById(headersId).orElse(null);
        DefaultHeaderDetails defaultHeaderDetails = resolveDefaultHeaderDetails(prescription.getConsultation());

        Document document = new Document(
                PageSize.A4,
                PDF_SIDE_MARGIN,
                PDF_SIDE_MARGIN,
                PDF_TOP_MARGIN,
                PDF_BOTTOM_MARGIN);
        try {
            PdfWriter pdfWriter = PdfWriter.getInstance(document, outputStream);
            pdfWriter.setPageEvent(new PrescriptionPdfPageEvent(headers, defaultHeaderDetails, downloadUrl));
            document.open();

            addPatientPdfRow(document, prescription.getConsultation(), prescription);
            addVitalsPdf(document, prescription.getConsultation());
            addClinicalPdfSections(document, prescription.getConsultation());
            addPastHistoryAndDiagnosisPdf(document, prescription.getConsultation());
            addInvestigationsAndTestsPdf(document, prescription);
            addMedicinesPdf(document, prescription.getMedicines());
            addAdvicePdf(document, prescription);
            addFollowUpPdf(document, prescription.getConsultation());
            addLastPageClosingBlock(document, downloadUrl);
        } catch (DocumentException e) {
            throw new IOException("Unable to generate prescription PDF", e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }

    private boolean drawPdfHeader(
            PdfWriter pdfWriter,
            PrintHeaders headers,
            DefaultHeaderDetails defaultHeaderDetails) {
        String headerUrl = headers == null ? null : headers.getHeaderUrl();
        if (hasText(headerUrl)) {
            try {
                Image headerImage = Image.getInstance(URI.create(headerUrl).toURL());
                headerImage.scaleAbsolute(PageSize.A4.getWidth(), PDF_HEADER_HEIGHT);
                headerImage.setAbsolutePosition(0, PageSize.A4.getHeight() - PDF_HEADER_HEIGHT);
                pdfWriter.getDirectContent().addImage(headerImage);
                return true;
            } catch (Exception ignored) {
                // If the external header image cannot be read, fall back to text header.
            }
        }
        return false;
    }

    private void drawFallbackPdfHeader(PdfWriter pdfWriter, DefaultHeaderDetails details) throws DocumentException {
        PdfContentByte canvas = pdfWriter.getDirectContent();
        float pageWidth = PageSize.A4.getWidth();
        float top = PageSize.A4.getHeight();
        float headerBottom = top - PDF_HEADER_HEIGHT;

        canvas.saveState();
        canvas.setColorFill(new Color(238, 252, 250));
        canvas.rectangle(0, headerBottom, pageWidth, PDF_HEADER_HEIGHT);
        canvas.fill();

        canvas.setColorFill(new Color(20, 184, 166));
        canvas.rectangle(0, headerBottom, pageWidth * 0.63f, PDF_HEADER_HEIGHT);
        canvas.fill();

        canvas.setColorFill(new Color(178, 245, 234));
        canvas.rectangle(pageWidth * 0.58f, headerBottom, 34, PDF_HEADER_HEIGHT);
        canvas.fill();
        canvas.setColorFill(new Color(153, 226, 219));
        canvas.rectangle(pageWidth * 0.64f, headerBottom, 24, PDF_HEADER_HEIGHT);
        canvas.fill();

        float logoCenterX = pageWidth / 2f;
        float logoCenterY = headerBottom + 58;
        canvas.setColorFill(new Color(255, 255, 255));
        canvas.circle(logoCenterX, logoCenterY, 32);
        canvas.fill();
        canvas.restoreState();

        drawHeaderLogo(canvas, logoCenterX - 24, logoCenterY - 24, 48, 48);

        com.lowagie.text.pdf.ColumnText.showTextAligned(
                canvas,
                Element.ALIGN_LEFT,
                pdfPhrase("Dr. " + value(details.doctorName(), "Doctor Name"), 22, Font.BOLD, Color.WHITE),
                PDF_SIDE_MARGIN,
                headerBottom + 70,
                0);
        com.lowagie.text.pdf.ColumnText.showTextAligned(
                canvas,
                Element.ALIGN_LEFT,
                pdfPhrase(value(details.qualification(), "QUALIFICATION"), 12, Font.BOLD, Color.WHITE),
                PDF_SIDE_MARGIN,
                headerBottom + 48,
                0);
        com.lowagie.text.pdf.ColumnText.showTextAligned(
                canvas,
                Element.ALIGN_LEFT,
                pdfPhrase(joinParts(details.specialization(), "Reg. No: " + value(details.registrationNumber(), "-")),
                        8, Font.NORMAL, Color.WHITE),
                PDF_SIDE_MARGIN,
                headerBottom + 30,
                0);

        String facilityName = value(details.facilityName(), "Clinic / Hospital");
        String address = value(details.facilityAddress(), "");
        String phone = value(details.phone(), "-");
        com.lowagie.text.pdf.ColumnText.showTextAligned(
                canvas,
                Element.ALIGN_RIGHT,
                pdfPhrase(facilityName, 14, Font.BOLD, new Color(15, 118, 110)),
                pageWidth - PDF_SIDE_MARGIN,
                headerBottom + 72,
                0);
        com.lowagie.text.pdf.ColumnText.showTextAligned(
                canvas,
                Element.ALIGN_RIGHT,
                pdfPhrase(address, 8, Font.NORMAL, PDF_MUTED),
                pageWidth - PDF_SIDE_MARGIN,
                headerBottom + 52,
                0);
        com.lowagie.text.pdf.ColumnText.showTextAligned(
                canvas,
                Element.ALIGN_RIGHT,
                pdfPhrase("Ph: " + phone, 8, Font.NORMAL, PDF_MUTED),
                pageWidth - PDF_SIDE_MARGIN,
                headerBottom + 36,
                0);
    }

    private void drawHeaderLogo(PdfContentByte canvas, float x, float y, float width, float height) {
        try {
            Image logo = Image.getInstance(
                    new ClassPathResource("watermarks/caduceus-watermark.png")
                            .getInputStream()
                            .readAllBytes());
            logo.scaleToFit(width, height);
            logo.setAbsolutePosition(
                    x + ((width - logo.getScaledWidth()) / 2),
                    y + ((height - logo.getScaledHeight()) / 2));
            canvas.addImage(logo);
        } catch (Exception ignored) {
            // Header logo is decorative.
        }
    }

    private DefaultHeaderDetails resolveDefaultHeaderDetails(ConsultationResponse consultation) {
        if (consultation != null && consultation.getDoctorId() != null) {
            return doctorRepository.findById(consultation.getDoctorId())
                    .map(this::toDefaultHeaderDetails)
                    .orElseGet(() -> toDefaultHeaderDetails(consultation));
        }
        return toDefaultHeaderDetails(consultation);
    }

    private DefaultHeaderDetails toDefaultHeaderDetails(Doctor doctor) {
        Hospital hospital = doctor.getHospital();
        Clinic clinic = doctor.getClinic();
        return new DefaultHeaderDetails(
                doctor.getDoctorName(),
                doctor.getQualification(),
                doctor.getSpecialization(),
                doctor.getRegistrationNumber(),
                hospital != null ? hospital.getHospitalName() : clinic == null ? null : clinic.getClinicName(),
                hospital != null ? formatAddress(hospital.getAddressLine1(), hospital.getCity(), hospital.getState(), hospital.getPincode())
                        : clinic == null ? null : formatAddress(clinic.getAddressLine1(), clinic.getCity(), clinic.getState(), clinic.getPincode()),
                hospital != null ? hospital.getMobileNumber() : clinic == null ? doctor.getMobileNumber() : clinic.getMobileNumber());
    }

    private DefaultHeaderDetails toDefaultHeaderDetails(ConsultationResponse consultation) {
        return new DefaultHeaderDetails(
                consultation == null ? null : consultation.getDoctorName(),
                consultation == null ? null : consultation.getQualification(),
                consultation == null ? null : consultation.getSpecialization(),
                consultation == null ? null : consultation.getDoctorRegistrationNo(),
                facilityName(consultation),
                facilityAddress(consultation),
                facilityPhone(consultation));
    }

    private String formatAddress(String line1, String city, String state, String pin) {
        return joinParts(line1, city, state, pin);
    }

    private void addPatientPdfRow(
            Document document,
            ConsultationResponse consultation,
            DetailedPrescriptionResponse prescription) throws DocumentException {

        PdfPTable patient = new PdfPTable(3);
        patient.setWidthPercentage(100);
        patient.setWidths(new float[]{1.55f, .8f, .82f});
        patient.addCell(noBorderCell(pdfText(
                "ID: " + value(consultation == null ? null : consultation.getRegistrationNumber(),
                        String.valueOf(prescription.getPrescriptionId()))
                        + " - " + value(consultation == null ? null : consultation.getPatientName(), "-")
                        + " (" + value(consultation == null ? null : consultation.getGender(), "-")
                        + " / " + (consultation == null ? "-" : consultation.getAge()) + " Y)",
                9,
                Font.BOLD), Element.ALIGN_LEFT));
        patient.addCell(noBorderCell(pdfText(
                "Mob. No.: " + value(consultation == null ? null : consultation.getMobileNumber(), "-"),
                9,
                Font.BOLD), Element.ALIGN_LEFT));
        patient.addCell(noBorderCell(pdfText(
                "Date: " + formatDateTime(prescription.getCreatedAt()),
                9,
                Font.BOLD), Element.ALIGN_LEFT));
        document.add(patient);
        addSeparator(document);
        addSeparator(document);
    }

    private void addClinicalPdfSections(Document document, ConsultationResponse consultation) throws DocumentException {
        List<String> complaints = getComplaints(consultation);
        List<String> findings = getFindings(consultation);
        addTwoColumnSections(document, "Chief Complaints", complaints, "Clinical Findings", findings, 10);
    }

    private void addVitalsPdf(Document document, ConsultationResponse consultation) throws DocumentException {
        if (consultation == null || consultation.getVitalId() <= 0) {
            return;
        }

        java.util.ArrayList<String> vitals = new java.util.ArrayList<>();
        if (consultation.getHeight() > 0) {
            vitals.add("Height: " + consultation.getHeight() + " cm");
        }
        if (consultation.getWeight() > 0) {
            vitals.add("Weight: " + trimNumber(consultation.getWeight()) + " kg");
        }
        if (consultation.getTemperature() > 0) {
            vitals.add("Temperature: " + trimNumber(consultation.getTemperature()));
        }
        if (consultation.getPulse() > 0) {
            vitals.add("Pulse: " + trimNumber(consultation.getPulse()));
        }
        if (consultation.getSpo2() > 0) {
            vitals.add("SpO2: " + trimNumber(consultation.getSpo2()) + "%");
        }
        if (hasText(consultation.getBp())) {
            vitals.add("BP: " + consultation.getBp());
        }
        if (consultation.getRespiratoryRate() > 0) {
            vitals.add("Resp. Rate: " + trimNumber(consultation.getRespiratoryRate()));
        }

        if (!vitals.isEmpty()) {
            addSection(document, "Vitals", List.of(String.join(" | ", vitals)));
        }
    }

    private void addPastHistoryAndDiagnosisPdf(Document document, ConsultationResponse consultation) throws DocumentException {
        addTwoColumnSections(
                document,
                "Past History",
                getPastHistories(consultation),
                "Diagnosis:",
                getDiagnoses(consultation),
                12);
    }

    private void addInvestigationsAndTestsPdf(Document document, DetailedPrescriptionResponse prescription) throws DocumentException {
        addTwoColumnSections(
                document,
                "Investigations / Results",
                getInvestigations(prescription),
                "Tests Requested",
                getTestsRequested(prescription),
                12);
    }

    private void addAdvicePdf(Document document, DetailedPrescriptionResponse prescription) throws DocumentException {
        ConsultationResponse consultation = prescription.getConsultation();
        String advice = consultation == null ? null : consultation.getAdvice();
        String notes = prescription.getNotes();
        String value = hasText(advice) ? advice : notes;
        if (!hasText(value)) {
            return;
        }
        addSection(document, "Advice / Instructions:", List.of(value));
    }

    private void addMedicinesPdf(
            Document document,
            List<DetailedPrescriptionResponse.MedicineDetailResponse> medicines) throws DocumentException {

        if (medicines == null || medicines.isEmpty()) {
            return;
        }

        Paragraph rx = pdfText("Rx", 19, Font.BOLDITALIC);
        rx.setSpacingBefore(14);
        document.add(rx);

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.75f, .85f, .85f});
        table.addCell(headerCell("Medicine Name"));
        table.addCell(headerCell("Dosage"));
        table.addCell(headerCell("Duration"));

        for (int i = 0; i < medicines.size(); i++) {
            DetailedPrescriptionResponse.MedicineDetailResponse item = medicines.get(i);
            table.addCell(medicineNameCell(i + 1, item));
            table.addCell(bodyCell(joinParts(item.getDosage(), item.getFrequency(), item.getInstruction())));
            table.addCell(bodyCell(joinParts(item.getDuration(), hasText(item.getQuantity()) ? "Qty: " + item.getQuantity() : null)));
        }
        document.add(table);
    }

    private void addFollowUpPdf(Document document, ConsultationResponse consultation) throws DocumentException {
        if (consultation == null || consultation.getFollowUpDate() == null) {
            return;
        }
        Paragraph followUp = pdfText("Follow Up: " + formatDate(consultation.getFollowUpDate()), 10, Font.BOLD);
        followUp.setSpacingBefore(20);
        document.add(followUp);
    }

    private void addLastPageClosingBlock(Document document, String downloadUrl) throws DocumentException {
        PdfPTable closing = new PdfPTable(2);
        closing.setWidthPercentage(100);
        closing.setWidths(new float[]{1, 1});
        closing.setSpacingBefore(48);
        closing.setKeepTogether(true);

        PdfPCell qrCell = new PdfPCell();
        qrCell.setBorder(Rectangle.NO_BORDER);
        qrCell.setPaddingTop(8);
        if (hasText(downloadUrl)) {
            try {
                Image qr = Image.getInstance(createQrPng(downloadUrl, 170));
                qr.scaleAbsolute(64, 64);
                qrCell.addElement(qr);
                Paragraph label = pdfText("SCAN FOR DIGITAL COPY", 7, Font.BOLD);
                label.setSpacingBefore(4);
                qrCell.addElement(label);
            } catch (Exception ignored) {
                // Closing block should still render if QR generation fails.
            }
        }
        closing.addCell(qrCell);

        PdfPCell signatureCell = new PdfPCell();
        signatureCell.setBorder(Rectangle.NO_BORDER);
        signatureCell.setPaddingTop(48);
        Paragraph line = pdfText("______________________________", 8, Font.NORMAL);
        line.setAlignment(Element.ALIGN_CENTER);
        signatureCell.addElement(line);
        Paragraph signature = pdfText("DOCTOR SIGNATURE / STAMP", 9, Font.BOLD);
        signature.setAlignment(Element.ALIGN_CENTER);
        signatureCell.addElement(signature);
        closing.addCell(signatureCell);

        document.add(closing);
        addClosingSeparator(document);

        Paragraph note = pdfText(
                "Substitute with equivalent Generics as required. This is a digitally signed prescription.",
                8,
                Font.ITALIC);
        note.setAlignment(Element.ALIGN_CENTER);
        note.setSpacingBefore(8);
        document.add(note);
    }

    private void addQrCode(PdfContentByte canvas, String downloadUrl) throws WriterException, IOException, DocumentException {
        if (!hasText(downloadUrl)) {
            return;
        }
        Image qr = Image.getInstance(createQrPng(downloadUrl, 170));
        qr.scaleAbsolute(64, 64);
        qr.setAbsolutePosition(PDF_SIDE_MARGIN + 12, 88);
        canvas.addImage(qr);

        Phrase label = pdfPhrase("SCAN FOR DIGITAL COPY", 7, Font.BOLD, PDF_MUTED);
        com.lowagie.text.pdf.ColumnText.showTextAligned(
                canvas,
                Element.ALIGN_LEFT,
                label,
                PDF_SIDE_MARGIN + 12,
                76,
                0);
    }

    private class PrescriptionPdfPageEvent extends PdfPageEventHelper {

        private final PrintHeaders headers;
        private final DefaultHeaderDetails defaultHeaderDetails;
        private final String downloadUrl;

        private PrescriptionPdfPageEvent(
                PrintHeaders headers,
                DefaultHeaderDetails defaultHeaderDetails,
                String downloadUrl) {
            this.headers = headers;
            this.defaultHeaderDetails = defaultHeaderDetails;
            this.downloadUrl = downloadUrl;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            try {
                if (!drawPdfHeader(writer, headers, defaultHeaderDetails)) {
                    drawFallbackPdfHeader(writer, defaultHeaderDetails);
                }
                drawPageNumber(writer);
                addWatermark(writer);
            } catch (Exception ignored) {
                // Page decoration should never break PDF generation.
            }
        }
    }

    private record DefaultHeaderDetails(
            String doctorName,
            String qualification,
            String specialization,
            String registrationNumber,
            String facilityName,
            String facilityAddress,
            String phone) {}

    private void drawPageNumber(PdfWriter writer) {
        Phrase pageNumber = pdfPhrase(
                "Page " + writer.getPageNumber(),
                7,
                Font.NORMAL,
                PDF_MUTED);
        com.lowagie.text.pdf.ColumnText.showTextAligned(
                writer.getDirectContent(),
                Element.ALIGN_RIGHT,
                pageNumber,
                PageSize.A4.getWidth() - PDF_SIDE_MARGIN,
                PageSize.A4.getHeight() - 18,
                0);
    }

    private void addWatermark(PdfWriter pdfWriter) {
        try {
            PdfContentByte canvas = pdfWriter.getDirectContentUnder();
            PdfGState state = new PdfGState();
            state.setFillOpacity(0.07f);

            Image watermark = Image.getInstance(
                    new ClassPathResource("watermarks/caduceus-watermark.png")
                            .getInputStream()
                            .readAllBytes());
            watermark.scaleToFit(340, 360);
            float x = (PageSize.A4.getWidth() - watermark.getScaledWidth()) / 2;
            float y = ((PageSize.A4.getHeight() - watermark.getScaledHeight()) / 2) - 35;
            watermark.setAbsolutePosition(x, y);

            canvas.saveState();
            canvas.setGState(state);
            canvas.addImage(watermark);
            canvas.restoreState();
        } catch (Exception ignored) {
            // Watermark is decorative; PDF generation should continue without it.
        }
    }

    private byte[] createQrPng(String value, int size) throws WriterException, IOException {
        BitMatrix matrix = new MultiFormatWriter().encode(value, BarcodeFormat.QR_CODE, size, size);
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                image.setRGB(x, y, matrix.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
            }
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        return bytes.toByteArray();
    }

    private void addSection(Document document, String title, List<String> items) throws DocumentException {
        List<String> filtered = filteredUnique(items);
        if (filtered.isEmpty()) {
            return;
        }
        Paragraph titleParagraph = pdfText(title.toUpperCase(), 10, Font.BOLD);
        titleParagraph.setSpacingBefore(9);
        titleParagraph.setSpacingAfter(3);
        document.add(titleParagraph);
        for (String item : filtered) {
            Paragraph line = pdfText("* " + item, 9, Font.NORMAL);
            line.setSpacingAfter(3);
            document.add(line);
        }
    }

    private void addTwoColumnSections(
            Document document,
            String leftTitle,
            List<String> leftItems,
            String rightTitle,
            List<String> rightItems,
            float spacingBefore) throws DocumentException {

        List<String> filteredLeft = filteredUnique(leftItems);
        List<String> filteredRight = filteredUnique(rightItems);
        if (filteredLeft.isEmpty() && filteredRight.isEmpty()) {
            return;
        }

        int columns = !filteredLeft.isEmpty() && !filteredRight.isEmpty() ? 2 : 1;
        PdfPTable table = new PdfPTable(columns);
        table.setWidthPercentage(100);
        table.setSpacingBefore(spacingBefore);
        if (columns == 2) {
            table.setWidths(new float[]{1, 1});
        }
        if (!filteredLeft.isEmpty()) {
            table.addCell(sectionCell(leftTitle, filteredLeft, false));
        }
        if (!filteredRight.isEmpty()) {
            table.addCell(sectionCell(rightTitle, filteredRight, columns == 2));
        }
        document.add(table);
    }

    private PdfPCell sectionCell(String title, List<String> items, boolean leftBorder) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(8);
        cell.setBorder(leftBorder ? Rectangle.BOTTOM | Rectangle.LEFT : Rectangle.BOTTOM);
        cell.setBorderColor(PDF_LINE);
        cell.addElement(pdfText(title.toUpperCase(), 10, Font.BOLD));
        items.stream()
                .filter(this::hasText)
                .forEach(item -> cell.addElement(pdfText("* " + item, 9, Font.NORMAL)));
        return cell;
    }

    private List<String> filteredUnique(List<String> items) {
        return items == null
                ? List.of()
                : new LinkedHashSet<>(items.stream().filter(this::hasText).toList()).stream().toList();
    }

    private PdfPCell headerCell(String value) {
        PdfPCell cell = new PdfPCell(pdfPhrase(value, 9, Font.BOLD, PDF_TEXT));
        cell.setPadding(6);
        cell.setBorder(Rectangle.TOP | Rectangle.BOTTOM);
        cell.setBorderColor(PDF_LINE);
        return cell;
    }

    private PdfPCell bodyCell(String value) {
        PdfPCell cell = new PdfPCell(pdfPhrase(value, 8, Font.NORMAL, PDF_TEXT));
        cell.setPadding(6);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(PDF_LIGHT_LINE);
        return cell;
    }

    private PdfPCell medicineNameCell(int serialNumber, DetailedPrescriptionResponse.MedicineDetailResponse item) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(6);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(PDF_LIGHT_LINE);

        Paragraph medicine = pdfText(serialNumber + ") " + joinParts(item.getMedicineName(), item.getStrength()), 8, Font.BOLD);
        medicine.setLeading(10);
        cell.addElement(medicine);

        return cell;
    }

    private PdfPCell noBorderCell(Paragraph paragraph, int alignment) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(4);
        cell.setHorizontalAlignment(alignment);
        paragraph.setAlignment(alignment);
        cell.addElement(paragraph);
        return cell;
    }

    private PdfPCell topBorderCell(Paragraph paragraph, int alignment) {
        PdfPCell cell = noBorderCell(paragraph, alignment);
        cell.setBorder(Rectangle.TOP);
        cell.setBorderColor(PDF_LINE);
        return cell;
    }

    private Paragraph pdfText(String value, int size, int style) {
        return new Paragraph(pdfPhrase(value, size, style, PDF_TEXT));
    }

    private Font pdfFont(int size, int style, Color color) {
        return FontFactory.getFont(FontFactory.HELVETICA, size, style, color);
    }

    private Phrase pdfPhrase(String value, int size, int style, Color color) {
        String text = value == null ? "" : value;
        Phrase phrase = new Phrase();
        StringBuilder chunk = new StringBuilder();
        Boolean unicodeChunk = null;

        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            boolean unicodeChar = requiresUnicodeFont(codePoint);
            if (unicodeChunk != null && unicodeChunk != unicodeChar) {
                phrase.add(new Chunk(chunk.toString(), fontForChunk(unicodeChunk, size, style, color)));
                chunk.setLength(0);
            }
            chunk.appendCodePoint(codePoint);
            unicodeChunk = unicodeChar;
            offset += Character.charCount(codePoint);
        }

        if (!chunk.isEmpty()) {
            phrase.add(new Chunk(chunk.toString(), fontForChunk(Boolean.TRUE.equals(unicodeChunk), size, style, color)));
        }
        return phrase;
    }

    private Font fontForChunk(boolean unicodeChunk, int size, int style, Color color) {
        if (unicodeChunk && unicodeBaseFont != null) {
            return new Font(unicodeBaseFont, size, style, color);
        }
        return pdfFont(size, style, color);
    }

    private boolean requiresUnicodeFont(int codePoint) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(codePoint);
        return block == Character.UnicodeBlock.DEVANAGARI
                || block == Character.UnicodeBlock.DEVANAGARI_EXTENDED;
    }

    private String resolveUnicodeFontPath() {
        if (hasText(configuredUnicodeFontPath) && fontExists(configuredUnicodeFontPath)) {
            return configuredUnicodeFontPath;
        }

        List<String> candidates = List.of(
                "/usr/share/fonts/truetype/noto/NotoSansDevanagari-Regular.ttf",
                "/usr/share/fonts/opentype/noto/NotoSansDevanagari-Regular.ttf",
                "/usr/local/share/fonts/NotoSansDevanagari-Regular.ttf",
                "/opt/homebrew/share/fonts/NotoSansDevanagari-Regular.ttf",
                "/System/Library/Fonts/Supplemental/Devanagari Sangam MN.ttc,0",
                "/System/Library/Fonts/Supplemental/DevanagariMT.ttc,0",
                "/System/Library/Fonts/Kohinoor.ttc,0"
        );

        return candidates.stream()
                .filter(this::fontExists)
                .findFirst()
                .orElse(null);
    }

    private boolean fontExists(String fontPath) {
        String path = fontPath;
        int ttcIndex = fontPath.indexOf(".ttc,");
        if (ttcIndex > -1) {
            path = fontPath.substring(0, ttcIndex + ".ttc".length());
        }
        return Files.isRegularFile(Path.of(path));
    }

    private void addSeparator(Document document) throws DocumentException {
        PdfPTable separator = new PdfPTable(1);
        separator.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell(new Phrase(""));
        cell.setFixedHeight(8);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(PDF_LINE);
        separator.addCell(cell);
        document.add(separator);
    }

    private void addClosingSeparator(Document document) throws DocumentException {
        PdfPTable separator = new PdfPTable(1);
        separator.setWidthPercentage(100);
        separator.setSpacingBefore(16);
        PdfPCell cell = new PdfPCell(new Phrase(""));
        cell.setFixedHeight(1);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(PDF_LINE);
        separator.addCell(cell);
        document.add(separator);
    }

    private void writeStyles(PrintWriter writer) {
        writer.println("<style>");
        writer.println("@page{size:A4;margin:0}");
        writer.println("*{box-sizing:border-box}");
        writer.println("body{margin:0;background:#eef2f7;color:#172033;font-family:Arial,Helvetica,sans-serif;font-size:12px;line-height:1.35}");
        writer.println(".print-toolbar{width:210mm;margin:18px auto;display:flex;justify-content:flex-end}");
        writer.println(".print-button{border:0;border-radius:6px;background:#2563eb;color:#fff;font-weight:700;padding:10px 16px;cursor:pointer}");
        writer.println(".page{width:210mm;min-height:297mm;margin:0 auto 24px;background:#fff;position:relative;box-shadow:0 18px 45px rgba(15,23,42,.16);overflow:hidden}");
        writer.println(".header-image{width:100%;height:42mm;object-fit:contain;display:block;border-bottom:1px solid #d8e0ec}");
        writer.println(".header-placeholder{height:42mm;padding:16mm 18mm 8mm;border-bottom:1px solid #d8e0ec;display:grid;grid-template-columns:1fr 1fr;gap:16mm;align-items:end}");
        writer.println(".doctor-name,.facility-name{font-size:18px;font-weight:800;margin-bottom:3px}");
        writer.println(".muted{color:#64748b}");
        writer.println(".content{padding:5mm 15mm 26mm}");
        writer.println(".patient-row{display:grid;grid-template-columns:1.45fr .85fr .75fr;gap:8mm;padding:4mm 0;border-bottom:1px solid #d8e0ec;font-weight:700}");
        writer.println(".two-column{display:grid;grid-template-columns:1fr 1fr;gap:8mm;border-bottom:1px solid #d8e0ec;padding:4mm 0}");
        writer.println(".section{padding:4mm 0 0}");
        writer.println(".section-title{margin:0 0 2mm;font-size:12px;font-weight:800;text-transform:uppercase;letter-spacing:0}");
        writer.println(".list{margin:0;padding:0;list-style:none}");
        writer.println(".list li{margin:0 0 2mm}");
        writer.println(".rx{margin-top:5mm;font-family:Georgia,serif;font-size:22px;font-weight:700}");
        writer.println("table{width:100%;border-collapse:collapse;margin-top:2mm}");
        writer.println("th{text-align:left;border-top:1px solid #cbd5e1;border-bottom:1px solid #cbd5e1;padding:3mm 1mm;font-size:11px}");
        writer.println("td{vertical-align:top;border-bottom:1px solid #edf2f7;padding:3mm 1mm}");
        writer.println(".watermark{position:absolute;inset:85mm 0 auto;text-align:center;font-size:150px;color:rgba(15,23,42,.045);font-weight:800;pointer-events:none;transform:rotate(-10deg)}");
        writer.println(".footer{position:absolute;left:15mm;right:15mm;bottom:13mm;display:grid;grid-template-columns:1fr 1fr;align-items:end;gap:16mm;color:#64748b;font-size:10px}");
        writer.println(".signature{text-align:center;border-top:1px dashed #cbd5e1;padding-top:3mm;font-weight:700}");
        writer.println(".note{border-top:1px solid #d8e0ec;grid-column:1/-1;text-align:center;padding-top:3mm;font-style:italic}");
        writer.println("@media print{body{background:#fff}.print-toolbar{display:none}.page{margin:0;box-shadow:none}}");
        writer.println("</style>");
    }

    private void writeHeader(PrintWriter writer, String headerUrl, ConsultationResponse consultation) {
        if (hasText(headerUrl)) {
            writer.print("<img class=\"header-image\" src=\"");
            writer.print(escapeAttribute(headerUrl));
            writer.println("\" alt=\"Prescription header\">");
            return;
        }

        writer.println("<header class=\"header-placeholder\">");
        writer.println("<div>");
        writer.print("<div class=\"doctor-name\">");
        writer.print(value(consultation == null ? null : consultation.getDoctorName(), "Doctor"));
        writer.println("</div>");
        writeTextDiv(writer, consultation == null ? null : consultation.getQualification(), "");
        writeMutedDiv(writer, "Reg. No: " + value(consultation == null ? null : consultation.getDoctorRegistrationNo(), "-"));
        writeMutedDiv(writer, value(consultation == null ? null : consultation.getSpecialization(), ""));
        writer.println("</div>");
        writer.println("<div style=\"text-align:right\">");
        writer.print("<div class=\"facility-name\">");
        writer.print(value(facilityName(consultation), "Clinic / Hospital"));
        writer.println("</div>");
        writeMutedDiv(writer, value(facilityAddress(consultation), ""));
        writeMutedDiv(writer, "Ph: " + value(facilityPhone(consultation), "-"));
        writer.println("</div>");
        writer.println("</header>");
    }

    private void writePatientRow(
            PrintWriter writer,
            ConsultationResponse consultation,
            DetailedPrescriptionResponse prescription) {

        writer.println("<div class=\"patient-row\">");
        writer.print("<div>ID: ");
        writer.print(value(consultation == null ? null : consultation.getRegistrationNumber(),
                String.valueOf(prescription.getPrescriptionId())));
        writer.print(" - ");
        writer.print(value(consultation == null ? null : consultation.getPatientName(), "-"));
        writer.print(" (");
        writer.print(value(consultation == null ? null : consultation.getGender(), "-"));
        writer.print(" / ");
        writer.print(consultation == null ? "-" : consultation.getAge());
        writer.println(" Y)</div>");
        writer.print("<div>Mob. No.: ");
        writer.print(value(consultation == null ? null : consultation.getMobileNumber(), "-"));
        writer.println("</div>");
        writer.print("<div>Date: ");
        writer.print(formatDate(prescription.getCreatedAt()));
        writer.println("</div>");
        writer.println("</div>");
    }

    private void writeClinicalSummary(PrintWriter writer, ConsultationResponse consultation) {
        List<String> complaints = getComplaints(consultation);
        List<String> findings = getFindings(consultation);
        if (complaints.isEmpty() && findings.isEmpty()) {
            return;
        }

        writer.println("<div class=\"two-column\">");
        writer.println("<section>");
        writer.println("<h2 class=\"section-title\">Chief Complaints</h2>");
        writeListOrDash(writer, complaints);
        writer.println("</section>");
        writer.println("<section>");
        writer.println("<h2 class=\"section-title\">Clinical Findings</h2>");
        writeListOrDash(writer, findings);
        writer.println("</section>");
        writer.println("</div>");
    }

    private void writePastHistory(PrintWriter writer, ConsultationResponse consultation) {
        if (consultation == null) {
            return;
        }

        List<String> histories = consultation.getPastMedicalHistories() == null
                ? List.of(joinParts(consultation.getMedicalHistory(), consultation.getCurrentMedicine(), consultation.getAllergies()))
                : consultation.getPastMedicalHistories().stream()
                .map(item -> joinParts(item.getMedicalHistory(), item.getCurrentMedicine(), item.getAllergies()))
                .filter(this::hasText)
                .toList();

        writeSectionWithList(writer, "Past History", histories);
    }

    private void writeDiagnosis(PrintWriter writer, ConsultationResponse consultation) {
        if (consultation == null) {
            return;
        }

        List<String> diagnoses = consultation.getDiagnoses() == null
                ? List.of(joinParts(consultation.getDiagnosisName(), consultation.getDiagnosisCode(), consultation.getDiagnosisDuration()))
                : consultation.getDiagnoses().stream()
                .map(item -> joinParts(item.getDiagnosisName(), item.getDiagnosisCode(), item.getDiagnosisDuration()))
                .filter(this::hasText)
                .toList();

        writeSectionWithList(writer, "Diagnosis", diagnoses);
    }

    private void writeInvestigations(PrintWriter writer, DetailedPrescriptionResponse prescription) {
        StringBuilder items = new StringBuilder();
        if (prescription.getInvestigations() != null) {
            for (DetailedPrescriptionResponse.InvestigationDetailResponse item : prescription.getInvestigations()) {
                appendListItem(items, joinParts(item.getInvestigationName(), item.getNotes()));
            }
        }
        if (prescription.getTestRequested() != null) {
            for (DetailedPrescriptionResponse.TestRequestedDetailResponse item : prescription.getTestRequested()) {
                appendListItem(items, joinParts(item.getTestName(), item.getNotes()));
            }
        }
        if (prescription.getDiagnostics() != null) {
            for (DetailedPrescriptionResponse.DiagnosticDetailResponse item : prescription.getDiagnostics()) {
                appendListItem(items, joinParts(item.getTestName(), item.getResultSummary(), item.getNotes()));
            }
        }

        if (items.isEmpty()) {
            return;
        }

        writer.println("<section class=\"section\">");
        writer.println("<h2 class=\"section-title\">Investigations / Results</h2>");
        writer.println("<ul class=\"list\">");
        writer.print(items);
        writer.println("</ul>");
        writer.println("</section>");
    }

    private void writeMedicines(PrintWriter writer, List<DetailedPrescriptionResponse.MedicineDetailResponse> medicines) {
        if (medicines == null || medicines.isEmpty()) {
            return;
        }

        writer.println("<div class=\"rx\">Rx</div>");
        writer.println("<table>");
        writer.println("<thead><tr><th>Medicine Name</th><th>Dosage</th><th>Duration</th></tr></thead>");
        writer.println("<tbody>");
        for (int i = 0; i < medicines.size(); i++) {
            DetailedPrescriptionResponse.MedicineDetailResponse item = medicines.get(i);
            writer.println("<tr>");
            writer.print("<td><strong>");
            writer.print(i + 1);
            writer.print(") ");
            writer.print(escape(joinParts(item.getMedicineName(), item.getStrength())));
            writer.print("</strong><br><span class=\"muted\">");
            writer.print(escape(value(item.getInstruction(), "")));
            writer.println("</span></td>");
            writer.print("<td><strong>");
            writer.print(value(item.getDosage(), "-"));
            writer.print("</strong><br><span class=\"muted\">");
            writer.print(value(item.getFrequency(), ""));
            writer.println("</span></td>");
            writer.print("<td><strong>");
            writer.print(value(item.getDuration(), "-"));
            writer.print("</strong><br><span class=\"muted\">Qty: ");
            writer.print(value(item.getQuantity(), "-"));
            writer.println("</span></td>");
            writer.println("</tr>");
        }
        writer.println("</tbody>");
        writer.println("</table>");
    }

    private void writeFollowUp(PrintWriter writer, ConsultationResponse consultation) {
        if (consultation == null || consultation.getFollowUpDate() == null) {
            return;
        }
        writer.print("<section class=\"section\"><strong>Follow Up: ");
        writer.print(escape(formatDate(consultation.getFollowUpDate())));
        writer.println("</strong></section>");
    }

    private void writeSectionWithList(PrintWriter writer, String title, List<String> items) {
        List<String> filtered = items == null ? List.of() : items.stream().filter(this::hasText).toList();
        if (filtered.isEmpty()) {
            return;
        }
        writer.println("<section class=\"section\">");
        writer.print("<h2 class=\"section-title\">");
        writer.print(escape(title));
        writer.println("</h2>");
        writeList(writer, filtered);
        writer.println("</section>");
    }

    private void writeListOrDash(PrintWriter writer, List<String> items) {
        if (items == null || items.isEmpty()) {
            writer.println("<p class=\"muted\">-</p>");
            return;
        }
        writeList(writer, items);
    }

    private void writeList(PrintWriter writer, List<String> items) {
        writer.println("<ul class=\"list\">");
        for (String item : items) {
            writeListItem(writer, item);
        }
        writer.println("</ul>");
    }

    private boolean writeListItem(PrintWriter writer, String item) {
        if (!hasText(item)) {
            return false;
        }
        writer.print("<li>* ");
        writer.print(escape(item));
        writer.println("</li>");
        return true;
    }

    private void appendListItem(StringBuilder builder, String item) {
        if (!hasText(item)) {
            return;
        }
        builder.append("<li>* ");
        builder.append(escape(item));
        builder.append("</li>");
    }

    private List<String> getComplaints(ConsultationResponse consultation) {
        if (consultation == null) {
            return List.of();
        }
        if (consultation.getComplaints() != null && !consultation.getComplaints().isEmpty()) {
            return consultation.getComplaints().stream()
                    .map(item -> joinParts(item.getComplaintName(), item.getSeverity(),
                            item.getComplaintDuration(), item.getComplaintFrequency()))
                    .filter(this::hasText)
                    .toList();
        }
        String value = joinParts(consultation.getComplaintName(), consultation.getSeverity(),
                consultation.getComplaintDuration(), consultation.getComplaintFrequency());
        return hasText(value) ? List.of(value) : List.of();
    }

    private List<String> getFindings(ConsultationResponse consultation) {
        if (consultation == null) {
            return List.of();
        }
        if (consultation.getGeneralExaminations() != null && !consultation.getGeneralExaminations().isEmpty()) {
            return consultation.getGeneralExaminations().stream().filter(this::hasText).toList();
        }
        return hasText(consultation.getGeneralExamination())
                ? List.of(consultation.getGeneralExamination())
                : List.of();
    }

    private List<String> getPastHistories(ConsultationResponse consultation) {
        if (consultation == null) {
            return List.of();
        }
        if (consultation.getPastMedicalHistories() != null && !consultation.getPastMedicalHistories().isEmpty()) {
            return consultation.getPastMedicalHistories().stream()
                    .map(item -> joinParts(item.getMedicalHistory(), item.getCurrentMedicine(), item.getAllergies()))
                    .filter(this::hasText)
                    .toList();
        }
        String value = joinParts(consultation.getMedicalHistory(), consultation.getCurrentMedicine(), consultation.getAllergies());
        return hasText(value) ? List.of(value) : List.of();
    }

    private List<String> getDiagnoses(ConsultationResponse consultation) {
        if (consultation == null) {
            return List.of();
        }
        if (consultation.getDiagnoses() != null && !consultation.getDiagnoses().isEmpty()) {
            return consultation.getDiagnoses().stream()
                    .map(item -> joinParts(item.getDiagnosisName(), item.getDiagnosisCode(), item.getDiagnosisDuration()))
                    .filter(this::hasText)
                    .toList();
        }
        String value = joinParts(consultation.getDiagnosisName(), consultation.getDiagnosisCode(), consultation.getDiagnosisDuration());
        return hasText(value) ? List.of(value) : List.of();
    }

    private List<String> getInvestigations(DetailedPrescriptionResponse prescription) {
        if (prescription.getInvestigations() == null || prescription.getInvestigations().isEmpty()) {
            return List.of();
        }
        return prescription.getInvestigations().stream()
                .map(item -> joinParts(item.getInvestigationName(), item.getNotes()))
                .filter(this::hasText)
                .toList();
    }

    private List<String> getTestsRequested(DetailedPrescriptionResponse prescription) {
        if (prescription.getTestRequested() == null || prescription.getTestRequested().isEmpty()) {
            return List.of();
        }
        return prescription.getTestRequested().stream()
                .map(item -> joinParts(item.getTestName(), item.getNotes()))
                .filter(this::hasText)
                .toList();
    }

    private void writeTextDiv(PrintWriter writer, String value, String fallback) {
        writer.print("<div>");
        writer.print(value(value, fallback));
        writer.println("</div>");
    }

    private void writeMutedDiv(PrintWriter writer, String value) {
        writer.print("<div class=\"muted\">");
        writer.print(value == null ? "" : value);
        writer.println("</div>");
    }

    private String facilityName(ConsultationResponse consultation) {
        if (consultation == null) {
            return null;
        }
        return hasText(consultation.getHospitalName()) ? consultation.getHospitalName() : consultation.getClinicName();
    }

    private String facilityAddress(ConsultationResponse consultation) {
        if (consultation == null) {
            return null;
        }
        return hasText(consultation.getHospitalAddress()) ? consultation.getHospitalAddress() : consultation.getClinicAddress();
    }

    private String facilityPhone(ConsultationResponse consultation) {
        if (consultation == null) {
            return null;
        }
        return hasText(consultation.getHospitalPhone()) ? consultation.getHospitalPhone() : consultation.getClinicPhone();
    }

    private String joinParts(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (!hasText(part)) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append(part.trim());
        }
        return builder.toString();
    }

    private String trimNumber(double value) {
        return value == Math.rint(value)
                ? String.valueOf((long) value)
                : String.valueOf(value);
    }

    private String formatDate(LocalDateTime dateTime) {
        return dateTime == null ? "-" : DATE_FORMAT.format(dateTime);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null
                ? "-"
                : DateTimeFormatter.ofPattern("dd MMM yyyy @ hh:mm a").format(dateTime).toLowerCase();
    }

    private String value(String value, String fallback) {
        return hasText(value) ? escape(value.trim()) : fallback;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String escapeAttribute(String value) {
        return escape(value).replace("\"", "&quot;");
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}

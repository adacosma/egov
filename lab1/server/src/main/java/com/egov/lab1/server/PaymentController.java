package com.egov.lab1.server;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class PaymentController {

    private final PaymentService service;
    private final PdfService pdfService;

    public PaymentController(PaymentService service, PdfService pdfService) {
        this.service = service;
        this.pdfService = pdfService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<Long> add(@RequestBody String xml) {
        Payment saved = service.saveFromXml(xml);
        return ResponseEntity.ok(saved.getId());
    }

    @GetMapping(value = "/{id}/ordin", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> order(@PathVariable Long id) throws IOException {
        Payment p = service.getById(id);
        byte[] pdf = pdfService.buildPaymentOrder(p);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ordin_plata_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
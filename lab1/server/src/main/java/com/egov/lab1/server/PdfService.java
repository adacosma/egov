package com.egov.lab1.server;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;

@Service
public class PdfService {

    public byte[] buildPaymentOrder(Payment p) throws IOException {
        PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                text(cs, bold, 24, 50, 780, "Ordin de plata");
                text(cs, regular, 12, 470, 782, "Nr. " + p.getId());
                hr(cs, 50, 765, 545);

                text(cs, bold, 14, 50, 735, "Plătitor");
                text(cs, regular, 12, 50, 715, "Nume: " + p.getNume() + " " + p.getPrenume());
                text(cs, regular, 12, 50, 697, "Email: " + p.getEmail());
                text(cs, regular, 12, 50, 679, "Telefon: " + p.getTelefon());

                text(cs, bold, 14, 50, 635, "Detalii comanda");
                text(cs, regular, 12, 50, 615, "Teatru: " + p.getTeatru());
                text(cs, regular, 12, 50, 597, "Spectacol: " + p.getSpectacol());
                text(cs, regular, 12, 50, 579, "Data si ora: " + p.getDataSpectacol() + ", " + p.getOra());
                text(cs, regular, 12, 50, 561, "Categorie loc: " + p.getCategorieLoc());
                text(cs, regular, 12, 50, 543, "Bilete intregi: " + p.getNrBileteIntregi() + " | Bilete studenti: " + p.getNrBileteStudenti() + " (Total: " + p.getNrBileteTotal() + ")");
                text(cs, regular, 12, 50, 525, "Pret unitar (TVA inclus): " + money(p.getPretUnitar()));

                // caseta cu totaluri
                cs.addRect(300, 330, 245, 150);
                cs.stroke();
                text(cs, regular, 12, 315, 458, "Subtotal: " + money(p.getSubtotal()));
                text(cs, regular, 12, 315, 436, "Reducere studenti: -" + money(p.getReducere()));
                text(cs, regular, 12, 315, 414, "Baza impozabila: " + money(p.getBazaImpozabila()));
                text(cs, regular, 12, 315, 392, "TVA 21%: " + money(p.getTva()));
                text(cs, bold, 16, 315, 350, "Total: " + money(p.getTotal()));

                hr(cs, 50, 310, 545);
                text(cs, bold, 14, 50, 285, "Beneficiar");
                text(cs, regular, 12, 50, 265, p.getTeatru());
                text(cs, regular, 12, 50, 247, "IBAN: " + p.getIban());

            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private void text(PDPageContentStream cs, PDType1Font font, float size,
                      float x, float y, String s) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(ascii(s));
        cs.endText();
    }

    private void hr(PDPageContentStream cs, float x1, float y, float x2) throws IOException {
        cs.moveTo(x1, y);
        cs.lineTo(x2, y);
        cs.stroke();
    }

    private String money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP).toPlainString() + " RON";
    }

    private String ascii(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
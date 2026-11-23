package com.egov.lab1.server;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class ReportService {

    static {
        System.setProperty("java.awt.headless", "true");
    }

    private static final Color ALBASTRU = new Color(106, 100, 241);
    private static final Color VERDE = new Color(22, 163, 74);

    private final PaymentRepository repository;

    public ReportService(PaymentRepository repository) {
        this.repository = repository;
    }

    public byte[] buildReport() throws IOException {
        long nrComenzi = repository.count();
        if (nrComenzi == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nu exista comenzi pentru raport");
        }

        // 1. Datele din baza de date
        List<PaymentRepository.RaportRand> venitPeSpectacol = repository.venitPeSpectacol();
        List<PaymentRepository.RaportRand> comenziPeCategorie = repository.comenziPeCategorie();
        PaymentRepository.TotaluriBilete bilete = repository.totaluriBilete();
        BigDecimal venitTotal = repository.venitTotal() == null ? BigDecimal.ZERO : repository.venitTotal();

        long intregi = bilete.getIntregi() == null ? 0 : bilete.getIntregi().longValue();
        long studenti = bilete.getStudenti() == null ? 0 : bilete.getStudenti().longValue();
        long totalBilete = intregi + studenti;
        double procentStudenti = totalBilete == 0 ? 0 : 100.0 * studenti / totalBilete;

        PaymentRepository.RaportRand top = venitPeSpectacol.get(0);
        double procentTop = venitTotal.signum() == 0 ? 0
                : 100.0 * top.getValoare().doubleValue() / venitTotal.doubleValue();
        PaymentRepository.RaportRand topCategorie = comenziPeCategorie.get(0);

        // 2. Graficele (imagini)
        BufferedImage g1 = graficVenit(venitPeSpectacol);
        BufferedImage g2 = graficStudenti(intregi, studenti);
        BufferedImage g3 = graficCategorii(comenziPeCategorie);

        // 3. Documentul PDF
        PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDImageXObject img1 = LosslessFactory.createFromImage(doc, g1);
            PDImageXObject img2 = LosslessFactory.createFromImage(doc, g2);
            PDImageXObject img3 = LosslessFactory.createFromImage(doc, g3);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                text(cs, bold, 22, 50, 790, "Raport comenzi bilete");
                text(cs, regular, 12, 50, 770, "Beneficiar: Teatrul National de Opereta si Comedie Muzicala");
                text(cs, regular, 12, 50, 754, "Generat la: "
                        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
                hr(cs, 50, 742, 545);

                text(cs, bold, 14, 50, 720, "Rezumat");
                text(cs, regular, 12, 50, 702, "Comenzi analizate: " + nrComenzi);
                text(cs, regular, 12, 50, 686, "Venit total (TVA inclus): " + money(venitTotal));
                text(cs, regular, 12, 50, 670, "Bilete vandute: " + totalBilete
                        + " (intregi: " + intregi + ", studenti: " + studenti + ")");
                text(cs, regular, 12, 50, 654, "Ponderea biletelor de student: " + pct(procentStudenti));

                // grafic mare, pe toata latimea
                cs.drawImage(img1, 50, 410, 495, 218);
                // doua grafice mici, unul langa altul
                cs.drawImage(img2, 50, 200, 240, 182);
                cs.drawImage(img3, 305, 200, 240, 182);

                hr(cs, 50, 180, 545);
                text(cs, bold, 14, 50, 160, "Concluzii");
                text(cs, regular, 12, 50, 142, "Cel mai mare venit: " + top.getEticheta() + " ("
                        + money(BigDecimal.valueOf(top.getValoare().doubleValue())) + ", " + pct(procentTop)
                        + " din venitul total).");
                text(cs, regular, 12, 50, 126, "Categoria de loc cea mai ceruta: " + topCategorie.getEticheta()
                        + " (" + topCategorie.getValoare() + " comenzi din " + nrComenzi + ").");
                text(cs, regular, 12, 50, 110, pct(procentStudenti) + " din bilete au fost cumparate la pret de student (-50%).");
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    // graficele

    @SuppressWarnings({"rawtypes", "unchecked"})
    private BufferedImage graficVenit(List<PaymentRepository.RaportRand> rows) {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        for (PaymentRepository.RaportRand r : rows) {
            ds.addValue(r.getValoare(), "Venit (RON)", r.getEticheta());
        }
        JFreeChart chart = ChartFactory.createBarChart(
                "Venit pe spectacol (RON)", null, null, ds,
                PlotOrientation.HORIZONTAL, false, false, false);
        chart.setBackgroundPaint(Color.WHITE);
        chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 26));

        Font base = new Font("SansSerif", Font.PLAIN, 18);
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        CategoryAxis domain = plot.getDomainAxis();
        domain.setTickLabelFont(base);
        plot.getRangeAxis().setTickLabelFont(base);
        plot.getRangeAxis().setUpperMargin(0.15);

        BarRenderer br = (BarRenderer) plot.getRenderer();
        br.setBarPainter(new StandardBarPainter());
        br.setShadowVisible(false);
        br.setSeriesPaint(0, ALBASTRU);
        br.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator());
        br.setDefaultItemLabelFont(base);
        br.setDefaultItemLabelsVisible(true);
        return chart.createBufferedImage(1000, 440);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private BufferedImage graficStudenti(long intregi, long studenti) {
        DefaultPieDataset ds = new DefaultPieDataset();
        ds.setValue("Bilete intregi", intregi);
        ds.setValue("Bilete studenti", studenti);
        JFreeChart chart = ChartFactory.createPieChart("Bilete: intregi vs studenti", ds, false, false, false);
        chart.setBackgroundPaint(Color.WHITE);
        chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 22));

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setLabelFont(new Font("SansSerif", Font.PLAIN, 16));
        plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{0}: {1} ({2})"));
        plot.setSectionPaint("Bilete intregi", ALBASTRU);
        plot.setSectionPaint("Bilete studenti", VERDE);
        return chart.createBufferedImage(500, 380);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private BufferedImage graficCategorii(List<PaymentRepository.RaportRand> rows) {
        DefaultPieDataset ds = new DefaultPieDataset();
        for (PaymentRepository.RaportRand r : rows) {
            ds.setValue(r.getEticheta(), r.getValoare());
        }
        JFreeChart chart = ChartFactory.createPieChart("Comenzi pe categorie de loc", ds, false, false, false);
        chart.setBackgroundPaint(Color.WHITE);
        chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 22));

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setLabelFont(new Font("SansSerif", Font.PLAIN, 16));
        plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{0}: {1} ({2})"));
        return chart.createBufferedImage(500, 380);
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

    private String pct(double v) {
        return String.format(Locale.ROOT, "%.1f%%", v);
    }

    private String ascii(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
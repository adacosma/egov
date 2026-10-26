package com.egov.lab1.server;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;

@Service
public class PaymentService {

    private static final BigDecimal TVA = new BigDecimal("0.21");
    private static final BigDecimal REDUCERE_STUDENT = new BigDecimal("0.50");

    private static final String TEATRU = "Teatrul Național de Operetă și Comedie Muzicală";
    private static final String IBAN = "RO00EXMP0000000000000001";

    private static final Map<String, Map<String, BigDecimal>> PRETURI = Map.of(
            "Romeo și Julieta",    Map.of("Sală", new BigDecimal("140"), "Lojă", new BigDecimal("180"), "Balcon", new BigDecimal("90")),
            "My Fair Lady",        Map.of("Sală", new BigDecimal("130"), "Lojă", new BigDecimal("160"), "Balcon", new BigDecimal("80")),
            "Rebecca",             Map.of("Sală", new BigDecimal("150"), "Lojă", new BigDecimal("190"), "Balcon", new BigDecimal("100")),
            "Chicago",             Map.of("Sală", new BigDecimal("160"), "Lojă", new BigDecimal("200"), "Balcon", new BigDecimal("110")),
            "Fantoma de la Operă", Map.of("Sală", new BigDecimal("170"), "Lojă", new BigDecimal("220"), "Balcon", new BigDecimal("120")),
            "Mamma Mia!",          Map.of("Sală", new BigDecimal("140"), "Lojă", new BigDecimal("170"), "Balcon", new BigDecimal("85")),
            "Les Misérables",      Map.of("Sală", new BigDecimal("155"), "Lojă", new BigDecimal("195"), "Balcon", new BigDecimal("95"))
    );

    private final PaymentRepository repository;

    public PaymentService(PaymentRepository repository) {
        this.repository = repository;
    }

    public Payment saveFromXml(String xml) {
        Document doc = parse(xml);
        try {
            if (!text(doc, "cnp").matches("\\d{13}")) {
                throw bad("CNP invalid (13 cifre)");
            }
            if (!text(doc, "email").matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                throw bad("Email invalid");
            }

            int nIntregi = Integer.parseInt(text(doc, "nrBileteIntregi"));
            int nStudenti = Integer.parseInt(text(doc, "nrBileteStudenti"));
            if (nIntregi < 0 || nStudenti < 0) {
                throw bad("Numarul de bilete nu poate fi negativ");
            }
            int totalBilete = nIntregi + nStudenti;

            if (totalBilete < 1 || totalBilete > 10) {
                throw bad("Numarul total de bilete trebuie sa fie intre 1 si 10");
            }

            String spectacol = text(doc, "spectacol");
            String categorie = text(doc, "categorieLoc");
            BigDecimal pret = Optional.ofNullable(PRETURI.get(spectacol))
                    .map(m -> m.get(categorie))
                    .orElseThrow(() -> bad("Spectacol sau categorie necunoscuta"));

            BigDecimal valoareBilete = pret.multiply(BigDecimal.valueOf(totalBilete))
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal reducere = pret.multiply(BigDecimal.valueOf(nStudenti))
                    .multiply(REDUCERE_STUDENT)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal total = valoareBilete.subtract(reducere);

            BigDecimal subtotal = total.divide(BigDecimal.ONE.add(TVA), 2, RoundingMode.HALF_UP);
            BigDecimal tva = total.subtract(subtotal);

            if (total.signum() <= 0) {
                throw bad("Totalul trebuie sa fie pozitiv");
            }
            if (new BigDecimal(text(doc, "total")).compareTo(total) != 0) {
                throw bad("Suma din formular nu corespunde cu cea calculata de server");
            }

            Payment p = new Payment();
            p.setNume(text(doc, "nume"));
            p.setPrenume(text(doc, "prenume"));
            p.setCnp(text(doc, "cnp"));
            p.setEmail(text(doc, "email"));
            p.setTelefon(text(doc, "telefon"));
            p.setTeatru(TEATRU);
            p.setSpectacol(spectacol);
            p.setDataSpectacol(LocalDate.parse(text(doc, "dataSpectacol")));
            p.setOra(text(doc, "ora"));
            p.setCategorieLoc(categorie);
            p.setNrBileteIntregi(nIntregi);
            p.setNrBileteStudenti(nStudenti);
            p.setNrBileteTotal(totalBilete);
            p.setPretUnitar(pret);
            p.setSubtotal(subtotal);
            p.setReducere(reducere);
            p.setValoareBilete(valoareBilete);
            p.setTva(tva);
            p.setTotal(total);
            p.setIban(IBAN);
            p.setXmlContent(xml);
            return repository.save(p);
        } catch (NumberFormatException | DateTimeParseException e) {
            throw bad("Valoare numerica sau data invalida");
        }
    }

    private String text(Document doc, String tag) {
        NodeList nodes = doc.getElementsByTagName(tag);
        if (nodes.getLength() == 0 || nodes.item(0).getTextContent() == null) {
            throw bad("Lipseste campul: " + tag);
        }
        return nodes.item(0).getTextContent().trim();
    }

    public Payment getById(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Comanda nu exista"));
    }

    private ResponseStatusException bad(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    private Document parse(String xml) {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return f.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) {
            throw bad("XML invalid");
        }
    }
}
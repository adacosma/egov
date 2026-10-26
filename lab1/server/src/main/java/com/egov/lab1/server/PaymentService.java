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

@Service
public class PaymentService {

    private static final BigDecimal TVA = new BigDecimal("0.21");
    private static final BigDecimal REDUCERE_STUDENT = new BigDecimal("0.50");

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
            int totalBilete = nIntregi + nStudenti;

            if (totalBilete < 1 || totalBilete > 10) {
                throw bad("Numarul total de bilete trebuie sa fie intre 1 si 10");
            }

            BigDecimal pret = new BigDecimal(text(doc, "pretUnitar"));

            BigDecimal subIntregi = pret.multiply(BigDecimal.valueOf(nIntregi));
            BigDecimal subStudenti = pret.multiply(BigDecimal.valueOf(nStudenti)).multiply(REDUCERE_STUDENT);
            BigDecimal subtotal = subIntregi.add(subStudenti).setScale(2, RoundingMode.HALF_UP);

            BigDecimal reducere = subStudenti.setScale(2, RoundingMode.HALF_UP);

            BigDecimal total = subtotal;

            BigDecimal baza = total.divide(BigDecimal.ONE.add(TVA), 2, RoundingMode.HALF_UP);
            BigDecimal tva = total.subtract(baza);

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
            p.setTeatru(text(doc, "teatru"));
            p.setSpectacol(text(doc, "spectacol"));
            p.setDataSpectacol(LocalDate.parse(text(doc, "dataSpectacol")));
            p.setOra(text(doc, "ora"));
            p.setCategorieLoc(text(doc, "categorieLoc"));
            p.setNrBileteIntregi(nIntregi);
            p.setNrBileteStudenti(nStudenti);
            p.setNrBileteTotal(totalBilete);
            p.setPretUnitar(pret);
            p.setSubtotal(subtotal);
            p.setReducere(reducere);
            p.setBazaImpozabila(baza);
            p.setTva(tva);
            p.setTotal(total);
            p.setIban(text(doc, "iban"));
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
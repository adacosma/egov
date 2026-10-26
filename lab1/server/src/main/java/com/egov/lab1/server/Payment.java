package com.egov.lab1.server;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "comenzi")
@Getter
@Setter
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nume;
    private String prenume;
    private String cnp;
    private String email;
    private String telefon;

    private String teatru;
    private String spectacol;
    private LocalDate dataSpectacol;
    private String ora;
    private String categorieLoc;

    private Integer nrBileteIntregi;
    private Integer nrBileteStudenti;
    private Integer nrBileteTotal;

    private BigDecimal pretUnitar;
    private BigDecimal subtotal;
    private BigDecimal reducere;
    private BigDecimal bazaImpozabila;
    private BigDecimal tva;
    private BigDecimal total;

    private String iban;
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String xmlContent;
}
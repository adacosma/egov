package com.egov.lab1.server;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    interface RaportRand {
        String getEticheta();
        Number getValoare();
    }

    interface TotaluriBilete {
        Number getIntregi();
        Number getStudenti();
    }

    @Query("select p.spectacol as eticheta, sum(p.total) as valoare " +
            "from Payment p group by p.spectacol order by sum(p.total) desc")
    List<RaportRand> venitPeSpectacol();

    @Query("select p.categorieLoc as eticheta, count(p) as valoare " +
            "from Payment p group by p.categorieLoc order by count(p) desc")
    List<RaportRand> comenziPeCategorie();

    @Query("select sum(p.nrBileteIntregi) as intregi, sum(p.nrBileteStudenti) as studenti from Payment p")
    TotaluriBilete totaluriBilete();

    @Query("select sum(p.total) from Payment p")
    BigDecimal venitTotal();
}
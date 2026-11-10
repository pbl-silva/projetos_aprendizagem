package br.com.spbank.mercado.adapter.in.api.rest.controller;

import br.com.spbank.mercado.application.model.MarketRatesSnapshot;
import br.com.spbank.mercado.application.usecase.MarketRatesService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/market")
public final class MarketRatesController {
    private final MarketRatesService rates;

    public MarketRatesController(MarketRatesService rates) {
        this.rates = rates;
    }

    @GetMapping("/rates")
    public MarketRatesSnapshot rates() {
        return rates.current();
    }
}

package br.com.spbank.mercado.application.port.out;

import br.com.spbank.mercado.application.model.MarketRate;
import java.util.List;

public interface MarketRatesProvider {
    List<MarketRate> fetch();
}

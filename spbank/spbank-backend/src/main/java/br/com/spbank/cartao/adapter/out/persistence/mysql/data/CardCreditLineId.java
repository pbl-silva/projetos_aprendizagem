package br.com.spbank.cartao.adapter.out.persistence.mysql.data;

import br.com.spbank.cartao.application.model.CardBrand;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class CardCreditLineId implements Serializable {
    private UUID customerId;
    private CardBrand brand;

    public CardCreditLineId() {
    }

    public CardCreditLineId(UUID customerId, CardBrand brand) {
        this.customerId = customerId;
        this.brand = brand;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CardCreditLineId id
            && Objects.equals(customerId, id.customerId) && brand == id.brand;
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId, brand);
    }
}

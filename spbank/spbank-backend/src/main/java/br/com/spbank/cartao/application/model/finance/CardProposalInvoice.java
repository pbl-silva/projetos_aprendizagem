package br.com.spbank.cartao.application.model.finance;
import java.util.UUID;
import br.com.spbank.cartao.application.model.CardBrand;
public record CardProposalInvoice(UUID id, UUID customerId, CardBrand brand, UUID proposalId, UUID invoiceId) implements CardFinanceRecord {}

package br.com.spbank.conta.adapter.in.api.rest.controller.account.v1;

import static br.com.spbank.conta.adapter.in.api.rest.mapper.AccountRestMapper.toDto;
import static br.com.spbank.shared.adapter.in.api.rest.security.AuthenticatedRequest.ACCOUNT_ID;

import br.com.spbank.conta.adapter.in.api.rest.dto.*;
import br.com.spbank.conta.application.port.in.*;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts/me")
public final class AccountController {

    private final GetAccountSummaryUseCase summary;
    private final ListAccountEntriesUseCase entries;

    public AccountController(
            GetAccountSummaryUseCase summary,
            ListAccountEntriesUseCase entries
    ) {
        this.summary = summary;
        this.entries = entries;
    }

    @GetMapping("/summary")
    public AccountSummaryDto summary(
            @RequestAttribute(ACCOUNT_ID) UUID id
    ) {

        return toDto(
                summary.get(id)
        );
    }

    @GetMapping("/entries")
    public List<AccountEntryDto> entries(
            @RequestAttribute(ACCOUNT_ID) UUID id,
            @RequestParam(defaultValue = "50") int limit
    ) {

        return entries
                .list(id, limit)
                .stream()
                .map(entry -> toDto(entry))
                .toList();
    }
}
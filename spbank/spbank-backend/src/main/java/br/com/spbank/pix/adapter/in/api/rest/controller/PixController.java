package br.com.spbank.pix.adapter.in.api.rest.controller;

import static br.com.spbank.pix.adapter.in.api.rest.mapper.PixRestMapper.toDto;
import static br.com.spbank.shared.adapter.in.api.rest.security.AuthenticatedRequest.ACCOUNT_ID;

import br.com.spbank.autenticacao.application.port.in.AuthenticationUseCase;
import br.com.spbank.pix.adapter.in.api.rest.dto.PixConfirmationDto;
import br.com.spbank.pix.adapter.in.api.rest.dto.PixCreationDto;
import br.com.spbank.pix.adapter.in.api.rest.dto.PixFavoriteDto;
import br.com.spbank.pix.adapter.in.api.rest.dto.PixFavoriteRegistrationDto;
import br.com.spbank.pix.adapter.in.api.rest.dto.PixKeyDto;
import br.com.spbank.pix.adapter.in.api.rest.dto.PixKeyRegistrationDto;
import br.com.spbank.pix.adapter.in.api.rest.dto.PixPreviewDto;
import br.com.spbank.pix.adapter.in.api.rest.dto.PixReceiptDto;
import br.com.spbank.pix.adapter.in.api.rest.dto.PixRecurrenceCancellationDto;
import br.com.spbank.pix.adapter.in.api.rest.dto.PixRecurrenceDto;
import br.com.spbank.pix.application.port.in.PixCreationCommand;
import br.com.spbank.pix.application.port.in.PixFavoriteRegistrationCommand;
import br.com.spbank.pix.application.port.in.PixFavoriteUseCase;
import br.com.spbank.pix.application.port.in.PixKeyUseCase;
import br.com.spbank.pix.application.port.in.PixPaymentUseCase;
import br.com.spbank.pix.application.port.in.PixPreview;

import jakarta.validation.Valid;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pix")
public final class PixController {

    private final PixKeyUseCase keys;
    private final PixPaymentUseCase payments;
    private final PixFavoriteUseCase favorites;
    private final AuthenticationUseCase authentication;

    public PixController(
        PixKeyUseCase keys,
        PixPaymentUseCase payments,
        PixFavoriteUseCase favorites,
        AuthenticationUseCase authentication
    ) {
        this.keys = keys;
        this.payments = payments;
        this.favorites = favorites;
        this.authentication = authentication;
    }

    @GetMapping("/keys")
    public List<PixKeyDto> keys(
        @RequestAttribute(ACCOUNT_ID)
        UUID accountId
    ) {
        return keys
            .listKeys(accountId)
            .stream()
            .map(
                value -> toDto(value)
            )
            .toList();
    }

    @PostMapping("/keys")
    @ResponseStatus(HttpStatus.CREATED)
    public PixKeyDto registerKey(
        @RequestAttribute(ACCOUNT_ID)
        UUID accountId,

        @Valid
        @RequestBody
        PixKeyRegistrationDto dto
    ) {
        authentication.confirmPassword(
            accountId,
            dto.confirmationPassword()
        );

        return toDto(
            keys.registerKey(
                accountId,
                dto.type(),
                dto.value()
            )
        );
    }

    @DeleteMapping("/keys/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteKey(
        @RequestAttribute(ACCOUNT_ID)
        UUID accountId,

        @PathVariable
        UUID id
    ) {
        keys.deactivateKey(
            accountId,
            id
        );
    }

    @PostMapping("/preview")
    public PixPreviewDto preview(
        @RequestAttribute(ACCOUNT_ID)
        UUID accountId,

        @Valid
        @RequestBody
        PixCreationDto dto
    ) {
        PixPreview result =
            payments.preview(
                command(
                    accountId,
                    null,
                    dto
                )
            );

        return new PixPreviewDto(
            result.recipientName(),
            result.maskedRecipientDocument(),
            result.destinationBankCode(),
            result.destinationBankName(),
            result.keyType(),
            result.maskedKey(),
            result.amount(),
            result.scheduledFor(),
            result.frequency(),
            result.occurrences()
        );
    }

    @PostMapping
    public ResponseEntity<PixReceiptDto> create(
        @RequestHeader("Idempotency-Key")
        UUID idempotencyKey,

        @RequestAttribute(ACCOUNT_ID)
        UUID accountId,

        @Valid
        @RequestBody
        PixConfirmationDto dto
    ) {
        authentication.confirmPassword(
            accountId,
            dto.confirmationPassword()
        );

        PixReceiptDto receipt =
            toDto(
                payments.create(
                    command(
                        accountId,
                        idempotencyKey,
                        dto.pix()
                    )
                )
            );

        return ResponseEntity
            .created(
                URI.create(
                    "/api/v1/pix/"
                        + receipt.id()
                )
            )
            .body(receipt);
    }

    @GetMapping("/scheduled")
    public List<PixReceiptDto> scheduled(
        @RequestAttribute(ACCOUNT_ID)
        UUID accountId
    ) {
        return payments
            .listScheduled(accountId)
            .stream()
            .map(
                value -> toDto(value)
            )
            .toList();
    }

    @GetMapping("/{id}")
    public PixReceiptDto get(
        @RequestAttribute(ACCOUNT_ID)
        UUID accountId,

        @PathVariable
        UUID id
    ) {
        return toDto(
            payments.get(
                accountId,
                id
            )
        );
    }

    @DeleteMapping("/{id}")
    public PixReceiptDto cancel(
        @RequestAttribute(ACCOUNT_ID)
        UUID accountId,

        @PathVariable
        UUID id
    ) {
        return toDto(
            payments.cancel(
                accountId,
                id
            )
        );
    }

    @GetMapping("/favorites")
    public List<PixFavoriteDto> favorites(
        @RequestAttribute(ACCOUNT_ID)
        UUID accountId
    ) {
        return favorites
            .listFavorites(accountId)
            .stream()
            .map(
                value -> toDto(value)
            )
            .toList();
    }

    @PostMapping("/favorites")
    @ResponseStatus(HttpStatus.CREATED)
    public PixFavoriteDto registerFavorite(
        @RequestAttribute(ACCOUNT_ID)
        UUID accountId,

        @Valid
        @RequestBody
        PixFavoriteRegistrationDto dto
    ) {
        authentication.confirmPassword(
            accountId,
            dto.confirmationPassword()
        );

        return toDto(
            favorites.registerFavorite(
                new PixFavoriteRegistrationCommand(
                    accountId,
                    dto.keyType(),
                    dto.key(),
                    dto.recipientName(),
                    dto.bankCode(),
                    dto.recipientDocument()
                )
            )
        );
    }

    @DeleteMapping("/favorites/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFavorite(
        @RequestAttribute(ACCOUNT_ID)
        UUID accountId,

        @PathVariable
        UUID id
    ) {
        favorites.deleteFavorite(
            accountId,
            id
        );
    }

    @DeleteMapping("/recurrences/{id}")
    public PixRecurrenceCancellationDto cancelRecurrence(
        @RequestAttribute(ACCOUNT_ID)
        UUID accountId,

        @PathVariable
        UUID id
    ) {
        return new PixRecurrenceCancellationDto(
            id,
            payments.cancelRecurrence(
                accountId,
                id
            )
        );
    }

    private PixCreationCommand command(
        UUID accountId,
        UUID key,
        PixCreationDto dto
    ) {
        PixRecurrenceDto recurrence =
            dto.recurrence();

        return new PixCreationCommand(
            accountId,
            key,
            dto.keyType(),
            dto.key(),
            dto.amount(),
            dto.scheduledFor(),
            recurrence == null
                ? null
                : recurrence.frequency(),
            recurrence == null
                ? null
                : recurrence.occurrences(),
            dto.saveFavorite()
        );
    }
}
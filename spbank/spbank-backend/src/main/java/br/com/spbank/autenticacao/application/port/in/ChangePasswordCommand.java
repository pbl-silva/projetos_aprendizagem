package br.com.spbank.autenticacao.application.port.in;

public record ChangePasswordCommand(
        String currentPassword,
        String newPassword,
        String newPasswordConfirmation
) {

    @Override
    public String toString() {
        return "ChangePasswordCommand[currentPassword=[REDACTED]"
                + ", newPassword=[REDACTED]"
                + ", newPasswordConfirmation=[REDACTED]]";
    }
}
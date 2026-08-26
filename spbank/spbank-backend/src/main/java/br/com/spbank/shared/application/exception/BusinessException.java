package br.com.spbank.shared.application.exception;

public class BusinessException extends RuntimeException {

    private final String code;
    private final String messageKey;

    public BusinessException(String code, String messageKey) {
        super(messageKey);
        this.code = code;
        this.messageKey = messageKey;
    }

    public String getCode() {
        return code;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
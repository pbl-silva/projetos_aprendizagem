package br.com.spbank.shared.adapter.in.api.rest.security;

public final class AuthenticatedRequest {

    public static final String ACCOUNT_ID =
            "spbank.account-id";

    public static final String CUSTOMER_ID =
            "spbank.customer-id";

    public static final String ACCESS_TOKEN =
            "spbank.access-token";

    public static final String ADMINISTRATOR_ID =
            "spbank.administrator-id";

    public static final String ADMINISTRATOR_NAME =
            "spbank.administrator-name";

    public static final String ADMIN_ACCESS_TOKEN =
            "spbank.admin-access-token";

    private AuthenticatedRequest() {
    }
}
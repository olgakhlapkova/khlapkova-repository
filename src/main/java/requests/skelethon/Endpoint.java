package requests.skelethon;

import lombok.AllArgsConstructor;
import lombok.Getter;
import models.*;

@Getter
@AllArgsConstructor
public enum Endpoint {
    ADMIN_USER(
            "/admin/users",
            CreateUserRequest.class,
            CreateUserResponse.class
    ),

    ADMIN_USER_DELETE(
            "/admin/users",
            null,
            null
    ),

    ADMIN_USERS_GET(
            "/admin/users",
            null,
            CreateUserResponse.class
    ),

    LOGIN(
            "/auth/login",
            LoginUserRequest.class,
            LoginUserResponse.class
    ),

    ACCOUNTS(
            "/accounts",
            BaseModel.class,
            CreateAccountResponse.class
    ),

    ACCOUNTS_TRANSACTIONS(
            "/accounts/{accountId}/transactions",
            null,
            TransactionResponse.class
    ),

    ACCOUNTS_DELETE(
            "/accounts/{accountId}",
            null,
            null
    ),

    TRANSFER(
            "/accounts/transfer",
            TransferRequest.class,
            TransferResponse.class
    ),

    DEPOSIT(
            "/accounts/deposit",
            UserDepositRequest.class,
            UserDepositResponse.class
    ),

    CUSTOMER_PROFILE_UPDATE(
            "/customer/profile",
            UpdateProfileRequest.class,
            UpdateProfileResponse.class
    ),

    CUSTOMER_PROFILE_GET(
            "/customer/profile",
            null,
            CustomerResponse.class
    ),

    CUSTOMER_ACCOUNTS(
            "/customer/accounts",
            BaseModel.class,
            AccountResponse.class
    );

    private final String url;
    private final Class<? extends BaseModel> requestModel;
    private final Class<? extends BaseModel> responseModel;

}

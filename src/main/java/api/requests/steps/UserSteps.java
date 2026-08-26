package api.requests.steps;

import api.models.*;
import api.requests.skelethon.Endpoint;
import api.requests.skelethon.requesters.CrudRequester;
import api.requests.skelethon.requesters.ValidatedCrudRequester;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import io.qameta.allure.Step;

import java.util.Arrays;
import java.util.List;

public class UserSteps {
    private String username;
    private String password;

    public UserSteps(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public List<CreateAccountResponse> getAllAccounts() {
        return new ValidatedCrudRequester<CreateAccountResponse>(
                RequestSpecs.authAsUser(username, password),
                Endpoint.CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOK()).getAll(CreateAccountResponse[].class);
    }

    @Step("Создание аккаунта")
    public static int createAccount(CreateUserRequest userRequest) {
        CreateAccountResponse response = new ValidatedCrudRequester<CreateAccountResponse>(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated()
        ).post();
        return response.getId();
    }

    @Step("Добавление депозита на аккаунт")
    public static void addDeposit(CreateUserRequest userRequest, int accountId, double amount) {
        UserDepositRequest depositRequest = UserDepositRequest.builder()
                .id(accountId)
                .balance(amount)
                .build();

        new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.DEPOSIT,
                ResponseSpecs.requestReturnsOK()
        ).post(depositRequest);
    }

    @Step("Получение баланса аккаунта")
    public static double getBalance(CreateUserRequest userRequest, int accountId) {
        AccountResponse[] accounts = new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOK()
        ).getAndExtract(Endpoint.CUSTOMER_ACCOUNTS.getUrl(), AccountResponse[].class);

        return Arrays.stream(accounts)
                .filter(account -> account.getId() == accountId)
                .findFirst()
                .map(AccountResponse::getBalance)
                .orElseThrow(() -> new AssertionError("Аккаунт с ID " + accountId + " не найден"));
    }

    @Step("Получение текущего имени пользователя")
    public static String getCurrentName(CreateUserRequest userRequest) {
        CustomerResponse response = new ValidatedCrudRequester<CustomerResponse>(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.CUSTOMER_PROFILE_GET,
                ResponseSpecs.requestReturnsOK()
        ).get(0);
        return response != null ? response.getName() : "";
    }

    @Step("Выполнение перевода с аккаунта на аккаунт")
    public static void transferMoney(CreateUserRequest userRequest, int senderAccountId, int receiverAccountId, double amount) {
        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .amount(amount)
                .build();

        new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.TRANSFER,
                ResponseSpecs.requestReturnsOK()
        ).post(transferRequest);
    }

    @Step("Выполнение перевода с аккаунта на аккаунт c ошибкой")
    public static String transferMoneyWithError(CreateUserRequest userRequest, int senderAccountId, int receiverAccountId, double amount) {
        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .amount(amount)
                .build();

        return new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.TRANSFER,
                ResponseSpecs.requestReturnsBadRequest()
        ).post(transferRequest)
                .extract()
                .body()
                .asString();
    }

    @Step("Добавление депозита на аккаунт с ошибкой")
    public static String addDepositWithError(CreateUserRequest userRequest, int accountId, double amount) {
        UserDepositRequest depositRequest = UserDepositRequest.builder()
                .id(accountId)
                .balance(amount)
                .build();

        return new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.DEPOSIT,
                ResponseSpecs.requestReturnsBadRequest()
        ).post(depositRequest)
                .extract()
                .body()
                .asString();
    }

    @Step("Добавление депозита на чужой аккаунт с ошибкой")
    public static String addDepositToDifferentAccountWithError(CreateUserRequest userRequest, int accountId, double amount) {
        UserDepositRequest depositRequest = UserDepositRequest.builder()
                .id(accountId)
                .balance(amount)
                .build();

        return new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.DEPOSIT,
                ResponseSpecs.requestReturnsForbidden()
        ).post(depositRequest)
                .extract()
                .body()
                .asString();
    }

    @Step("Обновление имени пользователя")
    public static UpdateProfileResponse updateProfile(CreateUserRequest userRequest, String newName) {
        UpdateProfileRequest updateRequest = UpdateProfileRequest.builder()
                .name(newName)
                .build();

        return new ValidatedCrudRequester<UpdateProfileResponse>(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.CUSTOMER_PROFILE_UPDATE,
                ResponseSpecs.requestReturnsOK()
        ).update(0, updateRequest);
    }

    @Step("Обновление имени пользователя с ошибкой")
    public static String updateProfileWithError(CreateUserRequest userRequest, String newName) {
        UpdateProfileRequest updateRequest = UpdateProfileRequest.builder()
                .name(newName)
                .build();

        return new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.CUSTOMER_PROFILE_UPDATE,
                ResponseSpecs.requestReturnsBadRequest()
        ).update(0, updateRequest)
                .extract()
                .body()
                .asString();
    }

    @Step("Удаление аккаунта")
    public static DeleteAccountResponse deleteAccount(CreateUserRequest userRequest, int accountId) {
        return new ValidatedCrudRequester<DeleteAccountResponse>(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.ACCOUNTS_DELETE,
                ResponseSpecs.accountDeletedSuccessfully()
        ).delete(accountId);
    }

    @Step("Удаление аккаунта и получение сообщения")
    public static String deleteAccountAndGetMessage(CreateUserRequest userRequest, int accountId) {
        DeleteAccountResponse response = deleteAccount(userRequest, accountId);
        return response.getMessage();
    }

    @Step("Удаление аккаунта с возвратом ID")
    public static int deleteAccountAndGetId(CreateUserRequest userRequest, int accountId) {
        DeleteAccountResponse response = deleteAccount(userRequest, accountId);
        return response.getAccountId();
    }

    @Step("Удаление всех аккаунтов пользователя")
    public static void deleteAllAccounts(CreateUserRequest userRequest) {
        AccountResponse[] accounts = new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOK()
        ).getAndExtract(Endpoint.CUSTOMER_ACCOUNTS.getUrl(), AccountResponse[].class);

        for (AccountResponse account : accounts) {
            deleteAccount(userRequest, account.getId());
        }
    }

    @Step("Получение всех аккаунтов пользователя")
    public static AccountResponse[] getAllAccounts(CreateUserRequest userRequest) {
        return new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOK()
        ).getAndExtract(Endpoint.CUSTOMER_ACCOUNTS.getUrl(), AccountResponse[].class);
    }
}
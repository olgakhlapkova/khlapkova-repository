package requests.steps;

import io.qameta.allure.Step;
import models.*;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.skelethon.requesters.ValidatedCrudRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.Arrays;

public class UserSteps {

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
}
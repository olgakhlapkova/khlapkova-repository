package api.iteration_1;

import api.Base.BaseTest;
import io.qameta.allure.Step;
import api.models.AccountResponse;
import api.models.CreateUserRequest;
import api.models.DeleteAccountResponse;
import org.junit.jupiter.api.Test;
import api.requests.skelethon.Endpoint;
import api.requests.skelethon.requesters.CrudRequester;
import api.requests.steps.AdminSteps;
import api.requests.steps.UserSteps;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;

import static api.specs.ResponseSpecs.ACCOUNT_DELETED;

public class CreateAccountTest extends BaseTest {

    @Test
    @Step("Создание аккаунта")
    public void userCanCreateAccountTest() {
        CreateUserRequest userRequest = AdminSteps.createUser();
        registerUser(userRequest);

        int accountId = UserSteps.createAccount(userRequest);
        registerAccount(accountId);

        AccountResponse[] accounts = new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOK()
        ).getAndExtract(Endpoint.CUSTOMER_ACCOUNTS.getUrl(), AccountResponse[].class);

        softly.assertThat(accounts)
                .as("Список аккаунтов пользователя")
                .isNotEmpty()
                .anyMatch(account -> account.getId() == accountId);
    }

    @Test
    @Step("Удаление аккаунта")
    public void userCanDeleteAccountTest() {
        CreateUserRequest userRequest = AdminSteps.createUser();
        registerUser(userRequest);

        int accountId = UserSteps.createAccount(userRequest);
        registerAccount(accountId);

        DeleteAccountResponse response = UserSteps.deleteAccount(userRequest, accountId);

        softly.assertThat(response.getMessage())
                .as("Сообщение об успешном удалении аккаунта")
                .isEqualTo(ACCOUNT_DELETED);

        softly.assertThat(response.getAccountId())
                .as("ID удаленного аккаунта")
                .isEqualTo(accountId);

        unregisterAccount(accountId);

        AccountResponse[] accounts = new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOK()
        ).getAndExtract(Endpoint.CUSTOMER_ACCOUNTS.getUrl(), AccountResponse[].class);

        softly.assertThat(accounts)
                .as("Список аккаунтов пользователя после удаления")
                .noneMatch(account -> account.getId() == accountId);
    }

    @Test
    @Step("Удаление аккаунта с проверкой ID в ответе")
    public void userCanDeleteAccountAndGetIdTest() {
        CreateUserRequest userRequest = AdminSteps.createUser();
        registerUser(userRequest);

        int accountId = UserSteps.createAccount(userRequest);
        registerAccount(accountId);

        int deletedAccountId = UserSteps.deleteAccountAndGetId(userRequest, accountId);

        softly.assertThat(deletedAccountId)
                .as("ID удаленного аккаунта в ответе")
                .isEqualTo(accountId);

        unregisterAccount(accountId);
    }
}

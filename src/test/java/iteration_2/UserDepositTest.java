package iteration_2;

import Base.BaseTest;
import generators.RandomData;
import models.AccountResponse;
import models.CreateAccountResponse;
import models.CreateUserRequest;
import models.UserDepositRequest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.skelethon.requesters.ValidatedCrudRequester;
import requests.steps.AdminSteps;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.stream.Stream;

import static specs.ResponseSpecs.*;

public class UserDepositTest extends BaseTest {
    private static int testAccountId;
    private static CreateUserRequest userRequest;
    private static boolean isSetupDone = false;

    @BeforeAll
    public static void testSetup() {
        if (isSetupDone) return;

        //создаем данные для регистрации нового юзера
        userRequest = AdminSteps.createUser();

        // создание аккаунта
        new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated())
                .post();
        isSetupDone = true;
    }

    @BeforeEach
    public void createNewAccount() {
        CreateAccountResponse response = new ValidatedCrudRequester<CreateAccountResponse>(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated()
        ).post();
        testAccountId = response.getId();
    }

    private static double getBalance(int accountId) {
        AccountResponse[] accounts = new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOK()
        ).getAndExtract(Endpoint.CUSTOMER_ACCOUNTS.getUrl(), AccountResponse[].class);

        return java.util.Arrays.stream(accounts)
                .filter(account -> account.getId() == accountId)
                .findFirst()
                .map(AccountResponse::getBalance)
                .orElseThrow(() -> new AssertionError("Аккаунт с ID " + accountId + " не найден"));
    }

    public static Stream<Arguments> depositValidData() {
        return Stream.of(
                // позитивный
                Arguments.of(1000.0),
                // граничные значения
                Arguments.of(0.01),
                Arguments.of(4999.99),
                Arguments.of(5000.0));
    }

    @MethodSource("depositValidData")
    @ParameterizedTest
    public void userCanAddDepositWithValidValue(double depositAmount) {
        // получаем баланс ДО депозита
        double balanceBefore = getBalance(testAccountId);

        //ожидаемый баланс
        double expectedBalance = balanceBefore + depositAmount;

        // создаем запрос на депозит
        UserDepositRequest depositRequest = UserDepositRequest.builder()
                .id(testAccountId)
                .balance(depositAmount)
                .build();

        new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.DEPOSIT,
                ResponseSpecs.requestReturnsOK()
        ).post(depositRequest)
                .body("id", Matchers.equalTo(testAccountId))
                .body("balance", Matchers.equalTo((float) expectedBalance))
                .body("transactions.amount", Matchers.hasItem((float) depositAmount))
                .body("transactions[-1].id", Matchers.notNullValue())
                .body("transactions[-1].type", Matchers.equalTo("DEPOSIT"));

        // проверяем, что баланс увеличился через отдельный запрос
        double balanceAfter = getBalance(testAccountId);
        Assertions.assertEquals(
                expectedBalance,
                balanceAfter,
                0.01,
                "Баланс должен увеличиться на " + depositAmount
        );
    }

    public static Stream<Arguments> depositInvalidData() {
        return Stream.of(
                // негативные
                Arguments.of(6000.0, DEPOSIT_AMOUNT_MAX_ERROR),
                Arguments.of(-100.0, DEPOSIT_AMOUNT_MIN_ERROR),
                // граничные значения
                Arguments.of(5000.01, DEPOSIT_AMOUNT_MAX_ERROR),
                Arguments.of(0.0, DEPOSIT_AMOUNT_MIN_ERROR));
    }

    @MethodSource("depositInvalidData")
    @ParameterizedTest
    public void userCannotAddDepositWithInvalidValue(double depositAmount, String errorValue) {
        // получаем баланс ДО попытки депозита
        double balanceBefore = getBalance(testAccountId);

        // создаем запрос на депозит
        UserDepositRequest depositRequest = UserDepositRequest.builder()
                .id(testAccountId)
                .balance(depositAmount)
                .build();

        // отправляем запрос и получаем ответ
        String actualErrorMessage = new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.DEPOSIT,
                ResponseSpecs.requestReturnsBadRequest()
        ).post(depositRequest)
                .extract()
                .body()
                .asString();

        // проверяем сообщение об ошибке
        softly.assertThat(actualErrorMessage)
                .as("Сообщение об ошибке для суммы %s", depositAmount)
                .isEqualTo(errorValue);

        // проверяем, что баланс НЕ ИЗМЕНИЛСЯ
        double balanceAfter = getBalance(testAccountId);
        softly.assertThat(balanceAfter)
                .as("Баланс после неудачного депозита (сумма: %s)", depositAmount)
                .isEqualByComparingTo(balanceBefore);
    }

    @Test
    public void userCannotAddDepositToDifferentAccount() {
        // получаем баланс ДО попытки депозита
        double balanceBefore = getBalance(testAccountId);
        int differentAccountId = 2; // чужой аккаунт

        double depositAmount = RandomData.getBalance();

        // создаем запрос на депозит для чужого аккаунта
        UserDepositRequest depositRequest = UserDepositRequest.builder()
                .id(differentAccountId)
                .balance(depositAmount)
                .build();

        // отправляем запрос и получаем ответ
        String actualErrorMessage = new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.DEPOSIT,
                ResponseSpecs.requestReturnsForbidden()
        ).post(depositRequest)
                .extract()
                .body()
                .asString();

        // проверяем сообщение об ошибке
        softly.assertThat(actualErrorMessage)
                .as("Сообщение об ошибке при попытке депозита на чужой аккаунт")
                .isEqualTo(UNAUTHORIZED_ACCESS_TO_ACCOUNT);

        // проверяем, что баланс НЕ ИЗМЕНИЛСЯ
        double balanceAfter = getBalance(testAccountId);
        softly.assertThat(balanceAfter)
                .as("Баланс после попытки депозита на чужой аккаунт")
                .isEqualTo(balanceBefore);
    }
}

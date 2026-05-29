package iteration_2;

import Base.BaseTest;
import generators.RandomData;
import models.CreateUserRequest;
import models.UserDepositRequest;
import models.UserRole;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.AccountRequester;
import requests.AdminCreateUserRequester;
import requests.CreateAccountRequester;
import requests.UserDepositRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.stream.Stream;

public class UserDepositTest extends BaseTest {
    private static int testAccountId;
    private static CreateUserRequest userRequest;
    private static boolean isSetupDone = false;

    @BeforeAll
    public static void testSetup() {
        if (isSetupDone) return;

        //создаем данные для регистрации нового юзера
        userRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        //админом создаем юзера
        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(userRequest);

        isSetupDone = true;
    }

    @BeforeEach
    public void createNewAccount() {
        testAccountId = new CreateAccountRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.entityWasCreated()
        ).createAndGetId();
    }

    private static double getBalance(int accountId) {
        Double balance =  new AccountRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK()
        ).getBalance(accountId);

        if (balance == null) {
            throw new AssertionError("Аккаунт с ID " + accountId + " не найден");
        }
        return balance;
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

        new UserDepositRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
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
                Arguments.of(6000.0, "Deposit amount cannot exceed 5000"),
                Arguments.of(-100.0, "Deposit amount must be at least 0.01"),
                // граничные значения
                Arguments.of(5000.01, "Deposit amount cannot exceed 5000"),
                Arguments.of(0.0, "Deposit amount must be at least 0.01"));
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
        String actualErrorMessage = new UserDepositRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
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

        // создаем запрос на депозит для чужого аккаунта
        UserDepositRequest depositRequest = UserDepositRequest.builder()
                .id(differentAccountId)
                .balance(100.0)
                .build();

        // отправляем запрос и получаем ответ
        String actualErrorMessage = new UserDepositRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsForbidden()
        ).post(depositRequest)
                .extract()
                .body()
                .asString();

        // проверяем сообщение об ошибке
        softly.assertThat(actualErrorMessage)
                .as("Сообщение об ошибке при попытке депозита на чужой аккаунт")
                .isEqualTo("Unauthorized access to account");

        // проверяем, что баланс НЕ ИЗМЕНИЛСЯ
        double balanceAfter = getBalance(testAccountId);
        softly.assertThat(balanceAfter)
                .as("Баланс после попытки депозита на чужой аккаунт")
                .isEqualTo(balanceBefore);
    }
}

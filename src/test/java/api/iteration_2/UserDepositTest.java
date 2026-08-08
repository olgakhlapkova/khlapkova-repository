package api.iteration_2;

import api.Base.BaseTest;
import generators.RandomData;
import io.qameta.allure.Step;
import models.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.ValidatedCrudRequester;
import requests.steps.AdminSteps;
import requests.steps.UserSteps;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.within;
import static specs.ResponseSpecs.*;

public class UserDepositTest extends BaseTest {
    private static int testAccountId;
    private static CreateUserRequest userRequest;
    private static boolean isSetupDone = false;

    @BeforeAll
    @Step("Создаем юзера и добавляем аккаунт")
    public static void testSetup() {
        if (isSetupDone) return;
        userRequest = AdminSteps.createUser();
        registerUser(userRequest);
        int accountId = UserSteps.createAccount(userRequest);
        registerAccount(accountId);

        AccountResponse[] accounts = UserSteps.getAllAccounts(userRequest);
        boolean accountExists = Arrays.stream(accounts)
                .anyMatch(account -> account.getId() == accountId);

        if (!accountExists) {
            throw new AssertionError("Аккаунт с ID " + accountId + " не был создан");
        }

        isSetupDone = true;
    }

    @BeforeEach
    @Step("Добавляем аккаунт")
    public void createNewAccount() {
        testAccountId = UserSteps.createAccount(userRequest);
        registerAccount(testAccountId);

        AccountResponse[] accounts = UserSteps.getAllAccounts(userRequest);
        boolean accountExists = Arrays.stream(accounts)
                .anyMatch(account -> account.getId() == testAccountId);

        softly.assertThat(accountExists)
                .as("Аккаунт с ID " + testAccountId + " должен существовать после создания")
                .isTrue();
    }

    public static Stream<Arguments> depositValidData() {
        return Stream.of(
                Arguments.of(RandomData.getAmount()),
                Arguments.of(0.01),
                Arguments.of(4999.99),
                Arguments.of(5000.0));
    }

    @MethodSource("depositValidData")
    @ParameterizedTest
    @Step("Проверка позитивного сценария и граничных значений")
    public void userCanAddDepositWithValidValue(double depositAmount) {
        double balanceBefore = UserSteps.getBalance(userRequest, testAccountId);
        double expectedBalance = balanceBefore + depositAmount;

        UserDepositResponse response = new ValidatedCrudRequester<UserDepositResponse>(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.DEPOSIT,
                ResponseSpecs.requestReturnsOK()
        ).post(UserDepositRequest.builder()
                .id(testAccountId)
                .balance(depositAmount)
                .build());

        softly.assertThat(response.getId())
                .as("ID аккаунта")
                .isEqualTo(testAccountId);

        softly.assertThat(response.getBalance())
                .as("Баланс после депозита")
                .isEqualTo(expectedBalance, within(0.01));

        softly.assertThat(response.getTransactions())
                .as("Список транзакций")
                .isNotEmpty();

        TransactionResponse lastTransaction = response.getTransactions()
                .get(response.getTransactions().size() - 1);

        softly.assertThat(lastTransaction.getAmount())
                .as("Сумма последней транзакции")
                .isEqualTo(depositAmount, within(0.01));

        softly.assertThat(lastTransaction.getType())
                .as("Тип последней транзакции")
                .isEqualTo(TRANSACTION_TYPE_DEPOSIT);

        softly.assertThat(lastTransaction.getId())
                .as("ID последней транзакции")
                .isGreaterThan(0);

        double balanceAfter = UserSteps.getBalance(userRequest, testAccountId);
        softly.assertThat(balanceAfter)
                .as("Баланс должен увеличиться на %.2f", depositAmount)
                .isEqualTo(expectedBalance, within(0.01));
    }

    public static Stream<Arguments> depositInvalidData() {
        return Stream.of(
                Arguments.of(6000.0, DEPOSIT_AMOUNT_MAX_ERROR),
                Arguments.of(-100.0, DEPOSIT_AMOUNT_MIN_ERROR),
                Arguments.of(5000.01, DEPOSIT_AMOUNT_MAX_ERROR),
                Arguments.of(0.0, DEPOSIT_AMOUNT_MIN_ERROR));
    }

    @MethodSource("depositInvalidData")
    @ParameterizedTest
    @Step("Проверка негативных сценариев и граничных значений")
    public void userCannotAddDepositWithInvalidValue(double depositAmount, String errorValue) {
        double balanceBefore = UserSteps.getBalance(userRequest, testAccountId);

        String actualErrorMessage = UserSteps.addDepositWithError(userRequest, testAccountId, depositAmount);

        softly.assertThat(actualErrorMessage)
                .as("Сообщение об ошибке для суммы %s", depositAmount)
                .isEqualTo(errorValue);

        double balanceAfter = UserSteps.getBalance(userRequest, testAccountId);
        softly.assertThat(balanceAfter)
                .as("Баланс после неудачного депозита (сумма: %s)", depositAmount)
                .isEqualByComparingTo(balanceBefore);
    }

    @Test
    @Step("Проверка, что невозможно добавить депозит на чужой аккаунт")
    public void userCannotAddDepositToDifferentAccount() {
        double balanceBefore = UserSteps.getBalance(userRequest, testAccountId);
        int differentAccountId = RandomData.getRandomAccountId();

        double depositAmount = RandomData.getBalance();

        String actualErrorMessage = UserSteps.addDepositToDifferentAccountWithError(userRequest, differentAccountId, depositAmount);

        softly.assertThat(actualErrorMessage)
                .as("Сообщение об ошибке при попытке депозита на чужой аккаунт")
                .isEqualTo(UNAUTHORIZED_ACCESS_TO_ACCOUNT);

        double balanceAfter = UserSteps.getBalance(userRequest, testAccountId);
        softly.assertThat(balanceAfter)
                .as("Баланс после попытки депозита на чужой аккаунт")
                .isEqualTo(balanceBefore);
    }
}

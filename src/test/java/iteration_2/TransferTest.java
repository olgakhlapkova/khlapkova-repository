package iteration_2;

import Base.BaseTest;
import generators.RandomData;
import io.qameta.allure.Step;
import models.CreateUserRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.steps.AdminSteps;
import requests.steps.UserSteps;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.within;
import static specs.ResponseSpecs.*;

public class TransferTest extends BaseTest {
    private static CreateUserRequest userRequest;
    private static int accountId1;
    private static int accountId2;
    private static boolean isSetupDone = false;

    @BeforeAll
    @Step("Создаем юзера с 2 аккаунтами, добавляем депозиты (account1 = 5*5000 = 25000, account2 = 4*5000 = 20000")
    public static void testSetup() {
        if (isSetupDone) return;
        userRequest = AdminSteps.createUser();

        accountId1 = UserSteps.createAccount(userRequest);
        accountId2 = UserSteps.createAccount(userRequest);

        repeat(5, () -> UserSteps.addDeposit(userRequest, accountId1, 5000.0));
        repeat(4, () -> UserSteps.addDeposit(userRequest, accountId2, 5000.0));

        isSetupDone = true;
    }

    public static Stream<Arguments> transferValidData() {
        return Stream.of(
                Arguments.of(accountId1, accountId2, RandomData.getAmount()),
                Arguments.of(accountId1, accountId2, 0.01),
                Arguments.of(accountId1, accountId2, 9999.99),
                Arguments.of(accountId1, accountId2, 10000),
                Arguments.of(accountId2, accountId1, 100));
    }

    @MethodSource("transferValidData")
    @ParameterizedTest
    @Step("Проверка позитивных сценариев и граничных значений")
    public void userCanAddTransferWithValidValue(int senderAccountId, int receiverAccountId, double transferAmount) {
        double balanceBefore1 = UserSteps.getBalance(userRequest, senderAccountId);
        double balanceBefore2 = UserSteps.getBalance(userRequest, receiverAccountId);

        UserSteps.transferMoney(userRequest, senderAccountId, receiverAccountId, transferAmount);

        double balanceAfter1 = UserSteps.getBalance(userRequest, senderAccountId);
        double balanceAfter2 = UserSteps.getBalance(userRequest, receiverAccountId);

        softly.assertThat(balanceAfter1)
                .as("Баланс отправителя должен уменьшиться на %.2f", transferAmount)
                .isEqualTo(balanceBefore1 - transferAmount, within(0.01));

        softly.assertThat(balanceAfter2)
                .as("Баланс получателя должен увеличиться на %.2f", transferAmount)
                .isEqualTo(balanceBefore2 + transferAmount, within(0.01));
    }

    public static Stream<Arguments> transferInvalidData() {
        return Stream.of(
                Arguments.of(accountId1, accountId2, 0, TRANSFER_AMOUNT_MIN_ERROR),
                Arguments.of(accountId1, accountId2, -100, TRANSFER_AMOUNT_MIN_ERROR),
                Arguments.of(accountId1, accountId2, 10000.01, TRANSFER_AMOUNT_MAX_ERROR));
    }

    @MethodSource("transferInvalidData")
    @ParameterizedTest
    @Step("Проверка негативных сценариев и граничных значений")
    public void userCannotAddTransferWithInvalidValue(int senderAccountId, int receiverAccountId, double transferAmount, String errorValue) {
        double balanceBefore1 = UserSteps.getBalance(userRequest, senderAccountId);
        double balanceBefore2 = UserSteps.getBalance(userRequest, receiverAccountId);

        String actualErrorValue = UserSteps.transferMoneyWithError(userRequest, senderAccountId, receiverAccountId, transferAmount);

        softly.assertThat(actualErrorValue)
                .as("Сообщение об ошибке для суммы %.2f", transferAmount)
                .isEqualTo(errorValue);

        double balanceAfter1 = UserSteps.getBalance(userRequest, senderAccountId);
        double balanceAfter2 = UserSteps.getBalance(userRequest, receiverAccountId);

        softly.assertThat(balanceAfter1)
                .as("Баланс отправителя не должен измениться при ошибке")
                .isEqualTo(balanceBefore1, within(0.01));

        softly.assertThat(balanceAfter2)
                .as("Баланс получателя не должен измениться при ошибке")
                .isEqualTo(balanceBefore2, within(0.01));
    }

    @Test
    @Step("Проверка, что невозможно осуществить трансфер на несуществующий аккаунт")
    public void userCannotTransferToNonExistentAccount() {
        double balanceBefore = UserSteps.getBalance(userRequest, accountId1);
        double transferAmount = RandomData.getAmount();

        String actualErrorValue = UserSteps.transferMoneyWithError(userRequest, accountId1, RandomData.getRandomAccountId(), transferAmount);

        softly.assertThat(actualErrorValue)
                .as("Сообщение об ошибке при переводе на несуществующий аккаунт")
                .isEqualTo(INVALID_TRANSFER);

        double balanceAfter = UserSteps.getBalance(userRequest, accountId1);
        softly.assertThat(balanceAfter)
                .as("Баланс отправителя не должен измениться при переводе на несуществующий аккаунт")
                .isEqualTo(balanceBefore, within(0.01));
    }

    @Test
    @Step("Проверка, что невозможно сделать трансфер на сумму больше, чем баланс")
    public void userCannotTransferWithSumMoreThanUserBalanceTest() {
        int newAccountId = UserSteps.createAccount(userRequest);

        double startAmount = RandomData.getAmount();

        UserSteps.addDeposit(userRequest, newAccountId, startAmount);

        double balanceBeforeSender = UserSteps.getBalance(userRequest, newAccountId);
        double balanceBeforeReceiver = UserSteps.getBalance(userRequest, accountId2);

        double additionalBiggerAmount = startAmount * 2;

        String actualErrorMessage = UserSteps.transferMoneyWithError(userRequest, newAccountId, accountId2, additionalBiggerAmount);

        softly.assertThat(actualErrorMessage)
                .as("Сообщение об ошибке при недостатке средств")
                .isEqualTo(INVALID_TRANSFER);

        double balanceAfterSender = UserSteps.getBalance(userRequest, newAccountId);
        double balanceAfterReceiver = UserSteps.getBalance(userRequest, accountId2);

        softly.assertThat(balanceAfterSender)
                .as("Баланс отправителя не должен измениться при недостатке средств")
                .isEqualTo(balanceBeforeSender, within(0.01));

        softly.assertThat(balanceAfterReceiver)
                .as("Баланс получателя не должен измениться при ошибке перевода")
                .isEqualTo(balanceBeforeReceiver, within(0.01));
    }
}

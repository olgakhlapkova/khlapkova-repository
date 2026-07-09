package iteration_2;

import Base.BaseTest;
import generators.RandomData;
import models.*;
import org.junit.jupiter.api.BeforeAll;
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

import static org.assertj.core.api.Assertions.within;
import static specs.ResponseSpecs.*;

public class TransferTest extends BaseTest {
    private static CreateUserRequest userRequest;
    private static int accountId1;
    private static int accountId2;
    private static boolean isSetupDone = false;

    @BeforeAll
    public static void testSetup() {
        if (isSetupDone) return;
        //создаем данные для регистрации нового юзера
        userRequest = AdminSteps.createUser();

        //создаем 2 аккаунта
        accountId1 = createNewAccount();
        accountId2 = createNewAccount();

        // добавляем депозиты на оба аккаунта (account1 = 5*5000 = 25000, account2 = 4*5000 = 20000)
        repeat(5, () -> addDeposit(accountId1, 5000.0));
        repeat(4, () -> addDeposit(accountId2, 5000.0));

        isSetupDone = true;
    }

    public static int createNewAccount() {
        CreateAccountResponse response = new ValidatedCrudRequester<CreateAccountResponse>(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated()
        ).post();
        return response.getId();
    }

    private static void addDeposit(int accountId, double amount) {
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

    public static Stream<Arguments> transferValidData() {
        return Stream.of(
                // позитивный
                Arguments.of(accountId1, accountId2, 1000),
                // граничные значения
                Arguments.of(accountId1, accountId2, 0.01),
                Arguments.of(accountId1, accountId2, 9999.99),
                Arguments.of(accountId1, accountId2, 10000),
                // трансфер в обратную сторону
                Arguments.of(accountId2, accountId1, 100));
    }

    @MethodSource("transferValidData")
    @ParameterizedTest
    public void userCanAddTransferWithValidValue(int senderAccountId, int receiverAccountId, double transferAmount) {
        // получаем балансы до трансфера
        double balanceBefore1 = getBalance(senderAccountId);
        double balanceBefore2 = getBalance(receiverAccountId);

        // создаем запрос на трансфер
        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .amount(transferAmount)
                .build();

        // отправляем запрос - проверка статуса через ResponseSpecs
        new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.TRANSFER,
                ResponseSpecs.requestReturnsOK()
        ).post(transferRequest);

        // Проверяем балансы после трансфера
        double balanceAfter1 = getBalance(senderAccountId);
        double balanceAfter2 = getBalance(receiverAccountId);

        softly.assertThat(balanceAfter1)
                .as("Баланс отправителя должен уменьшиться на %.2f", transferAmount)
                .isEqualTo(balanceBefore1 - transferAmount, within(0.01));

        softly.assertThat(balanceAfter2)
                .as("Баланс получателя должен увеличиться на %.2f", transferAmount)
                .isEqualTo(balanceBefore2 + transferAmount, within(0.01));
    }

    public static Stream<Arguments> transferInvalidData() {
        return Stream.of(
                // негативные
                // неверная сумма
                Arguments.of(accountId1, accountId2, 0, TRANSFER_AMOUNT_MIN_ERROR),
                Arguments.of(accountId1, accountId2, -100, TRANSFER_AMOUNT_MIN_ERROR),
                // граничные значения
                Arguments.of(accountId1, accountId2, 10000.01, TRANSFER_AMOUNT_MAX_ERROR));
    }

    @MethodSource("transferInvalidData")
    @ParameterizedTest
    public void userCannotAddTransferWithInvalidValue(int senderAccountId, int receiverAccountId, double transferAmount, String errorValue) {
        //получаем балансы до трансфера
        double balanceBefore1 = getBalance(senderAccountId);
        double balanceBefore2 = getBalance(receiverAccountId);

        // создаем запрос на трансфер
        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .amount(transferAmount)
                .build();

        // отправляем запрос и получаем сообщение об ошибке
        String actualErrorValue = new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.TRANSFER,
                ResponseSpecs.requestReturnsBadRequest()
        ).post(transferRequest)
                .extract()
                .body()
                .asString();

        // проверяем сообщение об ошибке
        softly.assertThat(actualErrorValue)
                .as("Сообщение об ошибке для суммы %.2f", transferAmount)
                .isEqualTo(errorValue);

        // проверяем, что балансы не изменились
        double balanceAfter1 = getBalance(senderAccountId);
        double balanceAfter2 = getBalance(receiverAccountId);

        softly.assertThat(balanceAfter1)
                .as("Баланс отправителя не должен измениться при ошибке")
                .isEqualTo(balanceBefore1, within(0.01));

        softly.assertThat(balanceAfter2)
                .as("Баланс получателя не должен измениться при ошибке")
                .isEqualTo(balanceBefore2, within(0.01));
    }

    @Test
    public void userCannotTransferToNonExistentAccount() {
        // получаем баланс отправителя ДО
        double balanceBefore = getBalance(accountId1);

        double transferAmount = RandomData.getAmount();

        // создаем запрос на перевод на несуществующий аккаунт (ID = 999)
        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(accountId1)
                .receiverAccountId(999)
                .amount(transferAmount)
                .build();

        // отправляем запрос и получаем сообщение об ошибке
        String actualErrorValue = new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.TRANSFER,
                ResponseSpecs.requestReturnsBadRequest()
        ).post(transferRequest)
                .extract()
                .body()
                .asString();

        // проверяем сообщение об ошибке
        softly.assertThat(actualErrorValue)
                .as("Сообщение об ошибке при переводе на несуществующий аккаунт")
                .isEqualTo(INVALID_TRANSFER);

        // проверяем, что баланс отправителя не изменился
        double balanceAfter = getBalance(accountId1);
        softly.assertThat(balanceAfter)
                .as("Баланс отправителя не должен измениться при переводе на несуществующий аккаунт")
                .isEqualTo(balanceBefore, within(0.01));
    }


    @Test
    public void userCannotTransferWithSumMoreThanUserBalanceTest() {
        // создаем новый аккаунт у текущего пользователя
        int newAccountId = createNewAccount();

        double startAmount = RandomData.getAmount();

        // добавляем депозит на новый аккаунт
        addDeposit(newAccountId, startAmount);

        // получаем балансы ДО перевода
        double balanceBeforeSender = getBalance(newAccountId);
        double balanceBeforeReceiver = getBalance(accountId2);

        double additionalBiggerAmount = startAmount * 2;

        // создаем запрос на трансфер на сумму x2 больше баланса
        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(newAccountId)
                .receiverAccountId(accountId2)
                .amount(additionalBiggerAmount)
                .build();

        // отправляем запрос и получаем сообщение об ошибке
        String actualErrorMessage = new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.TRANSFER,
                ResponseSpecs.requestReturnsBadRequest()
        ).post(transferRequest)
                .extract()
                .body()
                .asString();

        // проверяем сообщение об ошибке
        softly.assertThat(actualErrorMessage)
                .as("Сообщение об ошибке при недостатке средств")
                .isEqualTo(INVALID_TRANSFER);

        // проверяем, что балансы НЕ ИЗМЕНИЛИСЬ
        double balanceAfterSender = getBalance(newAccountId);
        double balanceAfterReceiver = getBalance(accountId2);

        softly.assertThat(balanceAfterSender)
                .as("Баланс отправителя не должен измениться при недостатке средств")
                .isEqualTo(balanceBeforeSender, within(0.01));

        softly.assertThat(balanceAfterReceiver)
                .as("Баланс получателя не должен измениться при ошибке перевода")
                .isEqualTo(balanceBeforeReceiver, within(0.01));
    }
}

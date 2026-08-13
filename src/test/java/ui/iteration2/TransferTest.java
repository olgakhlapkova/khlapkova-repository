package ui.iteration2;

import api.generators.RandomData;
import api.models.AccountResponse;
import api.models.CreateUserRequest;
import api.requests.steps.AdminSteps;
import api.requests.steps.UserSteps;
import com.codeborne.selenide.Condition;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ui.Base.BaseUITest;
import ui.pages.BankAlert;
import ui.pages.TransactionType;
import ui.pages.TransferPage;
import ui.pages.UserDashboard;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.refresh;
import static org.assertj.core.api.Assertions.within;


public class TransferTest extends BaseUITest {
    private static final double DEPOSIT_AMOUNT = 5000.0;
    private static final int DEPOSIT_COUNT_ACCOUNT1 = 5;
    private static final int DEPOSIT_COUNT_ACCOUNT2 = 4;

    private static CreateUserRequest user;
    private static int accountId1;
    private static int accountId2;
    private static boolean isSetupDone = false;

    @BeforeAll
    public static void testSetup() {
        //Создаем юзера с 2 аккаунтами, добавляем депозиты (account1 = 5*5000 = 25000, account2 = 4*5000 = 20000)
        if (isSetupDone) return;
        user = AdminSteps.createUser();
        registerUser(user);

        accountId1 = UserSteps.createAccount(user);
        registerAccount(accountId1);

        accountId2 = UserSteps.createAccount(user);
        registerAccount(accountId2);

        repeat(DEPOSIT_COUNT_ACCOUNT1, () -> UserSteps.addDeposit(user, accountId1, DEPOSIT_AMOUNT));
        repeat(DEPOSIT_COUNT_ACCOUNT2, () -> UserSteps.addDeposit(user, accountId2, DEPOSIT_AMOUNT));

        AccountResponse[] accounts = UserSteps.getAllAccounts(user);
        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(accounts)
                .as("Список аккаунтов пользователя")
                .anyMatch(account -> account.getId() == accountId1);

        softly.assertThat(accounts)
                .as("Список аккаунтов пользователя")
                .anyMatch(account -> account.getId() == accountId2);

        double actualBalance1 = UserSteps.getBalance(user, accountId1);
        double actualBalance2 = UserSteps.getBalance(user, accountId2);

        double expectedBalance1 = DEPOSIT_COUNT_ACCOUNT1 * DEPOSIT_AMOUNT; // 25000
        double expectedBalance2 = DEPOSIT_COUNT_ACCOUNT2 * DEPOSIT_AMOUNT; // 20000

        softly.assertThat(actualBalance1)
                .as("Баланс accountId1")
                .isEqualTo(expectedBalance1, within(0.01));

        softly.assertThat(actualBalance2)
                .as("Баланс accountId2")
                .isEqualTo(expectedBalance2, within(0.01));

        softly.assertAll();
        authAsUser(user);

        isSetupDone = true;
    }
    @Test
    public void userCanAddTransferWithValidValue() {

        double transferAmount = 1000.0;
        double balanceBeforeAccount1 = UserSteps.getBalance(user, accountId1);
        double balanceBeforeAccount2 = UserSteps.getBalance(user, accountId2);

        new UserDashboard().open().openTransferPage().getTransferPageText().shouldBe(visible);

        new TransferPage().makeATransfer(user, accountId1, accountId2, transferAmount);

        String expectedAlert = String.format(BankAlert.SUCCESSFULL_TRANSFER.getMessage(),
                transferAmount, accountId2);
        new TransferPage().checkAlertMessageAndAccept(expectedAlert);

        new TransferPage().getTransferPageText().shouldBe(visible);

        SoftAssertions softly = new SoftAssertions();
        double balanceAfterAccount1 = UserSteps.getBalance(user, accountId1);
        double balanceAfterAccount2 = UserSteps.getBalance(user, accountId2);

        softly.assertThat(balanceAfterAccount1)
                .as("Баланс отправителя после перевода")
                .isEqualTo(balanceBeforeAccount1 - transferAmount, within(0.01));

        softly.assertThat(balanceAfterAccount2)
                .as("Баланс получателя после перевода")
                .isEqualTo(balanceBeforeAccount2 + transferAmount, within(0.01));

        refresh();

        new TransferPage().checkTransactions(TransactionType.TRANSFER_OUT);
        new TransferPage().checkTransactions(TransactionType.TRANSFER_IN);
    }

    @Test
    public void transferIsNotAddedWithoutSelectedAccount() {
        double transferAmount = 1000.0;

        double balanceBeforeAccount1 = UserSteps.getBalance(user, accountId1);
        double balanceBeforeAccount2 = UserSteps.getBalance(user, accountId2);

        new UserDashboard().open().openTransferPage().getTransferPageText().shouldBe(visible);

        new TransferPage().transferWithoutSelectedAccount(user, accountId2, transferAmount);
        new TransferPage().checkAlertMessageAndAccept(BankAlert.TRANSFER_WITHOUT_ALL_FIELDS.getMessage());

        new TransferPage().getTransferPageText().shouldBe(visible);

        SoftAssertions softly = new SoftAssertions();
        double balanceAfterAccount1 = UserSteps.getBalance(user, accountId1);
        double balanceAfterAccount2 = UserSteps.getBalance(user, accountId2);

        softly.assertThat(balanceAfterAccount1)
                .as("Баланс отправителя после перевода")
                .isEqualTo(balanceBeforeAccount1, within(0.01));

        softly.assertThat(balanceAfterAccount2)
                .as("Баланс получателя после перевода")
                .isEqualTo(balanceBeforeAccount2, within(0.01));
    }

    @Test
    public void transferIsNotAddedWithIncorrectRecipientAccountNumber() {
        double transferAmount = 1000.0;

        double balanceBeforeAccount1 = UserSteps.getBalance(user, accountId1);
        double balanceBeforeAccount2 = UserSteps.getBalance(user, accountId2);

        new UserDashboard().open().openTransferPage().getTransferPageText().shouldBe(visible);

        new TransferPage().transferWithIncorrectAccountNumber(user, accountId1, transferAmount);
        new TransferPage().checkAlertMessageAndAccept(BankAlert.TRANSFER_WITH_INCORRECT_ACCOUNT_NUMBER.getMessage());

        new TransferPage().getTransferPageText().shouldBe(visible);

        SoftAssertions softly = new SoftAssertions();
        double balanceAfterAccount1 = UserSteps.getBalance(user, accountId1);
        double balanceAfterAccount2 = UserSteps.getBalance(user, accountId2);

        softly.assertThat(balanceAfterAccount1)
                .as("Баланс отправителя после перевода")
                .isEqualTo(balanceBeforeAccount1, within(0.01));

        softly.assertThat(balanceAfterAccount2)
                .as("Баланс получателя после перевода")
                .isEqualTo(balanceBeforeAccount2, within(0.01));
    }

    @Test
    public void transferIsNotAddedWithIncorrectTransferAmount() {
        double incorrectTransferAmount = RandomData.getBigAmount();

        double balanceBeforeAccount1 = UserSteps.getBalance(user, accountId1);
        double balanceBeforeAccount2 = UserSteps.getBalance(user, accountId2);

        new UserDashboard().open().openTransferPage().getTransferPageText().shouldBe(visible);

        new TransferPage().transferWithIncorrectTransferAmount(user, accountId1, accountId2, incorrectTransferAmount);
        new TransferPage().checkAlertMessageAndAccept(BankAlert.TRANSFER_WITH_BIG_AMOUNT.getMessage());

        new TransferPage().getTransferPageText().shouldBe(visible);

        SoftAssertions softly = new SoftAssertions();
        double balanceAfterAccount1 = UserSteps.getBalance(user, accountId1);
        double balanceAfterAccount2 = UserSteps.getBalance(user, accountId2);

        softly.assertThat(balanceAfterAccount1)
                .as("Баланс отправителя после перевода")
                .isEqualTo(balanceBeforeAccount1, within(0.01));

        softly.assertThat(balanceAfterAccount2)
                .as("Баланс получателя после перевода")
                .isEqualTo(balanceBeforeAccount2, within(0.01));
    }

    @Test
    public void transferIsNotAddedWithUncheckedCheckbox() {
        double transferAmount = 1000.0;

        double balanceBeforeAccount1 = UserSteps.getBalance(user, accountId1);
        double balanceBeforeAccount2 = UserSteps.getBalance(user, accountId2);

        new UserDashboard().open().openTransferPage().getTransferPageText().shouldBe(visible);

        new TransferPage().transferWithUncheckedCheckbox(user, accountId1, accountId2, transferAmount);
        new TransferPage().checkAlertMessageAndAccept(BankAlert.TRANSFER_WITHOUT_ALL_FIELDS.getMessage());

        new TransferPage().getTransferPageText().shouldBe(visible);

        SoftAssertions softly = new SoftAssertions();
        double balanceAfterAccount1 = UserSteps.getBalance(user, accountId1);
        double balanceAfterAccount2 = UserSteps.getBalance(user, accountId2);

        softly.assertThat(balanceAfterAccount1)
                .as("Баланс отправителя после перевода")
                .isEqualTo(balanceBeforeAccount1, within(0.01));

        softly.assertThat(balanceAfterAccount2)
                .as("Баланс получателя после перевода")
                .isEqualTo(balanceBeforeAccount2, within(0.01));
    }

    @Test
    public void userCanAddTransferAgain() {
        double transferAmount = 1000.0;

        double balanceBeforeAccount1 = UserSteps.getBalance(user, accountId1);
        double balanceBeforeAccount2 = UserSteps.getBalance(user, accountId2);

        new UserDashboard().open().openTransferPage().getTransferPageText().shouldBe(visible);

        new TransferPage().makeATransfer(user, accountId1, accountId2, transferAmount);
        String expectedAlert = String.format(BankAlert.SUCCESSFULL_TRANSFER.getMessage(),
                transferAmount, accountId2);
        new TransferPage().checkAlertMessageAndAccept(expectedAlert);

        refresh();

        new TransferPage().transferAgain(accountId2);

        String expectedAlert2 = String.format(BankAlert.SUCCESSFULL_TRANSFER_AGAIN.getMessage(),
                transferAmount, accountId2, accountId2); //сейчас баг - в алерте 2 раза accountId2, после фикса исправить на accountId1, accountId2
        new TransferPage().checkAlertMessageAndAccept(expectedAlert2);

        new TransferPage().getTransferPageText().shouldBe(visible);

        SoftAssertions softly = new SoftAssertions();
        double balanceAfterAccount1 = UserSteps.getBalance(user, accountId1);
        double balanceAfterAccount2 = UserSteps.getBalance(user, accountId2);

        softly.assertThat(balanceAfterAccount1)
                .as("Баланс отправителя после перевода")
                .isEqualTo(balanceBeforeAccount1 - 2 * transferAmount, within(0.01));

        softly.assertThat(balanceAfterAccount2)
                .as("Баланс получателя после перевода")
                .isEqualTo(balanceBeforeAccount2 + 2 * transferAmount, within(0.01));
    }

    @Test
    public void userCannotAddTransferAgainWithIncorrectAmount() {
        double transferAmount = 1000.0;

        double balanceBeforeAccount1 = UserSteps.getBalance(user, accountId1);
        double balanceBeforeAccount2 = UserSteps.getBalance(user, accountId2);

        new UserDashboard().open().openTransferPage().getTransferPageText().shouldBe(visible);

        new TransferPage().makeATransfer(user, accountId1, accountId2, transferAmount);
        String expectedAlert = String.format(BankAlert.SUCCESSFULL_TRANSFER.getMessage(),
                transferAmount, accountId2);
        new TransferPage().checkAlertMessageAndAccept(expectedAlert);

        refresh();

        new TransferPage().transferAgainWithInvalidAmount(accountId2);

        String expectedAlert2 = String.format(BankAlert.TRANSFER_FAILED.getMessage());
        new TransferPage().checkAlertMessageAndAccept(expectedAlert2);

        new TransferPage().getRepeatTransferText().shouldBe(visible);

        SoftAssertions softly = new SoftAssertions();
        double balanceAfterAccount1 = UserSteps.getBalance(user, accountId1);
        double balanceAfterAccount2 = UserSteps.getBalance(user, accountId2);

        softly.assertThat(balanceAfterAccount1)
                .as("Баланс отправителя после перевода")
                .isEqualTo(balanceBeforeAccount1 - transferAmount, within(0.01));

        softly.assertThat(balanceAfterAccount2)
                .as("Баланс получателя после перевода")
                .isEqualTo(balanceBeforeAccount2 + transferAmount, within(0.01));
    }

    @Test
    public void transferAgainIsNotAddedAfterCancelViaCancelButton() {
        double transferAmount = 1000.0;

        double balanceBeforeAccount1 = UserSteps.getBalance(user, accountId1);
        double balanceBeforeAccount2 = UserSteps.getBalance(user, accountId2);

        new UserDashboard().open().openTransferPage().getTransferPageText().shouldBe(visible);

        new TransferPage().makeATransfer(user, accountId1, accountId2, transferAmount);
        String expectedAlert = String.format(BankAlert.SUCCESSFULL_TRANSFER.getMessage(),
                transferAmount, accountId2);
        new TransferPage().checkAlertMessageAndAccept(expectedAlert);
        refresh();

        new TransferPage().cancelTransferAgainViaCancelButton(accountId2);

        new TransferPage().getMatchingTransactionsText().shouldBe(Condition.visible);

        SoftAssertions softly = new SoftAssertions();
        double balanceAfterAccount1 = UserSteps.getBalance(user, accountId1);
        double balanceAfterAccount2 = UserSteps.getBalance(user, accountId2);

        softly.assertThat(balanceAfterAccount1)
                .as("Баланс отправителя после перевода")
                .isEqualTo(balanceBeforeAccount1 - transferAmount, within(0.01));

        softly.assertThat(balanceAfterAccount2)
                .as("Баланс получателя после перевода")
                .isEqualTo(balanceBeforeAccount2 + transferAmount, within(0.01));
    }

    @Test
    public void transferAgainIsNotAddedAfterCancelViaCrossIcon() {
        double transferAmount = 1000.0;

        double balanceBeforeAccount1 = UserSteps.getBalance(user, accountId1);
        double balanceBeforeAccount2 = UserSteps.getBalance(user, accountId2);

        new UserDashboard().open().openTransferPage().getTransferPageText().shouldBe(visible);

        new TransferPage().makeATransfer(user, accountId1, accountId2, transferAmount);
        String expectedAlert = String.format(BankAlert.SUCCESSFULL_TRANSFER.getMessage(),
                transferAmount, accountId2);
        new TransferPage().checkAlertMessageAndAccept(expectedAlert);
        refresh();

        new TransferPage().cancelTransferAgainViaCrossIcon(accountId2);

        new TransferPage().getMatchingTransactionsText().shouldBe(Condition.visible);

        SoftAssertions softly = new SoftAssertions();
        double balanceAfterAccount1 = UserSteps.getBalance(user, accountId1);
        double balanceAfterAccount2 = UserSteps.getBalance(user, accountId2);

        softly.assertThat(balanceAfterAccount1)
                .as("Баланс отправителя после перевода")
                .isEqualTo(balanceBeforeAccount1 - transferAmount, within(0.01));

        softly.assertThat(balanceAfterAccount2)
                .as("Баланс получателя после перевода")
                .isEqualTo(balanceBeforeAccount2 + transferAmount, within(0.01));
    }
}

package ui.iteration2;

import api.generators.RandomData;
import api.models.CreateUserRequest;
import api.requests.steps.AdminSteps;
import api.requests.steps.UserSteps;
import org.junit.jupiter.api.Test;
import ui.Base.BaseUITest;
import ui.pages.*;

import static com.codeborne.selenide.Condition.visible;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.within;

public class UserDepositTest extends BaseUITest {
    @Test
    public void userCanAddDepositWithValidValue() {
        CreateUserRequest user = AdminSteps.createUser();
        registerUser(user);

        authAsUser(user);

        int accountId = UserSteps.createAccount(user);
        registerAccount(accountId);

        double balanceBefore = UserSteps.getBalance(user, accountId);

        new UserDashboard().open().openDepositPage().getDepositMoneyText().shouldBe(visible);

        double depositAmount = RandomData.getAmount();
        new DepositMoneyPage().makeADeposit(accountId, String.valueOf(depositAmount));

        //Проверка, что депозит добавлен в API
        double balanceAfter = UserSteps.getBalance(user, accountId);
        assertThat(balanceAfter)
                .as("Баланс после депозита")
                .isEqualTo(balanceBefore + depositAmount, within(0.01));


        String expectedAlert = String.format(BankAlert.SUCCESSFULL_DEPOSIT.getMessage(),
                depositAmount, accountId);
        new DepositMoneyPage().checkAlertMessageAndAccept(expectedAlert);

        new UserDashboard().getUserDashboardText().shouldBe(visible);

        new TransferPage().open().checkTransactions(TransactionType.DEPOSIT);
    }

    @Test
    public void userCanNotAddDepositWithInvalidValue() {
        CreateUserRequest user = AdminSteps.createUser();
        registerUser(user);

        authAsUser(user);

        int accountId = UserSteps.createAccount(user);
        registerAccount(accountId);

        double balanceBefore = UserSteps.getBalance(user, accountId);

        new UserDashboard().open().openDepositPage().getDepositMoneyText().shouldBe(visible);

        double depositAmount = RandomData.getBigAmount();
        new DepositMoneyPage().makeADeposit(accountId, String.valueOf(depositAmount));

        new DepositMoneyPage().checkAlertMessageAndAccept(BankAlert.UNSUCCESSFULL_DEPOSIT.getMessage());

        new DepositMoneyPage().getDepositMoneyText().shouldBe(visible);

        //Проверка, что депозит НЕ добавлен в API
        double balanceAfter = UserSteps.getBalance(user, accountId);
        assertThat(balanceAfter)
                .as("Баланс после депозита")
                .isEqualTo(balanceBefore);
    }
}

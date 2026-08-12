package ui.iteration1;

import api.models.CreateAccountResponse;
import api.models.CreateUserRequest;
import api.requests.steps.AdminSteps;
import api.requests.steps.UserSteps;
import org.junit.jupiter.api.Test;
import ui.Base.BaseUITest;
import ui.pages.BankAlert;
import ui.pages.UserDashboard;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateAccountTest extends BaseUITest {
    @Test
    public void userCanCreateAccountTest() {
        CreateUserRequest user = AdminSteps.createUser();
        registerUser(user);

        authAsUser(user);

        new UserDashboard().open().createNewAccount();

        List<CreateAccountResponse> createdAccounts = new UserSteps(user.getUsername(), user.getPassword())
                .getAllAccounts();

        assertThat(createdAccounts).hasSize(1);

        new UserDashboard().checkAlertMessageAndAccept
                (BankAlert.NEW_ACCOUNT_CREATED.getMessage() + createdAccounts.getFirst().getAccountNumber());

        assertThat(createdAccounts.getFirst().getBalance()).isZero();

        registerAccount(createdAccounts.getFirst().getId());
    }
}

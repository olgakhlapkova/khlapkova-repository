package ui.iteration2;

import api.generators.RandomData;
import api.models.CreateUserRequest;
import api.models.UpdateProfileRequest;
import api.requests.steps.AdminSteps;
import api.requests.steps.UserSteps;
import org.junit.jupiter.api.Test;
import ui.Base.BaseUITest;
import ui.pages.BankAlert;
import ui.pages.EditProfilePage;
import ui.pages.UserDashboard;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static org.assertj.core.api.Assertions.assertThat;

public class UpdateProfileTest extends BaseUITest {
    @Test
    public void userCanUpdateName() {
        CreateUserRequest user = AdminSteps.createUser();
        registerUser(user);

        setupUserWithDefaultName(user);
//        authAsUser(user);

        new UserDashboard().open().openEditProfilePage().getEditProfileText().shouldBe(visible);

        String updatedName = RandomData.generateRandomShortValidName();
        new EditProfilePage().editUsername(updatedName);

        new EditProfilePage().checkAlertMessageAndAccept(BankAlert.NAME_UPDATED_SUCCESSFULLY.getMessage());

        new EditProfilePage().getEditProfileText().shouldBe(visible);

        //Проверка, что имя обновлено в API
        String nameAfterApi = UserSteps.getCurrentName(user);
        assertThat(nameAfterApi)
                .as("Имя после обновления (API)")
                .isEqualTo(updatedName);

        new UserDashboard().openViaHomeButton().getWelcomeTextUsername().shouldHave(text(updatedName));

        //Дополнительная проверка: Имя пользователя обновилось в хедере (над username) - здесь баг, раскомментить после фикса
        //new UserDashboard().getUserInfoUsername().shouldHave(text(updatedName));
    }

    @Test
    public void userCannotUpdateName() {
        CreateUserRequest user = AdminSteps.createUser();
        registerUser(user);

        setupUserWithDefaultName(user);
//        authAsUser(user);

        new UserDashboard().open().openEditProfilePage().getEditProfileText().shouldBe(visible);

        String updatedInvalidName = RandomData.generateOneWordName();
        new EditProfilePage().editUsername(updatedInvalidName);

        new EditProfilePage().checkAlertMessageAndAccept(BankAlert.NAME_MUST_CONTAIN_TWO_WORDS.getMessage());

        new EditProfilePage().getEditProfileText().shouldBe(visible);

        //Проверка, что имя НЕ обновлено в API
        String nameAfterApi = UserSteps.getCurrentName(user);
        assertThat(nameAfterApi)
                .as("Имя после обновления (API)")
                .isEqualTo(UpdateProfileRequest.DEFAULT_NAME);

        new UserDashboard().openViaHomeButton().getWelcomeTextUsername().shouldHave(text(UpdateProfileRequest.DEFAULT_NAME));

        //Дополнительная проверка: Имя пользователя обновилось в хедере (над username) - здесь баг, раскомментить после фикса
        //new UserDashboard().getUserInfoUsername().shouldHave(text(defaultName));
    }
}

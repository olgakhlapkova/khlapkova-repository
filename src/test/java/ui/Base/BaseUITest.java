package ui.Base;

import api.Base.BaseTest;
import api.configs.Config;
import api.models.CreateUserRequest;
import api.models.UpdateProfileRequest;
import api.specs.RequestSpecs;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import api.requests.steps.AdminSteps;
import api.requests.steps.UserSteps;
import org.junit.jupiter.api.BeforeAll;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.codeborne.selenide.Selenide.executeJavaScript;
import static com.codeborne.selenide.Selenide.refresh;
import static org.assertj.core.api.Assertions.assertThat;

public class BaseUITest extends BaseTest {
    protected static final List<Integer> createdAccountIds = new ArrayList<>();
    protected static final List<Integer> createdUserIds = new ArrayList<>();
    protected static CreateUserRequest currentUserRequest;

    @BeforeAll
    public static void setupSelenoid() {
        Configuration.remote = Config.getProperty("uiRemote");
        Configuration.baseUrl = Config.getProperty("uiBaseUrl");
        Configuration.browser = Config.getProperty("browser");
        Configuration.browserSize = Config.getProperty("browserSize");

        Configuration.browserCapabilities.setCapability("selenoid:options",
                Map.of("enableVNC", true, "enableLog", true)
        );
    }

    // Регистрация созданного аккаунта
    public static void registerAccount(int accountId) {
        if (accountId > 0) {
            createdAccountIds.add(accountId);
        }
    }

    // Регистрация созданного пользователя
    public static void registerUser(CreateUserRequest userRequest) {
        currentUserRequest = userRequest;
    }

    // Регистрация пользователя по ID
    public static void registerUser(int userId) {
        if (userId > 0) {
            createdUserIds.add(userId);
        }
    }

    // Удаление аккаунта из списка для очистки (если уже удален в тесте)
    public static void unregisterAccount(int accountId) {
        createdAccountIds.remove(Integer.valueOf(accountId));
    }

    // Удаление пользователя из списка для очистки (если уже удален в тесте)
    public static void unregisterUser(int userId) {
        createdUserIds.remove(Integer.valueOf(userId));
    }

    // Очистка после каждого теста
    @AfterAll
    public static void cleanup() {
        // 1. Удаляем аккаунты пользователем
        if (currentUserRequest != null) {
            for (int accountId : createdAccountIds) {
                try {
                    UserSteps.deleteAccount(currentUserRequest, accountId);
                    System.out.println("Аккаунт с ID " + accountId + " успешно удален");
                } catch (Exception e) {
                    if (e.getMessage().contains("404") || e.getMessage().contains("Not Found")) {
                        System.out.println("Аккаунт с ID " + accountId + " уже был удален");
                    } else {
                        System.err.println("Не удалось удалить аккаунт с ID " + accountId + ": " + e.getMessage());
                    }
                }
            }
            createdAccountIds.clear();
        }

        // 2. Удаляем пользователей админом
        for (int userId : createdUserIds) {
            try {
                AdminSteps.deleteUserAndVerify(userId);
                System.out.println("Пользователь с ID " + userId + " успешно удален");
            } catch (Exception e) {
                if (e.getMessage().contains("404") || e.getMessage().contains("Not Found")) {
                    System.out.println("Пользователь с ID " + userId + " уже был удален");
                } else {
                    System.err.println("Не удалось удалить пользователя с ID " + userId + ": " + e.getMessage());
                }
            }
        }
        createdUserIds.clear();
    }

    public static void authAsUser(String username, String password) {
        Selenide.open("/");
        String userAuthHeader = RequestSpecs.getUserAuthHeader(username, password);
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", userAuthHeader);
    }

    public static void authAsUser(CreateUserRequest createUserRequest) {
        authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword());
    }

    public void setupUserWithDefaultName(CreateUserRequest user) {
        String defaultName = UpdateProfileRequest.DEFAULT_NAME;
        UserSteps.updateProfile(user, defaultName);

        String currentName = UserSteps.getCurrentName(user);
        assertThat(currentName)
                .as("Имя пользователя должно быть установлено на значение по умолчанию")
                .isEqualTo(defaultName);
    }

    @AfterEach
    public void pageRefresh() {
        refresh();
    }
}

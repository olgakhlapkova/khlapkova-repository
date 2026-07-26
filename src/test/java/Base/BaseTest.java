package Base;

import io.qameta.allure.Step;
import models.CreateUserRequest;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import requests.steps.AdminSteps;
import requests.steps.UserSteps;

import java.util.ArrayList;
import java.util.List;

public class BaseTest {
    protected static final List<Integer> createdAccountIds = new ArrayList<>();
    protected static final List<Integer> createdUserIds = new ArrayList<>();
    protected static CreateUserRequest currentUserRequest;
    protected SoftAssertions softly;

    @BeforeEach
    public void setupTest() {
        this.softly = new SoftAssertions();
    }

    @AfterEach
    public void afterTest() {
        softly.assertAll();
    }

    public static void repeat(int times, Runnable action) {
        for (int i = 0; i < times; i++) {
            action.run();
        }
    }

    public static void registerAccount(int accountId) {
        if (accountId > 0) {
            createdAccountIds.add(accountId);
        }
    }

    public static void registerUser(CreateUserRequest userRequest) {
        currentUserRequest = userRequest;
    }

    public static void registerUser(int userId) {
        createdUserIds.add(userId);
    }

    public static void unregisterUser(int userId) {
        createdUserIds.remove(Integer.valueOf(userId));
    }

    public static void unregisterAccount(int accountId) {
        createdAccountIds.remove(Integer.valueOf(accountId));
    }

    @Step("Очистка после каждого теста")
    @AfterAll
    public static void cleanup() {
        if (currentUserRequest != null) {
            for (int accountId : createdAccountIds) {
                try {
                    UserSteps.deleteAccount(currentUserRequest, accountId);
                    System.out.println("Аккаунт с ID " + accountId + " успешно удален");
                } catch (Exception e) {
                    if (e.getMessage().contains("404")) {
                        System.out.println("Аккаунт с ID " + accountId + " уже был удален");
                    } else {
                        System.err.println("Не удалось удалить аккаунт с ID " + accountId + ": " + e.getMessage());
                    }
                }
            }
            createdAccountIds.clear();
        }

        for (int userId : createdUserIds) {
            try {
                AdminSteps.deleteUserAndVerify(userId);
                System.out.println("Пользователь с ID " + userId + " успешно удален");
            } catch (Exception e) {
                if (e.getMessage().contains("404")) {
                    System.out.println("Пользователь с ID " + userId + " уже был удален");
                } else {
                    System.err.println("Не удалось удалить пользователя с ID " + userId + ": " + e.getMessage());
                }
            }
        }
        createdUserIds.clear();
    }
}

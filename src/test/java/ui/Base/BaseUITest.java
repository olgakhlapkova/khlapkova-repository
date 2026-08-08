package ui.Base;

import models.CreateUserRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import requests.steps.AdminSteps;
import requests.steps.UserSteps;

import java.util.ArrayList;
import java.util.List;

import static com.codeborne.selenide.Selenide.refresh;

public class BaseUITest {
    protected static final List<Integer> createdAccountIds = new ArrayList<>();
    protected static final List<Integer> createdUserIds = new ArrayList<>();
    protected static CreateUserRequest currentUserRequest;

    // Регистрация созданного аккаунта
    protected static void registerAccount(int accountId) {
        if (accountId > 0) {
            createdAccountIds.add(accountId);
        }
    }

    // Регистрация созданного пользователя
    protected static void registerUser(CreateUserRequest userRequest) {
        currentUserRequest = userRequest;
    }

    // Регистрация пользователя по ID
    protected static void registerUser(int userId) {
        if (userId > 0) {
            createdUserIds.add(userId);
        }
    }

    // Удаление аккаунта из списка для очистки (если уже удален в тесте)
    protected static void unregisterAccount(int accountId) {
        createdAccountIds.remove(Integer.valueOf(accountId));
    }

    // Удаление пользователя из списка для очистки (если уже удален в тесте)
    protected static void unregisterUser(int userId) {
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
    @AfterEach
    public void pageRefresh() {
        refresh();
    }
}

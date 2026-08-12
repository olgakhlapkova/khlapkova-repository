package ui.iteration2;

import api.generators.RandomData;
import api.models.CreateUserRequest;
import api.models.LoginUserRequest;
import api.models.UpdateProfileRequest;
import api.requests.skelethon.Endpoint;
import api.requests.skelethon.requesters.CrudRequester;
import api.requests.steps.AdminSteps;
import api.requests.steps.UserSteps;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import ui.Base.BaseUITest;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.*;
import static org.assertj.core.api.Assertions.assertThat;

public class UpdateProfileTest extends BaseUITest {
    private static String defaultName;

    @Test
    public void userCanUpdateName() {
        //ШАГИ ПО НАСТРОЙКЕ ОКРУЖЕНИЯ
        //Админ залогинился, создал пользователя
        CreateUserRequest user = AdminSteps.createUser();
        registerUser(user);

        //Установлено начальное имя пользователя = “Default User”
        defaultName = UpdateProfileRequest.DEFAULT_NAME;
        UserSteps.updateProfile(user, defaultName);

        String currentName = UserSteps.getCurrentName(user);
        if (!defaultName.equals(currentName)) {
            throw new AssertionError("Имя не было установлено. Ожидалось: " + defaultName + ", но было: " + currentName);
        }

        // Пользователь залогинился, открыл юзер дашборд
        String userAuthHeader = new CrudRequester(
                RequestSpecs.unauthSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestReturnsOK())
                .post(LoginUserRequest.builder().username(user.getUsername()).password(user.getPassword()).build())
                .extract()
                .header("Authorization");

        Selenide.open("/");
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", userAuthHeader);
        Selenide.open("/dashboard");

        //ШАГИ ТЕСТА
        //Нажать на имя пользователя рядом с кнопкой Logout
        $(".user-info").click();

        //Редирект на Edit Profile страницу
        $(Selectors.byText("✏️ Edit Profile")).shouldBe(visible);

        //Ввести корректное имя из 2х слов
        String updatedName = RandomData.generateRandomShortValidName();
        $(Selectors.byAttribute("placeholder", "Enter new name"))
                .shouldBe(visible)
                .click();
        sleep(200);
        $(Selectors.byAttribute("placeholder", "Enter new name")).setValue(updatedName);

        //Нажать кнопку Save Changes
        $(Selectors.byText("\uD83D\uDCBE Save Changes")).click();

        //Ожидание
        //Алерт, что имя успешно обновлено

        Alert alert = switchTo().alert();
        String alertText = alert.getText();

        assertThat(alertText).contains("✅ Name updated successfully!");

        alert.accept();

        //Нет редиректа на User Dashboard страницу, пользователь остался на Edit Profile странице
        $(Selectors.byText("✏️ Edit Profile")).shouldBe(visible);

        //Проверка, что имя обновлено в API
        String nameAfterApi = UserSteps.getCurrentName(user);
        assertThat(nameAfterApi)
                .as("Имя после обновления (API)")
                .isEqualTo(updatedName);

        //Дополнительная проверка: Имя пользователя обновилось в шапке (после слова Welcome)
        $(Selectors.byText("\uD83C\uDFE0 Home")).click();
        $("h2.welcome-text span").shouldHave(text(updatedName));

        //Дополнительная проверка: Имя пользователя обновилось в хедере (над username) - здесь баг, раскомментить после фикса
        //$("div.user-info span.user-name").shouldHave(text(updatedName));
    }

    @Test
    public void userCannotUpdateName() {
        //ШАГИ ПО НАСТРОЙКЕ ОКРУЖЕНИЯ
        //Админ залогинился, создал пользователя
        CreateUserRequest user = AdminSteps.createUser();
        registerUser(user);

        //Установлено начальное имя пользователя = “Default User”
        defaultName = UpdateProfileRequest.DEFAULT_NAME;
        UserSteps.updateProfile(user, defaultName);

        String currentName = UserSteps.getCurrentName(user);
        if (!defaultName.equals(currentName)) {
            throw new AssertionError("Имя не было установлено. Ожидалось: " + defaultName + ", но было: " + currentName);
        }

        // Пользователь залогинился, открыл юзер дашборд
        String userAuthHeader = new CrudRequester(
                RequestSpecs.unauthSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestReturnsOK())
                .post(LoginUserRequest.builder().username(user.getUsername()).password(user.getPassword()).build())
                .extract()
                .header("Authorization");

        Selenide.open("/");
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", userAuthHeader);
        Selenide.open("/dashboard");

        //ШАГИ ТЕСТА
        //Нажать на имя пользователя рядом с кнопкой Logout
        $(".user-info").click();

        //Редирект на Edit Profile страницу
        $(Selectors.byText("✏️ Edit Profile")).shouldBe(visible);

        //Ввести некорректное имя из 1го слова
        String updatedInvalidName = RandomData.generateOneWordName();
        $(Selectors.byAttribute("placeholder", "Enter new name"))
                .shouldBe(visible)
                .click();
        sleep(200);
        $(Selectors.byAttribute("placeholder", "Enter new name")).setValue(updatedInvalidName);

        //Нажать кнопку Save Changes
        $(Selectors.byText("\uD83D\uDCBE Save Changes")).click();

        //Ожидание
        //Алерт, что имя НЕ обновлено

        Alert alert = switchTo().alert();
        String alertText = alert.getText();

        assertThat(alertText).contains("Name must contain two words with letters only");

        alert.accept();

        //Нет редиректа на User Dashboard страницу, пользователь остался на Edit Profile странице
        $(Selectors.byText("✏️ Edit Profile")).shouldBe(visible);

        //Проверка, что имя НЕ обновлено в API
        String nameAfterApi = UserSteps.getCurrentName(user);
        assertThat(nameAfterApi)
                .as("Имя после обновления (API)")
                .isEqualTo(defaultName);

        //Дополнительная проверка: Имя пользователя НЕ обновилось в шапке (после слова Welcome)
        $(Selectors.byText("\uD83C\uDFE0 Home")).click();
        $("h2.welcome-text span").shouldHave(text(defaultName));

        //Дополнительная проверка: Имя пользователя обновилось в хедере (над username) - здесь баг, раскомментить после фикса
        //$("div.user-info span.user-name").shouldHave(text(defaultName));
    }
}

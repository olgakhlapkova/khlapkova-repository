package ui.iteration2;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import api.models.CreateUserRequest;
import api.models.LoginUserRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import api.requests.skelethon.Endpoint;
import api.requests.skelethon.requesters.CrudRequester;
import api.requests.steps.AdminSteps;
import api.requests.steps.UserSteps;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import ui.Base.BaseUITest;

import java.util.Map;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.within;

public class UserDepositTest extends BaseUITest {
    @BeforeAll
    public static void setupSelenoid() {
        Configuration.remote = "http://localhost:4444/wd/hub";
        Configuration.baseUrl = "http://192.168.100.7:3000";
        Configuration.browser = "chrome";
        Configuration.browserSize = "1920x1080";

        Configuration.browserCapabilities.setCapability("selenoid:options",
                Map.of("enableVNC", true, "enableLog", true)
        );
    }

    @Test
    public void userCanAddDepositWithValidValue() {
        //ШАГИ ПО НАСТРОЙКЕ ОКРУЖЕНИЯ
        //Админ залогинился, создал пользователя
        CreateUserRequest user = AdminSteps.createUser();
        registerUser(user);

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

        // Пользователь создал аккаунт
        int accountId = UserSteps.createAccount(user);
        registerAccount(accountId);

        double balanceBefore = UserSteps.getBalance(user, accountId);

        //ШАГИ ТЕСТА
        //Нажать Deposit Money -> редирект на Deposit Money страницу
        $(Selectors.byText("\uD83D\uDCB0 Deposit Money")).click();

        $(Selectors.byText("\uD83D\uDCB0 Deposit Money")).shouldBe(visible);

        //Выбрать аккаунт
        $("select.form-control.account-selector").click();
        $("select.form-control.account-selector option[value='" + accountId + "']").shouldBe(visible).click();

        //Ввести корректную сумму
        $(Selectors.byAttribute("placeholder", "Enter amount")).setValue("1000");

        //Нажать кнопку Deposit
        $(Selectors.byText("\uD83D\uDCB5 Deposit")).click();

        //ОЖИДАНИЕ
        //Алерт, что депозит успешно добавлен
        Alert alert = switchTo().alert();
        String alertText = alert.getText();

        assertThat(alertText).contains("✅ Successfully deposited $1000 to account ACC" + accountId + "!");

        alert.accept();

        //Редирект на User Dashboard страницу
        $(Selectors.byText("User Dashboard")).shouldBe(Condition.visible);

        //Проверка, что депозит добавлен в API
        //проверка баланса
        double balanceAfter = UserSteps.getBalance(user, accountId);
        assertThat(balanceAfter)
                .as("Баланс после депозита")
                .isEqualTo(balanceBefore + 1000, within(0.01));


        //Проверка, что транзакция Deposit есть в списке Matching Transactions на странице http://localhost:3000/transfer
        Selenide.open("/transfer");
        $(Selectors.byText("\uD83D\uDD01 Transfer Again")).click();
        $(Selectors.byText("Matching Transactions")).shouldBe(Condition.visible);
        $(Selectors.byText("DEPOSIT")).shouldBe(Condition.visible);
    }

    @Test
    public void userCanNotAddDepositWithValidValue() {
        //ШАГИ ПО НАСТРОЙКЕ ОКРУЖЕНИЯ
        //Админ залогинился, создал пользователя
        CreateUserRequest user = AdminSteps.createUser();
        registerUser(user);

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

        // Пользователь создал аккаунт
        int accountId = UserSteps.createAccount(user);

        double balanceBefore = UserSteps.getBalance(user, accountId);

        //ШАГИ ТЕСТА
        //Нажать Deposit Money -> редирект на Deposit Money страницу
        $(Selectors.byText("\uD83D\uDCB0 Deposit Money")).click();

        $(Selectors.byText("\uD83D\uDCB0 Deposit Money")).shouldBe(visible);

        //Выбрать аккаунт
        $("select.form-control.account-selector").click();
        $("select.form-control.account-selector option[value='" + accountId + "']").shouldBe(visible).click();

        //Ввести некорректную сумму
        $(Selectors.byAttribute("placeholder", "Enter amount")).setValue("6000");

        //Нажать кнопку Deposit
        $(Selectors.byText("\uD83D\uDCB5 Deposit")).click();

        //ОЖИДАНИЕ
        //Алерт с ошибкой
        Alert alert = switchTo().alert();
        String alertText = alert.getText();

        assertThat(alertText).contains("❌ Please deposit less or equal to 5000$.");

        alert.accept();

        //Пользователь остается на странице Deposit Money
        $(Selectors.byText("\uD83D\uDCB0 Deposit Money")).shouldBe(Condition.visible);

        //Проверка, что депозит НЕ добавлен в API
        //проверка баланса
        double balanceAfter = UserSteps.getBalance(user, accountId);
        assertThat(balanceAfter)
                .as("Баланс после депозита")
                .isEqualTo(balanceBefore);
    }
}

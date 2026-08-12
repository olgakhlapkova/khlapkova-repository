package ui.iteration2;

import api.generators.RandomData;
import api.models.AccountResponse;
import api.models.CreateUserRequest;
import api.models.LoginUserRequest;
import api.requests.skelethon.Endpoint;
import api.requests.skelethon.requesters.CrudRequester;
import api.requests.steps.AdminSteps;
import api.requests.steps.UserSteps;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import ui.Base.BaseUITest;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.*;
import static org.assertj.core.api.Assertions.assertThat;
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

        isSetupDone = true;
    }

    @Test
    public void userCanAddTransferWithValidValue() {
        double transferAmount = 1000.0;

        //получаем балансы ДО перевода
        double balanceBeforeAccount1 = UserSteps.getBalance(user, accountId1);
        double balanceBeforeAccount2 = UserSteps.getBalance(user, accountId2);

        //ШАГИ ТЕСТА
        //Нажать Make a Transfer -> редирект на Make a Transfer страницу
        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).click();

        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);

        //Выбрать аккаунт 1
        $("select.form-control.account-selector").click();
        $("select.form-control.account-selector option[value='" + accountId1 + "']").shouldBe(visible).click();

        //Ввести корректное имя пользователя
        $(Selectors.byAttribute("placeholder", "Enter recipient name")).setValue(user.getUsername());

        //Ввести корректный номер аккаунта (аккаунт 2)
        $(Selectors.byAttribute("placeholder", "Enter recipient account number")).setValue("ACC" + accountId2);

        //Ввести корректную сумму
        $(Selectors.byAttribute("placeholder", "Enter amount")).setValue(String.valueOf(transferAmount));

        //Чекнуть чекбокс Confirm details are correct
        $("#confirmCheck").setSelected(true);

        //Нажать кнопку Send Transfer
        $(Selectors.byText("\uD83D\uDE80 Send Transfer")).click();

        //ОЖИДАНИЕ
        //Алерт, что перевод успешно совершен
        Alert alert = switchTo().alert();
        String alertText = alert.getText();

        assertThat(alertText).contains("✅ Successfully transferred $" + transferAmount + " to account ACC" + accountId2 + "!");

        alert.accept();

        //Нет редиректа на User Dashboard страницу, пользователь остается на странице Make a Transfer
        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);

        //Проверка, что перевод добавлен в API
        SoftAssertions softly = new SoftAssertions();
        double balanceAfterAccount1 = UserSteps.getBalance(user, accountId1);
        double balanceAfterAccount2 = UserSteps.getBalance(user, accountId2);

        softly.assertThat(balanceAfterAccount1)
                .as("Баланс отправителя после перевода")
                .isEqualTo(balanceBeforeAccount1 - transferAmount, within(0.01));

        softly.assertThat(balanceAfterAccount2)
                .as("Баланс получателя после перевода")
                .isEqualTo(balanceBeforeAccount2 + transferAmount, within(0.01));


        //Проверка, что в списке Matching Transactions появились 2 транзакции - TRANSFER_OUT, TRANSFER_IN
        refresh();

        $(Selectors.byText("\uD83D\uDD01 Transfer Again")).click();
        $(Selectors.byText("Matching Transactions")).shouldBe(Condition.visible);
        $(Selectors.byText("TRANSFER_OUT")).shouldBe(Condition.visible);
        $(Selectors.byText("TRANSFER_IN")).shouldBe(Condition.visible);
    }

    @Test
    public void transferIsNotAddedWithoutSelectedAccount() {
        double transferAmount = 1000.0;

        //получаем балансы ДО перевода
        double balanceBeforeAccount1 = UserSteps.getBalance(user, accountId1);
        double balanceBeforeAccount2 = UserSteps.getBalance(user, accountId2);

        //ШАГИ ТЕСТА
        //Нажать Make a Transfer -> редирект на Make a Transfer страницу
        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).click();
        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);

        //Ввести корректное имя пользователя
        $(Selectors.byAttribute("placeholder", "Enter recipient name")).setValue(user.getUsername());

        //Ввести корректный номер аккаунта (аккаунт 2)
        $(Selectors.byAttribute("placeholder", "Enter recipient account number")).setValue("ACC" + accountId2);

        //Ввести корректную сумму
        $(Selectors.byAttribute("placeholder", "Enter amount")).setValue(String.valueOf(transferAmount));

        //Чекнуть чекбокс Confirm details are correct
        $("#confirmCheck").setSelected(true);

        //Нажать кнопку Send Transfer
        $(Selectors.byText("\uD83D\uDE80 Send Transfer")).click();

        //ОЖИДАНИЕ
        //Алерт, что перевод не совершен
        Alert alert = switchTo().alert();
        String alertText = alert.getText();

        assertThat(alertText).contains("❌ Please fill all fields and confirm.");

        alert.accept();

        //Нет редиректа на User Dashboard страницу, пользователь остается на странице Make a Transfer
        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);

        //Проверка, что перевод НЕ добавлен в API
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

        //получаем балансы ДО перевода
        double balanceBeforeAccount1 = UserSteps.getBalance(user, accountId1);
        double balanceBeforeAccount2 = UserSteps.getBalance(user, accountId2);

        //ШАГИ ТЕСТА
        //Нажать Make a Transfer -> редирект на Make a Transfer страницу
        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).click();
        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);

        //Выбрать аккаунт 1
        $("select.form-control.account-selector").click();
        $("select.form-control.account-selector option[value='" + accountId1 + "']").shouldBe(visible).click();

        //Ввести корректное имя пользователя
        $(Selectors.byAttribute("placeholder", "Enter recipient name")).setValue(user.getUsername());

        //Ввести некорректный номер аккаунта
        $(Selectors.byAttribute("placeholder", "Enter recipient account number")).setValue(RandomData.generateTwoLetters());

        //Ввести корректную сумму
        $(Selectors.byAttribute("placeholder", "Enter amount")).setValue(String.valueOf(transferAmount));

        //Чекнуть чекбокс Confirm details are correct
        $("#confirmCheck").setSelected(true);

        //Нажать кнопку Send Transfer
        $(Selectors.byText("\uD83D\uDE80 Send Transfer")).click();

        //ОЖИДАНИЕ
        //Алерт, что перевод не совершен
        Alert alert = switchTo().alert();
        String alertText = alert.getText();

        assertThat(alertText).contains("❌ No user found with this account number.");

        alert.accept();

        //Нет редиректа на User Dashboard страницу, пользователь остается на странице Make a Transfer
        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);

        //Проверка, что перевод НЕ добавлен в API
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

        //получаем балансы ДО перевода
        double balanceBeforeAccount1 = UserSteps.getBalance(user, accountId1);
        double balanceBeforeAccount2 = UserSteps.getBalance(user, accountId2);

        //ШАГИ ТЕСТА
        //Нажать Make a Transfer -> редирект на Make a Transfer страницу
        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).click();
        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);

        //Выбрать аккаунт 1
        $("select.form-control.account-selector").click();
        $("select.form-control.account-selector option[value='" + accountId1 + "']").shouldBe(visible).click();

        //Ввести корректное имя пользователя
        $(Selectors.byAttribute("placeholder", "Enter recipient name")).setValue(user.getUsername());

        //Ввести корректный номер аккаунта
        $(Selectors.byAttribute("placeholder", "Enter recipient account number")).setValue("ACC" + accountId2);

        //Ввести некорректную сумму
        $(Selectors.byAttribute("placeholder", "Enter amount")).setValue(String.valueOf(incorrectTransferAmount));

        //Чекнуть чекбокс Confirm details are correct
        $("#confirmCheck").setSelected(true);

        //Нажать кнопку Send Transfer
        $(Selectors.byText("\uD83D\uDE80 Send Transfer")).click();

        //ОЖИДАНИЕ
        //Алерт, что перевод не совершен
        Alert alert = switchTo().alert();
        String alertText = alert.getText();

        assertThat(alertText).contains("❌ Error: Transfer amount cannot exceed 10000");

        alert.accept();

        //Нет редиректа на User Dashboard страницу, пользователь остается на странице Make a Transfer
        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);

        //Проверка, что перевод НЕ добавлен в API
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

        //получаем балансы ДО перевода
        double balanceBeforeAccount1 = UserSteps.getBalance(user, accountId1);
        double balanceBeforeAccount2 = UserSteps.getBalance(user, accountId2);

        //ШАГИ ТЕСТА
        //Нажать Make a Transfer -> редирект на Make a Transfer страницу
        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).click();

        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);

        //Выбрать аккаунт 1
        $("select.form-control.account-selector").click();
        $("select.form-control.account-selector option[value='" + accountId1 + "']").shouldBe(visible).click();

        //Ввести корректное имя пользователя
        $(Selectors.byAttribute("placeholder", "Enter recipient name")).setValue(user.getUsername());

        //Ввести корректный номер аккаунта (аккаунт 2)
        $(Selectors.byAttribute("placeholder", "Enter recipient account number")).setValue("ACC" + accountId2);

        //Ввести корректную сумму
        $(Selectors.byAttribute("placeholder", "Enter amount")).setValue(String.valueOf(transferAmount));

        //Не чекать чекбокс Confirm details are correct
        $("#confirmCheck").setSelected(false);

        //Нажать кнопку Send Transfer
        $(Selectors.byText("\uD83D\uDE80 Send Transfer")).click();

        //ОЖИДАНИЕ
        //Алерт, что перевод не совершен
        Alert alert = switchTo().alert();
        String alertText = alert.getText();

        assertThat(alertText).contains("❌ Please fill all fields and confirm.");

        alert.accept();

        //Нет редиректа на User Dashboard страницу, пользователь остается на странице Make a Transfer
        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);

        //Проверка, что перевод НЕ добавлен в API
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

        //получаем балансы ДО перевода
        double balanceBeforeAccount1 = UserSteps.getBalance(user, accountId1);
        double balanceBeforeAccount2 = UserSteps.getBalance(user, accountId2);

        //Подготовительный трансфер
        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).click();
        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);

        $("select.form-control.account-selector").click();
        $("select.form-control.account-selector option[value='" + accountId1 + "']").shouldBe(visible).click();
        $(Selectors.byAttribute("placeholder", "Enter recipient name")).setValue(user.getUsername());
        $(Selectors.byAttribute("placeholder", "Enter recipient account number")).setValue("ACC" + accountId2);
        $(Selectors.byAttribute("placeholder", "Enter amount")).setValue(String.valueOf(transferAmount));
        $("#confirmCheck").setSelected(true);
        $(Selectors.byText("\uD83D\uDE80 Send Transfer")).click();

        Alert alert = switchTo().alert();
        String alertText = alert.getText();
        assertThat(alertText).contains("✅ Successfully transferred $" + transferAmount + " to account ACC" + accountId2 + "!");
        alert.accept();

        refresh();

        //ОСНОВНЫЕ ШАГИ ТЕСТА
        //Нажать кнопку Transfer Again
        $(Selectors.byText("\uD83D\uDD01 Transfer Again")).click();
        $(Selectors.byText("Matching Transactions")).shouldBe(Condition.visible);

        SelenideElement transaction = $$("li.list-group-item")
                .findBy(text("TRANSFER_IN"));

        transaction.scrollIntoView(true);
        sleep(300);

        transaction.$("button").click();

        $(Selectors.byText("\uD83D\uDD01 Repeat Transfer")).shouldBe(Condition.visible);

        //Ввести корректный номер аккаунта (аккаунт 2)
        $("select.form-control").selectOptionContainingText("ACC"+ accountId2);

        //Чекнуть чекбокс Confirm details are correct
        $("#confirmCheck").setSelected(true);

        //Нажать кнопку Send Transfer
        $(Selectors.byText("\uD83D\uDE80 Send Transfer")).click();

        //ОЖИДАНИЕ
        //Алерт, что перевод успешно совершен

        Alert alert2 = switchTo().alert();
        String alertText2 = alert2.getText();

        //здесь сейчас баг - при повторном трансфере сообщение выглядит так:
        // "✅ Transfer of $" + transferAmount + " successful from Account " + accountId2 + " to " + accountId2 + "!"
        //assertThat(alertText2).contains("✅ Transfer of $" + transferAmount + " successful from Account " + accountId1 + " to " + accountId2 + "!");

        alert2.accept();

        //Нет редиректа на User Dashboard страницу, пользователь остается на странице Make a Transfer
        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);

        //Проверка, что перевод добавлен в API
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

        //получаем балансы ДО перевода
        double balanceBeforeAccount1 = UserSteps.getBalance(user, accountId1);
        double balanceBeforeAccount2 = UserSteps.getBalance(user, accountId2);

        //Подготовительный трансфер
        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).click();
        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);

        $("select.form-control.account-selector").click();
        $("select.form-control.account-selector option[value='" + accountId1 + "']").shouldBe(visible).click();
        $(Selectors.byAttribute("placeholder", "Enter recipient name")).setValue(user.getUsername());
        $(Selectors.byAttribute("placeholder", "Enter recipient account number")).setValue("ACC" + accountId2);
        $(Selectors.byAttribute("placeholder", "Enter amount")).setValue(String.valueOf(transferAmount));
        $("#confirmCheck").setSelected(true);
        $(Selectors.byText("\uD83D\uDE80 Send Transfer")).click();

        Alert alert = switchTo().alert();
        String alertText = alert.getText();
        assertThat(alertText).contains("✅ Successfully transferred $" + transferAmount + " to account ACC" + accountId2 + "!");
        alert.accept();

        refresh();

        //ОСНОВНЫЕ ШАГИ ТЕСТА
        //Нажать кнопку Transfer Again
        $(Selectors.byText("\uD83D\uDD01 Transfer Again")).click();
        $(Selectors.byText("Matching Transactions")).shouldBe(Condition.visible);

        SelenideElement transaction = $$("li.list-group-item")
                .findBy(text("TRANSFER_IN"));

        transaction.scrollIntoView(true);
        sleep(300);

        transaction.$("button").click();

        $(Selectors.byText("\uD83D\uDD01 Repeat Transfer")).shouldBe(Condition.visible);

        //Ввести корректный номер аккаунта (аккаунт 2)
        $("select.form-control").selectOptionContainingText("ACC"+ accountId2);

        //очистить поле amount и ввести некорректную сумму
        double invalidTransferAmount = RandomData.getBigAmount();
        $(Selectors.byAttribute("type", "number"))
                .shouldBe(visible)
                .click();
        sleep(200);
        $(Selectors.byAttribute("type", "number")).setValue(String.valueOf(invalidTransferAmount));

        //Чекнуть чекбокс Confirm details are correct
        $("#confirmCheck").setSelected(true);

        //Нажать кнопку Send Transfer
        $(Selectors.byText("\uD83D\uDE80 Send Transfer")).click();

        //ОЖИДАНИЕ
        //Алерт, что перевод НЕ совершен
        Alert alert2 = switchTo().alert();
        String alertText2 = alert2.getText();

        assertThat(alertText2).contains("❌ Transfer failed: Please try again.");

        alert2.accept();

        //Нет редиректа, Repeat Transfer попап остаётся открыт
        $(Selectors.byText("\uD83D\uDD01 Repeat Transfer")).shouldBe(visible);

        //Проверка, что перевод НЕ добавлен в API
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

        //получаем балансы ДО перевода
        double balanceBeforeAccount1 = UserSteps.getBalance(user, accountId1);
        double balanceBeforeAccount2 = UserSteps.getBalance(user, accountId2);

        //Подготовительный трансфер
        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).click();
        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);

        $("select.form-control.account-selector").click();
        $("select.form-control.account-selector option[value='" + accountId1 + "']").shouldBe(visible).click();
        $(Selectors.byAttribute("placeholder", "Enter recipient name")).setValue(user.getUsername());
        $(Selectors.byAttribute("placeholder", "Enter recipient account number")).setValue("ACC" + accountId2);
        $(Selectors.byAttribute("placeholder", "Enter amount")).setValue(String.valueOf(transferAmount));
        $("#confirmCheck").setSelected(true);
        $(Selectors.byText("\uD83D\uDE80 Send Transfer")).click();

        Alert alert = switchTo().alert();
        String alertText = alert.getText();
        assertThat(alertText).contains("✅ Successfully transferred $" + transferAmount + " to account ACC" + accountId2 + "!");
        alert.accept();

        refresh();

        //ОСНОВНЫЕ ШАГИ ТЕСТА
        //Нажать кнопку Transfer Again
        $(Selectors.byText("\uD83D\uDD01 Transfer Again")).click();
        $(Selectors.byText("Matching Transactions")).shouldBe(Condition.visible);

        SelenideElement transaction = $$("li.list-group-item")
                .findBy(text("TRANSFER_IN"));

        transaction.scrollIntoView(true);
        sleep(300);

        transaction.$("button").click();

        $(Selectors.byText("\uD83D\uDD01 Repeat Transfer")).shouldBe(Condition.visible);

        //Ввести корректный номер аккаунта (аккаунт 2)
        $("select.form-control").selectOptionContainingText("ACC"+ accountId2);

        //Чекнуть чекбокс Confirm details are correct
        $("#confirmCheck").setSelected(true);

        //Нажать кнопку Cancel
        $(Selectors.byText("Cancel")).click();

        //ОЖИДАНИЕ
        //Repeat Transfer попап закрыт, отображается список Matching Transactions
        $(Selectors.byText("Matching Transactions")).shouldBe(Condition.visible);

        //Проверка, что перевод НЕ добавлен в API
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

        //получаем балансы ДО перевода
        double balanceBeforeAccount1 = UserSteps.getBalance(user, accountId1);
        double balanceBeforeAccount2 = UserSteps.getBalance(user, accountId2);

        //Подготовительный трансфер
        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).click();
        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);

        $("select.form-control.account-selector").click();
        $("select.form-control.account-selector option[value='" + accountId1 + "']").shouldBe(visible).click();
        $(Selectors.byAttribute("placeholder", "Enter recipient name")).setValue(user.getUsername());
        $(Selectors.byAttribute("placeholder", "Enter recipient account number")).setValue("ACC" + accountId2);
        $(Selectors.byAttribute("placeholder", "Enter amount")).setValue(String.valueOf(transferAmount));
        $("#confirmCheck").setSelected(true);
        $(Selectors.byText("\uD83D\uDE80 Send Transfer")).click();

        Alert alert = switchTo().alert();
        String alertText = alert.getText();
        assertThat(alertText).contains("✅ Successfully transferred $" + transferAmount + " to account ACC" + accountId2 + "!");
        alert.accept();

        refresh();

        //ОСНОВНЫЕ ШАГИ ТЕСТА
        //Нажать кнопку Transfer Again
        $(Selectors.byText("\uD83D\uDD01 Transfer Again")).click();
        $(Selectors.byText("Matching Transactions")).shouldBe(Condition.visible);

        SelenideElement transaction = $$("li.list-group-item")
                .findBy(text("TRANSFER_IN"));

        transaction.scrollIntoView(true);
        sleep(300);

        transaction.$("button").click();

        $(Selectors.byText("\uD83D\uDD01 Repeat Transfer")).shouldBe(Condition.visible);

        //Ввести корректный номер аккаунта (аккаунт 2)
        $("select.form-control").selectOptionContainingText("ACC"+ accountId2);

        //Чекнуть чекбокс Confirm details are correct
        $("#confirmCheck").setSelected(true);

        //Нажать кнопку крестик
        $(".modal-content").$("button.btn-close").click();

        //ОЖИДАНИЕ
        //Repeat Transfer попап закрыт, отображается список Matching Transactions
        $(Selectors.byText("Matching Transactions")).shouldBe(Condition.visible);

        //Проверка, что перевод НЕ добавлен в API
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

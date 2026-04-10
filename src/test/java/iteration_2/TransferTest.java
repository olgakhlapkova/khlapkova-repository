package iteration_2;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

public class TransferTest {
    private static String userAuthHeader;
    private static int accountId1;
    private static int accountId2;
    private static int accountId3;
    private static boolean isSetupDone = false;

    @BeforeAll
    public static void setupRestAssured() {
        RestAssured.filters(
                List.of(new RequestLoggingFilter(),
                        new ResponseLoggingFilter()));
    }

    @BeforeAll
    public static void testSetup() {
        if (isSetupDone) return;
        //создаем юзера
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                        {
                           "username": "kate2005",
                           "password": "Kate2000#!",
                           "role": "USER"
                        }
                        """)
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED);

        //логинимся и получаем токен
        userAuthHeader = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                           "username": "kate2005",
                           "password": "Kate2000#!"
                        }
                        """)
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("Authorization");

        //создаем 2 аккаунта
        accountId1 = createAccount();
        accountId2 = createAccount();

        // добавляем депозиты на оба аккаунта (account1 = 25000, account2 = 20000)
        for (int i = 0; i < 5; i++) {
            addDeposit(accountId1, 5000.0);
        }
        for (int i = 0; i < 4; i++) {
            addDeposit(accountId2, 5000.0);
        }

        isSetupDone = true;
    }

    private static int createAccount() {
        return given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .jsonPath()
                .getInt("id");
    }

    private static void addDeposit(int accountId, double amount) {
        given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(String.format("""
                        {
                            "id": %d,
                            "balance": %.2f
                        }
                        """, accountId, amount))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    public static Stream<Arguments> transferValidData() {
        return Stream.of(
                // позитивный
                Arguments.of(accountId1, accountId2, 1000),
                // граничные значения
                Arguments.of(accountId1, accountId2, 0.01),
                Arguments.of(accountId1, accountId2, 9999.99),
                Arguments.of(accountId1, accountId2, 10000),
                // трансфер в обратную сторону
                Arguments.of(accountId2, accountId1, 100));
    }

    @MethodSource("transferValidData")
    @ParameterizedTest
    public void userCanAddTransferWithValidValue(int senderAccountId, int receiverAccountId, double transferAmount) {
        // получаем балансы до трансфера
        double balanceBefore1 = getBalance(senderAccountId);
        double balanceBefore2 = getBalance(receiverAccountId);

        // выполняем трансфер
        given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(String.format("""
                        {
                          "senderAccountId": %d,
                          "receiverAccountId": %d,
                          "amount": %.2f
                        }
                        """, senderAccountId, receiverAccountId, transferAmount))
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        // Проверяем балансы после трансфера
        double balanceAfter1 = getBalance(senderAccountId);
        double balanceAfter2 = getBalance(receiverAccountId);

        Assertions.assertEquals(balanceBefore1 - transferAmount, balanceAfter1, 0.01);
        Assertions.assertEquals(balanceBefore2 + transferAmount, balanceAfter2, 0.01);
    }

    private double getBalance(int accountId) {
        return given()
                .header("Authorization", userAuthHeader)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .extract()
                .jsonPath()
                .getDouble(String.format("find { it.id == %d }.balance", accountId));
    }

    public static Stream<Arguments> transferInvalidData() {
        return Stream.of(
                // негативные
                // неверная сумма
                Arguments.of(accountId1, accountId2, 0, "Transfer amount must be at least 0.01"),
                Arguments.of(accountId1, accountId2, -100, "Transfer amount must be at least 0.01"),
                // несуществующий аккаунт
                Arguments.of(accountId1, 0, 100, "Invalid transfer: insufficient funds or invalid accounts"),
                // граничные значения
                Arguments.of(accountId1, accountId2, 10000.01, "Transfer amount cannot exceed 10000"));
    }

    @MethodSource("transferInvalidData")
    @ParameterizedTest
    public void userCannotAddTransferWithInvalidValue(int senderAccountId, int receiverAccountId, double transferAmount, String errorValue) {
        given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(String.format("""
                        {
                          "senderAccountId": %d,
                          "receiverAccountId": %d,
                          "amount": %.2f
                        }
                        """, senderAccountId, receiverAccountId, transferAmount))
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(Matchers.equalTo(errorValue));
    }

    @Test
    public void userCannotTransferWithSumMoreThanUserBalanceTest() {
        //создаем новый аккаунт у текущего пользователя
        accountId3 = createAccount();

        // добавляем депозит на новый аккаунт
        addDeposit(accountId3, 1000.0);

        // делаем трансфер
        given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(String.format("""
                        {
                          "senderAccountId": %d,
                          "receiverAccountId": %d,
                          "amount": %.2f
                        }
                        """, accountId3, accountId2, 5000.0))
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(Matchers.equalTo("Invalid transfer: insufficient funds or invalid accounts"));
    }
}

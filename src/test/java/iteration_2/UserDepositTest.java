package iteration_2;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

public class UserDepositTest {
    private static String userAuthHeader;
    private static int testAccountId;
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
                           "username": "kate20656",
                           "password": "Kate2000#!",
                           "role": "USER"
                        }
                        """)
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED);

        // Логинимся и получаем токен
        userAuthHeader = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                           "username": "kate20656",
                           "password": "Kate2000#!"
                        }
                        """)
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("Authorization");

        isSetupDone = true;
    }

    @BeforeEach
    public void createNewAccount() {
        testAccountId = createAccount();
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

    private static double getBalance(int accountId) {
        Double balance = given()
                .header("Authorization", userAuthHeader)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .extract()
                .jsonPath()
                .getDouble(String.format("find { it.id == %d }.balance", accountId));

        if (balance == null) {
            throw new AssertionError("Аккаунт с ID " + accountId + " не найден");
        }
        return balance;
    }

    public static Stream<Arguments> depositValidData() {
        return Stream.of(
                // позитивный
                Arguments.of(1000.0),
                // граничные значения
                Arguments.of(0.01),
                Arguments.of(4999.99),
                Arguments.of(5000.0));
    }

    @MethodSource("depositValidData")
    @ParameterizedTest
    public void userCanAddDepositWithValidValue(double depositAmount) {
        // получаем баланс ДО депозита
        double balanceBefore = getBalance(testAccountId);

        given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(String.format("""
                        {
                          "id": %d,
                          "balance": %f
                        }
                        """, testAccountId, depositAmount))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", Matchers.equalTo(testAccountId))
                .body("balance", Matchers.equalTo((float)(balanceBefore + depositAmount)))
                .body("transactions.amount", Matchers.hasItem((float)depositAmount))
                //проверяем ID последней транзакции
                .body("transactions[-1].id", Matchers.notNullValue())
                //проверяем тип последней транзакции
                .body("transactions[-1].type", Matchers.equalTo("DEPOSIT"));

        // проверяем, что баланс увеличился
        double balanceAfter = getBalance(testAccountId);
        Assertions.assertEquals(balanceBefore + depositAmount, balanceAfter, 0.01,
                "Баланс должен увеличиться на " + depositAmount);
    }

    public static Stream<Arguments> depositInvalidData() {
        return Stream.of(
                // негативные
                Arguments.of(6000.0, "Deposit amount cannot exceed 5000"),
                Arguments.of(-100.0, "Deposit amount must be at least 0.01"),
                // граничные значения
                Arguments.of(5000.01, "Deposit amount cannot exceed 5000"),
                Arguments.of(0.0, "Deposit amount must be at least 0.01"));
    }

    @MethodSource("depositInvalidData")
    @ParameterizedTest
    public void userCannotAddDepositWithInvalidValue(double depositAmount, String errorValue) {
        // получаем баланс ДО попытки депозита
        double balanceBefore = getBalance(testAccountId);

        given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(String.format("""
                        {
                          "id": %d,
                          "balance": %f
                        }
                        """, testAccountId, depositAmount))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(Matchers.equalTo(errorValue));

        // проверяем, что баланс НЕ ИЗМЕНИЛСЯ
        double balanceAfter = getBalance(testAccountId);
        Assertions.assertEquals(balanceBefore, balanceAfter, 0.01,
                "Баланс не должен измениться при попытке депозита с невалидной суммой: " + depositAmount);
    }

    @Test
    public void userCannotAddDepositToDifferentAccount() {
        // получаем баланс ДО попытки депозита
        double balanceBefore = getBalance(testAccountId);

        given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "id": 2,
                          "balance": 100
                        }
                        """)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_FORBIDDEN)
                .body(Matchers.equalTo("Unauthorized access to account"));

        // проверяем, что баланс НЕ ИЗМЕНИЛСЯ
        double balanceAfter = getBalance(testAccountId);
        Assertions.assertEquals(balanceBefore, balanceAfter, 0.01,
                "Баланс не должен измениться при попытке депозита на чужой аккаунт");
    }
}

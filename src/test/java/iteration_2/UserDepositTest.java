package iteration_2;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

public class UserDepositTest {
    private static String userAuthHeader;

    @BeforeAll
    public static void setupRestAssured() {
        RestAssured.filters(
                List.of(new RequestLoggingFilter(),
                        new ResponseLoggingFilter()));
    }

    @BeforeAll
    public static void getUserAuthToken() {
        userAuthHeader = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                           "username": "kate20002",
                           "password": "Kate2000#"
                        }
                        """)
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("Authorization");
    }

    public static Stream<Arguments> depositValidData() {
        return Stream.of(
                // позитивный
                Arguments.of(1, 1000),
                // граничные значения
                Arguments.of(1, 0.01),
                Arguments.of(1, 4999.99),
                Arguments.of(1, 5000));
    }

    @MethodSource("depositValidData")
    @ParameterizedTest
    public void userCanAddDepositWithValidValue(int id, double balance) {
        given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(String.format("""
                        {
                          "id": %d,
                          "balance": %f
                        }
                        """, id, balance))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", Matchers.equalTo(id))
                .body("transactions.amount", Matchers.hasItem((float)balance))
                //проверяем ID последней транзакции
                .body("transactions[-1].id", Matchers.notNullValue())
                //проверяем тип последней транзакции
                .body("transactions[-1].type", Matchers.equalTo("DEPOSIT"));
    }

    @Test
    public void balanceIsIncreasedAfterDepositTest() {
        int accountId = 1;
        double depositAmount = 200.0;
        // получаем текущий баланс для accountId = 1
        double initialBalance = given()
                .header("Authorization", userAuthHeader)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .extract()
                .jsonPath()
                .getDouble(String.format("find { it.id == %d }.balance", accountId));

        //добавляем депозит
        given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(String.format("""
                        {
                          "id": %d,
                          "balance": %.2f
                        }
                        """, accountId, depositAmount))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("balance", Matchers.equalTo((float)(initialBalance + depositAmount)));
    }

    public static Stream<Arguments> depositInvalidData() {
        return Stream.of(
                // негативные
                Arguments.of(1, 6000, "Deposit amount cannot exceed 5000"),
                Arguments.of(1, -100, "Deposit amount must be at least 0.01"),
                // граничные значения
                Arguments.of(1, 5000.01, "Deposit amount cannot exceed 5000"),
                Arguments.of(1, 0, "Deposit amount must be at least 0.01"));
    }

    @MethodSource("depositInvalidData")
    @ParameterizedTest
    public void userCannotAddDepositWithInvalidValue(int id, double balance, String errorValue) {
        given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(String.format("""
                        {
                          "id": %d,
                          "balance": %f
                        }
                        """, id, balance))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(Matchers.equalTo(errorValue));
    }

    @Test
    public void userCannotAddDepositToDifferentAccount() {
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
    }
}

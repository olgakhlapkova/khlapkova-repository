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

public class UpdateNameTest {
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

    public static Stream<Arguments> nameValidData() {
        return Stream.of(
                // позитивный, 2 слова
                Arguments.of("Jane Air"),
                // 2 длинных слова
                Arguments.of("sdfdafdagdafgdfgadfgfdgfdagdbcbdafafdgd dgdgafgadfbabafadgadgafbadfgadfgadfgdfadfbadfg"),
                // 2 буквы
                Arguments.of("a a"));
    }

    @MethodSource("nameValidData")
    @ParameterizedTest
    public void userCanUpdateNameWithValidValue(String updatedName) {
        given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(String.format("""
                        {
                          "name": "%s"
                        }
                        """, updatedName))
                .put("http://localhost:4111/api/v1/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("customer.name", Matchers.equalTo(updatedName))
                .body("customer.username", Matchers.equalTo("kate20002"))
                .body("message", Matchers.equalTo("Profile updated successfully"));
    }

    @Test
    public void nameIsUpdatedInTheCustomerProfileTest() {
        given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("name", Matchers.equalTo("a a"));
    }

    public static Stream<Arguments> nameInvalidData() {
        return Stream.of(
                // негативные
                // пустое имя
                Arguments.of("", "Name must contain two words with letters only"),
                // 1 слово
                Arguments.of("John", "Name must contain two words with letters only"),
                // 3 слова
                Arguments.of("John Junior Smith", "Name must contain two words with letters only"),
                // имя содержит спецсимволы $%^&*()@#
                Arguments.of("John Smith$", "Name must contain two words with letters only"),
                Arguments.of("John% Smith", "Name must contain two words with letters only"),
                Arguments.of("John Smith^", "Name must contain two words with letters only"),
                Arguments.of("John& Smith", "Name must contain two words with letters only"),
                Arguments.of("John Smith*", "Name must contain two words with letters only"),
                Arguments.of("John( Smith", "Name must contain two words with letters only"),
                Arguments.of("John Smith)", "Name must contain two words with letters only"),
                Arguments.of("John@ Smith", "Name must contain two words with letters only"),
                Arguments.of("John Smith#", "Name must contain two words with letters only"),
                // имя содержит цифры 0123456789
                Arguments.of("John0 Smith", "Name must contain two words with letters only"),
                Arguments.of("John Smith1", "Name must contain two words with letters only"),
                Arguments.of("John2 Smith", "Name must contain two words with letters only"),
                Arguments.of("John Smith3", "Name must contain two words with letters only"),
                Arguments.of("John4 Smith", "Name must contain two words with letters only"),
                Arguments.of("John Smith5", "Name must contain two words with letters only"),
                Arguments.of("John6 Smith", "Name must contain two words with letters only"),
                Arguments.of("John Smith7", "Name must contain two words with letters only"),
                Arguments.of("John8 Smith", "Name must contain two words with letters only"),
                Arguments.of("John Smith9", "Name must contain two words with letters only"));
    }

    @MethodSource("nameInvalidData")
    @ParameterizedTest
    public void userCannotUpdateNameWithInvalidValue(String updatedName, String errorValue) {
        given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(String.format("""
                        {
                          "name": "%s"
                        }
                        """, updatedName))
                .put("http://localhost:4111/api/v1/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(Matchers.equalTo(errorValue));
    }
}

package iteration_2;

import Base.BaseTest;
import generators.RandomData;
import models.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.skelethon.requesters.ValidatedCrudRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.stream.Stream;

import static specs.ResponseSpecs.NAME_INVALID_ERROR;
import static specs.ResponseSpecs.PROFILE_UPDATED_SUCCESSFULLY;

public class UpdateProfileTest extends BaseTest {
    private static CreateUserRequest userRequest;
    private static String defaultName;
    private static boolean isSetupDone = false;

    @BeforeAll
    public static void testSetup() {
        if (isSetupDone) return;
        //создаем данные для регистрации нового юзера
        userRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        //админом создаем юзера
        new CrudRequester(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USER,
                ResponseSpecs.entityWasCreated())
                .post(userRequest);

        // устанавливаем начальное имя "Default User"
        defaultName = UpdateProfileRequest.DEFAULT_NAME;
        UpdateProfileRequest initialProfileRequest = UpdateProfileRequest.builder()
                .name(defaultName)
                .build();

        new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.CUSTOMER_PROFILE_UPDATE,
                ResponseSpecs.requestReturnsOK()
        ).update(0, initialProfileRequest);

        isSetupDone = true;
    }

    // Восстанавливаем имя перед КАЖДЫМ тестом
    @BeforeEach
    public void restoreDefaultName() {
        UpdateProfileRequest restoreRequest = UpdateProfileRequest.builder()
                .name(defaultName)
                .build();

        new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.CUSTOMER_PROFILE_UPDATE,
                ResponseSpecs.requestReturnsOK()
        ).update(0, restoreRequest);
    }

    // Метод для получения текущего имени через GET
    private static String getCurrentName() {
        CustomerResponse response = new ValidatedCrudRequester<CustomerResponse>(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.CUSTOMER_PROFILE_GET,
                ResponseSpecs.requestReturnsOK()
        ).get(0);

        return response != null ? response.getName() : "";
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
        // создаем запрос на обновление имени
        UpdateProfileRequest updateRequest = UpdateProfileRequest.builder()
                .name(updatedName)
                .build();

        // отправляем запрос и получаем ответ
        UpdateProfileResponse response = new ValidatedCrudRequester<UpdateProfileResponse>(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.CUSTOMER_PROFILE_UPDATE,
                ResponseSpecs.requestReturnsOK()
        ).update(0, updateRequest);

        // проверяем ответ через объект
        softly.assertThat(response.getMessage())
                .as("Сообщение в ответе")
                .isEqualTo(PROFILE_UPDATED_SUCCESSFULLY);

        softly.assertThat(response.getCustomer())
                .as("Customer в ответе")
                .isNotNull();

        if (response.getCustomer() != null) {
            softly.assertThat(response.getCustomer().getName())
                    .as("Имя в ответе")
                    .isEqualTo(updatedName);

            softly.assertThat(response.getCustomer().getUsername())
                    .as("Username в ответе")
                    .isEqualTo(userRequest.getUsername());
        }

        // проверяем, что имя действительно обновилось через отдельный GET запрос
        String nameAfter = getCurrentName();
        softly.assertThat(nameAfter)
                .as("Имя после обновления")
                .isEqualTo(updatedName);
    }

    public static Stream<Arguments> nameInvalidData() {
        return Stream.of(
                // негативные
                // пустое имя
                Arguments.of("", NAME_INVALID_ERROR),
                // 1 слово
                Arguments.of("John", NAME_INVALID_ERROR),
                // 3 слова
                Arguments.of("John Junior Smith", NAME_INVALID_ERROR),
                // имя содержит спецсимволы $%^&*()@#
                Arguments.of("John$%^&*()@# Smith$%^&*()@#", NAME_INVALID_ERROR),
                // имя содержит цифры 0123456789
                Arguments.of("John0123456789 Smith0123456789", NAME_INVALID_ERROR));
    }

    @MethodSource("nameInvalidData")
    @ParameterizedTest
    public void userCannotUpdateNameWithInvalidValue(String updatedName, String errorValue) {
        // получаем имя ДО попытки обновления
        String nameBefore = getCurrentName();

        // создаем запрос с невалидным именем
        UpdateProfileRequest updateRequest = UpdateProfileRequest.builder()
                .name(updatedName)
                .build();

        // отправляем запрос и получаем сообщение об ошибке
        String actualErrorValue = new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.CUSTOMER_PROFILE_UPDATE,
                ResponseSpecs.requestReturnsBadRequest()
        ).update(0, updateRequest)
                .extract()
                .body()
                .asString();

        // проверяем сообщение об ошибке
        softly.assertThat(actualErrorValue)
                .as("Сообщение об ошибке для имени '%s'", updatedName)
                .isEqualTo(errorValue);

        // проверяем, что имя НЕ ИЗМЕНИЛОСЬ
        String nameAfter = getCurrentName();
        softly.assertThat(nameAfter)
                .as("Имя не должно измениться при попытке обновления с невалидным значением: %s", updatedName)
                .isEqualTo(nameBefore);
    }
}

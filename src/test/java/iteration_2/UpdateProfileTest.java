package iteration_2;

import Base.BaseTest;
import io.qameta.allure.Step;
import models.CreateUserRequest;
import models.CustomerResponse;
import models.UpdateProfileRequest;
import models.UpdateProfileResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.skelethon.requesters.ValidatedCrudRequester;
import requests.steps.AdminSteps;
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
    @Step("Создаем юзера и устанавливаем ему начальное имя Default User")
    public static void testSetup() {
        if (isSetupDone) return;
        userRequest = AdminSteps.createUser();

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

    @BeforeEach
    @Step("Восстанавливаем имя перед КАЖДЫМ тестом")
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

    @Step("Метод для получения текущего имени через GET")
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
                Arguments.of("Jane Air"),
                Arguments.of("sdfdafdagdafgdfgadfgfdgfdagdbcbdafafdgd dgdgafgadfbabafadgadgafbadfgadfgadfgdfadfbadfg"),
                Arguments.of("a a"));
    }

    @MethodSource("nameValidData")
    @ParameterizedTest
    @Step("Проверка позитивного сценария, 2 длинных слова, 2 буквы")
    public void userCanUpdateNameWithValidValue(String updatedName) {
        UpdateProfileRequest updateRequest = UpdateProfileRequest.builder()
                .name(updatedName)
                .build();

        UpdateProfileResponse response = new ValidatedCrudRequester<UpdateProfileResponse>(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.CUSTOMER_PROFILE_UPDATE,
                ResponseSpecs.requestReturnsOK()
        ).update(0, updateRequest);

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

        String nameAfter = getCurrentName();
        softly.assertThat(nameAfter)
                .as("Имя после обновления")
                .isEqualTo(updatedName);
    }

    public static Stream<Arguments> nameInvalidData() {
        return Stream.of(
                Arguments.of("", NAME_INVALID_ERROR),
                Arguments.of("John", NAME_INVALID_ERROR),
                Arguments.of("John Junior Smith", NAME_INVALID_ERROR),
                Arguments.of("John$%^&*()@# Smith$%^&*()@#", NAME_INVALID_ERROR),
                Arguments.of("John0123456789 Smith0123456789", NAME_INVALID_ERROR));
    }

    @MethodSource("nameInvalidData")
    @ParameterizedTest
    @Step("Проверка негативных сценариев: пустое имя, 1 слово, 3 слова, спецсимволы, цифры")
    public void userCannotUpdateNameWithInvalidValue(String updatedName, String errorValue) {
        String nameBefore = getCurrentName();

        UpdateProfileRequest updateRequest = UpdateProfileRequest.builder()
                .name(updatedName)
                .build();

        String actualErrorValue = new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.CUSTOMER_PROFILE_UPDATE,
                ResponseSpecs.requestReturnsBadRequest()
        ).update(0, updateRequest)
                .extract()
                .body()
                .asString();

        softly.assertThat(actualErrorValue)
                .as("Сообщение об ошибке для имени '%s'", updatedName)
                .isEqualTo(errorValue);

        String nameAfter = getCurrentName();
        softly.assertThat(nameAfter)
                .as("Имя не должно измениться при попытке обновления с невалидным значением: %s", updatedName)
                .isEqualTo(nameBefore);
    }
}

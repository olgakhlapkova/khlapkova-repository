package api.iteration_2;

import api.Base.BaseTest;
import api.generators.RandomData;
import io.qameta.allure.Step;
import api.models.CreateUserRequest;
import api.models.UpdateProfileRequest;
import api.models.UpdateProfileResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import api.requests.steps.AdminSteps;
import api.requests.steps.UserSteps;

import java.util.stream.Stream;

import static api.specs.ResponseSpecs.NAME_INVALID_ERROR;
import static api.specs.ResponseSpecs.PROFILE_UPDATED_SUCCESSFULLY;

public class UpdateProfileTest extends BaseTest {
    private static CreateUserRequest userRequest;
    private static String defaultName;
    private static boolean isSetupDone = false;

    @BeforeAll
    @Step("Создаем юзера и устанавливаем ему начальное имя Default User")
    public static void testSetup() {
        if (isSetupDone) return;
        userRequest = AdminSteps.createUser();
        registerUser(userRequest);

        defaultName = UpdateProfileRequest.DEFAULT_NAME;
        UserSteps.updateProfile(userRequest, defaultName);

        String currentName = UserSteps.getCurrentName(userRequest);
        if (!defaultName.equals(currentName)) {
            throw new AssertionError("Имя не было установлено. Ожидалось: " + defaultName + ", но было: " + currentName);
        }

        isSetupDone = true;
    }

    @BeforeEach
    @Step("Восстанавливаем имя перед КАЖДЫМ тестом")
    public void restoreDefaultName() {
        UserSteps.updateProfile(userRequest, defaultName);
        String currentName = UserSteps.getCurrentName(userRequest);
        softly.assertThat(currentName)
                .as("Имя после восстановления")
                .isEqualTo(defaultName);
    }

    public static Stream<Arguments> nameValidData() {
        return Stream.of(
                Arguments.of(RandomData.generateRandomValidName()),
                Arguments.of(RandomData.generateRandomLongValidName()),
                Arguments.of(RandomData.generateRandomShortValidName()));
    }

    @MethodSource("nameValidData")
    @ParameterizedTest
    @Step("Проверка позитивного сценария, 2 длинных слова, 2 буквы")
    public void userCanUpdateNameWithValidValue(String updatedName) {
        UpdateProfileResponse response = UserSteps.updateProfile(userRequest, updatedName);

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

        String nameAfter = UserSteps.getCurrentName(userRequest);
        softly.assertThat(nameAfter)
                .as("Имя после обновления")
                .isEqualTo(updatedName);
    }

    public static Stream<Arguments> nameInvalidData() {
        return Stream.of(
                Arguments.of(RandomData.generateEmptyName(), NAME_INVALID_ERROR),
                Arguments.of(RandomData.generateOneWordName(), NAME_INVALID_ERROR),
                Arguments.of(RandomData.generateThreeWordsName(), NAME_INVALID_ERROR),
                Arguments.of(RandomData.generateInvalidNameWithRandomAscii(), NAME_INVALID_ERROR),
                Arguments.of(RandomData.generateInvalidNameWithNumbers(), NAME_INVALID_ERROR));
    }

    @MethodSource("nameInvalidData")
    @ParameterizedTest
    @Step("Проверка негативных сценариев: пустое имя, 1 слово, 3 слова, спецсимволы, цифры")
    public void userCannotUpdateNameWithInvalidValue(String updatedName, String errorValue) {
        String nameBefore = UserSteps.getCurrentName(userRequest);

        UpdateProfileRequest updateRequest = UpdateProfileRequest.builder()
                .name(updatedName)
                .build();

        String actualErrorValue = UserSteps.updateProfileWithError(userRequest, updatedName);

        softly.assertThat(actualErrorValue)
                .as("Сообщение об ошибке для имени '%s'", updatedName)
                .isEqualTo(errorValue);

        String nameAfter = UserSteps.getCurrentName(userRequest);
        softly.assertThat(nameAfter)
                .as("Имя не должно измениться при попытке обновления с невалидным значением: %s", updatedName)
                .isEqualTo(nameBefore);
    }
}

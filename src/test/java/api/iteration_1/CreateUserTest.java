package api.iteration_1;

import api.Base.BaseTest;
import api.generators.RandomData;
import api.generators.RandomModelGenerator;
import api.models.CreateUserRequest;
import api.models.CreateUserResponse;
import api.models.UserRole;
import api.models.comparison.ModelAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import api.requests.skelethon.Endpoint;
import api.requests.skelethon.requesters.CrudRequester;
import api.requests.skelethon.requesters.ValidatedCrudRequester;
import api.requests.steps.AdminSteps;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;

import java.util.stream.Stream;

import static api.specs.ResponseSpecs.*;

public class CreateUserTest extends BaseTest {

    @Test
    public void adminCanCreateUserWithCorrectData() {
        CreateUserRequest createUserRequest = RandomModelGenerator.generate(CreateUserRequest.class);

        CreateUserResponse createUserResponse = new ValidatedCrudRequester<CreateUserResponse>
                (RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USER,
                ResponseSpecs.entityWasCreated())
                .post(createUserRequest);

        registerUser(createUserResponse.getId());

        ModelAssertions.assertThatModels(createUserRequest,createUserResponse).match();
    }

    public static Stream<Arguments> userInvalidData() {
        String role = UserRole.USER.name();
        return Stream.of(
                Arguments.of(RandomData.generateSpaces(), RandomData.getPassword(), role, USERNAME_ERROR_KEY, new String[] {BLANK_USERNAME, USERNAME_ALLOWED_SYMBOLS}),
                Arguments.of(RandomData.generateTwoLetters(), RandomData.getPassword(), role, USERNAME_ERROR_KEY, new String[]{USERNAME_ALLOWED_SIZE}),
                Arguments.of(RandomData.generateInvalidNameWithRandomAscii(),RandomData.getPassword(), role, USERNAME_ERROR_KEY, new String[]{USERNAME_ALLOWED_SYMBOLS}));
    }

    @MethodSource("userInvalidData")
    @ParameterizedTest
    public void adminCanNotCreateUserWithInvalidData(String username, String password, String role, String errorKey, String... expectedMessages) {
        CreateUserRequest createUserRequest = CreateUserRequest.builder()
                .username(username)
                .password(password)
                .role(role)
                .build();

        new CrudRequester(RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USER,
                ResponseSpecs.requestReturnsBadRequestWithMessages(errorKey, expectedMessages))
                .post(createUserRequest);
    }

    @Test
    public void adminCanDeleteUserTest() {
        int userId = AdminSteps.createUserAndGetId();
        registerUser(userId);

        String response = AdminSteps.deleteUser(userId);
        String expectedMessage = USER_DELETED_PREFIX + userId + USER_DELETED_SUFFIX;

        softly.assertThat(response)
                .as("Сообщение об удалении пользователя")
                .isEqualTo(expectedMessage);

        unregisterUser(userId);
    }
}

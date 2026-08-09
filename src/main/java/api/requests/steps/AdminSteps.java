package api.requests.steps;

import api.generators.RandomModelGenerator;
import api.models.CreateUserRequest;
import api.models.CreateUserResponse;
import api.requests.skelethon.Endpoint;
import api.requests.skelethon.requesters.CrudRequester;
import api.requests.skelethon.requesters.ValidatedCrudRequester;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import io.qameta.allure.Step;

import java.util.List;

public class AdminSteps {
    public static CreateUserRequest createUser() {
        CreateUserRequest userRequest = RandomModelGenerator.generate(CreateUserRequest.class);

        new ValidatedCrudRequester<CreateUserResponse>(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USER,
                ResponseSpecs.entityWasCreated())
                .post(userRequest);

        return userRequest;
    }

    public static List<CreateUserResponse> getAllUsers() {
        return new ValidatedCrudRequester<CreateUserResponse>(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USER,
                ResponseSpecs.requestReturnsOK()).getAll(CreateUserResponse[].class);
    }

    @Step("Создание пользователя и получение его ID")
    public static int createUserAndGetId() {
        CreateUserRequest userRequest = RandomModelGenerator.generate(CreateUserRequest.class);

        CreateUserResponse response = new ValidatedCrudRequester<CreateUserResponse>(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USER,
                ResponseSpecs.entityWasCreated())
                .post(userRequest);

        return response.getId();
    }

    @Step("Удаление пользователя с ID {userId}")
    public static String deleteUser(int userId) {
        return new CrudRequester(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USER_DELETE,
                ResponseSpecs.userDeletedSuccessfully(userId)
        ).delete(userId)
                .extract()
                .body()
                .asString();
    }

    @Step("Удаление пользователя с ID {userId} и проверкой")
    public static void deleteUserAndVerify(int userId) {
        String response = deleteUser(userId);
        String expectedMessage = ResponseSpecs.USER_DELETED_PREFIX + userId + ResponseSpecs.USER_DELETED_SUFFIX;
        if (!response.equals(expectedMessage)) {
            throw new AssertionError("Expected: " + expectedMessage + ", but was: " + response);
        }
    }
}

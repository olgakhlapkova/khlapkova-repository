package iteration_1;

import Base.BaseTest;
import generators.RandomData;
import models.CreateUserRequest;
import models.UserRole;
import org.junit.jupiter.api.Test;
import requests.AccountRequester;
import requests.AdminCreateUserRequester;
import requests.CreateAccountRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

public class CreateAccountTest extends BaseTest {

    @Test
    public void userCanCreateAccountTest() {
        CreateUserRequest userRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        // создание пользователя
        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(userRequest);

        // создание аккаунта
        int accountId = new CreateAccountRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.entityWasCreated()
        ).createAndGetId();

        // получаем все аккаунты и проверяем, что созданный аккаунт есть в списке
        softly.assertThat(new AccountRequester(
                        RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                        ResponseSpecs.requestReturnsOK()
                ).getCustomerAccounts())
                .as("Список аккаунтов пользователя")
                .isNotEmpty()
                .anyMatch(account -> account.getId() == accountId);
    }
}

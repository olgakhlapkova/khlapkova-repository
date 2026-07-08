package iteration_1;

import Base.BaseTest;
import models.CreateUserRequest;
import org.junit.jupiter.api.Test;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.steps.AdminSteps;
import specs.RequestSpecs;
import specs.ResponseSpecs;

public class CreateAccountTest extends BaseTest {

    @Test
    public void userCanCreateAccountTest() {
        CreateUserRequest userRequest = AdminSteps.createUser();

        // создание аккаунта
        new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated())
                        .post(null);
        //.createAndGetId();

        // получаем все аккаунты и проверяем, что созданный аккаунт есть в списке
//        softly.assertThat(new AccountRequester(
//                        RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
//                        ResponseSpecs.requestReturnsOK()
//                ).getCustomerAccounts())
//                .as("Список аккаунтов пользователя")
//                .isNotEmpty()
//                .anyMatch(account -> account.getId() == accountId);
    }
}

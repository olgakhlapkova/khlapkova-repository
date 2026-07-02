package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.AccountResponse;
import models.BaseModel;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static io.restassured.RestAssured.given;

public class AccountRequester extends Request<BaseModel> {
    public AccountRequester(RequestSpecification requestSpecification,
                            ResponseSpecification responseSpecification) {
        super(requestSpecification, responseSpecification);
    }

    @Override
    public ValidatableResponse post(BaseModel model) {
        throw new UnsupportedOperationException("POST not supported for AccountRequester");
    }

    // Получить все аккаунты пользователя
    public List<AccountResponse> getCustomerAccounts() {
        AccountResponse[] accounts = given()
                .spec(requestSpecification)
                .get("/api/v1/customer/accounts")
                .then()
                .spec(responseSpecification)
                .extract()
                .as(AccountResponse[].class);

        return Arrays.asList(accounts);
    }

    // Получить баланс конкретного аккаунта
    public double getBalance(int accountId) {
        List<AccountResponse> accounts = getCustomerAccounts();

        return accounts.stream()
                .filter(account -> account.getId() == accountId)
                .findFirst()
                .map(AccountResponse::getBalance)
                .orElseThrow(() -> new AssertionError("Аккаунт с ID " + accountId + " не найден"));
    }
}

package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.UserDepositRequest;

import static io.restassured.RestAssured.given;

public class UserDepositRequester extends Request<UserDepositRequest>{

    public UserDepositRequester(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        super(requestSpecification,responseSpecification);
    }

    @Override
    public ValidatableResponse post(UserDepositRequest model) {
        return given()
                .spec(requestSpecification)
                .body(model)
                .post("/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .spec(responseSpecification);
    }
}

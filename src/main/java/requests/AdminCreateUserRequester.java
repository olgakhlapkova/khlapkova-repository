package requests;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.BaseModel;
import models.CreateUserRequest;
import org.apache.http.HttpStatus;

import static io.restassured.RestAssured.given;

public class AdminCreateUserRequest extends Request<CreateUserRequest> {

    public AdminCreateUserRequest(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        super(requestSpecification, responseSpecification);
    }

    @Override
    public ValidatableResponse post(CreateUserRequest model) {
        return given()
                .spec(requestSpecification)
                .body(model)
                .post("/api/v1/admin/users")
                .then()
                .assertThat()
                .spec(responseSpecification);
    }
}

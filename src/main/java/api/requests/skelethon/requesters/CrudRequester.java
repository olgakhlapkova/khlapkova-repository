package api.requests.skelethon.requesters;

import api.models.CreateUserResponse;
import api.requests.skelethon.interfaces.GetAllEndpointInterface;
import api.specs.RequestSpecs;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import api.models.BaseModel;
import api.requests.skelethon.Endpoint;
import api.requests.skelethon.HttpRequest;
import api.requests.skelethon.interfaces.CrudEndpointInterface;
import org.apache.http.HttpStatus;

import static io.restassured.RestAssured.given;

public class CrudRequester extends HttpRequest implements CrudEndpointInterface, GetAllEndpointInterface {
    public CrudRequester(RequestSpecification requestSpecification, Endpoint endpoint, ResponseSpecification responseSpecification) {
        super(requestSpecification, endpoint, responseSpecification);
    }

    @Override
    public ValidatableResponse post(BaseModel model) {
        var body = model == null ? "" : model;
        return  given()
                .spec(requestSpecification)
                .body(body)
                .post(endpoint.getUrl())
                .then()
                .assertThat()
                .spec(responseSpecification);
    }

    public ValidatableResponse post() {
        return given()
                .spec(requestSpecification)
                .post(endpoint.getUrl())
                .then()
                .assertThat()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse get(int id) {
        String url = endpoint.getUrl().contains("{id}")
                ? endpoint.getUrl().replace("{id}", String.valueOf(id))
                : endpoint.getUrl() + (id > 0 ? "/" + id : "");
        return given()
                .spec(requestSpecification)
                .get(url)
                .then()
                .assertThat()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse update(int id, BaseModel model) {
        var body = model == null ? "" : model;
        String url = endpoint.getUrl().contains("{id}")
                ? endpoint.getUrl().replace("{id}", String.valueOf(id))
                : endpoint.getUrl() + (id > 0 ? "/" + id : "");
        return given()
                .spec(requestSpecification)
                .body(body)
                .put(url)
                .then()
                .assertThat()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse delete(int id) {
        String url = endpoint.getUrl();
        if (url.contains("{id}") || url.contains("{accountId}")) {
            url = url.replace("{id}", String.valueOf(id))
                    .replace("{accountId}", String.valueOf(id));
        } else if (id > 0) {
            url = url + "/" + id;
        }
        return given()
                .spec(requestSpecification)
                .delete(url)
                .then()
                .assertThat()
                .spec(responseSpecification);
    }

    public ValidatableResponse getAll() {
        return given()
                .spec(requestSpecification)
                .get(endpoint.getUrl())
                .then()
                .assertThat()
                .spec(responseSpecification);
    }

    public <T> T getAndExtract(String path, Class<T> responseType) {
        return given()
                .spec(requestSpecification)
                .get(path)
                .then()
                .assertThat()
                .spec(responseSpecification)
                .extract()
                .as(responseType);
    }

    public ValidatableResponse getWithPath(String path) {
        return given()
                .spec(requestSpecification)
                .get(path)
                .then()
                .assertThat()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse getAll(Class<?> clazz) {
        return given()
                .spec(requestSpecification)
                .get(endpoint.getUrl())
                .then().assertThat()
                .spec(responseSpecification);
    }
}

package requests.skelethon.requesters;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.BaseModel;
import requests.skelethon.Endpoint;
import requests.skelethon.HttpRequest;
import requests.skelethon.interfaces.CrudEndpointInterface;

import static io.restassured.RestAssured.given;

public class CrudRequester extends HttpRequest implements CrudEndpointInterface {
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
        String url = endpoint.getUrl().contains("{id}")
                ? endpoint.getUrl().replace("{id}", String.valueOf(id))
                : endpoint.getUrl() + (id > 0 ? "/" + id : "");
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
}

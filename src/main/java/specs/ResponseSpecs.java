package specs;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.specification.ResponseSpecification;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;

public class ResponseSpecs {
    // Сообщения для UpdateProfile
    public static final String PROFILE_UPDATED_SUCCESSFULLY = "Profile updated successfully";
    public static final String NAME_INVALID_ERROR = "Name must contain two words with letters only";

    // Сообщения для Transfer
    public static final String INVALID_TRANSFER = "Invalid transfer: insufficient funds or invalid accounts";
    public static final String TRANSFER_AMOUNT_MIN_ERROR = "Transfer amount must be at least 0.01";
    public static final String TRANSFER_AMOUNT_MAX_ERROR = "Transfer amount cannot exceed 10000";

    // Сообщения для Deposit
    public static final String DEPOSIT_AMOUNT_MIN_ERROR = "Deposit amount must be at least 0.01";
    public static final String DEPOSIT_AMOUNT_MAX_ERROR = "Deposit amount cannot exceed 5000";
    public static final String UNAUTHORIZED_ACCESS_TO_ACCOUNT = "Unauthorized access to account";

    private ResponseSpecs() {}

    private static ResponseSpecBuilder defaultResponseBuilder() {
        return new ResponseSpecBuilder();
    }

    public static ResponseSpecification entityWasCreated() {
        return defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_CREATED)
                .build();
    }

    public static ResponseSpecification requestReturnsOK() {
        return defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_OK)
                .build();
    }

    public static ResponseSpecification requestReturnsBadRequest(String errorKey, String errorValue) {
        return defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_BAD_REQUEST)
                .expectBody(errorKey, Matchers.equalTo(errorValue))
                .build();
    }

    public static ResponseSpecification requestReturnsBadRequest() {
        return defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_BAD_REQUEST)
                .build();
    }

    public static ResponseSpecification requestReturnsForbidden() {
        return defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_FORBIDDEN)
                .build();
    }
}

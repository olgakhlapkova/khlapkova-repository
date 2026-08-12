package ui.pages;

import lombok.Getter;

@Getter
public enum BankAlert {
    USER_CREATED_SUCCESSFULLY("✅ User created successfully!"),
    USERNAME_MUST_BE_BETWEEN_3_AND_15_CHARACTERS("Username must be between 3 and 15 characters"),
    NEW_ACCOUNT_CREATED("✅ New Account Created! Account Number: "),
    SUCCESSFULL_DEPOSIT("✅ Successfully deposited $%s to account ACC%s!"),
    UNSUCCESSFULL_DEPOSIT("❌ Please deposit less or equal to 5000$.");

    private final String message;

    BankAlert(String message) {
        this.message = message;
    }
}

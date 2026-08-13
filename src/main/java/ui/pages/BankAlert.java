package ui.pages;

import lombok.Getter;

@Getter
public enum BankAlert {
    USER_CREATED_SUCCESSFULLY("✅ User created successfully!"),
    USERNAME_MUST_BE_BETWEEN_3_AND_15_CHARACTERS("Username must be between 3 and 15 characters"),
    NEW_ACCOUNT_CREATED("✅ New Account Created! Account Number: "),
    SUCCESSFULL_DEPOSIT("✅ Successfully deposited $%s to account ACC%s!"),
    UNSUCCESSFULL_DEPOSIT("❌ Please deposit less or equal to 5000$."),
    SUCCESSFULL_TRANSFER("✅ Successfully transferred $%s to account ACC%s!"),
    TRANSFER_WITHOUT_ALL_FIELDS("❌ Please fill all fields and confirm."),
    TRANSFER_WITH_INCORRECT_ACCOUNT_NUMBER("❌ No user found with this account number."),
    TRANSFER_WITH_BIG_AMOUNT("❌ Error: Transfer amount cannot exceed 10000"),
    SUCCESSFULL_TRANSFER_AGAIN("✅ Transfer of $%.0f successful from Account %s to %s!"),
    TRANSFER_FAILED("❌ Transfer failed: Please try again."),
    NAME_UPDATED_SUCCESSFULLY("✅ Name updated successfully!"),
    NAME_MUST_CONTAIN_TWO_WORDS("Name must contain two words with letters only");

    private final String message;

    BankAlert(String message) {
        this.message = message;
    }
}

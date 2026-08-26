package ui.pages;

import lombok.Getter;

@Getter
public enum TransactionType {
    DEPOSIT("DEPOSIT"),
    TRANSFER_IN("TRANSFER_IN"),
    TRANSFER_OUT("TRANSFER_OUT");

    private final String type;

    TransactionType(String type) {
        this.type = type;
    }
}

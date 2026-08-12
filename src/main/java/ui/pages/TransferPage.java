package ui.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class TransferPage extends BasePage<TransferPage> {
    private SelenideElement transferAgainButton = $(Selectors.byText("\uD83D\uDD01 Transfer Again"));
    private SelenideElement matchingTransactionsText = $(Selectors.byText("Matching Transactions"));

    @Override
    public String url() {
        return "/transfer";
    }

    public void checkTransactions(TransactionType transactionType) {
        transferAgainButton.click();
        matchingTransactionsText.shouldBe(Condition.visible);
        $(Selectors.byText(transactionType.getType())).shouldBe(Condition.visible);
    }
}

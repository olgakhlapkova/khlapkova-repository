package ui.pages;

import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import lombok.Getter;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

@Getter
public class DepositMoneyPage extends BasePage<DepositMoneyPage> {
    private SelenideElement depositMoneyText = $(Selectors.byText("\uD83D\uDCB0 Deposit Money"));
    private SelenideElement accountSelector = $("select.form-control.account-selector");
    private SelenideElement amountInput = $(Selectors.byAttribute("placeholder", "Enter amount"));
    private SelenideElement depositButton = $(Selectors.byText("\uD83D\uDCB5 Deposit"));

    @Override
    public String url() {
        return "/deposit";
    }

    public void makeADeposit(int accountId, String amount) {
        accountSelector.click();
        $("select.form-control.account-selector option[value='" + accountId + "']")
                .shouldBe(visible)
                .click();
        amountInput.setValue(amount);
        depositButton.click();
    }
}

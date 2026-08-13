package ui.pages;

import api.generators.RandomData;
import api.models.CreateUserRequest;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import lombok.Getter;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.*;

@Getter
public class TransferPage extends BasePage<TransferPage> {
    private SelenideElement transferPageText = $(Selectors.byText("\uD83D\uDD04 Make a Transfer"));
    private SelenideElement transferAgainButton = $(Selectors.byText("\uD83D\uDD01 Transfer Again"));
    private SelenideElement accountSelector =  $("select.form-control.account-selector");
    private SelenideElement recipientName = $(Selectors.byAttribute("placeholder", "Enter recipient name"));
    private SelenideElement recipientAccountNumber = $(Selectors.byAttribute("placeholder", "Enter recipient account number"));
    private SelenideElement transferAmountInput = $(Selectors.byAttribute("placeholder", "Enter amount"));
    private SelenideElement confirmCheckbox =  $("#confirmCheck");
    private SelenideElement sendTransferButton = $(Selectors.byText("\uD83D\uDE80 Send Transfer"));
    private SelenideElement matchingTransactionsText = $(Selectors.byText("Matching Transactions"));

    //Repeat transfer modal
    private SelenideElement repeatTransferText = $(Selectors.byText("\uD83D\uDD01 Repeat Transfer"));
    private SelenideElement accountSelectorModal = $("select.form-control");
    private SelenideElement amountInputModal = $(Selectors.byAttribute("type", "number"));
    private SelenideElement cancelButton = $(Selectors.byText("Cancel"));
    private SelenideElement crossIcon =  $(".modal-content").$("button.btn-close");


    @Override
    public String url() {
        return "/transfer";
    }

    public void makeATransfer(CreateUserRequest user, int accountId1, int accountId2, double transferAmount) {
        accountSelector.click();
        $("select.form-control.account-selector option[value='" + accountId1 + "']").shouldBe(visible).click();
        recipientName.setValue(user.getUsername());
        recipientAccountNumber.setValue("ACC" + accountId2);
        transferAmountInput.setValue(String.valueOf(transferAmount));
        confirmCheckbox.setSelected(true);
        sendTransferButton.click();
    }

    public void transferWithoutSelectedAccount(CreateUserRequest user, int accountId2, double transferAmount){
        recipientName.setValue(user.getUsername());
        recipientAccountNumber.setValue("ACC" + accountId2);
        transferAmountInput.setValue(String.valueOf(transferAmount));
        confirmCheckbox.setSelected(true);
        sendTransferButton.click();
    }

    public void transferWithIncorrectAccountNumber(CreateUserRequest user, int accountId1, double transferAmount){
        accountSelector.click();
        $("select.form-control.account-selector option[value='" + accountId1 + "']").shouldBe(visible).click();
        recipientName.setValue(user.getUsername());
        recipientAccountNumber.setValue(RandomData.generateTwoLetters());
        transferAmountInput.setValue(String.valueOf(transferAmount));
        confirmCheckbox.setSelected(true);
        sendTransferButton.click();
    }

    public void transferWithIncorrectTransferAmount(CreateUserRequest user, int accountId1, int accountId2, double incorrectTransferAmount){
        accountSelector.click();
        $("select.form-control.account-selector option[value='" + accountId1 + "']").shouldBe(visible).click();
        recipientName.setValue(user.getUsername());
        recipientAccountNumber.setValue("ACC" + accountId2);
        transferAmountInput.setValue(String.valueOf(incorrectTransferAmount));
        confirmCheckbox.setSelected(true);
        sendTransferButton.click();
    }

    public void transferWithUncheckedCheckbox(CreateUserRequest user, int accountId1, int accountId2, double transferAmount){
        accountSelector.click();
        $("select.form-control.account-selector option[value='" + accountId1 + "']").shouldBe(visible).click();
        recipientName.setValue(user.getUsername());
        recipientAccountNumber.setValue("ACC" + accountId2);
        transferAmountInput.setValue(String.valueOf(transferAmount));
        confirmCheckbox.setSelected(false);
        sendTransferButton.click();
    }

    public void transferAgain(int accountId2){
        transferAgainButton.click();
        matchingTransactionsText.shouldBe(Condition.visible);
        SelenideElement transaction = $$("li.list-group-item")
                .findBy(text("TRANSFER_IN"));
        transaction.scrollIntoView(true);
        sleep(300);
        transaction.$("button").click();

        repeatTransferText.shouldBe(Condition.visible);
        accountSelectorModal.selectOptionContainingText("ACC"+ accountId2);
        confirmCheckbox.setSelected(true);
        sendTransferButton.click();
    }

    public void transferAgainWithInvalidAmount(int accountId2){
        transferAgainButton.click();
        matchingTransactionsText.shouldBe(Condition.visible);
        SelenideElement transaction = $$("li.list-group-item")
                .findBy(text("TRANSFER_IN"));
        transaction.scrollIntoView(true);
        sleep(300);
        transaction.$("button").click();

        repeatTransferText.shouldBe(Condition.visible);
        accountSelectorModal.selectOptionContainingText("ACC"+ accountId2);
        double invalidTransferAmount = RandomData.getBigAmount();
        amountInputModal.shouldBe(visible).click();
        sleep(200);
        amountInputModal.setValue(String.valueOf(invalidTransferAmount));
        confirmCheckbox.setSelected(true);
        sendTransferButton.click();
    }

    public void cancelTransferAgainViaCancelButton(int accountId2){
        transferAgainButton.click();
        matchingTransactionsText.shouldBe(Condition.visible);
        SelenideElement transaction = $$("li.list-group-item")
                .findBy(text("TRANSFER_IN"));
        transaction.scrollIntoView(true);
        sleep(300);
        transaction.$("button").click();

        repeatTransferText.shouldBe(Condition.visible);
        accountSelectorModal.selectOptionContainingText("ACC"+ accountId2);
        confirmCheckbox.setSelected(true);
        cancelButton.click();
    }

    public void cancelTransferAgainViaCrossIcon(int accountId2){
        transferAgainButton.click();
        matchingTransactionsText.shouldBe(Condition.visible);
        SelenideElement transaction = $$("li.list-group-item")
                .findBy(text("TRANSFER_IN"));
        transaction.scrollIntoView(true);
        sleep(300);
        transaction.$("button").click();

        repeatTransferText.shouldBe(Condition.visible);
        accountSelectorModal.selectOptionContainingText("ACC"+ accountId2);
        confirmCheckbox.setSelected(true);
        crossIcon.click();
    }

    public void checkTransactions(TransactionType transactionType) {
        transferAgainButton.click();
        matchingTransactionsText.shouldBe(Condition.visible);
        $(Selectors.byText(transactionType.getType())).shouldBe(Condition.visible);
    }


}

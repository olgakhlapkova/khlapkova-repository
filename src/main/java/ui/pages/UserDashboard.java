package ui.pages;

import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import lombok.Getter;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

@Getter
public class UserDashboard extends BasePage<UserDashboard> {
    private SelenideElement userDashboardText = $(Selectors.byText("User Dashboard"));
    private SelenideElement welcomeText = $(Selectors.byClassName("welcome-text"));
    private SelenideElement welcomeTextUsername = $("h2.welcome-text span");
    private SelenideElement depositMoney = $(Selectors.byText("\uD83D\uDCB0 Deposit Money"));
    private SelenideElement makeATransfer = $(Selectors.byText("\uD83D\uDD04 Make a Transfer"));
    private SelenideElement createNewAccount = $(Selectors.byText("➕ Create New Account"));
    private SelenideElement userInfo = $(".user-info");
    private SelenideElement userInfoUsername = $("div.user-info span.user-name");

    @Override
    public String url() {
        return "/dashboard";
    }

    public EditProfilePage openEditProfilePage(){
        userInfo.click();
        return UserDashboard.getPage(EditProfilePage.class);
    }

    public DepositMoneyPage openDepositPage(){
        depositMoney.click();
        return UserDashboard.getPage(DepositMoneyPage.class);
    }

    public TransferPage openTransferPage(){
        makeATransfer.click();
        return UserDashboard.getPage(TransferPage.class);
    }

    public UserDashboard createNewAccount() {
        createNewAccount.click();
        return this;
    }

    public UserDashboard openViaHomeButton() {
        homeButton.click();
        userDashboardText.shouldBe(visible);
        return this;
    }
}

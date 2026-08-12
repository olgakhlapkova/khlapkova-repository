package ui.pages;

import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import lombok.Getter;

import static com.codeborne.selenide.Selenide.$;

@Getter
public class UserDashboard extends BasePage<UserDashboard> {
    private SelenideElement userDashboardText = $(Selectors.byText("User Dashboard"));
    private SelenideElement welcomeText = $(Selectors.byClassName("welcome-text"));
    private SelenideElement depositMoney = $(Selectors.byText("\uD83D\uDCB0 Deposit Money"));
    private SelenideElement createNewAccount = $(Selectors.byText("➕ Create New Account"));

    @Override
    public String url() {
        return "/dashboard";
    }

    public DepositMoneyPage openDepositPage(){
        depositMoney.click();
        return UserDashboard.getPage(DepositMoneyPage.class);
    }

    public UserDashboard createNewAccount() {
        createNewAccount.click();
        return this;
    }
}

package ui.pages;

import api.generators.RandomData;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import lombok.Getter;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.sleep;

@Getter
public class EditProfilePage extends BasePage<EditProfilePage> {
   private SelenideElement editProfileText = $(Selectors.byText("✏️ Edit Profile"));
   private SelenideElement newNameInput = $(Selectors.byAttribute("placeholder", "Enter new name"));
   private SelenideElement saveChangesButton = $(Selectors.byText("\uD83D\uDCBE Save Changes"));

    @Override
    public String url() {
        return "/edit-profile";
    }

    public void editUsername(String updatedName){
        newNameInput.shouldBe(visible).click();
        sleep(200);
        newNameInput.setValue(updatedName);
        saveChangesButton.click();
    }
}

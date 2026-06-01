package generators;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;

public class RandomData {
    private RandomData() {}

    public static String getUsername() {
        return RandomStringUtils.randomAlphabetic(10);
    }

    public static String getPassword() {
        return RandomStringUtils.randomAlphabetic(3).toUpperCase() +
                RandomStringUtils.randomAlphabetic(5).toLowerCase() +
                RandomStringUtils.randomNumeric(3) + "%$#";
    }

    public static double getBalance() {
        return RandomUtils.nextDouble(0.01, 10000.0);
    }

    public static double getAmount() {
        return RandomUtils.nextDouble(0.01, 1000.0);
    }
}

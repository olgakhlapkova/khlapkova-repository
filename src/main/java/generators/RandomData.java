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

    public static double getBigAmount() {
        return RandomUtils.nextDouble(10000.1, 20000.0);
    }

    public static int getRandomAccountId() {
        return RandomUtils.nextInt(1000, 3000);
    }

    public static String generateRandomValidName() {
        return RandomStringUtils.randomAlphabetic(3, 7).toLowerCase() +
                RandomStringUtils.randomAlphabetic(3, 7).toUpperCase() + " " +
                RandomStringUtils.randomAlphabetic(3, 7).toLowerCase() +
                RandomStringUtils.randomAlphabetic(3, 7).toUpperCase();
    }

    public static String generateRandomLongValidName() {
        return RandomStringUtils.randomAlphabetic(8).toLowerCase() +
                RandomStringUtils.randomAlphabetic(7).toUpperCase() + " " +
                RandomStringUtils.randomAlphabetic(8).toLowerCase() +
                RandomStringUtils.randomAlphabetic(7).toUpperCase();
    }

    public static String generateRandomShortValidName() {
        return RandomStringUtils.randomAlphabetic(1).toLowerCase() + " " +
                RandomStringUtils.randomAlphabetic(1).toUpperCase();
    }

    public static String generateEmptyName() {
        return "";
    }

    public static String generateOneWordName() {
        return RandomStringUtils.randomAlphabetic(1, 15);
    }

    public static String generateThreeWordsName() {
        return RandomStringUtils.randomAlphabetic(1, 15) + " " + RandomStringUtils.randomAlphabetic(1, 15) + " " + RandomStringUtils.randomAlphabetic(1, 15);
    }

    public static String generateInvalidNameWithNumbers() {
        return RandomStringUtils.randomAlphabetic(1, 8) + RandomStringUtils.randomNumeric(1, 7) + " " + RandomStringUtils.randomAlphabetic(1, 8) + RandomStringUtils.randomNumeric(1, 7);
    }

    public static String generateInvalidNameWithRandomAscii() {
        return RandomStringUtils.randomAlphabetic(1, 8) + RandomStringUtils.randomAscii(7) + " " + RandomStringUtils.randomAlphabetic(1, 8) + RandomStringUtils.randomAscii(7);
    }

    public static String generateTwoLetters() {
        return RandomStringUtils.randomAlphabetic(1, 2);
    }

    public static String generateSpaces() {
        return "   ";
    }
}

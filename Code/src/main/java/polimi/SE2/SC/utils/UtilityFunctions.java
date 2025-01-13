package polimi.SE2.SC.utils;

import java.util.ArrayList;
import java.util.Arrays;

public class UtilityFunctions {
    public static ArrayList<String> splitStringByComma(String input) {
        if (input == null || input.isEmpty()) {
            // Return an empty ArrayList if the input is null or empty
            return new ArrayList<>();
        }

        // Split the string using the comma as a delimiter
        String[] resultArray = input.split(",");

        // Convert the array to an ArrayList and return it
        return new ArrayList<>(Arrays.asList(resultArray));
    }
}

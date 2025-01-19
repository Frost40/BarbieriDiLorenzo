package polimi.SE2.SC.utils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

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

    public static Optional<Integer> parsingChecker(HttpServletRequest request, HttpServletResponse response, String stringToParse) throws ServletException, IOException {
        int idToReturn;

        try {
            idToReturn = Integer.parseInt(stringToParse);

        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(e.getMessage());
            return Optional.empty();
        }

        return Optional.of(idToReturn);
    }
}

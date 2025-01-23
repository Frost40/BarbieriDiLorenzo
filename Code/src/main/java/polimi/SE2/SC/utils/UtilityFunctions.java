package polimi.SE2.SC.utils;

import polimi.SE2.SC.beans.Feedback;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    /**
     * Method used to generate the average rating given the list of feedback of an internship
     *
     * @param feedbackList Array list containing all feedbacks of the internship
     * @return Double containing the average rating if found or else 0.0 by default
     */
    public static double calculateAverageRating(ArrayList<Feedback> feedbackList) {
        if (feedbackList == null || feedbackList.isEmpty()) {
            throw new IllegalArgumentException("Feedback list cannot be null or empty.");
        }

        return feedbackList.stream()
                .mapToDouble(Feedback::getRating) // Extract ratings as doubles
                .average()                        // Calculate the average
                .orElse(0.0);                     // Return 0.0 if no ratings are found
    }
}

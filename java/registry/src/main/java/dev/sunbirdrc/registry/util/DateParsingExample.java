package dev.sunbirdrc.registry.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateParsingExample {
    public static void formatDate(String inputDateString) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        try {
            LocalDate localDate = LocalDate.parse(inputDateString, formatter);
            System.out.println("Parsed LocalDate: " + localDate);
        } catch (DateTimeParseException e) {
            System.out.println("Parsing error: " + e.getMessage());
        }
    }
}


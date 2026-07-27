package com.cooperative.domain.validation;

import com.cooperative.domain.exception.InvalidAssociateException;
import java.util.regex.Pattern;

/**
 * Validation logic for Associate entity.
 */
public class AssociateValidator {

    private static final int MIN_NAME_LENGTH = 2;
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");

    private AssociateValidator() {
        // Utility class, no instantiation
    }

    /**
     * Validates associate fields.
     *
     * @param name the associate name
     * @param document the associate document
     * @param email the associate email (optional)
     * @throws InvalidAssociateException if validation fails
     */
    public static void validate(String name, String document, String email) {
        if (name == null || name.trim().length() < MIN_NAME_LENGTH) {
            throw new InvalidAssociateException(
                "Associate name must have at least " + MIN_NAME_LENGTH + " characters");
        }
        
        if (document == null || document.trim().isEmpty()) {
            throw new InvalidAssociateException(
                "Associate document cannot be empty");
        }
        
        if (email != null && !email.trim().isEmpty() && 
            !EMAIL_PATTERN.matcher(email).matches()) {
            throw new InvalidAssociateException(
                "Invalid email format: " + email);
        }
    }
}

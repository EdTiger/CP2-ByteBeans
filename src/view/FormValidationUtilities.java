package view;

import javax.swing.BorderFactory;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;
import java.util.function.Predicate;

/** Reusable live-validation behavior for Swing text fields. */
public final class FormValidationUtilities {

    private FormValidationUtilities() {
        // Static form module; prevent instantiation.
    }

    /**
     * Adds non-blocking visual validation while preserving the field's
     * original border for blank and valid values.
     */
    public static void addLiveValidation(
            JTextField field,
            Predicate<String> validator) {

        if (field == null || validator == null) {
            throw new IllegalArgumentException(
                    "Field and validator are required."
            );
        }

        Border defaultBorder = field.getBorder();
        Border errorBorder = BorderFactory.createLineBorder(
                new Color(185, 28, 28),
                2
        );

        field.getDocument().addDocumentListener(new DocumentListener() {
            private void validateField() {
                String value = field.getText();
                boolean blank = value == null || value.trim().isEmpty();
                field.setBorder(
                        blank || validator.test(value)
                                ? defaultBorder
                                : errorBorder
                );
            }

            @Override
            public void insertUpdate(DocumentEvent event) {
                validateField();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                validateField();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                validateField();
            }
        });
    }
}

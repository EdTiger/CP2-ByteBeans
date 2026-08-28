/*
 * DialogUtilities.java
 * Reusable dialog popups and error handling for views.
 */
package view;

import javax.swing.JOptionPane;
import java.awt.Component;

/**
 * Reusable dialog popups and error handling for views.
 *
 * <p>Provides standard warning, error, and confirmation dialogs to
 * eliminate duplicated JOptionPane calls across the application.</p>
 */
public final class DialogUtilities {

    /** Private constructor prevents instantiation of this utility class. */
    private DialogUtilities() {
        // Utility class; prevent instantiation.
    }

    /**
     * Displays a warning message dialog.
     *
     * @param parentComponent the parent component (usually a JFrame)
     * @param dialogTitle     the title of the dialog
     * @param warningMessage  the warning message to display
     */
    public static void showWarning(
            Component parentComponent,
            String dialogTitle,
            String warningMessage) {

        // Wrap the standard JOptionPane warning message.
        JOptionPane.showMessageDialog(
                parentComponent,
                warningMessage,
                dialogTitle,
                JOptionPane.WARNING_MESSAGE
        );
    }

    /**
     * Displays an error message dialog, optionally appending an exception cause.
     *
     * @param parentComponent the parent component (usually a JFrame)
     * @param dialogTitle     the title of the dialog
     * @param userMessage     the primary user-facing error message
     * @param cause           the exception that caused the error (may be null)
     */
    public static void showError(
            Component parentComponent,
            String dialogTitle,
            String userMessage,
            Throwable cause) {

        // Build the full message including the underlying cause if available.
        String fullMessage = userMessage + "\n\n" + getErrorMessage(cause);

        // Display the standard error dialog.
        JOptionPane.showMessageDialog(
                parentComponent,
                fullMessage,
                dialogTitle,
                JOptionPane.ERROR_MESSAGE
        );
    }

    /**
     * Displays a confirmation dialog with Yes/No options.
     *
     * @param parentComponent     the parent component (usually a JFrame)
     * @param dialogTitle         the title of the dialog
     * @param confirmationMessage the message to ask the user
     * @return true if the user clicks Yes, false otherwise
     */
    public static boolean confirmAction(
            Component parentComponent,
            String dialogTitle,
            String confirmationMessage) {

        // Show the Yes/No confirmation dialog.
        int choice = JOptionPane.showConfirmDialog(
                parentComponent,
                confirmationMessage,
                dialogTitle,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        // Return true only if the user explicitly clicked YES.
        return choice == JOptionPane.YES_OPTION;
    }

    /**
     * Extracts a safe, non-blank error message from a Throwable.
     *
     * @param throwable the exception (may be null)
     * @return the exception's message, or a fallback string
     */
    public static String getErrorMessage(Throwable throwable) {
        // Fallback for null throwables or empty messages.
        if (throwable == null
                || throwable.getMessage() == null
                || throwable.getMessage().trim().isEmpty()) {
            return "An unexpected error occurred.";
        }
        return throwable.getMessage();
    }
}

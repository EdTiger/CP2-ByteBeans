/*
 * BusyStateUtilities.java
 * Reusable busy state management for Swing views.
 */
package view;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import java.awt.Cursor;

/**
 * Reusable busy state management for Swing views.
 *
 * <p>Handles toggling UI controls, progress bars, cursors, and status
 * messages during background operations.</p>
 */
public final class BusyStateUtilities {

    /** Private constructor prevents instantiation of this utility class. */
    private BusyStateUtilities() {
        // Utility class; prevent instantiation.
    }

    /**
     * Applies a busy or idle state to a window and its controls.
     *
     * @param frame           the frame to receive cursor changes
     * @param isBusy          true to enter busy state, false for idle
     * @param statusLabel     the label to update with the status message
     * @param progressBar     the progress bar to toggle
     * @param statusMessage   the message to display
     * @param isIndeterminate true if progress is indeterminate (only applies if busy)
     * @param toggledControls controls to disable when busy and enable when idle
     */
    public static void applyBusyState(
            JFrame frame,
            boolean isBusy,
            JLabel statusLabel,
            JProgressBar progressBar,
            String statusMessage,
            boolean isIndeterminate,
            JComponent... toggledControls) {

        // Update the status text.
        statusLabel.setText(statusMessage);

        // Show or hide the progress bar.
        progressBar.setVisible(isBusy);

        if (isBusy) {
            // Set progress bar mode.
            progressBar.setIndeterminate(isIndeterminate);
            progressBar.setStringPainted(!isIndeterminate);
        } else {
            // Reset the progress bar when idle.
            progressBar.setValue(0);
            progressBar.setIndeterminate(false);
            progressBar.setStringPainted(false);
        }

        // Toggle the enabled state of all provided controls.
        for (JComponent control : toggledControls) {
            if (control != null) {
                control.setEnabled(!isBusy);
            }
        }

        // Change the mouse cursor.
        frame.setCursor(
                isBusy
                        ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
                        : Cursor.getDefaultCursor()
        );
    }
}

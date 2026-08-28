/*
 * WindowUtilities.java
 * Reusable window configuration and navigation for Swing views.
 */
package view;

import javax.swing.JFrame;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Reusable window configuration and navigation for Swing views.
 *
 * <p>Standardizes maximized and centered window bounds, and provides
 * a common logout navigation action.</p>
 */
public final class WindowUtilities {

    /** Private constructor prevents instantiation of this utility class. */
    private WindowUtilities() {
        // Utility class; prevent instantiation.
    }

    /**
     * Configures a window to open fully maximized while respecting OS taskbars.
     *
     * @param frame         the frame to configure
     * @param minimumSize   the minimum allowed resize dimensions
     * @param onCloseAction the action to run when the close button is clicked
     */
    public static void configureMaximizedWindow(
            JFrame frame,
            Dimension minimumSize,
            Runnable onCloseAction) {

        // Prevent default close so we can show a confirmation dialog.
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setMinimumSize(minimumSize);

        // Get the usable screen space (excluding the Windows taskbar).
        Rectangle usableScreenBounds = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();

        // Set the window bounds to fill the usable space.
        frame.setMaximizedBounds(usableScreenBounds);
        frame.setBounds(usableScreenBounds);

        // Inform the OS that the window is maximized.
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

        // Attach the custom close action.
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                if (onCloseAction != null) {
                    onCloseAction.run();
                }
            }
        });
    }

    /**
     * Configures a window to open centered on the screen with a specific size.
     *
     * @param frame         the frame to configure
     * @param minimumSize   the minimum allowed resize dimensions
     * @param preferredSize the starting dimensions
     * @param onCloseAction the action to run when the close button is clicked
     */
    public static void configureCenteredWindow(
            JFrame frame,
            Dimension minimumSize,
            Dimension preferredSize,
            Runnable onCloseAction) {

        // Prevent default close so we can show a confirmation dialog.
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setMinimumSize(minimumSize);
        frame.setSize(preferredSize);

        // Center the window on the primary display.
        frame.setLocationRelativeTo(null);

        // Attach the custom close action.
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                if (onCloseAction != null) {
                    onCloseAction.run();
                }
            }
        });
    }

    /**
     * Transitions from the current frame back to the login screen.
     *
     * @param currentFrame the frame to close
     */
    public static void performLogout(JFrame currentFrame) {
        // Instantiate and display the login frame.
        LoginFrame loginFrame = new LoginFrame();
        loginFrame.setVisible(true);

        // Dispose of the current frame to free memory.
        if (currentFrame != null) {
            currentFrame.dispose();
        }
    }
}

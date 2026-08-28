import config.ApplicationConfig;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import view.LoginFrame;

/**
 * The main entry point for the application.
 * Declared as final so it cannot be subclassed. Sample Feature
 */
public final class Main {

    /**
     * Private constructor to prevent instantiation of this utility class.
     * Since this class only contains static methods, there is no need 
     * to ever create a Main object.
     */
    private Main() {
        // Prevent instantiation.
    }

    /**
     * The main method where the application execution begins.
     * * @param args Command line arguments (not used in this application).
     */
    public static void main(String[] args) {
        // First, configure the UI to match the host operating system.
        setSystemLookAndFeel();

        /*
         * Swing GUIs are not thread-safe. All GUI creation and updates 
         * must occur on the Event Dispatch Thread (EDT). 
         * SwingUtilities.invokeLater queues this task to run asynchronously on the EDT.
         */
        SwingUtilities.invokeLater(() -> {
            try {
                ApplicationConfig.validate();

                // Instantiate the main login window.
                LoginFrame loginFrame = new LoginFrame();

                // Make the window visible to the user.
                loginFrame.setVisible(true);
            } catch (IllegalStateException exception) {
                JOptionPane.showMessageDialog(
                        null,
                        "MotorPH could not start because its configuration "
                                + "or data files are unavailable.\n\n"
                                + exception.getMessage()
                                + "\n\nConfiguration: "
                                + ApplicationConfig.getConfigurationFile(),
                        "MotorPH Startup Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }

    /**
     * Attempts to set the application's Look and Feel (L&F) to match the 
     * native operating system (e.g., making it look like a Windows app on Windows, 
     * or a macOS app on macOS), rather than using Java's default "Metal" L&F.
     */
    private static void setSystemLookAndFeel() {
        try {
            // Retrieve the class name for the system's default L&F and apply it.
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName()
            );
        } catch (ClassNotFoundException
                | InstantiationException
                | IllegalAccessException
                | UnsupportedLookAndFeelException exception) {
            
            /* * If the system L&F cannot be applied for any reason, catch the exception
             * and print an error message to the standard error stream. 
             * The application will safely fall back to the default Java Look and Feel.
             */
            System.err.println(
                    "Unable to apply system look and feel: "
                    + exception.getMessage()
            );
        }
    }
}

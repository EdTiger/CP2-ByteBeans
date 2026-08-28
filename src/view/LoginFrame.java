/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package view;

import services.AuthenticationService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;

import static view.UiTheme.*;

/**
 * The main authentication window for the MotorPH Payroll System.
 *
 * <p>Provides a Swing GUI for entering credentials and delegates to the
 * {@link services.AuthenticationService} to determine routing to either the
 * employee portal or the payroll staff portal.</p>
 */
public final class LoginFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JCheckBox showPasswordCheckBox;
    private final JButton loginButton;
    private final JButton exitButton;

    private final char defaultEchoCharacter;

    /**
     * Constructs the LoginFrame, initialises components, and builds the UI.
     */
    public LoginFrame() {
        super("MotorPH Login");

        usernameField = new JTextField(20);
        passwordField = new JPasswordField(20);
        showPasswordCheckBox = new JCheckBox("Show password");
        loginButton = new JButton("Login");
        exitButton = new JButton("Exit");

        defaultEchoCharacter = passwordField.getEchoChar();

        configureWindow();
        buildInterface();
        configureActions();
    }

    /**
     * Configures the main window properties (size, close operations).
     */
    private void configureWindow() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(520, 430));
        setResizable(false);

        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent event) {
                confirmExit();
            }
        });
    }

    /**
     * Builds and assembles the main interface layout.
     */
    private void buildInterface() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BACKGROUND);

        mainPanel.add(createHeaderPanel(), BorderLayout.NORTH);
        mainPanel.add(createLoginPanel(), BorderLayout.CENTER);

        setContentPane(mainPanel);
        pack();
        setSize(520, 430);
        setLocationRelativeTo(null);
    }

    /**
     * Creates the top header panel containing the application title.
     *
     * @return the configured header panel
     */
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(
                new BoxLayout(headerPanel, BoxLayout.Y_AXIS)
        );

        headerPanel.setBackground(NAVY);
        headerPanel.setBorder(
                new EmptyBorder(25, 20, 25, 20)
        );

        JLabel titleLabel =
                new JLabel("MotorPH Payroll System");

        titleLabel.setFont(
                new Font("SansSerif", Font.BOLD, 26)
        );

        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel =
                new JLabel("Employee Management Portal");

        subtitleLabel.setFont(
                new Font("SansSerif", Font.PLAIN, 14)
        );

        subtitleLabel.setForeground(
                HEADER_SUBTITLE
        );

        subtitleLabel.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(6));
        headerPanel.add(subtitleLabel);

        return headerPanel;
    }

    /**
     * Creates the central form panel for credential entry.
     *
     * @return the configured login form panel
     */
    private JPanel createLoginPanel() {
        JPanel wrapperPanel = new JPanel(new GridBagLayout());
        wrapperPanel.setBackground(BACKGROUND);
        wrapperPanel.setBorder(
                new EmptyBorder(22, 40, 25, 40)
        );

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(SURFACE);

        formPanel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER),
                        new EmptyBorder(22, 28, 22, 28)
                )
        );

        GridBagConstraints constraints =
                new GridBagConstraints();

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;

        JLabel formTitle = new JLabel("Sign in");
        formTitle.setFont(
                new Font("SansSerif", Font.BOLD, 20)
        );

        formTitle.setForeground(TEXT_PRIMARY);

        JLabel formSubtitle = new JLabel(
                "Enter your MotorPH account credentials."
        );

        formSubtitle.setFont(
                new Font("SansSerif", Font.PLAIN, 12)
        );

        formSubtitle.setForeground(TEXT_SECONDARY);

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        constraints.insets = new Insets(0, 5, 3, 5);

        formPanel.add(formTitle, constraints);

        constraints.gridy = 1;
        constraints.insets = new Insets(0, 5, 16, 5);

        formPanel.add(formSubtitle, constraints);

        JLabel usernameLabel = new JLabel("Username");
        configureLabel(usernameLabel, usernameField, 'U');

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        constraints.weightx = 0;
        constraints.insets = new Insets(6, 5, 6, 14);

        formPanel.add(usernameLabel, constraints);

        configureTextField(usernameField);

        usernameField.setToolTipText(
                "Enter your configured MotorPH username"
        );

        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.insets = new Insets(6, 5, 6, 5);

        formPanel.add(usernameField, constraints);

        JLabel passwordLabel = new JLabel("Password");
        configureLabel(passwordLabel, passwordField, 'P');

        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.weightx = 0;
        constraints.insets = new Insets(6, 5, 6, 14);

        formPanel.add(passwordLabel, constraints);

        configureTextField(passwordField);

        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.insets = new Insets(6, 5, 6, 5);

        formPanel.add(passwordField, constraints);

        showPasswordCheckBox.setOpaque(false);
        showPasswordCheckBox.setForeground(TEXT_SECONDARY);
        showPasswordCheckBox.setFont(
                new Font("SansSerif", Font.PLAIN, 12)
        );

        showPasswordCheckBox.setMnemonic('S');
        showPasswordCheckBox.setFocusPainted(true);

        constraints.gridx = 1;
        constraints.gridy = 4;
        constraints.insets = new Insets(2, 5, 15, 5);

        formPanel.add(showPasswordCheckBox, constraints);

        JPanel buttonPanel =
                new JPanel(new GridLayout(1, 2, 12, 0));

        buttonPanel.setOpaque(false);

        configureButton(
                loginButton,
                NAVY,
                NAVY_HOVER,
                'L'
        );

        configureButton(
                exitButton,
                SLATE,
                SLATE_HOVER,
                'E'
        );

        buttonPanel.add(loginButton);
        buttonPanel.add(exitButton);

        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.gridwidth = 2;
        constraints.insets = new Insets(6, 5, 0, 5);

        formPanel.add(buttonPanel, constraints);
        wrapperPanel.add(formPanel);

        return wrapperPanel;
    }

    /**
     * Configures a standard label for form fields.
     *
     * @param label     the label to configure
     * @param component the component the label is for
     * @param mnemonic  the keyboard shortcut character
     */
    private void configureLabel(
            JLabel label,
            JComponent component,
            char mnemonic) {

        label.setFont(
                new Font("SansSerif", Font.PLAIN, 13)
        );

        label.setForeground(TEXT_PRIMARY);
        label.setDisplayedMnemonic(mnemonic);
        label.setLabelFor(component);
    }

    /**
     * Hooks up event listeners for buttons and fields.
     */
    private void configureActions() {
        loginButton.addActionListener(
                event -> authenticateUser()
        );

        exitButton.addActionListener(
                event -> confirmExit()
        );

        showPasswordCheckBox.addActionListener(
                event -> togglePasswordVisibility()
        );

        usernameField.addActionListener(
                event -> passwordField.requestFocusInWindow()
        );

        getRootPane().setDefaultButton(loginButton);
    }

    /**
     * Validates input and attempts to authenticate the user.
     */
    private void authenticateUser() {
        String username = usernameField.getText().trim();

        char[] enteredPassword =
                passwordField.getPassword();

        try {
            if (username.isEmpty()) {
                showWarning("Please enter your username.");
                usernameField.requestFocusInWindow();
                return;
            }

            if (enteredPassword.length == 0) {
                showWarning("Please enter your password.");
                passwordField.requestFocusInWindow();
                return;
            }

            String role = AuthenticationService.authenticate(
                    username,
                    enteredPassword
            );

            if (AuthenticationService.EMPLOYEE_ROLE.equals(role)) {
                openEmployeePortal();
            } else if (AuthenticationService.PAYROLL_ROLE.equals(role)) {
                openPayrollPortal();
            } else {
                showLoginError();
            }
        } finally {
            Arrays.fill(enteredPassword, '\0');
        }
    }

    /**
     * Transitions the application to the standard Employee portal.
     */
    private void openEmployeePortal() {
        JOptionPane.showMessageDialog(
                this,
                "Login successful. Welcome, Employee!",
                "Login Successful",
                JOptionPane.INFORMATION_MESSAGE
        );

        EmployeeFrame employeeFrame = new EmployeeFrame();
        employeeFrame.setVisible(true);
        dispose();
    }

    /**
     * Transitions the application to the Payroll Staff portal.
     */
    private void openPayrollPortal() {
        JOptionPane.showMessageDialog(
                this,
                "Login successful. Welcome, Payroll Staff!",
                "Login Successful",
                JOptionPane.INFORMATION_MESSAGE
        );

        PayrollStaffFrame payrollFrame =
                new PayrollStaffFrame();

        payrollFrame.setVisible(true);
        dispose();
    }

    /**
     * Displays a generic error when authentication fails.
     */
    private void showLoginError() {
        JOptionPane.showMessageDialog(
                this,
                "Invalid username or password.",
                "Login Failed",
                JOptionPane.ERROR_MESSAGE
        );

        passwordField.setText("");
        passwordField.requestFocusInWindow();
    }

    /**
     * Displays a warning message to the user.
     *
     * @param message the text to display
     */
    private void showWarning(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Required Information",
                JOptionPane.WARNING_MESSAGE
        );
    }

    /**
     * Toggles the echo character of the password field to show/hide the password.
     */
    private void togglePasswordVisibility() {
        passwordField.setEchoChar(
                showPasswordCheckBox.isSelected()
                        ? (char) 0
                        : defaultEchoCharacter
        );
    }

    /**
     * Prompts the user before exiting the application completely.
     */
    private void confirmExit() {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to exit MotorPH?",
                "Confirm Exit",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (choice == JOptionPane.YES_OPTION) {
            dispose();
        }
    }
}

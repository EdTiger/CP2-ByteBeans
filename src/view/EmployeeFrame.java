/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package view;

import config.ApplicationConfig;
import models.EmployeeInformation;
import services.AtomicFileWriter;
import services.FileHandler;
import services.InputValidator;
import services.PayrollProcessor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import static view.UiTheme.*;

@SuppressWarnings("serial")
public final class EmployeeFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final String[] TABLE_COLUMNS = {
        "Employee Number",
        "Last Name",
        "First Name",
        "SSS Number",
        "PhilHealth Number",
        "TIN",
        "Pag-IBIG Number",
        "Hourly Rate"
    };

    private final ArrayList<EmployeeInformation> employees =
            new ArrayList<>();

    private final DefaultTableModel tableModel;
    private final JTable employeeTable;
    private final TableRowSorter<DefaultTableModel> tableSorter;

    private final JTextField searchField;
    private final JTextField employeeNumberField;
    private final JTextField lastNameField;
    private final JTextField firstNameField;
    private final JTextField sssField;
    private final JTextField philHealthField;
    private final JTextField tinField;
    private final JTextField pagIbigField;
    private final JTextField hourlyRateField;

    private final JButton addButton;
    private final JButton updateButton;
    private final JButton deleteButton;
    private final JButton viewButton;
    private final JButton clearButton;
    private final JButton undoButton;
    private final JButton refreshButton;
    private final JButton logoutButton;

    private final JLabel statusLabel;
    private final JProgressBar progressBar;

    private Integer selectedOriginalEmployeeNumber;
    private boolean busy;

    public EmployeeFrame() {
        super("MotorPH Employee Registry Manager");

        tableModel = createTableModel();
        employeeTable = new JTable(tableModel);
        tableSorter = new TableRowSorter<>(tableModel);

        searchField = new JTextField(25);
        employeeNumberField = new JTextField();
        employeeNumberField.setToolTipText(
                "Employee numbers are permanent after a record is created."
        );
        lastNameField = new JTextField();
        firstNameField = new JTextField();
        sssField = new JTextField();
        philHealthField = new JTextField();
        tinField = new JTextField();
        pagIbigField = new JTextField();
        hourlyRateField = new JTextField();
        hourlyRateField.setToolTipText(
                "For new records or changed rates, Basic Salary is "
                        + "calculated from 168 standard monthly hours."
        );

        addButton = new JButton("Add Record");
        addButton.setToolTipText("Create a new employee record and save to CSV");

        updateButton = new JButton("Update Record");
        updateButton.setToolTipText("Save changes made to the currently selected employee");

        deleteButton = new JButton("Delete Record");
        deleteButton.setToolTipText("Permanently delete the selected employee record");

        viewButton = new JButton("View Details");
        viewButton.setToolTipText("View the full details of the selected employee in a dialog");

        clearButton = new JButton("Clear Form");
        clearButton.setToolTipText("Clear all input fields and deselect the current employee");

        undoButton = new JButton("Undo Last Change");
        undoButton.setToolTipText("Restore the employee records from the most recent backup");

        refreshButton = new JButton("Refresh");
        refreshButton.setToolTipText("Reload employee records from the CSV file");

        logoutButton = new JButton("Logout");
        logoutButton.setToolTipText("Return to the login screen");

        statusLabel = new JLabel("Ready");
        progressBar = new JProgressBar();

        configureWindow();
        buildInterface();
        configureTable();
        configureActions();
        loadEmployeesAsync();
    }

    private DefaultTableModel createTableModel() {
        return new DefaultTableModel(TABLE_COLUMNS, 0) {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) {
                    return Integer.class;
                }

                if (columnIndex == 7) {
                    return Double.class;
                }

                return String.class;
            }
        };
    }

    private void configureWindow() {
        WindowUtilities.configureMaximizedWindow(
                this,
                new Dimension(1100, 680),
                this::confirmLogout
        );
    }

    private void buildInterface() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        mainPanel.setBackground(BACKGROUND);
        mainPanel.add(createHeaderPanel(), BorderLayout.NORTH);
        mainPanel.add(createContentPanel(), BorderLayout.CENTER);
        mainPanel.add(createStatusPanel(), BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());

        headerPanel.setBackground(NAVY);
        headerPanel.setBorder(
                new EmptyBorder(18, 24, 18, 24)
        );

        JPanel titlePanel = new JPanel();

        titlePanel.setOpaque(false);
        titlePanel.setLayout(
                new BoxLayout(titlePanel, BoxLayout.Y_AXIS)
        );

        JLabel titleLabel =
                new JLabel("Employee Registry Manager");

        titleLabel.setFont(
                new Font("SansSerif", Font.BOLD, 25)
        );

        titleLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel(
                "View and maintain MotorPH employee records"
        );

        subtitleLabel.setFont(
                new Font("SansSerif", Font.PLAIN, 13)
        );

        subtitleLabel.setForeground(
                HEADER_SUBTITLE
        );

        titlePanel.add(titleLabel);
        titlePanel.add(Box.createVerticalStrut(4));
        titlePanel.add(subtitleLabel);

        configureButton(
                logoutButton,
                SLATE,
                SLATE_HOVER
        );

        logoutButton.setPreferredSize(
                new Dimension(130, 42)
        );

        logoutButton.setMnemonic('L');

        headerPanel.add(titlePanel, BorderLayout.WEST);
        headerPanel.add(logoutButton, BorderLayout.EAST);

        return headerPanel;
    }

    private JPanel createContentPanel() {
        JPanel contentPanel = new JPanel(new BorderLayout());

        contentPanel.setOpaque(false);
        contentPanel.setBorder(
                new EmptyBorder(14, 14, 10, 14)
        );

        JPanel tablePanel = createTablePanel();
        JPanel formPanel = createFormPanel();

        tablePanel.setMinimumSize(
                new Dimension(760, 500)
        );

        formPanel.setMinimumSize(
                new Dimension(340, 500)
        );

        formPanel.setPreferredSize(
                new Dimension(380, 550)
        );

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                tablePanel,
                formPanel
        );

        splitPane.setResizeWeight(0.70);
        splitPane.setDividerSize(8);
        splitPane.setContinuousLayout(true);
        splitPane.setOneTouchExpandable(true);
        splitPane.setBorder(null);

        contentPanel.add(
                splitPane,
                BorderLayout.CENTER
        );

        SwingUtilities.invokeLater(() -> {
            splitPane.setDividerLocation(0.70);
        });

        return contentPanel;
    }

    private JPanel createTablePanel() {
        JPanel panel =
                new JPanel(new BorderLayout(0, 10));

        panel.setBackground(SURFACE);
        panel.setBorder(createPanelBorder());

        JPanel searchPanel =
                new JPanel(new BorderLayout(10, 0));

        searchPanel.setOpaque(false);

        JLabel searchLabel = new JLabel("Search");

        searchLabel.setFont(
                new Font("SansSerif", Font.PLAIN, 13)
        );

        searchLabel.setForeground(TEXT_PRIMARY);
        searchLabel.setDisplayedMnemonic('S');
        searchLabel.setLabelFor(searchField);

        configureTextField(searchField);

        searchField.setToolTipText(
                "Search any employee table field"
        );

        configureButton(
                refreshButton,
                SLATE,
                SLATE_HOVER
        );

        refreshButton.setPreferredSize(
                new Dimension(120, 36)
        );

        refreshButton.setMnemonic('R');

        searchPanel.add(searchLabel, BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(refreshButton, BorderLayout.EAST);

        JScrollPane tableScrollPane =
                new JScrollPane(employeeTable);

        tableScrollPane.setBorder(
                BorderFactory.createLineBorder(BORDER)
        );

        tableScrollPane.setBackground(SURFACE);

        tableScrollPane.getViewport()
                .setBackground(SURFACE);

        tableScrollPane.getHorizontalScrollBar()
                .setUnitIncrement(20);

        tableScrollPane.getVerticalScrollBar()
                .setUnitIncrement(18);

        JLabel tableHint = new JLabel(
                "Select a row to view, update, or delete. "
                        + "Enter views, Delete removes, Ctrl+F searches, "
                        + "and F5 refreshes."
        );

        tableHint.setFont(
                new Font("SansSerif", Font.PLAIN, 12)
        );

        tableHint.setForeground(TEXT_SECONDARY);

        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(tableScrollPane, BorderLayout.CENTER);
        panel.add(tableHint, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createFormPanel() {
        JPanel outerPanel =
                new JPanel(new BorderLayout());

        outerPanel.setBackground(SURFACE);
        outerPanel.setBorder(createPanelBorder());

        JPanel formContent = new JPanel();

        formContent.setOpaque(false);

        formContent.setLayout(
                new BoxLayout(
                        formContent,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel formTitle =
                new JLabel("Employee Information");

        formTitle.setFont(
                new Font("SansSerif", Font.BOLD, 19)
        );

        formTitle.setForeground(TEXT_PRIMARY);

        formTitle.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        JLabel formSubtitle = new JLabel(
                "Complete all fields before saving."
        );

        formSubtitle.setFont(
                new Font("SansSerif", Font.PLAIN, 12)
        );

        formSubtitle.setForeground(TEXT_SECONDARY);

        formSubtitle.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        JPanel fieldsPanel =
                new JPanel(new GridBagLayout());

        fieldsPanel.setOpaque(false);

        fieldsPanel.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        GridBagConstraints constraints =
                new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.fill =
                GridBagConstraints.HORIZONTAL;

        addFormField(
                fieldsPanel,
                constraints,
                "Employee Number",
                employeeNumberField,
                'N'
        );

        addFormField(
                fieldsPanel,
                constraints,
                "Last Name",
                lastNameField,
                'A'
        );

        addFormField(
                fieldsPanel,
                constraints,
                "First Name",
                firstNameField,
                'F'
        );

        addFormField(
                fieldsPanel,
                constraints,
                "SSS Number",
                sssField,
                'G'
        );

        addFormField(
                fieldsPanel,
                constraints,
                "PhilHealth Number",
                philHealthField,
                'H'
        );

        addFormField(
                fieldsPanel,
                constraints,
                "TIN",
                tinField,
                'T'
        );

        addFormField(
                fieldsPanel,
                constraints,
                "Pag-IBIG Number",
                pagIbigField,
                'P'
        );

        addFormField(
                fieldsPanel,
                constraints,
                "Hourly Rate",
                hourlyRateField,
                'Y'
        );

        fieldsPanel.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        fieldsPanel
                                .getPreferredSize()
                                .height
                )
        );

        JLabel formatHint = new JLabel(
                "<html>Government IDs may contain digits, spaces, "
                        + "or hyphens.</html>"
        );

        formatHint.setFont(
                new Font("SansSerif", Font.PLAIN, 11)
        );

        formatHint.setForeground(TEXT_SECONDARY);

        formatHint.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        JPanel buttonPanel =
                new JPanel(new GridLayout(4, 2, 9, 9));

        buttonPanel.setOpaque(false);

        buttonPanel.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        buttonPanel.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        135
                )
        );

        configureButton(
                addButton,
                NAVY,
                NAVY_HOVER
        );

        configureButton(
                updateButton,
                NAVY,
                NAVY_HOVER
        );

        configureButton(
                deleteButton,
                SLATE,
                SLATE_HOVER
        );

        configureButton(
                viewButton,
                SLATE,
                SLATE_HOVER
        );

        configureButton(
                clearButton,
                SLATE,
                SLATE_HOVER
        );

        JButton closeButton = new JButton("Close");

        configureButton(
                closeButton,
                SLATE,
                SLATE_HOVER
        );

        configureButton(
                undoButton,
                SLATE,
                SLATE_HOVER
        );

        addButton.setMnemonic('A');
        updateButton.setMnemonic('U');
        deleteButton.setMnemonic('D');
        viewButton.setMnemonic('V');
        clearButton.setMnemonic('C');
        undoButton.setMnemonic('Z');

        closeButton.addActionListener(
                event -> confirmLogout()
        );

        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(viewButton);
        buttonPanel.add(undoButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(closeButton);

        formContent.add(formTitle);
        formContent.add(Box.createVerticalStrut(3));
        formContent.add(formSubtitle);
        formContent.add(Box.createVerticalStrut(12));
        formContent.add(fieldsPanel);
        formContent.add(Box.createVerticalStrut(4));
        formContent.add(formatHint);
        formContent.add(Box.createVerticalStrut(14));
        formContent.add(buttonPanel);
        formContent.add(Box.createVerticalGlue());

        JScrollPane formScrollPane =
                new JScrollPane(
                        formContent,
                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                );

        formScrollPane.setBorder(null);
        formScrollPane.setOpaque(false);

        formScrollPane.getViewport()
                .setOpaque(false);

        formScrollPane.getVerticalScrollBar()
                .setUnitIncrement(16);

        outerPanel.add(
                formScrollPane,
                BorderLayout.CENTER
        );

        return outerPanel;
    }

    private void addFormField(
            JPanel panel,
            GridBagConstraints constraints,
            String labelText,
            JTextField field,
            char mnemonic) {

        JLabel label = new JLabel(labelText);

        label.setFont(
                new Font("SansSerif", Font.PLAIN, 12)
        );

        label.setForeground(TEXT_PRIMARY);
        label.setDisplayedMnemonic(mnemonic);
        label.setLabelFor(field);

        configureTextField(field);

        field.getAccessibleContext()
                .setAccessibleName(labelText);

        constraints.gridx = 0;
        constraints.gridwidth = 1;
        constraints.weightx = 1;

        constraints.insets =
                new Insets(3, 2, 2, 2);

        panel.add(label, constraints);

        constraints.gridy++;

        constraints.insets =
                new Insets(0, 2, 7, 2);

        panel.add(field, constraints);

        constraints.gridy++;
    }

    private JPanel createStatusPanel() {
        JPanel statusPanel =
                new JPanel(new BorderLayout(10, 0));

        statusPanel.setBackground(
                STATUS_BACKGROUND
        );

        statusPanel.setBorder(
                new EmptyBorder(7, 15, 7, 15)
        );

        statusLabel.setFont(
                new Font("SansSerif", Font.PLAIN, 12)
        );

        statusLabel.setForeground(TEXT_SECONDARY);

        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);

        progressBar.setPreferredSize(
                new Dimension(150, 16)
        );

        progressBar.setForeground(NAVY);
        progressBar.getAccessibleContext()
                .setAccessibleName("Employee data operation progress");

        statusPanel.add(
                statusLabel,
                BorderLayout.WEST
        );

        statusPanel.add(
                progressBar,
                BorderLayout.EAST
        );

        return statusPanel;
    }

    private void configureTable() {
        TableUtilities.configureStandardTable(employeeTable, tableSorter);
        employeeTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JTableHeader header = employeeTable.getTableHeader();
        configureTableHeader(header);
        header.setPreferredSize(new Dimension(0, 34));

        int[] preferredWidths = { 125, 110, 125, 125, 140, 130, 140, 100 };
        int[] minimumWidths = { 105, 85, 100, 100, 115, 105, 115, 90 };
        TableUtilities.setColumnWidths(employeeTable, preferredWidths, minimumWidths);

        DefaultTableCellRenderer employeeNumberRenderer =
                new DefaultTableCellRenderer();

        employeeNumberRenderer.setHorizontalAlignment(
                SwingConstants.LEFT
        );

        employeeNumberRenderer.setBorder(
                new EmptyBorder(0, 6, 0, 6)
        );

        employeeTable.getColumnModel()
                .getColumn(0)
                .setCellRenderer(
                        employeeNumberRenderer
                );

        DefaultTableCellRenderer hourlyRateRenderer = TableUtilities.createDecimalRenderer();
        hourlyRateRenderer.setBorder(new EmptyBorder(0, 6, 0, 6));

        employeeTable.getColumnModel()
                .getColumn(7)
                .setCellRenderer(hourlyRateRenderer);

        employeeTable.getSelectionModel()
                .addListSelectionListener(event -> {

                    if (!event.getValueIsAdjusting()) {
                        populateFormFromSelectedRow();
                    }
                });

        employeeTable.getAccessibleContext()
                .setAccessibleName("Employee records");

        employeeTable.getInputMap(
                JComponent.WHEN_FOCUSED
        ).put(
                KeyStroke.getKeyStroke(
                        KeyEvent.VK_ENTER,
                        0
                ),
                "viewEmployee"
        );

        employeeTable.getActionMap().put(
                "viewEmployee",
                new AbstractAction() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void actionPerformed(
                            java.awt.event.ActionEvent event) {

                        viewSelectedEmployee();
                    }
                }
        );

        employeeTable.getInputMap(
                JComponent.WHEN_FOCUSED
        ).put(
                KeyStroke.getKeyStroke(
                        KeyEvent.VK_DELETE,
                        0
                ),
                "deleteEmployee"
        );

        employeeTable.getActionMap().put(
                "deleteEmployee",
                new AbstractAction() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void actionPerformed(
                            java.awt.event.ActionEvent event) {

                        deleteSelectedEmployee();
                    }
                }
        );
    }

    private void configureActions() {
        addButton.addActionListener(
                event -> addEmployee()
        );

        updateButton.addActionListener(
                event -> updateEmployee()
        );

        deleteButton.addActionListener(
                event -> deleteSelectedEmployee()
        );

        viewButton.addActionListener(
                event -> viewSelectedEmployee()
        );

        clearButton.addActionListener(
                event -> clearForm()
        );

        undoButton.addActionListener(
                event -> undoLastChange()
        );

        refreshButton.addActionListener(
                event -> loadEmployeesAsync()
        );

        logoutButton.addActionListener(
                event -> confirmLogout()
        );

        FormValidationUtilities.addLiveValidation(
                sssField,
                InputValidator::isValidSSSNumber
        );
        FormValidationUtilities.addLiveValidation(
                philHealthField,
                InputValidator::isValidPhilHealthNumber
        );
        FormValidationUtilities.addLiveValidation(
                tinField,
                InputValidator::isValidTinNumber
        );
        FormValidationUtilities.addLiveValidation(
                pagIbigField,
                InputValidator::isValidPagIbigNumber
        );
        FormValidationUtilities.addLiveValidation(
                hourlyRateField,
                InputValidator::isValidHourlyRate
        );

        searchField.getDocument().addDocumentListener(
                TableUtilities.createSearchDocumentListener(
                        tableSorter,
                        searchField,
                        this::updateStatusLabel
                )
        );

        installSearchAndRefreshShortcuts(
                getRootPane(),
                searchField,
                this::loadEmployeesAsync
        );

        updateActionAvailability();
    }

    private void loadEmployeesAsync() {
        if (busy) {
            return;
        }

        setBusy(
                true,
                "Loading employee records..."
        );

        SwingWorker<ArrayList<EmployeeInformation>, Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected ArrayList<EmployeeInformation>
                            doInBackground() throws Exception {

                        return FileHandler.loadEmployees();
                    }

                    @Override
                    protected void done() {
                        try {
                            applyEmployeeData(get());

                            setBusy(
                                    false,
                                    employees.size()
                                            + " employee record(s) loaded."
                            );
                        } catch (InterruptedException exception) {
                            Thread.currentThread().interrupt();

                            setBusy(
                                    false,
                                    "Loading interrupted."
                            );
                        } catch (ExecutionException exception) {
                            setBusy(
                                    false,
                                    "Unable to load employee records."
                            );

                            showError(
                                    "Unable to load employee records.",
                                    exception.getCause()
                            );
                        }
                    }
                };

        worker.execute();
    }

    private void applyEmployeeData(
            ArrayList<EmployeeInformation> loadedEmployees) {

        employees.clear();
        employees.addAll(loadedEmployees);

        tableModel.setRowCount(0);

        for (EmployeeInformation employee : employees) {
            tableModel.addRow(new Object[]{
                employee.getEmployeeNumber(),
                employee.getLastName(),
                employee.getFirstName(),
                employee.getSssNumber(),
                employee.getPhilHealthNumber(),
                employee.getTinNumber(),
                employee.getPagIbigNumber(),
                employee.getHourlyRate()
            });
        }

        clearForm();
    }

    private void updateStatusLabel() {
        statusLabel.setText(
                employeeTable.getRowCount()
                        + " matching record(s)."
        );
    }

    private void addEmployee() {
        EmployeeInformation employee =
                createEmployeeFromForm(false);

        if (employee == null) {
            return;
        }

        executeEmployeeOperation(
                "Adding employee record...",
                "Employee record added successfully.",
                () -> FileHandler.addEmployee(employee)
        );
    }

    private void updateEmployee() {
        if (selectedOriginalEmployeeNumber == null) {
            showWarning(
                    "Select an employee record before updating."
            );

            return;
        }

        EmployeeInformation updatedEmployee =
                createEmployeeFromForm(true);

        if (updatedEmployee == null) {
            return;
        }

        int originalEmployeeNumber =
                selectedOriginalEmployeeNumber;

        executeEmployeeOperation(
                "Updating employee record...",
                "Employee record updated successfully.",
                () -> FileHandler.updateEmployee(
                        originalEmployeeNumber,
                        updatedEmployee
                )
        );
    }

    private void deleteSelectedEmployee() {
        EmployeeInformation employee =
                getSelectedEmployee();

        if (employee == null) {
            showWarning(
                    "Select an employee record before deleting."
            );

            return;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Delete employee "
                        + employee.getEmployeeNumber()
                        + " - "
                        + employee.getDisplayName()
                        + "?\n\nThis action cannot be undone.",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        executeEmployeeOperation(
                "Deleting employee record...",
                "Employee record deleted successfully.",
                () -> {
                    boolean deleted =
                            FileHandler.deleteEmployee(
                                    employee.getEmployeeNumber()
                            );

                    if (!deleted) {
                        throw new IllegalArgumentException(
                                "The employee record no longer exists."
                        );
                    }
                }
        );
    }

    private void executeEmployeeOperation(
            String busyMessage,
            String successMessage,
            FileOperation operation) {

        if (busy) {
            return;
        }

        setBusy(true, busyMessage);

        SwingWorker<ArrayList<EmployeeInformation>, Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected ArrayList<EmployeeInformation>
                            doInBackground() throws Exception {

                        operation.execute();

                        return FileHandler.loadEmployees();
                    }

                    @Override
                    protected void done() {
                        try {
                            applyEmployeeData(get());
                            setBusy(false, successMessage);

                            JOptionPane.showMessageDialog(
                                    EmployeeFrame.this,
                                    successMessage,
                                    "Operation Successful",
                                    JOptionPane.INFORMATION_MESSAGE
                            );
                        } catch (InterruptedException exception) {
                            Thread.currentThread().interrupt();

                            setBusy(
                                    false,
                                    "Operation interrupted."
                            );
                        } catch (ExecutionException exception) {
                            setBusy(
                                    false,
                                    "Operation failed."
                            );

                            showError(
                                    "The employee record could not be saved.",
                                    exception.getCause()
                            );
                        }
                    }
                };

        worker.execute();
    }

    private EmployeeInformation createEmployeeFromForm(
            boolean preserveExistingBasicSalary) {
        String employeeNumber =
                employeeNumberField.getText().trim();

        String lastName =
                lastNameField.getText().trim();

        String firstName =
                firstNameField.getText().trim();

        String sssNumber =
                sssField.getText().trim();

        String philHealthNumber =
                philHealthField.getText().trim();

        String tinNumber =
                tinField.getText().trim();

        String pagIbigNumber =
                pagIbigField.getText().trim();

        String hourlyRate =
                hourlyRateField.getText().trim();

        String validationMessage =
                InputValidator.validateEmployeeInput(
                        employeeNumber,
                        lastName,
                        firstName,
                        sssNumber,
                        philHealthNumber,
                        tinNumber,
                        pagIbigNumber,
                        hourlyRate
                );

        if (validationMessage != null) {
            showWarning(validationMessage);
            focusEmployeeField(
                    InputValidator.findFirstInvalidEmployeeField(
                            employeeNumber,
                            lastName,
                            firstName,
                            sssNumber,
                            philHealthNumber,
                            tinNumber,
                            pagIbigNumber,
                            hourlyRate
                    )
            );
            return null;
        }

        double parsedHourlyRate =
                InputValidator.parseDecimal(hourlyRate);

        double basicSalary = PayrollProcessor
                .computeMonthlyBasicSalary(parsedHourlyRate);

        if (preserveExistingBasicSalary) {
            EmployeeInformation selectedEmployee =
                    getSelectedEmployee();

            if (selectedEmployee != null
                    && Double.compare(
                            parsedHourlyRate,
                            selectedEmployee.getHourlyRate()
                    ) == 0) {

                basicSalary = selectedEmployee.getBasicSalary();
            }
        }

        return new EmployeeInformation(
                InputValidator.parseEmployeeNumber(
                        employeeNumber
                ),
                lastName,
                firstName,
                sssNumber,
                philHealthNumber,
                tinNumber,
                pagIbigNumber,
                basicSalary,
                parsedHourlyRate
        );
    }

    private void focusEmployeeField(int fieldIndex) {
        JTextField[] fields = {
            employeeNumberField,
            lastNameField,
            firstNameField,
            sssField,
            philHealthField,
            tinField,
            pagIbigField,
            hourlyRateField
        };

        if (fieldIndex < 0 || fieldIndex >= fields.length) {
            return;
        }

        JTextField invalidField = fields[fieldIndex];
        invalidField.requestFocusInWindow();
        invalidField.selectAll();
    }

    private void populateFormFromSelectedRow() {
        EmployeeInformation employee =
                getSelectedEmployee();

        if (employee == null) {
            selectedOriginalEmployeeNumber = null;
            updateActionAvailability();
            return;
        }

        selectedOriginalEmployeeNumber =
                employee.getEmployeeNumber();

        employeeNumberField.setText(
                String.valueOf(
                        employee.getEmployeeNumber()
                )
        );
        employeeNumberField.setEditable(false);

        lastNameField.setText(
                employee.getLastName()
        );

        firstNameField.setText(
                employee.getFirstName()
        );

        sssField.setText(
                employee.getSssNumber()
        );

        philHealthField.setText(
                employee.getPhilHealthNumber()
        );

        tinField.setText(
                employee.getTinNumber()
        );

        pagIbigField.setText(
                employee.getPagIbigNumber()
        );

        hourlyRateField.setText(
                String.format(
                        Locale.US,
                        "%.2f",
                        employee.getHourlyRate()
                )
        );

        statusLabel.setText(
                "Selected employee "
                        + employee.getEmployeeNumber()
                        + "."
        );

        updateActionAvailability();
    }

    private EmployeeInformation getSelectedEmployee() {
        int selectedViewRow =
                employeeTable.getSelectedRow();

        if (selectedViewRow < 0) {
            return null;
        }

        int selectedModelRow =
                employeeTable.convertRowIndexToModel(
                        selectedViewRow
                );

        if (selectedModelRow < 0
                || selectedModelRow >= employees.size()) {

            return null;
        }

        return employees.get(selectedModelRow);
    }

    private void viewSelectedEmployee() {
        EmployeeInformation employee =
                getSelectedEmployee();

        if (employee == null) {
            showWarning(
                    "Select an employee record to view."
            );

            return;
        }

        JOptionPane.showMessageDialog(
                this,
                EmployeeDisplayModule.createDetailsHtml(employee),
                "Employee Details",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void clearForm() {
        employeeTable.clearSelection();

        employeeNumberField.setText("");
        employeeNumberField.setEditable(!busy);
        lastNameField.setText("");
        firstNameField.setText("");
        sssField.setText("");
        philHealthField.setText("");
        tinField.setText("");
        pagIbigField.setText("");
        hourlyRateField.setText("");

        selectedOriginalEmployeeNumber = null;

        updateActionAvailability();

        if (!busy) {
            employeeNumberField.requestFocusInWindow();
        }
    }

    private void undoLastChange() {
        if (busy) {
            DialogUtilities.showWarning(this, "Busy", "Please wait for the current operation to finish.");
            return;
        }

        Path targetFile = ApplicationConfig.getEmployeeFile();
        if (!AtomicFileWriter.hasBackup(targetFile)) {
            DialogUtilities.showWarning(this, "Undo Unavailable", "No backup is available to restore.");
            return;
        }

        if (DialogUtilities.confirmAction(
                this,
                "Confirm Undo",
                "Restore the previous employee records? This will undo your last save.")) {
            setBusy(true, "Restoring previous employee records...");

            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    return AtomicFileWriter.restoreBackup(targetFile);
                }

                @Override
                protected void done() {
                    try {
                        boolean restored = get();
                        setBusy(false, restored
                                ? "Backup restored successfully."
                                : "No backup was restored.");

                        if (!restored) {
                            DialogUtilities.showWarning(
                                    EmployeeFrame.this,
                                    "Undo Failed",
                                    "No backup was found to restore."
                            );
                            return;
                        }

                        JOptionPane.showMessageDialog(
                                EmployeeFrame.this,
                                "Backup restored successfully.",
                                "Undo Complete",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                        loadEmployeesAsync();
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        setBusy(false, "Backup restoration interrupted.");
                    } catch (ExecutionException exception) {
                        setBusy(false, "Backup restoration failed.");
                        DialogUtilities.showError(
                                EmployeeFrame.this,
                                "Undo Error",
                                "Failed to restore backup.",
                                exception.getCause()
                        );
                    }
                }
            };

            worker.execute();
        }
    }

    private void updateActionAvailability() {
        boolean hasSelection =
                !busy
                        && selectedOriginalEmployeeNumber != null;

        updateButton.setEnabled(hasSelection);
        deleteButton.setEnabled(hasSelection);
        viewButton.setEnabled(hasSelection);
    }

    private void setBusy(
            boolean busy,
            String message) {

        this.busy = busy;

        BusyStateUtilities.applyBusyState(
                this,
                busy,
                statusLabel,
                progressBar,
                message,
                true,
                addButton,
                undoButton,
                clearButton,
                refreshButton,
                logoutButton,
                searchField,
                employeeTable
        );

        setFormEnabled(!busy);
        updateActionAvailability();
    }

    private void setFormEnabled(boolean enabled) {
        employeeNumberField.setEnabled(enabled);
        employeeNumberField.setEditable(
                enabled && selectedOriginalEmployeeNumber == null
        );
        lastNameField.setEnabled(enabled);
        firstNameField.setEnabled(enabled);
        sssField.setEnabled(enabled);
        philHealthField.setEnabled(enabled);
        tinField.setEnabled(enabled);
        pagIbigField.setEnabled(enabled);
        hourlyRateField.setEnabled(enabled);
    }

    private void showWarning(String message) {
        DialogUtilities.showWarning(this, "Validation Warning", message);
    }

    private void showError(
            String message,
            Throwable throwable) {

        DialogUtilities.showError(
                this,
                "Operation Failed",
                message,
                throwable
        );
    }

    private void confirmLogout() {
        if (busy) {
            showWarning(
                    "Please wait for the current operation to finish."
            );
            return;
        }

        if (DialogUtilities.confirmAction(
                this,
                "Confirm Logout",
                "Log out and return to the login screen?")) {
            WindowUtilities.performLogout(this);
        }
    }

    @FunctionalInterface
    private interface FileOperation {

        void execute() throws Exception;
    }
}

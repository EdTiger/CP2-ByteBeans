/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package view;

import models.EmployeeInformation;
import services.FileHandler;
import services.PayrollProcessor;
import services.PayrollProcessor.PayrollResult;
import services.AtomicFileWriter;
import config.ApplicationConfig;
import java.nio.file.Path;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import static view.UiTheme.*;

@SuppressWarnings("serial")
public final class PayrollStaffFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("M/d/uuuu")
                    .withResolverStyle(ResolverStyle.STRICT);

    private static final DateTimeFormatter MONTH_FORMAT =
            DateTimeFormatter.ofPattern("MMMM uuuu");

    private static final String[] TABLE_COLUMNS = {
        "Employee No.",
        "Employee Name",
        "1st Cutoff Hours",
        "2nd Cutoff Hours",
        "Total Hours",
        "Hourly Rate",
        "Gross Pay",
        "SSS",
        "PhilHealth (Employee)",
        "Pag-IBIG (Employee)",
        "Withholding Tax",
        "Total Deductions",
        "Net Pay"
    };

    private final ArrayList<EmployeeInformation> employees =
            new ArrayList<>();

    private final ArrayList<String[]> attendanceRecords =
            new ArrayList<>();

    private final ArrayList<PayrollRow> payrollRows =
            new ArrayList<>();

    private final JComboBox<EmployeeInformation> employeeComboBox;
    private final JComboBox<YearMonth> monthComboBox;
    private final JTextField searchField;
    private final JTextArea payrollDetailsArea;

    private final DefaultTableModel tableModel;
    private final JTable payrollTable;
    private final TableRowSorter<DefaultTableModel> tableSorter;

    private final JButton computeSelectedButton;
    private final JButton computeAllButton;
    private final JButton generateSummaryButton;
    private final JButton savePayrollButton;
    private final JButton restoreBackupButton;
    private final JButton refreshButton;
    private final JButton clearButton;
    private final JButton logoutButton;

    private final JLabel statusLabel;
    private final JProgressBar progressBar;

    private boolean busy;
    private boolean dataLoaded;

    public PayrollStaffFrame() {
        super("MotorPH Payroll Processing");

        employeeComboBox = new JComboBox<>();
        monthComboBox = new JComboBox<>();
        searchField = new JTextField(25);
        payrollDetailsArea = new JTextArea();

        tableModel = createTableModel();
        payrollTable = new JTable(tableModel);
        tableSorter = new TableRowSorter<>(tableModel);

        computeSelectedButton =
                new JButton("Compute Selected");
        computeSelectedButton.setToolTipText("Compute payroll only for the employee currently selected in the dropdown");

        computeAllButton =
                new JButton("Compute All Employees");
        computeAllButton.setToolTipText("Compute payroll for all active employees for the selected month");

        generateSummaryButton =
                new JButton("Generate Summary");
        generateSummaryButton.setToolTipText("View aggregate payroll totals for all employees in the selected month");

        savePayrollButton =
                new JButton("Save Payroll Data");
        savePayrollButton.setToolTipText("Commit the computed payroll results to the CSV file");

        restoreBackupButton =
                new JButton("Undo Last Save");
        restoreBackupButton.setToolTipText("Restore the payroll CSV file from the most recent backup");

        refreshButton = new JButton("Refresh Data");
        refreshButton.setToolTipText("Reload employee and attendance records from CSV");

        clearButton = new JButton("Clear Results");
        clearButton.setToolTipText("Clear the computed payroll table and reset selections");

        logoutButton = new JButton("Logout");
        logoutButton.setToolTipText("Return to the login screen");

        statusLabel = new JLabel("Ready");
        progressBar = new JProgressBar();

        configureWindow();
        buildInterface();
        configureTable();
        configureComboBoxes();
        configureActions();
        loadDataAsync();
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

                if (columnIndex >= 2) {
                    return Double.class;
                }

                return String.class;
            }
        };
    }

    private void configureWindow() {
        WindowUtilities.configureCenteredWindow(
                this,
                new Dimension(1100, 700),
                new Dimension(1400, 820),
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
                new JLabel("MotorPH Payroll Processing");

        titleLabel.setFont(
                new Font("SansSerif", Font.BOLD, 25)
        );

        titleLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel(
                "Compute, review, and save employee salaries"
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
        JPanel contentPanel =
                new JPanel(new BorderLayout(0, 12));

        contentPanel.setOpaque(false);
        contentPanel.setBorder(
                new EmptyBorder(14, 14, 10, 14)
        );

        contentPanel.add(
                createControlPanel(),
                BorderLayout.NORTH
        );

        JPanel tablePanel = createTablePanel();
        JPanel detailsPanel = createDetailsPanel();

        tablePanel.setMinimumSize(
                new Dimension(700, 280)
        );

        detailsPanel.setMinimumSize(
                new Dimension(700, 190)
        );

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                tablePanel,
                detailsPanel
        );

        splitPane.setResizeWeight(0.64);
        splitPane.setDividerSize(8);
        splitPane.setContinuousLayout(true);
        splitPane.setOneTouchExpandable(true);
        splitPane.setBorder(null);

        contentPanel.add(splitPane, BorderLayout.CENTER);

        SwingUtilities.invokeLater(
                () -> splitPane.setDividerLocation(0.64)
        );

        return contentPanel;
    }

    private JPanel createControlPanel() {
        JPanel outerPanel =
                new JPanel(new BorderLayout(0, 12));

        outerPanel.setBackground(SURFACE);
        outerPanel.setBorder(createPanelBorder());

        JPanel selectionPanel =
                new JPanel(new GridBagLayout());

        selectionPanel.setOpaque(false);

        GridBagConstraints constraints =
                new GridBagConstraints();

        constraints.insets = new Insets(4, 6, 4, 6);
        constraints.fill = GridBagConstraints.HORIZONTAL;

        JLabel employeeLabel = createLabel(
                "Employee",
                employeeComboBox,
                'E'
        );

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0;

        selectionPanel.add(employeeLabel, constraints);

        employeeComboBox.setPreferredSize(
                new Dimension(300, 34)
        );

        constraints.gridx = 1;
        constraints.weightx = 1;

        selectionPanel.add(employeeComboBox, constraints);

        JLabel monthLabel = createLabel(
                "Payroll Month",
                monthComboBox,
                'M'
        );

        constraints.gridx = 2;
        constraints.weightx = 0;

        selectionPanel.add(monthLabel, constraints);

        monthComboBox.setPreferredSize(
                new Dimension(190, 34)
        );

        constraints.gridx = 3;
        constraints.weightx = 0.5;

        selectionPanel.add(monthComboBox, constraints);

        JPanel buttonPanel =
                new JPanel(new GridLayout(2, 4, 10, 8));

        buttonPanel.setOpaque(false);

        configureButton(
                computeSelectedButton,
                NAVY,
                NAVY_HOVER
        );

        configureButton(
                computeAllButton,
                NAVY,
                NAVY_HOVER
        );

        configureButton(
                generateSummaryButton,
                NAVY,
                NAVY_HOVER
        );

        configureButton(
                savePayrollButton,
                NAVY,
                NAVY_HOVER
        );

        configureButton(
                restoreBackupButton,
                SLATE,
                SLATE_HOVER
        );

        configureButton(
                refreshButton,
                SLATE,
                SLATE_HOVER
        );

        configureButton(
                clearButton,
                SLATE,
                SLATE_HOVER
        );

        computeSelectedButton.setMnemonic('S');
        computeAllButton.setMnemonic('A');
        generateSummaryButton.setMnemonic('G');
        savePayrollButton.setMnemonic('V');
        restoreBackupButton.setMnemonic('U');
        refreshButton.setMnemonic('R');
        clearButton.setMnemonic('C');

        buttonPanel.add(computeSelectedButton);
        buttonPanel.add(computeAllButton);
        buttonPanel.add(generateSummaryButton);
        buttonPanel.add(savePayrollButton);
        buttonPanel.add(restoreBackupButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(clearButton);

        outerPanel.add(selectionPanel, BorderLayout.NORTH);
        outerPanel.add(buttonPanel, BorderLayout.SOUTH);

        return outerPanel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 9));
        panel.setBackground(SURFACE);
        panel.setBorder(createPanelBorder());

        JPanel searchPanel =
                new JPanel(new BorderLayout(10, 0));

        searchPanel.setOpaque(false);

        JLabel searchLabel = createLabel(
                "Search Results",
                searchField,
                'H'
        );

        configureTextField(searchField);

        searchField.setToolTipText(
                "Search any computed payroll field"
        );

        searchPanel.add(searchLabel, BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);

        JScrollPane tableScrollPane =
                new JScrollPane(payrollTable);

        tableScrollPane.setBorder(
                BorderFactory.createLineBorder(BORDER)
        );

        tableScrollPane.getHorizontalScrollBar()
                .setUnitIncrement(20);

        tableScrollPane.getVerticalScrollBar()
                .setUnitIncrement(18);

        JLabel tableHint = new JLabel(
                "Select a row for its payroll breakdown. "
                        + "Ctrl+F searches and F5 refreshes source data."
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

    private JPanel createDetailsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(SURFACE);
        panel.setBorder(createPanelBorder());

        JLabel titleLabel =
                new JLabel("Payroll Breakdown");

        titleLabel.setFont(
                new Font("SansSerif", Font.BOLD, 18)
        );

        titleLabel.setForeground(TEXT_PRIMARY);

        payrollDetailsArea.setEditable(false);
        payrollDetailsArea.setRows(10);

        payrollDetailsArea.setFont(
                new Font("Monospaced", Font.PLAIN, 13)
        );

        payrollDetailsArea.setForeground(TEXT_PRIMARY);
        payrollDetailsArea.setBackground(
                DETAILS_BACKGROUND
        );

        payrollDetailsArea.setCaretColor(TEXT_PRIMARY);

        payrollDetailsArea.setMargin(
                new Insets(10, 12, 10, 12)
        );

        payrollDetailsArea.setLineWrap(false);

        payrollDetailsArea.setText(
                "Select a payroll month and employee, "
                        + "then compute salaries."
        );

        payrollDetailsArea.getAccessibleContext()
                .setAccessibleName("Payroll breakdown");

        JScrollPane detailsScrollPane =
                new JScrollPane(payrollDetailsArea);

        detailsScrollPane.setBorder(
                BorderFactory.createLineBorder(BORDER)
        );

        detailsScrollPane.getVerticalScrollBar()
                .setUnitIncrement(16);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(detailsScrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));

        panel.setBackground(
                STATUS_BACKGROUND
        );

        panel.setBorder(
                new EmptyBorder(7, 14, 7, 14)
        );

        statusLabel.setFont(
                new Font("SansSerif", Font.PLAIN, 12)
        );

        statusLabel.setForeground(TEXT_SECONDARY);

        progressBar.setVisible(false);
        progressBar.setPreferredSize(
                new Dimension(180, 18)
        );

        progressBar.setForeground(NAVY);
        progressBar.getAccessibleContext()
                .setAccessibleName("Payroll computation progress");

        panel.add(statusLabel, BorderLayout.WEST);
        panel.add(progressBar, BorderLayout.EAST);

        return panel;
    }

    private JLabel createLabel(
            String text,
            JComponent component,
            char mnemonic) {

        JLabel label = new JLabel(text);

        label.setFont(
                new Font("SansSerif", Font.PLAIN, 13)
        );

        label.setForeground(TEXT_PRIMARY);
        label.setDisplayedMnemonic(mnemonic);
        label.setLabelFor(component);

        return label;
    }

    private void configureTable() {
        TableUtilities.configureStandardTable(payrollTable, tableSorter);
        payrollTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JTableHeader header = payrollTable.getTableHeader();
        configureTableHeader(header);

        int[] columnWidths = {
            105, 180, 120, 120, 100, 100, 115, 90, 155, 155, 130, 135, 115
        };
        TableUtilities.setColumnWidths(payrollTable, columnWidths, null);

        DefaultTableCellRenderer decimalRenderer = TableUtilities.createDecimalRenderer();

        for (int column = 2;
                column < TABLE_COLUMNS.length;
                column++) {

            payrollTable.getColumnModel()
                    .getColumn(column)
                    .setCellRenderer(decimalRenderer);
        }

        payrollTable.getAccessibleContext()
                .setAccessibleName(
                        "Computed payroll results"
                );

        payrollTable.getSelectionModel()
                .addListSelectionListener(event -> {

                    if (!event.getValueIsAdjusting()) {
                        displaySelectedPayroll();
                    }
                });
    }

    private void configureComboBoxes() {
        employeeComboBox.setFont(
                new Font("SansSerif", Font.PLAIN, 13)
        );

        monthComboBox.setFont(
                new Font("SansSerif", Font.PLAIN, 13)
        );

        employeeComboBox.setRenderer(
                new DefaultListCellRenderer() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public Component getListCellRendererComponent(
                            JList<?> list,
                            Object value,
                            int index,
                            boolean isSelected,
                            boolean cellHasFocus) {

                        super.getListCellRendererComponent(
                                list,
                                value,
                                index,
                                isSelected,
                                cellHasFocus
                        );

                        if (value instanceof EmployeeInformation) {
                            EmployeeInformation employee =
                                    (EmployeeInformation) value;

                            setText(
                                    employee.getEmployeeNumber()
                                            + " - "
                                            + employee.getDisplayName()
                            );
                        }

                        return this;
                    }
                }
        );

        monthComboBox.setRenderer(
                new DefaultListCellRenderer() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public Component getListCellRendererComponent(
                            JList<?> list,
                            Object value,
                            int index,
                            boolean isSelected,
                            boolean cellHasFocus) {

                        super.getListCellRendererComponent(
                                list,
                                value,
                                index,
                                isSelected,
                                cellHasFocus
                        );

                        if (value instanceof YearMonth) {
                            setText(
                                    ((YearMonth) value).format(
                                            MONTH_FORMAT
                                    )
                            );
                        }

                        return this;
                    }
                }
        );
    }

    private void configureActions() {
        computeSelectedButton.addActionListener(
                event -> startPayrollComputation(false)
        );

        computeAllButton.addActionListener(
                event -> startPayrollComputation(true)
        );

        generateSummaryButton.addActionListener(
                event -> generateSummary()
        );

        savePayrollButton.addActionListener(
                event -> confirmAndSavePayroll()
        );

        restoreBackupButton.addActionListener(
                event -> undoLastSave()
        );

        refreshButton.addActionListener(
                event -> loadDataAsync()
        );

        clearButton.addActionListener(
                event -> clearResults()
        );

        logoutButton.addActionListener(
                event -> confirmLogout()
        );

        searchField.getDocument().addDocumentListener(
                TableUtilities.createSearchDocumentListener(tableSorter, searchField, null)
        );

        installSearchAndRefreshShortcuts(
                getRootPane(),
                searchField,
                this::loadDataAsync
        );

        getRootPane().setDefaultButton(
                computeSelectedButton
        );

        updateActionAvailability();
    }

    private void loadDataAsync() {
        if (busy) {
            return;
        }

        dataLoaded = false;

        setBusy(
                true,
                "Loading employee and attendance records...",
                true
        );

        SwingWorker<LoadedData, Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected LoadedData doInBackground()
                            throws Exception {

                        return new LoadedData(
                                FileHandler.loadEmployees(),
                                FileHandler.loadAttendance()
                        );
                    }

                    @Override
                    protected void done() {
                        try {
                            applyLoadedData(get());

                            setBusy(
                                    false,
                                    employees.size()
                                            + " employee record(s) and "
                                            + attendanceRecords.size()
                                            + " attendance record(s) loaded.",
                                    false
                            );
                        } catch (InterruptedException exception) {
                            Thread.currentThread().interrupt();
                            dataLoaded = false;

                            setBusy(
                                    false,
                                    "Loading interrupted.",
                                    false
                            );
                        } catch (ExecutionException exception) {
                            dataLoaded = false;
                            setBusy(
                                    false,
                                    "Unable to load payroll data.",
                                    false
                            );

                            showError(
                                    "Unable to load payroll data.",
                                    exception.getCause()
                            );
                        }
                    }
                };

        worker.execute();
    }

    private void applyLoadedData(LoadedData loadedData) {
        employees.clear();
        employees.addAll(loadedData.employees);

        attendanceRecords.clear();
        attendanceRecords.addAll(
                loadedData.attendanceRecords
        );

        dataLoaded = true;

        employeeComboBox.setModel(
                new DefaultComboBoxModel<>(
                        employees.toArray(
                                new EmployeeInformation[0]
                        )
                )
        );

        populatePayrollMonths();
        clearResults();
        updateActionAvailability();
    }

    private void populatePayrollMonths() {
        TreeSet<YearMonth> payrollMonths =
                new TreeSet<>();

        for (String[] record : attendanceRecords) {
            if (record == null || record.length < 4) {
                continue;
            }

            try {
                LocalDate date = LocalDate.parse(
                        record[3].trim(),
                        DATE_FORMAT
                );

                payrollMonths.add(YearMonth.from(date));
            } catch (DateTimeParseException ignored) {
                // Invalid dates are reported during computation.
            }
        }

        DefaultComboBoxModel<YearMonth> monthModel =
                new DefaultComboBoxModel<>();

        for (YearMonth month : payrollMonths) {
            monthModel.addElement(month);
        }

        monthComboBox.setModel(monthModel);
    }

    private void startPayrollComputation(
            boolean processAllEmployees) {

        if (busy) {
            return;
        }

        if (attendanceRecords.isEmpty()) {
            showWarning(
                    "No attendance records are available. Refresh the data "
                            + "after restoring the configured attendance CSV."
            );
            refreshButton.requestFocusInWindow();
            return;
        }

        YearMonth selectedMonth =
                (YearMonth) monthComboBox.getSelectedItem();

        if (selectedMonth == null) {
            showWarning("Select a payroll month.");
            monthComboBox.requestFocusInWindow();
            return;
        }

        List<EmployeeInformation> targets =
                new ArrayList<>();

        if (processAllEmployees) {
            targets.addAll(employees);
        } else {
            EmployeeInformation selectedEmployee =
                    (EmployeeInformation)
                            employeeComboBox.getSelectedItem();

            if (selectedEmployee == null) {
                showWarning("Select an employee.");
                employeeComboBox.requestFocusInWindow();
                return;
            }

            targets.add(selectedEmployee);
        }

        if (targets.isEmpty()) {
            showWarning(
                    "There are no employee records to process."
            );

            return;
        }

        progressBar.setMinimum(0);
        progressBar.setMaximum(targets.size());
        progressBar.setValue(0);

        setBusy(
                true,
                "Computing payroll for "
                        + targets.size()
                        + " employee(s)...",
                false
        );

        SwingWorker<ComputationBatch, Integer> worker =
                new SwingWorker<>() {

                    @Override
                    protected ComputationBatch doInBackground()
                            throws Exception {
                        ComputationBatch batch =
                                new ComputationBatch();

                        Map<Integer, double[]> monthlyHours =
                                PayrollProcessor
                                        .calculateMonthlyHoursByEmployee(
                                                attendanceRecords,
                                                selectedMonth
                                        );

                        int completed = 0;

                        for (EmployeeInformation employee : targets) {
                            try {
                                PayrollResult result =
                                        PayrollProcessor
                                                .processEmployeePayroll(
                                                        employee,
                                                        monthlyHours,
                                                        selectedMonth
                                                );

                                batch.rows.add(
                                        new PayrollRow(
                                                employee,
                                                selectedMonth,
                                                result
                                        )
                                );
                            } catch (Exception exception) {
                                batch.errors.add(
                                        employee.getEmployeeNumber()
                                                + " - "
                                                + employee.getDisplayName()
                                                + ": "
                                                + DialogUtilities.getErrorMessage(exception)
                                );
                            }

                            completed++;
                            publish(completed);
                        }

                        return batch;
                    }

                    @Override
                    protected void process(
                            List<Integer> completedValues) {

                        if (!completedValues.isEmpty()) {
                            int completed =
                                    completedValues.get(
                                            completedValues.size() - 1
                                    );

                            progressBar.setValue(completed);

                            statusLabel.setText(
                                    "Processed "
                                            + completed
                                            + " of "
                                            + targets.size()
                                            + " employee(s)..."
                            );
                        }
                    }

                    @Override
                    protected void done() {
                        try {
                            ComputationBatch batch = get();
                            applyPayrollRows(batch.rows);

                            setBusy(
                                    false,
                                    batch.rows.size()
                                            + " payroll record(s) computed. Ready to preview.",
                                    false
                            );

                            showPayrollPreviewDialog(batch);
                        } catch (InterruptedException exception) {
                            Thread.currentThread().interrupt();

                            setBusy(
                                    false,
                                    "Payroll computation interrupted.",
                                    false
                            );
                        } catch (ExecutionException exception) {
                            setBusy(
                                    false,
                                    "Payroll computation failed.",
                                    false
                            );

                            showError(
                                    "Unable to compute payroll.",
                                    exception.getCause()
                            );
                        }
                    }
                };

        worker.execute();
    }

    private void applyPayrollRows(List<PayrollRow> rows) {
        payrollRows.clear();
        payrollRows.addAll(rows);

        tableModel.setRowCount(0);

        for (PayrollRow payrollRow : payrollRows) {
            PayrollResult result = payrollRow.result;

            tableModel.addRow(new Object[]{
                payrollRow.employee.getEmployeeNumber(),
                payrollRow.employee.getDisplayName(),
                result.getFirstCutoffHours(),
                result.getSecondCutoffHours(),
                result.getTotalHours(),
                result.getHourlyRate(),
                result.getGrossPay(),
                result.getSss(),
                result.getPhilHealth(),
                result.getPagIbig(),
                result.getWithholdingTax(),
                result.getTotalDeductions(),
                result.getNetPay()
            });
        }

        if (!payrollRows.isEmpty()) {
            payrollTable.setRowSelectionInterval(0, 0);
        } else {
            payrollDetailsArea.setText(
                    "No payroll results were generated."
            );
        }

        TableUtilities.applySearchFilter(tableSorter, searchField.getText());
    }

    private void savePayrollBatch(List<PayrollRow> rows)
            throws Exception {

        int recordCount = rows.size();
        int[] employeeNumbers = new int[recordCount];
        double[] hoursWorked = new double[recordCount];
        double[] deductions = new double[recordCount];
        double[] grossPay = new double[recordCount];
        double[] netPay = new double[recordCount];

        for (int index = 0; index < recordCount; index++) {
            PayrollRow row = rows.get(index);
            PayrollResult result = row.result;

            employeeNumbers[index] = row.employee.getEmployeeNumber();
            hoursWorked[index] = result.getTotalHours();
            deductions[index] = result.getTotalDeductions();
            grossPay[index] = result.getGrossPay();
            netPay[index] = result.getNetPay();
        }

        FileHandler.savePayrollResults(
                employeeNumbers,
                hoursWorked,
                deductions,
                grossPay,
                netPay
        );
    }

    private void displaySelectedPayroll() {
        int selectedViewRow =
                payrollTable.getSelectedRow();

        if (selectedViewRow < 0) {
            return;
        }

        int selectedModelRow =
                payrollTable.convertRowIndexToModel(
                        selectedViewRow
                );

        if (selectedModelRow < 0
                || selectedModelRow >= payrollRows.size()) {

            return;
        }

        PayrollRow payrollRow =
                payrollRows.get(selectedModelRow);

        payrollDetailsArea.setText(
                PayrollDisplayModule.createPayslipText(
                        payrollRow.employee,
                        payrollRow.month,
                        payrollRow.result
                )
        );

        payrollDetailsArea.setCaretPosition(0);
    }

    /**
     * Validates the loaded CSV data and displays aggregate payroll values for
     * all employees in the selected month.
     */
    private void generateSummary() {
        if (busy) {
            showWarning("Please wait for the current operation to finish.");
            return;
        }

        if (!dataLoaded) {
            showWarning(
                    "Payroll data is not loaded. Click 'Refresh Data' and try again."
            );
            refreshButton.requestFocusInWindow();
            return;
        }

        if (employees.isEmpty()) {
            showWarning(
                    "The employee CSV file is empty. Add employee records before generating a summary."
            );
            return;
        }

        if (attendanceRecords.isEmpty()) {
            showWarning(
                    "The attendance CSV file is empty. Attendance records are required to generate a payroll summary."
            );
            return;
        }

        YearMonth selectedMonth =
                (YearMonth) monthComboBox.getSelectedItem();

        if (selectedMonth == null) {
            showWarning("Select a payroll month before generating a summary.");
            monthComboBox.requestFocusInWindow();
            return;
        }

        setBusy(
                true,
                "Generating payroll summary...",
                true
        );

        SwingWorker<double[], Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected double[] doInBackground() {
                        return PayrollProcessor.generateSummary(
                                employees,
                                attendanceRecords,
                                selectedMonth
                        );
                    }

                    @Override
                    protected void done() {
                        try {
                            double[] summary = get();
                            setBusy(
                                    false,
                                    "Payroll summary generated for "
                                            + (int) summary[PayrollProcessor.SUMMARY_EMPLOYEE_COUNT]
                                            + " employee(s).",
                                    false
                            );

                            PayrollDisplayModule.showSummaryDialog(
                                    PayrollStaffFrame.this,
                                    selectedMonth,
                                    summary
                            );
                        } catch (InterruptedException exception) {
                            Thread.currentThread().interrupt();
                            setBusy(
                                    false,
                                    "Summary generation interrupted.",
                                    false
                            );
                        } catch (ExecutionException exception) {
                            setBusy(
                                    false,
                                    "Unable to generate payroll summary.",
                                    false
                            );
                            showError(
                                    "Unable to generate payroll summary. Verify that every employee has valid attendance for the selected month.",
                                    exception.getCause()
                            );
                        }
                    }
                };

        worker.execute();
    }

    private void clearResults() {
        payrollRows.clear();
        tableModel.setRowCount(0);
        searchField.setText("");

        payrollDetailsArea.setText(
                "Select a payroll month and employee, "
                        + "then compute salaries."
        );

        if (!busy) {
            statusLabel.setText(
                    "Payroll results cleared."
            );
        }

        updateActionAvailability();
    }

    private void showPayrollPreviewDialog(ComputationBatch batch) {
        if (batch.errors.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    batch.rows.size()
                            + " payroll record(s) were computed.\n"
                            + "Please preview the results and click 'Save Payroll Data' when ready.",
                    "Computation Complete",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        int choice = PayrollDisplayModule
                .showComputationOutcomeDialog(
                this,
                batch.rows.size(),
                batch.errors
        );

        if (choice == PayrollDisplayModule.SAVE_SUCCESSFUL) {
            confirmAndSavePayroll();
        } else if (choice == PayrollDisplayModule.DISCARD_RESULTS) {
            clearResults();
        } else {
            statusLabel.setText(
                    "Successful results kept. Correct source errors before recomputing."
            );
        }
    }

    private void confirmAndSavePayroll() {
        if (busy) {
            showWarning("Please wait for the current operation to finish.");
            return;
        }

        if (payrollRows.isEmpty()) {
            showWarning("No payroll results to save. Compute payroll first.");
            return;
        }

        if (DialogUtilities.confirmAction(
                this,
                "Confirm Save",
                "Save " + payrollRows.size() + " payroll record(s) to CSV?")) {

            setBusy(true, "Saving payroll results...", true);

            SwingWorker<Void, Void> saveWorker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    savePayrollBatch(payrollRows);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        setBusy(false, "Payroll saved successfully.", false);
                        JOptionPane.showMessageDialog(
                                PayrollStaffFrame.this,
                                payrollRows.size() + " payroll record(s) were saved successfully.",
                                "Save Complete",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    } catch (Exception ex) {
                        setBusy(false, "Failed to save payroll.", false);
                        showError("Unable to save payroll data.", ex.getCause());
                    }
                }
            };
            saveWorker.execute();
        }
    }

    private void undoLastSave() {
        if (busy) {
            showWarning("Please wait for the current operation to finish.");
            return;
        }

        Path targetFile = ApplicationConfig.getEmployeeFile();
        if (!AtomicFileWriter.hasBackup(targetFile)) {
            showWarning("No backup is available to restore.");
            return;
        }

        if (DialogUtilities.confirmAction(
                this,
                "Confirm Undo",
                "Restore the previous payroll CSV file? This will undo your last save.")) {
            setBusy(true, "Restoring previous payroll data...", true);

            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    return AtomicFileWriter.restoreBackup(targetFile);
                }

                @Override
                protected void done() {
                    try {
                        boolean restored = get();
                        setBusy(
                                false,
                                restored
                                        ? "Backup restored successfully."
                                        : "No backup was restored.",
                                false
                        );

                        if (!restored) {
                            showWarning("No backup was found to restore.");
                            return;
                        }

                        JOptionPane.showMessageDialog(
                                PayrollStaffFrame.this,
                                "Backup restored successfully.",
                                "Undo Complete",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                        clearResults();
                        loadDataAsync();
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        setBusy(
                                false,
                                "Backup restoration interrupted.",
                                false
                        );
                    } catch (ExecutionException exception) {
                        setBusy(
                                false,
                                "Backup restoration failed.",
                                false
                        );
                        showError(
                                "Failed to restore backup.",
                                exception.getCause()
                        );
                    }
                }
            };

            worker.execute();
        }
    }

    private void setBusy(
            boolean busy,
            String message,
            boolean indeterminate) {

        this.busy = busy;

        BusyStateUtilities.applyBusyState(
                this,
                busy,
                statusLabel,
                progressBar,
                message,
                indeterminate,
                employeeComboBox,
                monthComboBox,
                searchField,
                payrollTable,
                generateSummaryButton,
                savePayrollButton,
                restoreBackupButton,
                refreshButton,
                clearButton,
                logoutButton
        );

        updateActionAvailability();
    }

    private void updateActionAvailability() {
        boolean hasSourceData = !employees.isEmpty()
                && !attendanceRecords.isEmpty()
                && monthComboBox.getItemCount() > 0;

        computeSelectedButton.setEnabled(
                !busy && hasSourceData
        );

        computeAllButton.setEnabled(
                !busy && hasSourceData
        );

        generateSummaryButton.setEnabled(!busy);
        savePayrollButton.setEnabled(
                !busy && !payrollRows.isEmpty()
        );
        restoreBackupButton.setEnabled(!busy);
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

    private void showWarning(String message) {
        DialogUtilities.showWarning(this, "Payroll Warning", message);
    }

    private void showError(
            String message,
            Throwable throwable) {

        DialogUtilities.showError(
                this,
                "Payroll Error",
                message,
                throwable
        );
    }

    private static final class LoadedData {

        private final ArrayList<EmployeeInformation> employees;
        private final ArrayList<String[]> attendanceRecords;

        private LoadedData(
                ArrayList<EmployeeInformation> employees,
                ArrayList<String[]> attendanceRecords) {

            this.employees = employees;
            this.attendanceRecords = attendanceRecords;
        }
    }

    private static final class PayrollRow {

        private final EmployeeInformation employee;
        private final YearMonth month;
        private final PayrollResult result;

        private PayrollRow(
                EmployeeInformation employee,
                YearMonth month,
                PayrollResult result) {

            this.employee = employee;
            this.month = month;
            this.result = result;
        }
    }

    private static final class ComputationBatch {

        private final List<PayrollRow> rows =
                new ArrayList<>();

        private final List<String> errors =
                new ArrayList<>();
    }


}

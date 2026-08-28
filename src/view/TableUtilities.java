/*
 * TableUtilities.java
 * Reusable table configuration and filtering for Swing views.
 */
package view;

import static view.UiTheme.BORDER;
import static view.UiTheme.NAVY;
import static view.UiTheme.SURFACE;
import static view.UiTheme.TEXT_PRIMARY;

import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.Color;
import java.awt.Font;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Reusable table configuration and filtering for Swing views.
 *
 * <p>Standardizes the appearance, column sizing, number formatting,
 * and search filtering of JTable instances across the application.</p>
 */
public final class TableUtilities {

    /** Private constructor prevents instantiation of this utility class. */
    private TableUtilities() {
        // Utility class; prevent instantiation.
    }

    /**
     * Applies standard MotorPH visual styles to a table.
     *
     * @param table       the table to configure
     * @param tableSorter the row sorter to attach
     */
    public static void configureStandardTable(
            JTable table,
            TableRowSorter<?> tableSorter) {

        // Attach the sorter.
        table.setRowSorter(tableSorter);

        // Standardize selection and scrolling.
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Standardize visual appearance.
        table.setRowHeight(27);
        table.setShowVerticalLines(false);
        table.setGridColor(BORDER);
        table.setBackground(SURFACE);
        table.setForeground(TEXT_PRIMARY);
        table.setSelectionBackground(NAVY);
        table.setSelectionForeground(Color.WHITE);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
    }

    /**
     * Sets preferred and optional minimum widths for all table columns.
     *
     * @param table           the table to resize
     * @param preferredWidths array of preferred widths per column
     * @param minimumWidths   array of minimum widths per column (may be null)
     */
    public static void setColumnWidths(
            JTable table,
            int[] preferredWidths,
            int[] minimumWidths) {

        // Iterate through all provided widths and apply them to the table columns.
        for (int index = 0; index < preferredWidths.length; index++) {
            if (index < table.getColumnModel().getColumnCount()) {
                table.getColumnModel()
                        .getColumn(index)
                        .setPreferredWidth(preferredWidths[index]);

                // Apply minimum widths if provided.
                if (minimumWidths != null && index < minimumWidths.length) {
                    table.getColumnModel()
                            .getColumn(index)
                            .setMinWidth(minimumWidths[index]);
                }
            }
        }
    }

    /**
     * Applies a case-insensitive regex search filter to a table sorter.
     *
     * @param tableSorter the sorter to filter
     * @param searchText  the raw text to search for (may be blank)
     */
    public static void applySearchFilter(
            TableRowSorter<?> tableSorter,
            String searchText) {

        // Clear the filter if the search text is empty.
        if (searchText == null || searchText.trim().isEmpty()) {
            tableSorter.setRowFilter(null);
            return;
        }

        // Apply a case-insensitive (?i) regex filter, escaping user input.
        tableSorter.setRowFilter(
                RowFilter.regexFilter("(?i)" + Pattern.quote(searchText))
        );
    }

    /**
     * Creates a DocumentListener that applies a search filter when text changes.
     *
     * @param tableSorter the sorter to filter
     * @param searchField the field containing search text
     * @param afterFilter an optional action to run after filtering (e.g. status updates)
     * @return the new DocumentListener
     */
    public static DocumentListener createSearchDocumentListener(
            TableRowSorter<?> tableSorter,
            JTextField searchField,
            Runnable afterFilter) {

        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                applyFilter();
            }

            private void applyFilter() {
                applySearchFilter(tableSorter, searchField.getText());
                if (afterFilter != null) {
                    afterFilter.run();
                }
            }
        };
    }

    /**
     * Creates a cell renderer for formatting decimal numbers (right-aligned).
     *
     * @return a new DefaultTableCellRenderer for numbers
     */
    public static DefaultTableCellRenderer createDecimalRenderer() {
        return new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public void setValue(Object value) {
                // Right-align numeric columns.
                setHorizontalAlignment(RIGHT);

                // Format numbers to 2 decimal places with commas.
                if (value instanceof Number) {
                    setText(String.format(
                            Locale.US,
                            "%,.2f",
                            ((Number) value).doubleValue()
                    ));
                } else {
                    super.setValue(value);
                }
            }
        };
    }
}

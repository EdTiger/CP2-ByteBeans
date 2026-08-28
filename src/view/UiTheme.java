package view;

import javax.swing.BorderFactory;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/** Shared Swing appearance functions and constants for every view. */
public final class UiTheme {

    public static final Color NAVY = new Color(28, 55, 82);
    public static final Color NAVY_HOVER = new Color(20, 43, 65);
    public static final Color SLATE = new Color(91, 103, 112);
    public static final Color SLATE_HOVER = new Color(68, 78, 86);
    public static final Color BACKGROUND = new Color(244, 246, 248);
    public static final Color SURFACE = Color.WHITE;
    public static final Color BORDER = new Color(210, 216, 222);
    public static final Color TEXT_PRIMARY = new Color(35, 42, 48);
    public static final Color TEXT_SECONDARY = new Color(94, 105, 115);
    public static final Color DISABLED = new Color(174, 181, 188);
    public static final Color HEADER_SUBTITLE = new Color(220, 228, 235);
    public static final Color DETAILS_BACKGROUND = new Color(249, 250, 251);
    public static final Color STATUS_BACKGROUND = new Color(235, 239, 242);
    public static final Color HEADER_DIVIDER = new Color(70, 91, 111);

    private UiTheme() {
        // Static utility module; prevent instantiation.
    }

    /**
     * Configures a JButton with standard MotorPH styling and hover effects.
     *
     * @param button     the button to configure
     * @param background the default background color
     * @param hoverColor the background color when hovered
     */
    public static void configureButton(
            JButton button,
            Color background,
            Color hoverColor) {

        button.setUI(new BasicButtonUI());
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setBackground(background);
        button.setForeground(Color.WHITE);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        button.setFocusPainted(true);
        button.setRolloverEnabled(true);
        button.setBorder(new EmptyBorder(10, 14, 10, 14));
        button.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );

        Dimension contentSize = button.getPreferredSize();
        button.setPreferredSize(new Dimension(
                Math.max(150, contentSize.width),
                Math.max(40, contentSize.height)
        ));

        button.getModel().addChangeListener(event -> {
            if (!button.isEnabled()) {
                button.setBackground(DISABLED);
            } else if (button.getModel().isRollover()) {
                button.setBackground(hoverColor);
            } else {
                button.setBackground(background);
            }
        });
    }

    /**
     * Configures a JButton with standard styling and assigns a keyboard mnemonic.
     *
     * @param button     the button to configure
     * @param background the default background color
     * @param hoverColor the background color when hovered
     * @param mnemonic   the Alt+Key character for shortcut activation
     */
    public static void configureButton(
            JButton button,
            Color background,
            Color hoverColor,
            char mnemonic) {

        configureButton(button, background, hoverColor);
        button.setMnemonic(mnemonic);
    }

    /**
     * Configures a JTextField with standard MotorPH borders, font, and dimensions.
     *
     * @param field the text field to configure
     */
    public static void configureTextField(JTextField field) {
        field.setFont(new Font("SansSerif", Font.PLAIN, 13));
        field.setForeground(TEXT_PRIMARY);
        field.setBackground(Color.WHITE);
        field.setCaretColor(TEXT_PRIMARY);
        field.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER),
                        new EmptyBorder(6, 8, 6, 8)
                )
        );
        field.setPreferredSize(new Dimension(220, 34));
    }

    /**
     * Creates a standard border for content panels, combining a line border and padding.
     *
     * @return the configured Border instance
     */
    public static Border createPanelBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(14, 14, 14, 14)
        );
    }

    /** Adds platform-standard search focus and F5 refresh shortcuts. */
    @SuppressWarnings("serial")
    public static void installSearchAndRefreshShortcuts(
            JRootPane rootPane,
            JTextField searchField,
            Runnable refreshAction) {

        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(
                        KeyEvent.VK_F,
                        Toolkit.getDefaultToolkit()
                                .getMenuShortcutKeyMaskEx()
                ),
                "focusSearch"
        );

        rootPane.getActionMap().put(
                "focusSearch",
                new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent event) {
                        searchField.requestFocusInWindow();
                        searchField.selectAll();
                    }
                }
        );

        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0),
                "refreshData"
        );

        rootPane.getActionMap().put(
                "refreshData",
                new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent event) {
                        refreshAction.run();
                    }
                }
        );
    }

    /** Uses a fixed renderer so Windows rollover painting cannot hide text. */
    public static void configureTableHeader(JTableHeader header) {
        header.setReorderingAllowed(false);
        header.setOpaque(true);
        header.setBackground(NAVY);
        header.setForeground(Color.WHITE);
        header.setFont(new Font("SansSerif", Font.BOLD, 12));

        JPanel headerCell = new JPanel(new BorderLayout());
        headerCell.setOpaque(true);
        headerCell.setBackground(NAVY);
        headerCell.setForeground(Color.WHITE);
        headerCell.setBorder(
                BorderFactory.createMatteBorder(
                        0, 0, 1, 1, HEADER_DIVIDER
                )
        );

        JLabel headerLabel = new JLabel();
        headerLabel.setOpaque(false);
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        headerLabel.setHorizontalAlignment(SwingConstants.LEFT);
        headerLabel.setBorder(new EmptyBorder(7, 8, 7, 8));

        headerCell.add(headerLabel, BorderLayout.CENTER);

        header.setDefaultRenderer((
                table,
                value,
                isSelected,
                hasFocus,
                row,
                column) -> {

            String headerText = value == null ? "" : value.toString();
            headerLabel.setText(
                    headerText + getSortMarker(table, column)
            );
            headerCell.getAccessibleContext()
                    .setAccessibleName(headerText);

            return headerCell;
        });
    }

    /**
     * Helper method to generate the ascending/descending sort indicator arrow.
     *
     * @param table      the JTable containing the RowSorter
     * @param viewColumn the view column index
     * @return the arrow string, or an empty string if not sorted
     */
    private static String getSortMarker(JTable table, int viewColumn) {
        if (table.getRowSorter() == null || viewColumn < 0) {
            return "";
        }

        int modelColumn = table.convertColumnIndexToModel(viewColumn);

        for (RowSorter.SortKey sortKey
                : table.getRowSorter().getSortKeys()) {

            if (sortKey.getColumn() != modelColumn) {
                continue;
            }

            if (sortKey.getSortOrder() == SortOrder.ASCENDING) {
                return " \u25B2";
            }

            if (sortKey.getSortOrder() == SortOrder.DESCENDING) {
                return " \u25BC";
            }
        }

        return "";
    }
}

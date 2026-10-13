import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class WarehouseInventoryApp extends JFrame {
    private static final String DATA_FILE = "warehouse_inventory.csv";

    private static final Color BG_MAIN = new Color(241, 245, 249);
    private static final Color BG_CARD = Color.WHITE;
    private static final Color TEXT_PRIMARY = new Color(15, 23, 42);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);
    private static final Color BORDER_COLOR = new Color(226, 232, 240);

    private static final Color ACCENT_BLUE = new Color(37, 99, 235);
    private static final Color ACCENT_GREEN = new Color(16, 185, 129);
    private static final Color ACCENT_RED = new Color(239, 68, 68);
    private static final Color ACCENT_SLATE = new Color(71, 85, 105);

    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 20);
    private static final Font FONT_STAT_VAL = new Font("Segoe UI", Font.BOLD, 24);
    private static final Font FONT_STAT_LBL = new Font("Segoe UI", Font.BOLD, 12);
    private static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);

    private DefaultTableModel tableModel;
    private JTable inventoryTable;
    private TableRowSorter<DefaultTableModel> rowSorter;

    private JTextField skuField;
    private JTextField nameField;
    private JComboBox<String> categoryBox;
    private JTextField quantityField;
    private JTextField priceField;
    private JTextField searchField;

    private JLabel totalItemsValue;
    private JLabel totalValuationValue;
    private JLabel lowStockValue;

    public WarehouseInventoryApp() {
        setTitle("Warehouse Inventory Management");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 720);
        setMinimumSize(new Dimension(980, 620));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_MAIN);
        setLayout(new BorderLayout(0, 0));

        initComponents();
        loadDataFromFile();
        updateDashboardStats();
    }

    private void initComponents() {
        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setBackground(TEXT_PRIMARY);
        headerBar.setBorder(new EmptyBorder(16, 24, 16, 24));

        JLabel appTitle = new JLabel("Warehouse Stock Control");
        appTitle.setFont(FONT_TITLE);
        appTitle.setForeground(Color.WHITE);

        JLabel subTitle = new JLabel("Auto-Sync CSV Persistence Active");
        subTitle.setFont(FONT_REGULAR);
        subTitle.setForeground(new Color(148, 163, 184));

        headerBar.add(appTitle, BorderLayout.WEST);
        headerBar.add(subTitle, BorderLayout.EAST);
        add(headerBar, BorderLayout.NORTH);

        JPanel workspace = new JPanel(new BorderLayout(20, 20));
        workspace.setBackground(BG_MAIN);
        workspace.setBorder(new EmptyBorder(20, 24, 24, 24));

        JPanel statsGrid = new JPanel(new GridLayout(1, 3, 20, 0));
        statsGrid.setOpaque(false);

        totalItemsValue = new JLabel("0");
        totalValuationValue = new JLabel("$0.00");
        lowStockValue = new JLabel("0");

        statsGrid.add(createKpiCard("TOTAL UNIQUE SKUS", totalItemsValue, ACCENT_BLUE));
        statsGrid.add(createKpiCard("TOTAL WAREHOUSE VALUE", totalValuationValue, ACCENT_GREEN));
        statsGrid.add(createKpiCard("LOW STOCK ALERTS (UNDER 10)", lowStockValue, ACCENT_RED));

        workspace.add(statsGrid, BorderLayout.NORTH);

        JPanel tableCard = new JPanel(new BorderLayout(0, 14));
        tableCard.setBackground(BG_CARD);
        tableCard.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(18, 18, 18, 18)
        ));

        JPanel searchBar = new JPanel(new BorderLayout(12, 0));
        searchBar.setOpaque(false);
        JLabel searchTitle = new JLabel("Inventory Records");
        searchTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        searchTitle.setForeground(TEXT_PRIMARY);

        searchField = createStyledTextField();
        searchField.setPreferredSize(new Dimension(280, 36));

        JPanel searchRight = new JPanel(new BorderLayout(8, 0));
        searchRight.setOpaque(false);
        JLabel searchIconLabel = new JLabel("Filter:");
        searchIconLabel.setFont(FONT_BOLD);
        searchIconLabel.setForeground(TEXT_MUTED);
        searchRight.add(searchIconLabel, BorderLayout.WEST);
        searchRight.add(searchField, BorderLayout.CENTER);

        searchBar.add(searchTitle, BorderLayout.WEST);
        searchBar.add(searchRight, BorderLayout.EAST);
        tableCard.add(searchBar, BorderLayout.NORTH);

        String[] columns = {"SKU", "Item Name", "Category", "Quantity", "Unit Price ($)", "Total Value ($)"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        inventoryTable = new JTable(tableModel);
        inventoryTable.setFont(FONT_REGULAR);
        inventoryTable.setRowHeight(38);
        inventoryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        inventoryTable.setShowVerticalLines(false);
        inventoryTable.setGridColor(BORDER_COLOR);
        inventoryTable.setIntercellSpacing(new Dimension(0, 1));
        inventoryTable.setSelectionBackground(new Color(219, 234, 254));
        inventoryTable.setSelectionForeground(TEXT_PRIMARY);

        JTableHeader header = inventoryTable.getTableHeader();
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 42));
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setBackground(new Color(30, 41, 59));
                lbl.setForeground(Color.WHITE);
                lbl.setFont(FONT_BOLD);
                lbl.setBorder(new EmptyBorder(0, 12, 0, 12));
                lbl.setOpaque(true);
                return lbl;
            }
        });

        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBorder(new EmptyBorder(0, 12, 0, 12));
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    c.setForeground(TEXT_PRIMARY);
                }

                if (column == 3) {
                    try {
                        int qty = Integer.parseInt(value.toString());
                        if (qty < 10) {
                            c.setForeground(ACCENT_RED);
                            c.setFont(FONT_BOLD);
                        } else {
                            c.setFont(FONT_REGULAR);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                } else {
                    c.setFont(FONT_REGULAR);
                }
                return c;
            }
        };

        for (int i = 0; i < inventoryTable.getColumnCount(); i++) {
            inventoryTable.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }

        rowSorter = new TableRowSorter<>(tableModel);
        inventoryTable.setRowSorter(rowSorter);

        JScrollPane scrollPane = new JScrollPane(inventoryTable);
        scrollPane.setBorder(new LineBorder(BORDER_COLOR, 1));
        scrollPane.getViewport().setBackground(Color.WHITE);
        tableCard.add(scrollPane, BorderLayout.CENTER);

        workspace.add(tableCard, BorderLayout.CENTER);

        JPanel formCard = new JPanel(new BorderLayout(0, 16));
        formCard.setBackground(BG_CARD);
        formCard.setPreferredSize(new Dimension(340, 0));
        formCard.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(20, 20, 20, 20)
        ));

        JLabel formHeading = new JLabel("Manage Stock Item");
        formHeading.setFont(new Font("Segoe UI", Font.BOLD, 16));
        formHeading.setForeground(TEXT_PRIMARY);
        formCard.add(formHeading, BorderLayout.NORTH);

        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        skuField = createStyledTextField();
        nameField = createStyledTextField();
        String[] categories = {"Electronics", "Hardware", "Raw Materials", "Packaging", "Machinery", "Apparel", "Safety Gear"};
        categoryBox = new JComboBox<>(categories);
        categoryBox.setFont(FONT_REGULAR);
        categoryBox.setBackground(Color.WHITE);
        categoryBox.setForeground(TEXT_PRIMARY);
        categoryBox.setPreferredSize(new Dimension(0, 36));

        quantityField = createStyledTextField();
        priceField = createStyledTextField();

        int row = 0;
        row = addFieldToGrid(fieldsPanel, gbc, row, "SKU Code", skuField);
        row = addFieldToGrid(fieldsPanel, gbc, row, "Item Name", nameField);
        row = addFieldToGrid(fieldsPanel, gbc, row, "Category", categoryBox);
        row = addFieldToGrid(fieldsPanel, gbc, row, "Stock Quantity", quantityField);
        row = addFieldToGrid(fieldsPanel, gbc, row, "Unit Price ($)", priceField);

        gbc.gridy = row;
        gbc.weighty = 1.0;
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        fieldsPanel.add(spacer, gbc);

        formCard.add(fieldsPanel, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        actionPanel.setOpaque(false);

        ModernButton addButton = new ModernButton("Add Item", ACCENT_GREEN);
        ModernButton updateButton = new ModernButton("Update Item", ACCENT_BLUE);
        ModernButton deleteButton = new ModernButton("Delete Item", ACCENT_RED);
        ModernButton clearButton = new ModernButton("Clear Form", ACCENT_SLATE);

        actionPanel.add(addButton);
        actionPanel.add(updateButton);
        actionPanel.add(deleteButton);
        actionPanel.add(clearButton);

        formCard.add(actionPanel, BorderLayout.SOUTH);
        workspace.add(formCard, BorderLayout.EAST);

        add(workspace, BorderLayout.CENTER);

        addButton.addActionListener(e -> addItem());
        updateButton.addActionListener(e -> updateItem());
        deleteButton.addActionListener(e -> deleteItem());
        clearButton.addActionListener(e -> clearForm());

        inventoryTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && inventoryTable.getSelectedRow() != -1) {
                populateFormFromSelection();
            }
        });

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { filterTable(); }
            @Override
            public void removeUpdate(DocumentEvent e) { filterTable(); }
            @Override
            public void changedUpdate(DocumentEvent e) { filterTable(); }
        });
    }

    private JPanel createKpiCard(String title, JLabel valueLabel, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(BG_CARD);
        card.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new CompoundBorder(
                        BorderFactory.createMatteBorder(0, 5, 0, 0, accent),
                        new EmptyBorder(16, 18, 16, 18)
                )
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(FONT_STAT_LBL);
        titleLbl.setForeground(TEXT_MUTED);

        valueLabel.setFont(FONT_STAT_VAL);
        valueLabel.setForeground(TEXT_PRIMARY);

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setFont(FONT_REGULAR);
        field.setForeground(TEXT_PRIMARY);
        field.setBackground( new Color(248, 250, 252));
        field.setPreferredSize(new Dimension(0, 36));
        field.setBorder(new CompoundBorder(
                new LineBorder(new Color(203, 213, 225), 1, true),
                new EmptyBorder(4, 10, 4, 10)
        ));
        return field;
    }

    private int addFieldToGrid(JPanel panel, GridBagConstraints gbc, int row, String labelText, JComponent comp) {
        gbc.gridy = row++;
        gbc.insets = new Insets(4, 0, 4, 0);
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(FONT_BOLD);
        lbl.setForeground(TEXT_MUTED);
        panel.add(lbl, gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 12, 0);
        panel.add(comp, gbc);
        return row;
    }

    private void filterTable() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            rowSorter.setRowFilter(null);
        } else {
            rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + query));
        }
    }

    private void populateFormFromSelection() {
        int viewRow = inventoryTable.getSelectedRow();
        if (viewRow != -1) {
            int modelRow = inventoryTable.convertRowIndexToModel(viewRow);
            skuField.setText(tableModel.getValueAt(modelRow, 0).toString());
            nameField.setText(tableModel.getValueAt(modelRow, 1).toString());
            categoryBox.setSelectedItem(tableModel.getValueAt(modelRow, 2).toString());
            quantityField.setText(tableModel.getValueAt(modelRow, 3).toString());
            priceField.setText(tableModel.getValueAt(modelRow, 4).toString());
        }
    }

    private void addItem() {
        String sku = skuField.getText().trim();
        String name = nameField.getText().trim();
        String category = categoryBox.getSelectedItem().toString();
        String qtyText = quantityField.getText().trim();
        String priceText = priceField.getText().trim();

        if (sku.isEmpty() || name.isEmpty() || qtyText.isEmpty() || priceText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (sku.contains(",") || name.contains(",")) {
            JOptionPane.showMessageDialog(this, "SKU and Item Name cannot contain commas.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (tableModel.getValueAt(i, 0).toString().equalsIgnoreCase(sku)) {
                JOptionPane.showMessageDialog(this, "SKU already exists in inventory.", "Duplicate SKU", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        try {
            int qty = Integer.parseInt(qtyText);
            double price = Double.parseDouble(priceText);
            if (qty < 0 || price < 0) {
                JOptionPane.showMessageDialog(this, "Quantity and Price cannot be negative.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            double total = qty * price;
            tableModel.addRow(new Object[]{
                    sku,
                    name,
                    category,
                    qty,
                    String.format("%.2f", price),
                    String.format("%.2f", total)
            });

            saveDataToFile();
            updateDashboardStats();
            clearForm();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid numeric input for Quantity or Price.", "Format Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateItem() {
        int viewRow = inventoryTable.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this, "Select an item from the table to update.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = inventoryTable.convertRowIndexToModel(viewRow);
        String sku = skuField.getText().trim();
        String name = nameField.getText().trim();
        String category = categoryBox.getSelectedItem().toString();
        String qtyText = quantityField.getText().trim();
        String priceText = priceField.getText().trim();

        if (sku.isEmpty() || name.isEmpty() || qtyText.isEmpty() || priceText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (sku.contains(",") || name.contains(",")) {
            JOptionPane.showMessageDialog(this, "SKU and Item Name cannot contain commas.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (i != modelRow && tableModel.getValueAt(i, 0).toString().equalsIgnoreCase(sku)) {
                JOptionPane.showMessageDialog(this, "SKU belongs to another existing record.", "Duplicate SKU", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        try {
            int qty = Integer.parseInt(qtyText);
            double price = Double.parseDouble(priceText);
            if (qty < 0 || price < 0) {
                JOptionPane.showMessageDialog(this, "Quantity and Price cannot be negative.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            double total = qty * price;
            tableModel.setValueAt(sku, modelRow, 0);
            tableModel.setValueAt(name, modelRow, 1);
            tableModel.setValueAt(category, modelRow, 2);
            tableModel.setValueAt(qty, modelRow, 3);
            tableModel.setValueAt(String.format("%.2f", price), modelRow, 4);
            tableModel.setValueAt(String.format("%.2f", total), modelRow, 5);

            saveDataToFile();
            updateDashboardStats();
            clearForm();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid numeric input for Quantity or Price.", "Format Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteItem() {
        int viewRow = inventoryTable.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this, "Select an item from the table to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Delete selected record permanently?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            int modelRow = inventoryTable.convertRowIndexToModel(viewRow);
            tableModel.removeRow(modelRow);
            saveDataToFile();
            updateDashboardStats();
            clearForm();
        }
    }

    private void clearForm() {
        skuField.setText("");
        nameField.setText("");
        categoryBox.setSelectedIndex(0);
        quantityField.setText("");
        priceField.setText("");
        inventoryTable.clearSelection();
    }

    private void updateDashboardStats() {
        int rowCount = tableModel.getRowCount();
        double grandTotal = 0.0;
        int lowStockCount = 0;

        for (int i = 0; i < rowCount; i++) {
            int qty = Integer.parseInt(tableModel.getValueAt(i, 3).toString());
            double total = Double.parseDouble(tableModel.getValueAt(i, 5).toString());
            grandTotal += total;
            if (qty < 10) {
                lowStockCount++;
            }
        }

        totalItemsValue.setText(String.valueOf(rowCount));
        totalValuationValue.setText(String.format("$%,.2f", grandTotal));
        lowStockValue.setText(String.valueOf(lowStockCount));
    }

    private void saveDataToFile() {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String sku = tableModel.getValueAt(i, 0).toString();
            String name = tableModel.getValueAt(i, 1).toString();
            String category = tableModel.getValueAt(i, 2).toString();
            String qty = tableModel.getValueAt(i, 3).toString();
            String price = tableModel.getValueAt(i, 4).toString();
            lines.add(String.join(",", sku, name, category, qty, price));
        }

        try {
            Files.write(Paths.get(DATA_FILE), lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to auto-save data: " + ex.getMessage(), "I/O Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadDataFromFile() {
        Path path = Paths.get(DATA_FILE);
        if (!Files.exists(path)) {
            try {
                Files.createFile(path);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Could not create data file: " + ex.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
            }
            return;
        }

        try {
            List<String> lines = Files.readAllLines(path);
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length == 5) {
                    String sku = parts[0];
                    String name = parts[1];
                    String category = parts[2];
                    int qty = Integer.parseInt(parts[3]);
                    double price = Double.parseDouble(parts[4]);
                    double total = qty * price;

                    tableModel.addRow(new Object[]{
                            sku,
                            name,
                            category,
                            qty,
                            String.format("%.2f", price),
                            String.format("%.2f", total)
                    });
                }
            }
        } catch (IOException | NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Error reading saved inventory file: " + ex.getMessage(), "Load Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static class ModernButton extends JButton {
        private final Color baseColor;
        private final Color hoverColor;
        private final Color pressedColor;
        private boolean hovered = false;
        private boolean pressed = false;

        public ModernButton(String text, Color color) {
            super(text);
            this.baseColor = color;
            this.hoverColor = color.darker();
            this.pressedColor = color.darker().darker();

            setFont(FONT_BOLD);
            setForeground(Color.WHITE);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(130, 40));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    pressed = false;
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    pressed = true;
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    pressed = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (pressed) {
                g2.setColor(pressedColor);
            } else if (hovered) {
                g2.setColor(hoverColor);
            } else {
                g2.setColor(baseColor);
            }

            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.dispose();

            super.paintComponent(g);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            WarehouseInventoryApp app = new WarehouseInventoryApp();
            app.setVisible(true);
        });
    }
}
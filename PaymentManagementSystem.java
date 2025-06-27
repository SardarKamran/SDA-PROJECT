import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.io.*;
import java.util.Vector;
import java.awt.print.*;

public class PaymentManagementSystem extends JFrame {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/fee_management";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "root";

    private JTable table;
    private DefaultTableModel tableModel;

    private JTextField nameSearchField;
    private JTextField idSearchField;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PaymentManagementSystem().showLoginWindow());
    }

    // Login Window with username/password authentication
    public void showLoginWindow() {
        JFrame loginFrame = new JFrame("System Login");
        JLabel userLabel = new JLabel("Username:");
        JTextField userField = new JTextField(10);
        JLabel passLabel = new JLabel("Password:");
        JPasswordField passField = new JPasswordField(10);
        JButton loginBtn = new JButton("Login");

        loginFrame.setLayout(new GridLayout(3, 2));
        loginFrame.add(userLabel);
        loginFrame.add(userField);
        loginFrame.add(passLabel);
        loginFrame.add(passField);
        loginFrame.add(new JLabel());
        loginFrame.add(loginBtn);

        loginBtn.addActionListener(e -> {
            String username = userField.getText().trim();
            String password = new String(passField.getPassword());

            if (username.equals("Kamran") && password.equals("123")) {
                loginFrame.dispose();
                showMainWindow();
            } else {
                JOptionPane.showMessageDialog(loginFrame, "Invalid username or password.");
            }
        });

        loginFrame.setSize(300, 150);
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setVisible(true);
    }

    // Main Window
    public void showMainWindow() {
        setTitle("Fee Management System - Payment History & Search");
        setLayout(new BorderLayout());

        // Top panel buttons
        JButton viewBtn = new JButton("View All Payments");
        JButton downloadBtn = new JButton("Download Receipt");
        JButton printBtn = new JButton("Print Receipt");
        JButton searchByIdBtn = new JButton("Search by ID");
        JButton searchByNameBtn = new JButton("Search by Name");

        idSearchField = new JTextField(5);
        idSearchField.setToolTipText("Enter Student ID");

        nameSearchField = new JTextField(10);
        nameSearchField.setToolTipText("Enter Student Name");

        JPanel topPanel = new JPanel();
        topPanel.add(viewBtn);
        topPanel.add(downloadBtn);
        topPanel.add(printBtn);
        topPanel.add(new JLabel("ID:"));
        topPanel.add(idSearchField);
        topPanel.add(searchByIdBtn);
        topPanel.add(new JLabel("Name:"));
        topPanel.add(nameSearchField);
        topPanel.add(searchByNameBtn);

        // Table setup
        tableModel = new DefaultTableModel();
        tableModel.setColumnIdentifiers(new String[]{"Student ID", "Name", "Date", "Amount", "Mode", "Status"});
        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // Button actions
        viewBtn.addActionListener(e -> fetchAllPayments());
        downloadBtn.addActionListener(e -> downloadReceipt());
        printBtn.addActionListener(e -> printReceipt());
        searchByIdBtn.addActionListener(e -> searchPaymentsById());
        searchByNameBtn.addActionListener(e -> searchPaymentsByName());

        setSize(900, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    // Fetch all payments
    private void fetchAllPayments() {
        displayQueryResults("SELECT student_id, student_name, payment_date, amount_paid, payment_mode, status FROM payments", null);
    }

    // Search payments by student ID
    private void searchPaymentsById() {
        String idText = idSearchField.getText().trim();
        if (idText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a student ID to search.");
            return;
        }
        try {
            int id = Integer.parseInt(idText);
            displayQueryResults("SELECT student_id, student_name, payment_date, amount_paid, payment_mode, status FROM payments WHERE student_id = ?", id);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid student ID format.");
        }
    }

    // Search payments by student name
    private void searchPaymentsByName() {
        String name = nameSearchField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a student name to search.");
            return;
        }
        displayQueryResults("SELECT student_id, student_name, payment_date, amount_paid, payment_mode, status FROM payments WHERE student_name LIKE ?", "%" + name + "%");
    }

    // Helper to execute query and display in table
    private void displayQueryResults(String query, Object param) {
        tableModel.setRowCount(0);
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            Class.forName("com.mysql.cj.jdbc.Driver");
            PreparedStatement stmt = conn.prepareStatement(query);

            if (param != null) {
                if (param instanceof Integer)
                    stmt.setInt(1, (Integer) param);
                else if (param instanceof String)
                    stmt.setString(1, (String) param);
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("student_id"));
                row.add(rs.getString("student_name"));
                row.add(rs.getDate("payment_date"));
                row.add(rs.getDouble("amount_paid"));
                row.add(rs.getString("payment_mode"));
                row.add(rs.getString("status"));
                tableModel.addRow(row);
            }

            if (tableModel.getRowCount() == 0)
                JOptionPane.showMessageDialog(this, "No records found.");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Database error occurred.");
            ex.printStackTrace();
        }
    }

    // Download receipt to file
    private void downloadReceipt() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No data to download.");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("PaymentReceipt.txt"));
        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();

            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fileToSave), "UTF-8"))) {
                writer.println("=== Payment Receipt ===");
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    writer.println("Student ID: " + tableModel.getValueAt(i, 0)
                            + " | Name: " + tableModel.getValueAt(i, 1)
                            + " | Date: " + tableModel.getValueAt(i, 2)
                            + " | Amount: Rs. " + tableModel.getValueAt(i, 3)
                            + " | Mode: " + tableModel.getValueAt(i, 4)
                            + " | Status: " + tableModel.getValueAt(i, 5));
                }
                writer.println("=== End of Receipt ===");

                JOptionPane.showMessageDialog(this, "Receipt downloaded at: " + fileToSave.getAbsolutePath());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error writing receipt to file.");
                ex.printStackTrace();
            }
        }
    }

    // Print receipt
    private void printReceipt() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No data to print.");
            return;
        }

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Payment Receipt Print");

        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE;

            Graphics2D g2d = (Graphics2D) graphics;
            g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

            int y = 20;
            g2d.drawString("=== Payment Receipt ===", 100, y);
            y += 15;

            for (int i = 0; i < tableModel.getRowCount(); i++) {
                String line = "Student ID: " + tableModel.getValueAt(i, 0)
                        + " | Name: " + tableModel.getValueAt(i, 1)
                        + " | Date: " + tableModel.getValueAt(i, 2)
                        + " | Amount: Rs. " + tableModel.getValueAt(i, 3)
                        + " | Mode: " + tableModel.getValueAt(i, 4)
                        + " | Status: " + tableModel.getValueAt(i, 5);
                g2d.drawString(line, 100, y);
                y += 15;
            }

            y += 10;
            g2d.drawLine(100, y, 400, y);
            y += 15;
            g2d.drawString("End of Receipt", 100, y);

            return Printable.PAGE_EXISTS;
        });

        boolean doPrint = job.printDialog();
        if (doPrint) {
            try {
                job.print();
                JOptionPane.showMessageDialog(this, "Receipt sent to printer successfully.");
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this, "Printing error occurred.");
                ex.printStackTrace();
            }
        }
    }
}

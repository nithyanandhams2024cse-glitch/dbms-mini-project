import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class CarRentalSystem extends JFrame {

    // 1. Database Configuration (!!! CHANGE PASSWORD !!!)
    private static final String URL = "jdbc:mysql://localhost:3306/car_rentalll_db";
    private static final String USER = "root";
    private static final String PASSWORD = "keerthana2210"; // <--- CHANGE THIS

    // Login Components
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;

    // Store logged-in user ID for customer-specific actions (e.g., booking history)
    private int currentUserId = -1;

    public CarRentalSystem() {
        // --- Login Frame Setup ---
        setTitle("Car Rental System Login");
        setSize(400, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center the window

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        txtUsername = new JTextField(15);
        txtPassword = new JPasswordField(15);
        btnLogin = new JButton("Login");

        panel.add(new JLabel("Username:"));
        panel.add(txtUsername);
        panel.add(new JLabel("Password:"));
        panel.add(txtPassword);
        panel.add(new JLabel()); // Empty cell for spacing
        panel.add(btnLogin);

        add(panel, BorderLayout.CENTER);

        // --- Listener ---
        btnLogin.addActionListener(e -> authenticateUser());

        setVisible(true);
    }

    // --- JDBC Connection Helper ---
    private Connection getConnection() throws SQLException {
        // We assume the driver is loaded in the main method
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // --- Authentication Logic ---
    private void authenticateUser() {
        String username = txtUsername.getText();
        String password = new String(txtPassword.getPassword());
        String role = null;
        int userId = -1;

        // Fetch both role and user_id
        String sql = "SELECT user_id, role FROM Users WHERE username = ? AND password_hash = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            // NOTE: The current code uses plain text passwords for simplicity.
            // In a real application, 'password' should be hashed before comparing.
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                userId = rs.getInt("user_id");
                role = rs.getString("role");

                // Set the current user ID
                currentUserId = userId;

                // Close the login window
                this.dispose();

                // Launch the appropriate dashboard based on the role
                if ("Admin".equals(role)) {
                    new AdminDashboard();
                } else if ("Customer".equals(role)) {
                    // Pass the logged-in user ID to the customer view
                    new CustomerView(userId);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Invalid Username or Password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ====================================================================
    // 2. ADMIN DASHBOARD CLASS (Car Management Functionality)
    // ====================================================================

    private class AdminDashboard extends JFrame {
        private JTextField txtLicense, txtMake, txtModel, txtRate;
        private JComboBox<String> cmbStatus;
        private JButton btnAdd, btnUpdate, btnDelete, btnClear;
        private JTable carTable;
        private DefaultTableModel tableModel;

        public AdminDashboard() {
            setTitle("Admin Dashboard - Car Management");
            setSize(800, 600);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLayout(new BorderLayout(10, 10));
            setLocationRelativeTo(null);

            // --- Input Panel (North) ---
            JPanel inputPanel = new JPanel(new GridLayout(6, 2, 5, 5));

            txtLicense = new JTextField(15);
            txtMake = new JTextField(15);
            txtModel = new JTextField(15);
            txtRate = new JTextField(15);
            cmbStatus = new JComboBox<>(new String[]{"Available", "Rented", "Maintenance"});

            inputPanel.add(new JLabel("License Plate:"));
            inputPanel.add(txtLicense);
            inputPanel.add(new JLabel("Make:"));
            inputPanel.add(txtMake);
            inputPanel.add(new JLabel("Model:"));
            inputPanel.add(txtModel);
            inputPanel.add(new JLabel("Daily Rate:"));
            inputPanel.add(txtRate);
            inputPanel.add(new JLabel("Status:"));
            inputPanel.add(cmbStatus);

            // --- Button Panel ---
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
            btnAdd = new JButton("Add Car");
            btnUpdate = new JButton("Update Car");
            btnDelete = new JButton("Delete Car");
            btnClear = new JButton("Clear Form");

            buttonPanel.add(btnAdd);
            buttonPanel.add(btnUpdate);
            buttonPanel.add(btnDelete);
            buttonPanel.add(btnClear);

            JPanel northPanel = new JPanel(new BorderLayout());
            northPanel.add(inputPanel, BorderLayout.NORTH);
            northPanel.add(buttonPanel, BorderLayout.CENTER);
            add(northPanel, BorderLayout.NORTH);

            // --- Table Panel ---
            tableModel = new DefaultTableModel(new String[]{"ID", "License Plate", "Make", "Model", "Rate", "Status"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) { return false; }
            };
            carTable = new JTable(tableModel);
            carTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            add(new JScrollPane(carTable), BorderLayout.CENTER);

            // --- Load Data & Setup Listeners ---
            loadCars();
            setupListeners();

            setVisible(true);
        }

        // CRUD: READ
        private void loadCars() {
            tableModel.setRowCount(0);
            String sql = "SELECT * FROM Cars";
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    row.add(rs.getInt("car_id"));
                    row.add(rs.getString("license_plate"));
                    row.add(rs.getString("make"));
                    row.add(rs.getString("model"));
                    row.add(rs.getDouble("daily_rate"));
                    row.add(rs.getString("status"));
                    tableModel.addRow(row);
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error loading cars: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        // CRUD: CREATE
        private void addCar() {
            String license = txtLicense.getText().trim();
            String make = txtMake.getText().trim();
            String model = txtModel.getText().trim();
            String rateText = txtRate.getText().trim();

            if (license.isEmpty() || make.isEmpty() || model.isEmpty() || rateText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields must be filled.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String status = (String) cmbStatus.getSelectedItem();
            double rate;

            try {
                rate = Double.parseDouble(rateText);
                if (rate <= 0) { throw new NumberFormatException(); }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Daily Rate must be a valid positive number.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String sql = "INSERT INTO Cars (license_plate, make, model, daily_rate, status) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, license);
                pstmt.setString(2, make);
                pstmt.setString(3, model);
                pstmt.setDouble(4, rate);
                pstmt.setString(5, status);

                if (pstmt.executeUpdate() > 0) {
                    JOptionPane.showMessageDialog(this, "Car added successfully!");
                    loadCars();
                    clearForm();
                }
            } catch (SQLIntegrityConstraintViolationException e) {
                JOptionPane.showMessageDialog(this, "Error: License Plate already exists.", "Database Error", JOptionPane.ERROR_MESSAGE);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error adding car: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        // CRUD: UPDATE
        private void updateCar() {
            int selectedRow = carTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Please select a car from the table to update.", "Selection Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int carId = (int) tableModel.getValueAt(selectedRow, 0);

            String license = txtLicense.getText().trim();
            String make = txtMake.getText().trim();
            String model = txtModel.getText().trim();
            String rateText = txtRate.getText().trim();

            if (license.isEmpty() || make.isEmpty() || model.isEmpty() || rateText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields must be filled.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String status = (String) cmbStatus.getSelectedItem();
            double rate;

            try {
                rate = Double.parseDouble(rateText);
                if (rate <= 0) { throw new NumberFormatException(); }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Daily Rate must be a valid positive number.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String sql = "UPDATE Cars SET license_plate = ?, make = ?, model = ?, daily_rate = ?, status = ? WHERE car_id = ?";
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, license);
                pstmt.setString(2, make);
                pstmt.setString(3, model);
                pstmt.setDouble(4, rate);
                pstmt.setString(5, status);
                pstmt.setInt(6, carId);

                if (pstmt.executeUpdate() > 0) {
                    JOptionPane.showMessageDialog(this, "Car updated successfully!");
                    loadCars();
                    clearForm();
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error updating car: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        // CRUD: DELETE
        private void deleteCar() {
            int selectedRow = carTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Please select a car from the table to delete.", "Selection Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this car?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) { return; }

            int carId = (int) tableModel.getValueAt(selectedRow, 0);
            String sql = "DELETE FROM Cars WHERE car_id = ?";

            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setInt(1, carId);

                if (pstmt.executeUpdate() > 0) {
                    JOptionPane.showMessageDialog(this, "Car deleted successfully!");
                    loadCars();
                    clearForm();
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error deleting car: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        // --- UI Utility Methods ---
        private void clearForm() {
            txtLicense.setText("");
            txtMake.setText("");
            txtModel.setText("");
            txtRate.setText("");
            cmbStatus.setSelectedIndex(0);
            carTable.clearSelection();
        }

        private void setupListeners() {
            btnAdd.addActionListener(e -> addCar());
            btnUpdate.addActionListener(e -> updateCar());
            btnDelete.addActionListener(e -> deleteCar());
            btnClear.addActionListener(e -> clearForm());

            // Fill form fields when a row in the table is clicked
            carTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    if (!e.getValueIsAdjusting() && carTable.getSelectedRow() != -1) {
                        int selectedRow = carTable.getSelectedRow();
                        txtLicense.setText(tableModel.getValueAt(selectedRow, 1).toString());
                        txtMake.setText(tableModel.getValueAt(selectedRow, 2).toString());
                        txtModel.setText(tableModel.getValueAt(selectedRow, 3).toString());
                        txtRate.setText(tableModel.getValueAt(selectedRow, 4).toString());
                        cmbStatus.setSelectedItem(tableModel.getValueAt(selectedRow, 5).toString());
                    }
                }
            });
        }
    }

    // ====================================================================
    // 3. CUSTOMER VIEW CLASS (Car Listing and Booking)
    // ====================================================================
    private class CustomerView extends JFrame {
        private JTable carTable;
        private DefaultTableModel tableModel;
        private final int userId;

        public CustomerView(int userId) {
            this.userId = userId;
            setTitle("Customer Rental Portal (User ID: " + userId + ")"); // Show ID for clarity
            setSize(900, 600);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLayout(new BorderLayout(10, 10));
            setLocationRelativeTo(null);

            // --- Header Panel ---
            JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            JLabel titleLabel = new JLabel("Available Cars for Rent", SwingConstants.CENTER);
            titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
            headerPanel.add(titleLabel);
            add(headerPanel, BorderLayout.NORTH);

            // --- Table Setup ---
            // Columns relevant to the customer:
            tableModel = new DefaultTableModel(new String[]{"License Plate", "Make", "Model", "Daily Rate"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) { return false; }
            };
            carTable = new JTable(tableModel);
            carTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            // Set font for better readability
            carTable.setFont(new Font("SansSerif", Font.PLAIN, 14));

            add(new JScrollPane(carTable), BorderLayout.CENTER);

            // --- Action Panel (Bottom) ---
            JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
            JButton btnBook = new JButton("Book Selected Car (Coming Soon)");
            // Placeholder listener for future implementation
            btnBook.addActionListener(e -> JOptionPane.showMessageDialog(this,
                    "Booking feature for Car ID: [Selected Car ID] is coming soon!", "Feature Alert", JOptionPane.INFORMATION_MESSAGE));

            actionPanel.add(btnBook);
            add(actionPanel, BorderLayout.SOUTH);

            // --- Load Data ---
            loadAvailableCars();

            setVisible(true);
        }

        /**
         * Loads cars with 'Available' status and populates the customer table.
         */
        private void loadAvailableCars() {
            tableModel.setRowCount(0); // Clear existing data
            // Select only Available cars, ordering by Make and Model for better browsing
            String sql = "SELECT license_plate, make, model, daily_rate FROM Cars WHERE status = 'Available' ORDER BY make, model";

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    row.add(rs.getString("license_plate"));
                    row.add(rs.getString("make"));
                    row.add(rs.getString("model"));
                    row.add(rs.getDouble("daily_rate"));
                    tableModel.addRow(row);
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error loading available cars: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ====================================================================
    // 4. MAIN ENTRY POINT
    // ====================================================================
    public static void main(String[] args) {
        try {
            // Register the MySQL JDBC Driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            SwingUtilities.invokeLater(() -> new CarRentalSystem());
        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(null, "MySQL JDBC Driver not found. Check your classpath.", "Driver Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

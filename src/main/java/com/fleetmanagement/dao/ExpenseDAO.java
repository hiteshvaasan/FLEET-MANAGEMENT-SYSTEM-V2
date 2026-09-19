package com.fleetmanagement.dao;

import com.fleetmanagement.model.Expense;
import com.fleetmanagement.db.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExpenseDAO {

    // ==============================================================
    // GENERATE EXPENSE ID (e.g., E001)
    // ==============================================================
    public String generateNextExpenseId() {
        String sql = "SELECT COUNT(*) FROM expenses";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int count = rs.getInt(1);
                return String.format("E%03d", count + 1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "E001";
    }

    // ==============================================================
    // ADD EXPENSE
    // ==============================================================
    public boolean addExpense(Expense expense) {
        String generatedId = generateNextExpenseId();
        expense.setExpenseId(generatedId);

        String sql = "INSERT INTO expenses (expense_id, trip_id, expense_type, amount, expense_date, description) VALUES (?, ?, ?, ?, ?, ?)";
        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, expense.getExpenseId());
            pstmt.setString(2, expense.getTripId());
            pstmt.setString(3, expense.getExpenseType());
            pstmt.setDouble(4, expense.getAmount());
            pstmt.setDate(5, Date.valueOf(expense.getExpenseDate()));
            pstmt.setString(6, expense.getDescription());

            int rowsInserted = pstmt.executeUpdate();
            if (rowsInserted > 0) {
                System.out.println("Expense added successfully!");
                return true;
            }
        } catch (SQLException e) {
            System.out.println("Error: Unable to add expense. " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // ==============================================================
    // GET ALL EXPENSES
    // ==============================================================
    public List<Expense> getAllExpenses() {
        List<Expense> list = new ArrayList<>();
        String query = "SELECT * FROM expenses";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                list.add(new Expense(
                        rs.getString("expense_id"), // Fixed: Changed from getInt to getString
                        rs.getString("trip_id"),
                        rs.getString("expense_type"),
                        rs.getString("description"),
                        rs.getDouble("amount"),
                        rs.getDate("expense_date").toLocalDate()
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ==============================================================
    // UPDATE EXPENSE
    // ==============================================================
    public boolean updateExpense(Expense expense) {
        String query = "UPDATE expenses SET trip_id = ?, expense_type = ?, description = ?, amount = ?, expense_date = ? WHERE expense_id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, expense.getTripId());
            pstmt.setString(2, expense.getExpenseType());
            pstmt.setString(3, expense.getDescription());
            pstmt.setDouble(4, expense.getAmount());
            pstmt.setDate(5, Date.valueOf(expense.getExpenseDate()));
            pstmt.setString(6, expense.getExpenseId()); // Fixed: Changed from setInt to setString
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ==============================================================
    // DELETE EXPENSE
    // ==============================================================
    public boolean deleteExpense(String expenseId) {
        String sql = "DELETE FROM expenses WHERE expense_id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, expenseId);
            int rowsDeleted = pstmt.executeUpdate();
            if (rowsDeleted > 0) {
                System.out.println("Expense deleted successfully!");
                return true;
            }
        } catch (SQLException e) {
            System.out.println("Error: Unable to delete expense. " + e.getMessage());
        }
        return false;
    }

    // ==============================================================
    // CLEAR ALL EXPENSES
    // ==============================================================
    public boolean clearAllExpenses() {
        String sql = "TRUNCATE TABLE expenses";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            return true;
        } catch (SQLException e) {
            System.out.println("Error: Unable to clear all expenses. " + e.getMessage());
        }
        return false;
    }
}
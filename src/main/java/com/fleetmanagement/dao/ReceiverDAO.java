package com.fleetmanagement.dao;

import com.fleetmanagement.db.DatabaseConnection;
import com.fleetmanagement.model.Receiver;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReceiverDAO {

    // 1. Used by the Web Portal Login
    public boolean validateReceiver(String receiverId, String password) {
        String query = "SELECT * FROM receivers WHERE receiver_id = ? AND password = ?";
        System.out.println("Trying login -> ID: " + receiverId + " | Pass: " + password);

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, receiverId);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();

            boolean found = rs.next();
            System.out.println("Did MySQL find a match? " + found);
            return found;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 2. Used by the Admin Panel to Auto-Generate IDs
    public String generateNextReceiverId() {
        String sql = "SELECT receiver_id FROM receivers ORDER BY receiver_id DESC LIMIT 1";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                String lastId = rs.getString("receiver_id"); // e.g. "R001"
                int num = Integer.parseInt(lastId.substring(1)) + 1;
                return String.format("R%03d", num);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "R001"; // Default if table is empty
    }

    // 3. Admin Panel: Add Receiver
    public boolean addReceiver(Receiver receiver) {
        String sql = "INSERT INTO receivers (receiver_id, name, email, password, phone, company_name) VALUES (?, ?, ?, ?, ?, ?)";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, receiver.getReceiverId());
            pstmt.setString(2, receiver.getName());
            pstmt.setString(3, receiver.getEmail());
            pstmt.setString(4, receiver.getPassword());
            pstmt.setString(5, receiver.getPhone());
            pstmt.setString(6, receiver.getCompanyName());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // 4. Admin Panel: Get All Receivers
    public List<Receiver> getAllReceivers() {
        List<Receiver> list = new ArrayList<>();
        String sql = "SELECT * FROM receivers";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(new Receiver(
                        rs.getString("receiver_id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("phone"),
                        rs.getString("company_name")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 5. Admin Panel: Update Receiver
    public boolean updateReceiver(Receiver receiver) {
        String sql = "UPDATE receivers SET name = ?, email = ?, phone = ?, company_name = ? WHERE receiver_id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, receiver.getName());
            pstmt.setString(2, receiver.getEmail());
            pstmt.setString(3, receiver.getPhone());
            pstmt.setString(4, receiver.getCompanyName());
            pstmt.setString(5, receiver.getReceiverId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // 6. Admin Panel: Delete Receiver
    public boolean deleteReceiver(String receiverId) {
        String sql = "DELETE FROM receivers WHERE receiver_id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, receiverId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // 7. Admin Panel: Delete All Receivers
    public boolean deleteAllReceivers() {
        String sql = "DELETE FROM receivers";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            return pstmt.executeUpdate() >= 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
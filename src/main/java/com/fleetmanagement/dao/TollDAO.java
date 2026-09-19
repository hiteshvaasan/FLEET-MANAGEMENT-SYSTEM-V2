package com.fleetmanagement.dao;

import com.fleetmanagement.model.Toll;
import com.fleetmanagement.db.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TollDAO {

    // ==============================================================
    // ADD TOLL
    // ==============================================================
    public boolean addToll(Toll toll) {
        String generatedId = generateNextTollId();
        toll.setTollId(generatedId);

        String sql = "INSERT INTO tolls (toll_id, trip_id, toll_name, location, amount, toll_date) VALUES (?, ?, ?, ?, ?, ?)";
        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, toll.getTollId());
            pstmt.setString(2, toll.getTripId());
            pstmt.setString(3, toll.getTollName());
            pstmt.setString(4, toll.getLocation());
            pstmt.setDouble(5, toll.getAmount());
            pstmt.setDate(6, Date.valueOf(toll.getTollDate()));

            int rowsInserted = pstmt.executeUpdate();
            if (rowsInserted > 0) {
                System.out.println("Toll added successfully!");
                return true;
            }
        } catch (SQLException e) {
            System.out.println("Error: Unable to add toll. " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    // ==============================================================
    // GENERATE TOLL ID (e.g., TL001)
    // ==============================================================
    public String generateNextTollId() {
        String sql = "SELECT COUNT(*) FROM tolls";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int count = rs.getInt(1);
                return String.format("TL%03d", count + 1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "TL001";
    }

    public List<Toll> getAllTolls() {
        List<Toll> list = new ArrayList<>();
        String query = "SELECT * FROM tolls";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                list.add(new Toll(
                        rs.getString("toll_id"),
                        rs.getString("trip_id"),
                        rs.getString("toll_name"),
                        rs.getString("location"),
                        rs.getDouble("amount"),
                        rs.getDate("toll_date").toLocalDate()
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean updateToll(Toll toll) {
        String query = "UPDATE tolls SET trip_id = ?, toll_name = ?, location = ?, amount = ?, toll_date = ? WHERE toll_id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, toll.getTripId());
            pstmt.setString(2, toll.getTollName());
            pstmt.setString(3, toll.getLocation());
            pstmt.setDouble(4, toll.getAmount());
            pstmt.setDate(5, Date.valueOf(toll.getTollDate()));
            pstmt.setString(6, toll.getTollId()); // Change from setInt to setString
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteToll(String tollId) {
        String sql = "DELETE FROM tolls WHERE toll_id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tollId);
            int rowsDeleted = pstmt.executeUpdate();
            if (rowsDeleted > 0) {
                System.out.println("Toll deleted successfully!");
                return true;
            }
        } catch (SQLException e) {
            System.out.println("Error: Unable to delete toll. " + e.getMessage());
        }
        return false;
    }
    // ==============================================================
    // CLEAR ALL TOLLS
    // ==============================================================
    public boolean clearAllTolls() {
        String sql = "TRUNCATE TABLE tolls";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            return true;
        } catch (SQLException e) {
            System.out.println("Error: Unable to clear all tolls. " + e.getMessage());
        }
        return false;
    }
}
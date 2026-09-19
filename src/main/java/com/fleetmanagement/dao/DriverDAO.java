package com.fleetmanagement.dao;

import com.fleetmanagement.db.DatabaseConnection;
import com.fleetmanagement.model.Driver;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DriverDAO {

    public boolean addDriver(Driver driver) {
        String sql = "INSERT INTO drivers (driver_id, driver_name, phone, license_number, license_expiry, status, license_photo_path) VALUES (?, ?, ?, ?, ?, ?, ?)";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, driver.getDriverId());
            pstmt.setString(2, driver.getDriverName());
            pstmt.setString(3, driver.getPhone());
            pstmt.setString(4, driver.getLicenseNumber());
            pstmt.setDate(5, Date.valueOf(driver.getLicenseExpiry()));
            pstmt.setString(6, driver.getStatus());
            pstmt.setString(7, driver.getLicensePhotoPath());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Driver> getAllDrivers() {
        List<Driver> list = new ArrayList<>();
        String sql = "SELECT * FROM drivers";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Date expiryDate = rs.getDate("license_expiry");
                LocalDate localExpiry = expiryDate != null ? expiryDate.toLocalDate() : null;

                list.add(new Driver(
                        rs.getString("driver_id"),
                        rs.getString("driver_name"),
                        rs.getString("phone"),
                        rs.getString("license_number"),
                        localExpiry,
                        rs.getString("status"),
                        rs.getString("license_photo_path")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean updateDriverStatus(String driverId, String status) {
        String sql = "UPDATE drivers SET status = ? WHERE driver_id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setString(2, driverId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteDriver(String driverId) {
        String sql = "DELETE FROM drivers WHERE driver_id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, driverId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteAllDrivers() {
        String sql = "DELETE FROM drivers";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            return pstmt.executeUpdate() >= 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
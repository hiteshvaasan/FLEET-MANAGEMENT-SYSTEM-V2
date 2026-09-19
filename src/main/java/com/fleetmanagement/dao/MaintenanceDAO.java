package com.fleetmanagement.dao;

import com.fleetmanagement.db.DatabaseConnection;
import com.fleetmanagement.model.Maintenance;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MaintenanceDAO {

    public boolean addMaintenance(Maintenance maintenance) {
        String sql = "INSERT INTO maintenance (vehicle_id, maintenance_date, maintenance_type, description, cost, next_service_date, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, maintenance.getVehicleId());
            pstmt.setDate(2, maintenance.getMaintenanceDate() != null ? Date.valueOf(maintenance.getMaintenanceDate()) : null);
            pstmt.setString(3, maintenance.getMaintenanceType());
            pstmt.setString(4, maintenance.getDescription());
            pstmt.setDouble(5, maintenance.getCost());
            pstmt.setDate(6, maintenance.getNextServiceDate() != null ? Date.valueOf(maintenance.getNextServiceDate()) : null);
            pstmt.setString(7, maintenance.getStatus());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                // If maintenance is 'In Progress', set vehicle status to 'Under Maintenance'
                if ("In Progress".equalsIgnoreCase(maintenance.getStatus())) {
                    updateVehicleStatus(conn, maintenance.getVehicleId(), "Under Maintenance");
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Maintenance> getAllMaintenance() {
        List<Maintenance> list = new ArrayList<>();
        String sql = "SELECT * FROM maintenance";
        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToMaintenance(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean updateMaintenance(Maintenance maintenance) {
        String sql = "UPDATE maintenance SET vehicle_id = ?, maintenance_date = ?, maintenance_type = ?, description = ?, cost = ?, next_service_date = ?, status = ? WHERE maintenance_id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, maintenance.getVehicleId());
            pstmt.setDate(2, maintenance.getMaintenanceDate() != null ? Date.valueOf(maintenance.getMaintenanceDate()) : null);
            pstmt.setString(3, maintenance.getMaintenanceType());
            pstmt.setString(4, maintenance.getDescription());
            pstmt.setDouble(5, maintenance.getCost());
            pstmt.setDate(6, maintenance.getNextServiceDate() != null ? Date.valueOf(maintenance.getNextServiceDate()) : null);
            pstmt.setString(7, maintenance.getStatus());
            pstmt.setInt(8, maintenance.getMaintenanceId());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                // Handle automatic vehicle status sync based on maintenance completion
                if ("Completed".equalsIgnoreCase(maintenance.getStatus()) || "Scheduled".equalsIgnoreCase(maintenance.getStatus())) {
                    updateVehicleStatus(conn, maintenance.getVehicleId(), "Available");
                } else if ("In Progress".equalsIgnoreCase(maintenance.getStatus())) {
                    updateVehicleStatus(conn, maintenance.getVehicleId(), "Under Maintenance");
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteMaintenance(int maintenanceId, String vehicleId) {
        String sql = "DELETE FROM maintenance WHERE maintenance_id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, maintenanceId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                // Free the vehicle back to Available when maintenance record is deleted
                updateVehicleStatus(conn, vehicleId, "Available");
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean clearAllMaintenance() {
        String sql = "DELETE FROM maintenance";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             PreparedStatement updateV = conn.prepareStatement("UPDATE vehicles SET status = 'Available' WHERE status = 'Under Maintenance'")) {
            pstmt.executeUpdate();
            updateV.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void updateVehicleStatus(Connection conn, String vehicleId, String status) {
        String sql = "UPDATE vehicles SET status = ? WHERE vehicle_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setString(2, vehicleId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String[]> getAvailableVehicles() {
        List<String[]> vehicles = new ArrayList<>();
        String sql = "SELECT vehicle_id, registration_number, vehicle_type FROM vehicles WHERE status = 'Available'";
        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                vehicles.add(new String[]{
                        rs.getString("vehicle_id"),
                        rs.getString("registration_number"),
                        rs.getString("vehicle_type")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return vehicles;
    }

    private Maintenance mapResultSetToMaintenance(ResultSet rs) throws SQLException {
        Maintenance m = new Maintenance();
        m.setMaintenanceId(rs.getInt("maintenance_id"));
        m.setVehicleId(rs.getString("vehicle_id"));
        Date mDate = rs.getDate("maintenance_date");
        m.setMaintenanceDate(mDate != null ? mDate.toLocalDate() : null);
        m.setMaintenanceType(rs.getString("maintenance_type"));
        m.setDescription(rs.getString("description"));
        m.setCost(rs.getDouble("cost"));
        Date nDate = rs.getDate("next_service_date");
        m.setNextServiceDate(nDate != null ? nDate.toLocalDate() : null);
        m.setStatus(rs.getString("status"));
        return m;
    }
}
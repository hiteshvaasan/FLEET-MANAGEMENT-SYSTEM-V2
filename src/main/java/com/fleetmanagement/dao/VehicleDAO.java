package com.fleetmanagement.dao;

import com.fleetmanagement.db.DatabaseConnection;
import com.fleetmanagement.model.Vehicle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class VehicleDAO {

    // ==============================================================
    // ADD VEHICLE (with duplicate ID / registration check)
    // ==============================================================
    public boolean addVehicle(Vehicle vehicle) {

        if (vehicleIdExists(vehicle.getVehicleId())) {
            System.out.println("Error: A vehicle with ID '" + vehicle.getVehicleId() + "' already exists.");
            return false;
        }

        if (registrationNumberExists(vehicle.getRegistrationNumber())) {
            System.out.println("Error: A vehicle with registration number '" +
                    vehicle.getRegistrationNumber() + "' already exists.");
            return false;
        }

        String sql = "INSERT INTO vehicles " +
                "(vehicle_id, registration_number, vehicle_type, model, capacity_kg, fuel_type, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, vehicle.getVehicleId());
            pstmt.setString(2, vehicle.getRegistrationNumber());
            pstmt.setString(3, vehicle.getVehicleType());
            pstmt.setString(4, vehicle.getModel());
            pstmt.setDouble(5, vehicle.getCapacityKg());
            pstmt.setString(6, vehicle.getFuelType());
            pstmt.setString(7, vehicle.getStatus());

            int rowsInserted = pstmt.executeUpdate();

            if (rowsInserted > 0) {
                System.out.println("Vehicle added successfully!");
                return true;
            }

        } catch (SQLException e) {
            System.out.println("Error: Unable to add vehicle. " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // ==============================================================
    // CHECK: Vehicle ID already exists
    // ==============================================================
    public boolean vehicleIdExists(String vehicleId) {
        String sql = "SELECT vehicle_id FROM vehicles WHERE vehicle_id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, vehicleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.out.println("Error: Unable to check vehicle ID. " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // ==============================================================
    // CHECK: Registration number already exists
    // ==============================================================
    public boolean registrationNumberExists(String registrationNumber) {
        String sql = "SELECT registration_number FROM vehicles WHERE registration_number = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, registrationNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.out.println("Error: Unable to check registration number. " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // ==============================================================
    // GET ALL VEHICLES
    // ==============================================================
    public List<Vehicle> getAllVehicles() {
        List<Vehicle> vehicleList = new ArrayList<>();
        String sql = "SELECT * FROM vehicles";

        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Vehicle vehicle = new Vehicle(
                        rs.getString("vehicle_id"),
                        rs.getString("registration_number"),
                        rs.getString("vehicle_type"),
                        rs.getString("model"),
                        rs.getDouble("capacity_kg"),
                        rs.getString("fuel_type"),
                        rs.getString("status")
                );
                vehicleList.add(vehicle);
            }

        } catch (SQLException e) {
            System.out.println("Error: Unable to retrieve vehicles. " + e.getMessage());
            e.printStackTrace();
        }

        return vehicleList;
    }

    // ==============================================================
    // UPDATE VEHICLE (full record update)
    // ==============================================================
    public boolean updateVehicle(Vehicle vehicle) {
        String sql = "UPDATE vehicles SET registration_number = ?, vehicle_type = ?, model = ?, " +
                "capacity_kg = ?, fuel_type = ?, status = ? WHERE vehicle_id = ?";

        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, vehicle.getRegistrationNumber());
            pstmt.setString(2, vehicle.getVehicleType());
            pstmt.setString(3, vehicle.getModel());
            pstmt.setDouble(4, vehicle.getCapacityKg());
            pstmt.setString(5, vehicle.getFuelType());
            pstmt.setString(6, vehicle.getStatus());
            pstmt.setString(7, vehicle.getVehicleId());

            int rowsUpdated = pstmt.executeUpdate();

            if (rowsUpdated > 0) {
                System.out.println("Vehicle updated successfully!");
                return true;
            } else {
                System.out.println("No vehicle found with ID: " + vehicle.getVehicleId());
            }

        } catch (SQLException e) {
            System.out.println("Error: Unable to update vehicle. " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // ==============================================================
    // UPDATE VEHICLE STATUS ONLY
    // ==============================================================
    public boolean updateVehicleStatus(String vehicleId, String newStatus) {
        String sql = "UPDATE vehicles SET status = ? WHERE vehicle_id = ?";

        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newStatus);
            pstmt.setString(2, vehicleId);

            int rowsUpdated = pstmt.executeUpdate();

            if (rowsUpdated > 0) {
                System.out.println("Vehicle status updated successfully!");
                return true;
            } else {
                System.out.println("No vehicle found with ID: " + vehicleId);
            }

        } catch (SQLException e) {
            System.out.println("Error: Unable to update vehicle status. " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // ==============================================================
    // DELETE A SINGLE VEHICLE
    // Handles ON DELETE RESTRICT failures (vehicle linked to trips)
    // ==============================================================
    public boolean deleteVehicle(String vehicleId) {
        String sql = "DELETE FROM vehicles WHERE vehicle_id = ?";

        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, vehicleId);

            int rowsDeleted = pstmt.executeUpdate();

            if (rowsDeleted > 0) {
                System.out.println("Vehicle deleted successfully!");
                return true;
            } else {
                System.out.println("No vehicle found with ID: " + vehicleId);
            }

        } catch (SQLException e) {
            // MySQL error code 1451 = Cannot delete or update a parent row:
            // a foreign key constraint fails (ON DELETE RESTRICT from trips table)
            System.out.println("Error: Unable to delete vehicle. It is likely linked to existing trip records.");
            e.printStackTrace();
        }

        return false;
    }

    // ==============================================================
    // DELETE ALL VEHICLES (destructive - wipes entire table)
    // Note: Will fail (and roll back naturally per-row via exception)
    // if ANY vehicle in the table is linked to a trip, due to
    // ON DELETE RESTRICT. This is intentional data-safety behavior.
    // ==============================================================
    public boolean deleteAllVehicles() {
        String sql = "DELETE FROM vehicles";

        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            int rowsDeleted = pstmt.executeUpdate();
            System.out.println(rowsDeleted + " vehicle record(s) deleted successfully!");
            return true;

        } catch (SQLException e) {
            System.out.println("Error: Unable to clear vehicles table. " +
                    "One or more vehicles may be linked to existing trip records.");
            e.printStackTrace();
        }

        return false;
    }
}
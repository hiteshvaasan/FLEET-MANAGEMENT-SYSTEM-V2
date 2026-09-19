package com.fleetmanagement.dao;

import com.fleetmanagement.model.Fuel;
import com.fleetmanagement.db.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FuelDAO {

    public String[] getCompletedTripDetails(String tripId) {
        String query = "SELECT t.vehicle_id, v.fuel_type, t.status, (t.end_odometer - t.start_odometer) AS distance " +
                "FROM trips t JOIN vehicles v ON t.vehicle_id = v.vehicle_id WHERE t.trip_id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, tripId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String vehicleId = rs.getString("vehicle_id");
                String fuelType = rs.getString("fuel_type");
                String status = rs.getString("status");
                String distance = String.valueOf(rs.getDouble("distance"));
                return new String[]{vehicleId, fuelType, status, distance};
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String generateNextFuelId() {
        String query = "SELECT fuel_id FROM fuel_records ORDER BY fuel_id DESC LIMIT 1";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                String lastId = rs.getString("fuel_id");
                int num = Integer.parseInt(lastId.substring(1)) + 1;
                return String.format("F%03d", num);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "F001";
    }

    public boolean addFuelRecord(Fuel fuel) {
        String query = "INSERT INTO fuel_records (fuel_id, trip_id, vehicle_id, fuel_type, distance, fuel_used, fuel_price, fuel_cost, mileage) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, fuel.getFuelId());
            pstmt.setString(2, fuel.getTripId());
            pstmt.setString(3, fuel.getVehicleId());
            pstmt.setString(4, fuel.getFuelType());
            pstmt.setDouble(5, fuel.getDistance());
            pstmt.setDouble(6, fuel.getFuelUsed());
            pstmt.setDouble(7, fuel.getFuelPrice());
            pstmt.setDouble(8, fuel.getFuelCost());
            pstmt.setDouble(9, fuel.getMileage());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Fuel> getAllFuelRecords() {
        List<Fuel> list = new ArrayList<>();
        String query = "SELECT * FROM fuel_records";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                list.add(new Fuel(
                        rs.getString("fuel_id"),
                        rs.getString("trip_id"),
                        rs.getString("vehicle_id"),
                        rs.getString("fuel_type"),
                        rs.getDouble("distance"),
                        rs.getDouble("fuel_used"),
                        rs.getDouble("fuel_price"),
                        rs.getDouble("fuel_cost"),
                        rs.getDouble("mileage")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean updateFuelRecord(Fuel fuel) {
        String query = "UPDATE fuel_records SET trip_id = ?, vehicle_id = ?, fuel_type = ?, distance = ?, fuel_used = ?, fuel_price = ?, fuel_cost = ?, mileage = ? WHERE fuel_id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, fuel.getTripId());
            pstmt.setString(2, fuel.getVehicleId());
            pstmt.setString(3, fuel.getFuelType());
            pstmt.setDouble(4, fuel.getDistance());
            pstmt.setDouble(5, fuel.getFuelUsed());
            pstmt.setDouble(6, fuel.getFuelPrice());
            pstmt.setDouble(7, fuel.getFuelCost());
            pstmt.setDouble(8, fuel.getMileage());
            pstmt.setString(9, fuel.getFuelId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteFuelRecord(String fuelId) {
        String query = "DELETE FROM fuel_records WHERE fuel_id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, fuelId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean clearAllFuelRecords() {
        String query = "DELETE FROM fuel_records";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (Statement stmt = conn.createStatement()) {
            return stmt.executeUpdate(query) >= 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    public List<String[]> getAllTripsForSelection() {
        List<String[]> list = new ArrayList<>();
        // Only fetch 'Completed' trips, because we need the final distance to calculate mileage
        String query = "SELECT trip_id, vehicle_id, status FROM trips WHERE status = 'Completed'";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                list.add(new String[]{
                        rs.getString("trip_id"),
                        rs.getString("vehicle_id"),
                        rs.getString("status")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
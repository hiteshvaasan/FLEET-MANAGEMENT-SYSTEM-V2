package com.fleetmanagement.dao;

import com.fleetmanagement.db.DatabaseConnection;
import com.fleetmanagement.model.Cargo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CargoDAO {

    // ==============================================================
    // GENERATE CARGO ID (e.g., C001)
    // ==============================================================
    public String generateNextCargoId() {
        String sql = "SELECT COUNT(*) FROM cargo";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int count = rs.getInt(1);
                return String.format("C%03d", count + 1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "C001";
    }

    // ==============================================================
    // ADD CARGO (Fixed Capacity Validation)
    // ==============================================================
    public boolean addCargo(Cargo cargo) {
        Connection conn = DatabaseConnection.getInstance().getConnection();

        // 1. Verify Trip ID exists and check EXACT remaining capacity mathematically
        String checkSql = "SELECT v.capacity_kg, " +
                "(SELECT COALESCE(SUM(weight_kg), 0) FROM cargo WHERE trip_id = ?) as loaded_weight " +
                "FROM trips t JOIN vehicles v ON t.vehicle_id = v.vehicle_id WHERE t.trip_id = ?";

        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, cargo.getTripId());
            checkStmt.setString(2, cargo.getTripId());

            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                double capacity = rs.getDouble("capacity_kg");
                double loaded = rs.getDouble("loaded_weight");

                if (loaded + cargo.getWeightKg() > capacity) {
                    System.out.println("Error: Exceeds Capacity. Max: " + capacity + "kg, Loaded: " + loaded + "kg");
                    return false; // Fails validation, too heavy!
                }
            } else {
                System.out.println("Error: Invalid Trip ID.");
                return false; // Trip doesn't exist
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        // 2. Insert the Cargo
        String generatedId = generateNextCargoId();
        cargo.setCargoId(generatedId);

        String sql = "INSERT INTO cargo (cargo_id, trip_id, goods_type, quantity, weight_kg, description) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cargo.getCargoId());
            pstmt.setString(2, cargo.getTripId());
            pstmt.setString(3, cargo.getGoodsType());
            pstmt.setInt(4, cargo.getQuantity());
            pstmt.setDouble(5, cargo.getWeightKg());
            pstmt.setString(6, cargo.getDescription());

            int rowsInserted = pstmt.executeUpdate();
            if (rowsInserted > 0) {
                System.out.println("Cargo added successfully!");
                return true;
            }
        } catch (SQLException e) {
            System.out.println("Error: Unable to add cargo. " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // ==============================================================
    // GET SCHEDULED TRIPS FOR POPUP (New Method)
    // ==============================================================
    public List<String[]> getScheduledTripsWithCapacity() {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT t.trip_id, t.vehicle_id, v.capacity_kg, " +
                "COALESCE(SUM(c.weight_kg), 0) AS loaded_weight " +
                "FROM trips t " +
                "JOIN vehicles v ON t.vehicle_id = v.vehicle_id " +
                "LEFT JOIN cargo c ON t.trip_id = c.trip_id " +
                "WHERE t.status = 'Scheduled' OR t.status = 'In Progress' " +
                "GROUP BY t.trip_id, t.vehicle_id, v.capacity_kg";

        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String tripId = rs.getString("trip_id");
                String vehicleId = rs.getString("vehicle_id");
                double capacity = rs.getDouble("capacity_kg");
                double loaded = rs.getDouble("loaded_weight");
                double remaining = capacity - loaded;

                list.add(new String[]{
                        tripId,
                        vehicleId,
                        String.format("%.2f", capacity),
                        String.format("%.2f", remaining)
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ==============================================================
    // GET ALL CARGO
    // ==============================================================
    public List<Cargo> getAllCargo() {
        List<Cargo> cargoList = new ArrayList<>();
        String sql = "SELECT * FROM cargo";
        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Cargo cargo = new Cargo(
                        rs.getString("cargo_id"),
                        rs.getString("trip_id"),
                        rs.getString("goods_type"),
                        rs.getInt("quantity"),
                        rs.getDouble("weight_kg"),
                        rs.getString("description")
                );
                cargoList.add(cargo);
            }
        } catch (SQLException e) {
            System.out.println("Error: Unable to retrieve all cargo. " + e.getMessage());
            e.printStackTrace();
        }
        return cargoList;
    }

    // ==============================================================
    // GET CARGO BY TRIP ID
    // ==============================================================
    public List<Cargo> getCargoByTripId(String tripId) {
        List<Cargo> cargoList = new ArrayList<>();
        String sql = "SELECT * FROM cargo WHERE trip_id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tripId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Cargo cargo = new Cargo(
                            rs.getString("cargo_id"),
                            rs.getString("trip_id"),
                            rs.getString("goods_type"),
                            rs.getInt("quantity"),
                            rs.getDouble("weight_kg"),
                            rs.getString("description")
                    );
                    cargoList.add(cargo);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error: Unable to retrieve cargo for trip " + tripId + ". " + e.getMessage());
            e.printStackTrace();
        }
        return cargoList;
    }

    // ==============================================================
    // UPDATE CARGO (Fixed Capacity Validation)
    // ==============================================================
    public boolean updateCargo(Cargo cargo) {
        Connection conn = DatabaseConnection.getInstance().getConnection();

        // 1. Verify capacity EXCLUDING the current cargo's weight (so you don't double-count it)
        String checkSql = "SELECT v.capacity_kg, " +
                "(SELECT COALESCE(SUM(weight_kg), 0) FROM cargo WHERE trip_id = ? AND cargo_id != ?) as loaded_weight " +
                "FROM trips t JOIN vehicles v ON t.vehicle_id = v.vehicle_id WHERE t.trip_id = ?";

        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, cargo.getTripId());
            checkStmt.setString(2, cargo.getCargoId());
            checkStmt.setString(3, cargo.getTripId());

            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                double capacity = rs.getDouble("capacity_kg");
                double loaded = rs.getDouble("loaded_weight");

                if (loaded + cargo.getWeightKg() > capacity) {
                    System.out.println("Error: Update Exceeds Capacity.");
                    return false;
                }
            } else {
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        // 2. Proceed with Update
        String sql = "UPDATE cargo SET trip_id=?, goods_type=?, quantity=?, weight_kg=?, description=? WHERE cargo_id=?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cargo.getTripId());
            pstmt.setString(2, cargo.getGoodsType());
            pstmt.setInt(3, cargo.getQuantity());
            pstmt.setDouble(4, cargo.getWeightKg());
            pstmt.setString(5, cargo.getDescription());
            pstmt.setString(6, cargo.getCargoId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error: Unable to update cargo. " + e.getMessage());
        }
        return false;
    }

    // ==============================================================
    // DELETE CARGO
    // ==============================================================
    public boolean deleteCargo(String cargoId) {
        String sql = "DELETE FROM cargo WHERE cargo_id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cargoId);
            int rowsDeleted = pstmt.executeUpdate();
            if (rowsDeleted > 0) {
                System.out.println("Cargo deleted successfully!");
                return true;
            }
        } catch (SQLException e) {
            System.out.println("Error: Unable to delete cargo. " + e.getMessage());
        }
        return false;
    }

    // ==============================================================
    // CLEAR ALL CARGO
    // ==============================================================
    public boolean clearAllCargo() {
        String sql = "TRUNCATE TABLE cargo";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            return true;
        } catch (SQLException e) {
            System.out.println("Error: Unable to clear all cargo. " + e.getMessage());
        }
        return false;
    }

    // ==============================================================
    // HELPER: Resolve vehicle_id for a given trip_id
    // ==============================================================
    private String getVehicleIdForTrip(String tripId) {
        String sql = "SELECT vehicle_id FROM trips WHERE trip_id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tripId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("vehicle_id");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error: Unable to resolve vehicle for trip " + tripId + ". " + e.getMessage());
        }
        return null;
    }

    // ==============================================================
    // HELPER: Get vehicle capacity info for a specific trip
    // ==============================================================
    public String[] getVehicleCapacityInfoForTrip(String tripId) {
        String sql = "SELECT v.vehicle_id, v.vehicle_type, v.capacity_kg " +
                "FROM trips t JOIN vehicles v ON t.vehicle_id = v.vehicle_id " +
                "WHERE t.trip_id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tripId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new String[]{
                            rs.getString("vehicle_id"),
                            rs.getString("vehicle_type"),
                            String.valueOf(rs.getDouble("capacity_kg"))
                    };
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching vehicle capacity for trip: " + e.getMessage());
        }
        return null;
    }
}
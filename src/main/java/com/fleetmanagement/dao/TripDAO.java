package com.fleetmanagement.dao;

import com.fleetmanagement.db.DatabaseConnection;
import com.fleetmanagement.model.Trip;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TripDAO {

    // ==============================================================
    // ADD TRIP
    // ==============================================================
    public boolean addTrip(Trip trip) {
        String sql = "INSERT INTO trips " +
                "(trip_id, vehicle_id, driver_id, source, destination, trip_date, " +
                "start_odometer, end_odometer, status, total_fuel_cost, total_toll_cost, total_other_expenses) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, trip.getTripId());
            pstmt.setString(2, trip.getVehicleId());
            pstmt.setString(3, trip.getDriverId());
            pstmt.setString(4, trip.getSource());
            pstmt.setString(5, trip.getDestination());
            pstmt.setDate(6, trip.getTripDate() != null ? Date.valueOf(trip.getTripDate()) : null);
            setNullableDouble(pstmt, 7, trip.getStartOdometer());
            setNullableDouble(pstmt, 8, trip.getEndOdometer());
            pstmt.setString(9, trip.getStatus());
            pstmt.setDouble(10, trip.getTotalFuelCost() != null ? trip.getTotalFuelCost() : 0.0);
            pstmt.setDouble(11, trip.getTotalTollCost() != null ? trip.getTotalTollCost() : 0.0);
            pstmt.setDouble(12, trip.getTotalOtherExpenses() != null ? trip.getTotalOtherExpenses() : 0.0);

            int rowsInserted = pstmt.executeUpdate();

            if (rowsInserted > 0) {
                // AUTOMATIC STATUS SYNC: Set BOTH Vehicle and Driver to 'On Trip'
                try (PreparedStatement updateV = conn.prepareStatement("UPDATE vehicles SET status = 'On Trip' WHERE vehicle_id = ?");
                     PreparedStatement updateD = conn.prepareStatement("UPDATE drivers SET status = 'On Trip' WHERE driver_id = ?")) {

                    updateV.setString(1, trip.getVehicleId());
                    updateV.executeUpdate();

                    updateD.setString(1, trip.getDriverId());
                    updateD.executeUpdate();
                }
                System.out.println("Trip added successfully!");
                return true;
            }

        } catch (SQLException e) {
            System.out.println("Error: Unable to add trip. " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // ==============================================================
    // GET ALL TRIPS
    // ==============================================================
    public List<Trip> getAllTrips() {
        List<Trip> tripList = new ArrayList<>();
        String sql = "SELECT * FROM trips";

        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                tripList.add(mapResultSetToTrip(rs));
            }

        } catch (SQLException e) {
            System.out.println("Error: Unable to retrieve trips. " + e.getMessage());
            e.printStackTrace();
        }

        return tripList;
    }

    // ==============================================================
    // GET SINGLE TRIP BY ID
    // ==============================================================
    public Trip getTripById(String tripId) {
        String sql = "SELECT * FROM trips WHERE trip_id = ?";

        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tripId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToTrip(rs);
                }
            }

        } catch (SQLException e) {
            System.out.println("Error: Unable to retrieve trip. " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    // ==============================================================
    // UPDATE TRIP (full record)
    // ==============================================================
    public boolean updateTrip(Trip trip) {
        String sql = "UPDATE trips SET vehicle_id = ?, driver_id = ?, source = ?, destination = ?, " +
                "trip_date = ?, start_odometer = ?, end_odometer = ?, status = ? WHERE trip_id = ?";

        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, trip.getVehicleId());
            pstmt.setString(2, trip.getDriverId());
            pstmt.setString(3, trip.getSource());
            pstmt.setString(4, trip.getDestination());
            pstmt.setDate(5, trip.getTripDate() != null ? Date.valueOf(trip.getTripDate()) : null);
            setNullableDouble(pstmt, 6, trip.getStartOdometer());
            setNullableDouble(pstmt, 7, trip.getEndOdometer());
            pstmt.setString(8, trip.getStatus());
            pstmt.setString(9, trip.getTripId());

            int rowsUpdated = pstmt.executeUpdate();

            if (rowsUpdated > 0) {
                // AUTOMATIC STATUS SYNC: Check if completed, then update BOTH Vehicle and Driver
                String newStatus = "Completed".equalsIgnoreCase(trip.getStatus()) ? "Available" : "On Trip";

                try (PreparedStatement updateV = conn.prepareStatement("UPDATE vehicles SET status = ? WHERE vehicle_id = ?");
                     PreparedStatement updateD = conn.prepareStatement("UPDATE drivers SET status = ? WHERE driver_id = ?")) {

                    updateV.setString(1, newStatus);
                    updateV.setString(2, trip.getVehicleId());
                    updateV.executeUpdate();

                    updateD.setString(1, newStatus);
                    updateD.setString(2, trip.getDriverId());
                    updateD.executeUpdate();
                }
                System.out.println("Trip updated successfully!");
                return true;
            } else {
                System.out.println("No trip found with ID: " + trip.getTripId());
            }

        } catch (SQLException e) {
            System.out.println("Error: Unable to update trip. " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // ==============================================================
    // DELETE TRIP
    // ==============================================================
    public boolean deleteTrip(String tripId) {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        String vehicleId = null;
        String driverId = null;

        // 1. Fetch the vehicle and driver ID BEFORE deleting the trip
        String fetchSql = "SELECT vehicle_id, driver_id FROM trips WHERE trip_id = ?";
        try (PreparedStatement fetchStmt = conn.prepareStatement(fetchSql)) {
            fetchStmt.setString(1, tripId);
            try (ResultSet rs = fetchStmt.executeQuery()) {
                if (rs.next()) {
                    vehicleId = rs.getString("vehicle_id");
                    driverId = rs.getString("driver_id");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching trip details before deletion: " + e.getMessage());
        }

        // 2. Perform the deletion
        String sql = "DELETE FROM trips WHERE trip_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tripId);
            int rowsDeleted = pstmt.executeUpdate();

            if (rowsDeleted > 0) {
                // 3. AUTOMATIC STATUS SYNC: Set Vehicle and Driver back to 'Available'
                if (vehicleId != null) {
                    try (PreparedStatement updateV = conn.prepareStatement("UPDATE vehicles SET status = 'Available' WHERE vehicle_id = ?")) {
                        updateV.setString(1, vehicleId);
                        updateV.executeUpdate();
                    }
                }
                if (driverId != null) {
                    try (PreparedStatement updateD = conn.prepareStatement("UPDATE drivers SET status = 'Available' WHERE driver_id = ?")) {
                        updateD.setString(1, driverId);
                        updateD.executeUpdate();
                    }
                }

                System.out.println("Trip deleted successfully! Vehicle and Driver are now Available.");
                return true;
            } else {
                System.out.println("No trip found with ID: " + tripId);
            }
        } catch (SQLException e) {
            System.out.println("Error: Unable to delete trip. " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // ==============================================================
    // VALIDATION METHODS
    // ==============================================================
    public boolean isVehicleAvailable(String vehicleId) {
        String sql = "SELECT status FROM vehicles WHERE vehicle_id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, vehicleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String status = rs.getString("status");
                    return "Available".equalsIgnoreCase(status);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error: Unable to check vehicle availability. " + e.getMessage());
        }
        return false;
    }

    public boolean isDriverValid(String driverId) {
        String sql = "SELECT status, license_expiry FROM drivers WHERE driver_id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, driverId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String status = rs.getString("status");
                    Date licenseExpiry = rs.getDate("license_expiry");
                    boolean isAvailable = "Available".equalsIgnoreCase(status);
                    boolean isLicenseValid = licenseExpiry != null
                            && licenseExpiry.toLocalDate().isAfter(LocalDate.now());
                    return isAvailable && isLicenseValid;
                }
            }
        } catch (SQLException e) {
            System.out.println("Error: Unable to validate driver. " + e.getMessage());
        }
        return false;
    }

    public boolean validateCargoCapacity(String vehicleId, double cargoWeight) {
        String sql = "SELECT capacity_kg FROM vehicles WHERE vehicle_id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, vehicleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    double vehicleCapacity = rs.getDouble("capacity_kg");
                    return cargoWeight <= vehicleCapacity;
                }
            }
        } catch (SQLException e) {
            System.out.println("Error: Unable to validate cargo capacity. " + e.getMessage());
        }
        return false;
    }

    // ==============================================================
    // COST AGGREGATION
    // ==============================================================
    public boolean calculateAndSetTripCosts(String tripId) {
        String fuelQuery = "SELECT SUM(fuel_cost) FROM fuel_records WHERE trip_id = ?";
        String tollQuery = "SELECT SUM(amount) FROM tolls WHERE trip_id = ?";
        String expQuery  = "SELECT SUM(amount) FROM expenses WHERE trip_id = ?";

        // FIXED: Removed "total_cost = ?" because it doesn't exist in the database.
        // The Java model handles adding these 3 together dynamically for the UI.
        String updateQuery = "UPDATE trips SET total_fuel_cost = ?, total_toll_cost = ?, total_other_expenses = ? WHERE trip_id = ?";

        Connection conn = DatabaseConnection.getInstance().getConnection();
        try {
            double totalFuel = 0;
            double totalTolls = 0;
            double totalExpenses = 0;

            try (PreparedStatement pstmt = conn.prepareStatement(fuelQuery)) {
                pstmt.setString(1, tripId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) totalFuel = rs.getDouble(1);
            }

            try (PreparedStatement pstmt = conn.prepareStatement(tollQuery)) {
                pstmt.setString(1, tripId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) totalTolls = rs.getDouble(1);
            }

            try (PreparedStatement pstmt = conn.prepareStatement(expQuery)) {
                pstmt.setString(1, tripId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) totalExpenses = rs.getDouble(1);
            }

            try (PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {
                pstmt.setDouble(1, totalFuel);
                pstmt.setDouble(2, totalTolls);
                pstmt.setDouble(3, totalExpenses);
                pstmt.setString(4, tripId);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("SQL Error calculating costs: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // ==============================================================
    // HELPERS & CLEAR
    // ==============================================================
    private Trip mapResultSetToTrip(ResultSet rs) throws SQLException {
        Trip trip = new Trip();
        trip.setTripId(rs.getString("trip_id"));
        trip.setVehicleId(rs.getString("vehicle_id"));
        trip.setDriverId(rs.getString("driver_id"));
        trip.setSource(rs.getString("source"));
        trip.setDestination(rs.getString("destination"));
        Date tripDate = rs.getDate("trip_date");
        trip.setTripDate(tripDate != null ? tripDate.toLocalDate() : null);
        double startOdo = rs.getDouble("start_odometer");
        trip.setStartOdometer(rs.wasNull() ? null : startOdo);
        double endOdo = rs.getDouble("end_odometer");
        trip.setEndOdometer(rs.wasNull() ? null : endOdo);
        trip.setStatus(rs.getString("status"));
        double fuelCost = rs.getDouble("total_fuel_cost");
        trip.setTotalFuelCost(rs.wasNull() ? 0.0 : fuelCost);
        double tollCost = rs.getDouble("total_toll_cost");
        trip.setTotalTollCost(rs.wasNull() ? 0.0 : tollCost);
        double otherExpenses = rs.getDouble("total_other_expenses");
        trip.setTotalOtherExpenses(rs.wasNull() ? 0.0 : otherExpenses);
        return trip;
    }

    private void setNullableDouble(PreparedStatement pstmt, int index, Double value) throws SQLException {
        if (value != null) pstmt.setDouble(index, value); else pstmt.setNull(index, java.sql.Types.DOUBLE);
    }

    // ==============================================================
    // CLEAR ALL TRIPS
    // ==============================================================
    public boolean clearAllTrips() {
        String sql = "DELETE FROM trips";
        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int rowsDeleted = pstmt.executeUpdate();

            if (rowsDeleted > 0) {
                // AUTOMATIC STATUS SYNC: Reset ALL vehicles and drivers to 'Available'
                try (PreparedStatement updateV = conn.prepareStatement("UPDATE vehicles SET status = 'Available'");
                     PreparedStatement updateD = conn.prepareStatement("UPDATE drivers SET status = 'Available'")) {
                    updateV.executeUpdate();
                    updateD.executeUpdate();
                }
                System.out.println("All trips cleared. All vehicles and drivers reset to Available.");
                return true;
            }
        } catch (SQLException e) {
            System.out.println("Error: Unable to clear all trips. " + e.getMessage());
        }
        return false;
    }

    // ==============================================================
    // SMART FEATURE: Get Available Drivers with Valid Licenses
    // ==============================================================
    public List<String[]> getAvailableDrivers() {
        List<String[]> drivers = new ArrayList<>();
        String sql = "SELECT driver_id, driver_name, license_number, license_expiry FROM drivers " +
                "WHERE LOWER(TRIM(status)) = 'available' AND license_expiry > ?";

        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, Date.valueOf(LocalDate.now()));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    drivers.add(new String[]{
                            rs.getString("driver_id"),
                            rs.getString("driver_name"),
                            rs.getString("license_number"),
                            String.valueOf(rs.getDate("license_expiry"))
                    });
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching available drivers: " + e.getMessage());
        }
        return drivers;
    }

    public List<String[]> getAvailableVehiclesWithCapacity(double requiredCapacity) {
        List<String[]> vehicles = new ArrayList<>();
        String sql = "SELECT vehicle_id, registration_number, vehicle_type, capacity_kg FROM vehicles " +
                "WHERE status = 'Available' AND capacity_kg >= ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, requiredCapacity);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    vehicles.add(new String[]{
                            rs.getString("vehicle_id"),
                            rs.getString("registration_number"),
                            rs.getString("vehicle_type"),
                            String.valueOf(rs.getDouble("capacity_kg"))
                    });
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching available vehicles: " + e.getMessage());
        }
        return vehicles;
    }
}
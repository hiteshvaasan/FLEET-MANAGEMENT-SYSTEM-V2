package com.fleetmanagement.dao;

import com.fleetmanagement.db.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;

public class DashboardDAO {

    public int getCount(String query) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getTotalVehicles() { return getCount("SELECT COUNT(*) FROM vehicles"); }
    public int getAvailableVehicles() { return getCount("SELECT COUNT(*) FROM vehicles WHERE status = 'Available'"); }
    public int getTotalDrivers() { return getCount("SELECT COUNT(*) FROM drivers"); }
    public int getActiveTrips() { return getCount("SELECT COUNT(*) FROM trips WHERE status = 'In Progress'"); }
    public int getCompletedTrips() { return getCount("SELECT COUNT(*) FROM trips WHERE status = 'Completed'"); }
    public int getMaintenanceVehicles() { return getCount("SELECT COUNT(*) FROM vehicles WHERE status = 'Under Maintenance'"); }

    public ObservableList<RecentTrip> getRecentTrips() {
        ObservableList<RecentTrip> list = FXCollections.observableArrayList();

        // FIXED: Included (end_odometer - start_odometer) AS distance in query
        String sql = "SELECT trip_id, vehicle_id, driver_id, source, destination, trip_date, (end_odometer - start_odometer) AS distance, status FROM trips ORDER BY trip_date DESC LIMIT 10";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new RecentTrip(
                        rs.getString("trip_id"),
                        rs.getString("vehicle_id"),
                        rs.getString("driver_id"),
                        rs.getString("source"),
                        rs.getString("destination"),
                        rs.getString("trip_date"),
                        rs.getDouble("distance"), // FIXED: Read calculated distance from ResultSet
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public javafx.collections.ObservableList<javafx.scene.chart.PieChart.Data> getVehicleStatusDistribution() {
        javafx.collections.ObservableList<javafx.scene.chart.PieChart.Data> pieChartData =
                javafx.collections.FXCollections.observableArrayList();

        String sql = "SELECT status, COUNT(*) as count FROM vehicles GROUP BY status";

        try (Connection conn = com.fleetmanagement.db.DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String status = rs.getString("status");
                int count = rs.getInt("count");
                pieChartData.add(new javafx.scene.chart.PieChart.Data(status + " (" + count + ")", count));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (pieChartData.isEmpty()) {
            pieChartData.add(new javafx.scene.chart.PieChart.Data("Available", 0));
            pieChartData.add(new javafx.scene.chart.PieChart.Data("On Trip", 0));
            pieChartData.add(new javafx.scene.chart.PieChart.Data("Under Maintenance", 0));
        }

        return pieChartData;
    }

    public static class RecentTrip {
        private final String tripId, vehicleId, driverId, source, destination, tripDate, status;
        private final double distance;

        public RecentTrip(String tripId, String vehicleId, String driverId, String source, String destination, String tripDate, double distance, String status) {
            this.tripId = tripId; this.vehicleId = vehicleId; this.driverId = driverId;
            this.source = source; this.destination = destination; this.tripDate = tripDate;
            this.distance = distance; this.status = status;
        }

        public String getTripId() { return tripId; }
        public String getVehicleId() { return vehicleId; }
        public String getDriverId() { return driverId; }
        public String getSource() { return source; }
        public String getDestination() { return destination; }
        public String getTripDate() { return tripDate; }
        public double getDistance() { return distance; }
        public String getStatus() { return status; }
    }

    public double getMonthlyFuelCost() {
        String sql = "SELECT SUM(fuel_cost) AS total FROM fuel_records";
        try (Connection conn = com.fleetmanagement.db.DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble("total");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public double getMonthlyTollCost() {
        String sql = "SELECT SUM(amount) AS total FROM tolls";
        try (Connection conn = com.fleetmanagement.db.DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble("total");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }
}
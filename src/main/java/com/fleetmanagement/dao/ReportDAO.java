package com.fleetmanagement.dao;

import com.fleetmanagement.db.DatabaseConnection;
import com.fleetmanagement.model.ReportRow;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReportDAO {

    public List<ReportRow> generateReport(String reportType, LocalDate fromDate, LocalDate toDate, String vehicleId, String status) {
        List<ReportRow> rows = new ArrayList<>();
        Connection conn = DatabaseConnection.getInstance().getConnection();
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();

        switch (reportType) {
            case "Vehicle Report":
                sql.append("SELECT vehicle_id, registration_number, vehicle_type, model, capacity_kg, fuel_type, status FROM vehicles WHERE 1=1");
                if (status != null && !status.equals("All Status")) {
                    sql.append(" AND status = ?");
                    params.add(status);
                }
                break;

            case "Driver Report":
                sql.append("SELECT driver_id, driver_name, phone_number, license_number, license_expiry, status FROM drivers WHERE 1=1");
                if (status != null && !status.equals("All Status")) {
                    sql.append(" AND status = ?");
                    params.add(status);
                }
                break;

            case "Trip Report":
                // FIXED: Using valid columns from your trips table schema
                sql.append("SELECT trip_id, vehicle_id, driver_id, source, destination, trip_date, (end_odometer - start_odometer) AS distance_covered, status FROM trips WHERE 1=1");
                if (fromDate != null) { sql.append(" AND trip_date >= ?"); params.add(Date.valueOf(fromDate)); }
                if (toDate != null) { sql.append(" AND trip_date <= ?"); params.add(Date.valueOf(toDate)); }
                if (vehicleId != null && !vehicleId.equals("All Vehicles")) { sql.append(" AND vehicle_id = ?"); params.add(vehicleId); }
                if (status != null && !status.equals("All Status")) { sql.append(" AND status = ?"); params.add(status); }
                break;

            case "Fuel Report":
                sql.append("SELECT fuel_id, trip_id, vehicle_id, distance, fuel_used, fuel_price, fuel_cost, mileage FROM fuel_records WHERE 1=1");
                if (fromDate != null) { sql.append(" AND fuel_date >= ?"); params.add(Date.valueOf(fromDate)); }
                if (toDate != null) { sql.append(" AND fuel_date <= ?"); params.add(Date.valueOf(toDate)); }
                if (vehicleId != null && !vehicleId.equals("All Vehicles")) { sql.append(" AND vehicle_id = ?"); params.add(vehicleId); }
                break;

            case "Toll Report":
                sql.append("SELECT toll_id, trip_id, toll_name, location, amount, toll_date FROM tolls WHERE 1=1");
                if (fromDate != null) { sql.append(" AND toll_date >= ?"); params.add(Date.valueOf(fromDate)); }
                if (toDate != null) { sql.append(" AND toll_date <= ?"); params.add(Date.valueOf(toDate)); }
                break;

            case "Expense Report":
                sql.append("SELECT expense_id, trip_id, expense_type, description, amount, expense_date FROM expenses WHERE 1=1");
                if (fromDate != null) { sql.append(" AND expense_date >= ?"); params.add(Date.valueOf(fromDate)); }
                if (toDate != null) { sql.append(" AND expense_date <= ?"); params.add(Date.valueOf(toDate)); }
                break;

            case "Maintenance Report":
                sql.append("SELECT maintenance_id, vehicle_id, maintenance_date, maintenance_type, description, cost, next_service_date, status FROM maintenance WHERE 1=1");
                if (fromDate != null) { sql.append(" AND maintenance_date >= ?"); params.add(Date.valueOf(fromDate)); }
                if (toDate != null) { sql.append(" AND maintenance_date <= ?"); params.add(Date.valueOf(toDate)); }
                if (vehicleId != null && !vehicleId.equals("All Vehicles")) { sql.append(" AND vehicle_id = ?"); params.add(vehicleId); }
                if (status != null && !status.equals("All Status")) { sql.append(" AND status = ?"); params.add(status); }
                break;

            case "Trip Cost Report":
                sql.append("SELECT t.trip_id, t.vehicle_id, " +
                        "COALESCE(SUM(f.fuel_cost), 0) as total_fuel, " +
                        "COALESCE(SUM(to_.amount), 0) as total_toll, " +
                        "COALESCE(SUM(e.amount), 0) as total_exp " +
                        "FROM trips t " +
                        "LEFT JOIN fuel_records f ON t.trip_id = f.trip_id " +
                        "LEFT JOIN tolls to_ ON t.trip_id = to_.trip_id " +
                        "LEFT JOIN expenses e ON t.trip_id = e.trip_id WHERE 1=1");
                if (fromDate != null) { sql.append(" AND t.trip_date >= ?"); params.add(Date.valueOf(fromDate)); }
                if (toDate != null) { sql.append(" AND t.trip_date <= ?"); params.add(Date.valueOf(toDate)); }
                if (vehicleId != null && !vehicleId.equals("All Vehicles")) { sql.append(" AND t.vehicle_id = ?"); params.add(vehicleId); }
                sql.append(" GROUP BY t.trip_id, t.vehicle_id");
                break;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (rs.next()) {
                    String[] cols = new String[8];
                    for (int i = 0; i < 8; i++) {
                        cols[i] = (i < columnCount) ? rs.getString(i + 1) : "";
                    }

                    if (reportType.equals("Trip Cost Report")) {
                        double fuel = Double.parseDouble(cols[2]);
                        double toll = Double.parseDouble(cols[3]);
                        double exp = Double.parseDouble(cols[4]);
                        double grandTotal = fuel + toll + exp;
                        cols[5] = "₹" + grandTotal;
                        cols[2] = "₹" + fuel;
                        cols[3] = "₹" + toll;
                        cols[4] = "₹" + exp;
                        cols[6] = ""; cols[7] = "";
                    }

                    rows.add(new ReportRow(cols[0], cols[1], cols[2], cols[3], cols[4], cols[5], cols[6], cols[7]));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rows;
    }

    public List<String> getAllVehicleIds() {
        List<String> list = new ArrayList<>();
        list.add("All Vehicles");
        String sql = "SELECT vehicle_id FROM vehicles";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(rs.getString("vehicle_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
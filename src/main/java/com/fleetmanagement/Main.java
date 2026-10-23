package com.fleetmanagement;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import com.fleetmanagement.db.DatabaseConnection;

import com.fleetmanagement.dao.ReceiverDAO;
import com.fleetmanagement.model.ReceiverLoginRequest;
import java.util.Map;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Your existing JavaFX startup code
        Parent root = FXMLLoader.load(getClass().getResource("/main_portal.fxml"));
        primaryStage.setTitle("Fleet Management System V3");
        primaryStage.setScene(new Scene(root, 1050, 680));
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void main(String[] args) {

        // 1. START JAVALIN WEB SERVER ON PORT 8081
        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public", io.javalin.http.staticfiles.Location.CLASSPATH);
        }).start(8081);

        // 2. ADD A QUICK TEST API ROUTE
        app.get("/api/ping", ctx -> ctx.result("Fleet Management API is Live and connected!"));

        // 3. API TO HANDLE DRIVER LOGIN AND FETCH ASSIGNED TRIPS
        app.get("/api/driverLogin", ctx -> {
            String user = ctx.queryParam("user");
            String pass = ctx.queryParam("pass");

            if (user == null || pass == null) {
                ctx.result("{\"success\": false, \"message\": \"Missing credentials\"}");
                return;
            }

            user = user.trim().toUpperCase();
            String expectedPass = user + "@1228"; // e.g., D001@1228

            // Validate the dynamic password rule
            if (!pass.equals(expectedPass)) {
                ctx.result("{\"success\": false, \"message\": \"Invalid Password.\"}");
                return;
            }

            String driverQuery = "SELECT driver_name FROM drivers WHERE driver_id = ?";
            String tripQuery = "SELECT trip_id, source, destination, status FROM trips WHERE driver_id = ? AND status IN ('Scheduled', 'In Progress')";

            try (java.sql.Connection conn = com.fleetmanagement.db.DatabaseConnection.getInstance().getConnection()) {

                // Verify Driver exists in the database
                try (java.sql.PreparedStatement pstmt = conn.prepareStatement(driverQuery)) {
                    pstmt.setString(1, user);
                    java.sql.ResultSet rs = pstmt.executeQuery();

                    if (rs.next()) {
                        String driverName = rs.getString("driver_name");

                        // Find if they have an active trip assigned
                        try (java.sql.PreparedStatement tripStmt = conn.prepareStatement(tripQuery)) {
                            tripStmt.setString(1, user);
                            java.sql.ResultSet tripRs = tripStmt.executeQuery();

                            if (tripRs.next()) {
                                String tripId = tripRs.getString("trip_id");
                                String source = tripRs.getString("source");
                                String dest = tripRs.getString("destination");
                                String status = tripRs.getString("status"); // NEW: Get current status

                                ctx.result("{\"success\": true, \"driverName\": \"" + driverName + "\", \"tripAssigned\": true, \"tripId\": \"" + tripId + "\", \"source\": \"" + source + "\", \"dest\": \"" + dest + "\", \"status\": \"" + status + "\"}");
                            } else {
                                // Driver logged in, but no trips assigned today
                                ctx.result("{\"success\": true, \"driverName\": \"" + driverName + "\", \"tripAssigned\": false}");
                            }
                        }
                    } else {
                        ctx.result("{\"success\": false, \"message\": \"Driver ID not found in system.\"}");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result("{\"success\": false, \"message\": \"Database connection error.\"}");
            }
        });

        // 4. API TO START THE TRIP EXPLICITLY FROM THE PHONE
        app.get("/api/startTrip", ctx -> {
            String tripId = ctx.queryParam("tripId");
            if (tripId == null) {
                ctx.result("{\"success\": false}");
                return;
            }
            try (java.sql.Connection conn = com.fleetmanagement.db.DatabaseConnection.getInstance().getConnection();
                 java.sql.PreparedStatement pstmt = conn.prepareStatement("UPDATE trips SET status = 'In Progress' WHERE trip_id = ?")) {

                pstmt.setString(1, tripId);
                pstmt.executeUpdate();
                ctx.result("{\"success\": true}");

            } catch (Exception e) {
                e.printStackTrace();
                ctx.result("{\"success\": false}");
            }
        });

        System.out.println("✅ Background Web Server running at http://localhost:8081");


        // 5. API TO FETCH CHECKPOINTS FOR A SPECIFIC TRIP
        app.get("/api/checkpoints", ctx -> {
            String tripId = ctx.queryParam("tripId");
            if (tripId == null) {
                ctx.result("[]");
                return;
            }

            String query = "SELECT sequence_num, location_name, expected_time, actual_time, status FROM trip_checkpoints WHERE trip_id = ? ORDER BY sequence_num ASC";
            StringBuilder json = new StringBuilder("[");

            try (java.sql.Connection conn = com.fleetmanagement.db.DatabaseConnection.getInstance().getConnection();
                 java.sql.PreparedStatement pstmt = conn.prepareStatement(query)) {

                pstmt.setString(1, tripId);
                java.sql.ResultSet rs = pstmt.executeQuery();

                boolean first = true;
                while (rs.next()) {
                    if (!first) json.append(",");

                    String loc = rs.getString("location_name").replace("\"", "\\\""); // Escape quotes
                    String stat = rs.getString("status");
                    String actual = rs.getString("actual_time");
                    if (actual == null) actual = "Pending";

                    json.append("{")
                            .append("\"seq\": ").append(rs.getInt("sequence_num")).append(",")
                            .append("\"location\": \"").append(loc).append("\",")
                            .append("\"actual_time\": \"").append(actual).append("\",")
                            .append("\"status\": \"").append(stat).append("\"")
                            .append("}");
                    first = false;
                }
                json.append("]");
                ctx.contentType("application/json");
                ctx.result(json.toString());

            } catch (Exception e) {
                e.printStackTrace();
                ctx.result("[]");
            }
        });

        // 6. API TO MARK CHECKPOINT AS REACHED (Saves exact timestamp)
        app.get("/api/markCheckpoint", ctx -> {
            String tripId = ctx.queryParam("tripId");
            String seqStr = ctx.queryParam("seq");

            if (tripId == null || seqStr == null) {
                ctx.result("{\"success\": false}");
                return;
            }

            // This updates the status and automatically captures MySQL's CURRENT_TIMESTAMP
            String query = "UPDATE trip_checkpoints SET status = 'Reached', actual_time = CURRENT_TIMESTAMP WHERE trip_id = ? AND sequence_num = ?";

            try (java.sql.Connection conn = com.fleetmanagement.db.DatabaseConnection.getInstance().getConnection();
                 java.sql.PreparedStatement pstmt = conn.prepareStatement(query)) {

                pstmt.setString(1, tripId);
                pstmt.setInt(2, Integer.parseInt(seqStr));
                pstmt.executeUpdate();

                ctx.result("{\"success\": true}");

            } catch (Exception e) {
                e.printStackTrace();
                ctx.result("{\"success\": false}");
            }
        });

        // 7. API TO FETCH FULL TRIP, VEHICLE, AND CARGO DETAILS
        app.get("/api/tripDetails", ctx -> {
            String tripId = ctx.queryParam("tripId");

            // Exact SQL join matching your database schema
            String query = "SELECT t.vehicle_id, " +
                    "v.registration_number, v.model, v.manufacturer, v.capacity_kg, v.fuel_type, " +
                    "c.goods_type, c.quantity, c.weight_kg, c.description " +
                    "FROM trips t " +
                    "LEFT JOIN vehicles v ON t.vehicle_id = v.vehicle_id " +
                    "LEFT JOIN cargo c ON t.trip_id = c.trip_id " +
                    "WHERE t.trip_id = ?";

            try (java.sql.Connection conn = com.fleetmanagement.db.DatabaseConnection.getInstance().getConnection();
                 java.sql.PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, tripId);
                java.sql.ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    String vehicleId = rs.getString("vehicle_id");
                    String regNo = rs.getString("registration_number");
                    String model = rs.getString("model");
                    String manufacturer = rs.getString("manufacturer");
                    double totalCapKg = rs.getDouble("capacity_kg");
                    String fuelType = rs.getString("fuel_type");

                    String goodsType = rs.getString("goods_type");
                    double weightKg = rs.getDouble("weight_kg");
                    int quantity = rs.getInt("quantity");

                    // Fallbacks if data is missing
                    if (vehicleId == null) vehicleId = "Not Assigned";
                    if (regNo == null) regNo = "N/A";
                    if (model == null) model = "Standard Model";
                    if (manufacturer == null) manufacturer = "";
                    if (fuelType == null) fuelType = "Diesel";
                    if (goodsType == null) goodsType = "General Cargo";

                    String vehicleName = manufacturer + " " + model;

                    // Return formatted JSON
                    ctx.contentType("application/json");
                    ctx.result(String.format(
                            "{\"success\": true, \"vehicleName\": \"%s\", \"regNo\": \"%s\", \"fuelType\": \"%s\", \"totalCap\": \"%.0f kg\", \"goodsType\": \"%s\", \"goodsCap\": \"%.0f kg (Qty: %d)\"}",
                            vehicleName.trim(), regNo, fuelType, totalCapKg, goodsType, weightKg, quantity
                    ));
                } else {
                    ctx.result("{\"success\": false}");
                }
            } catch (Exception e) {
                e.printStackTrace();
                ctx.result("{\"success\": false}");
            }
        });

        // 8. API TO SUBMIT AN EXPENSE (Matched to your exact database schema)
        app.get("/api/addExpense", ctx -> {
            String tripId = ctx.queryParam("tripId");
            String type = ctx.queryParam("type");
            String amount = ctx.queryParam("amount");
            String desc = ctx.queryParam("desc");

            try (java.sql.Connection conn = com.fleetmanagement.db.DatabaseConnection.getInstance().getConnection()) {

                // 1. Generate the next Expense ID (e.g., find max ID or create one like E010)
                String countQuery = "SELECT COUNT(*) FROM expenses";
                int nextIdNum = 1;
                try (java.sql.Statement stmt = conn.createStatement();
                     java.sql.ResultSet rs = stmt.executeQuery(countQuery)) {
                    if (rs.next()) {
                        nextIdNum = rs.getInt(1) + 1;
                    }
                }
                String expenseId = String.format("E%03d", nextIdNum); // Generates E001, E002, etc.

                // 2. Insert using your exact table columns: expense_id, trip_id, expense_type, description, amount, expense_date
                String insertQuery = "INSERT INTO expenses (expense_id, trip_id, expense_type, description, amount, expense_date) VALUES (?, ?, ?, ?, ?, CURRENT_DATE)";

                try (java.sql.PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
                    pstmt.setString(1, expenseId);
                    pstmt.setString(2, tripId);
                    pstmt.setString(3, type);
                    pstmt.setString(4, desc);
                    pstmt.setDouble(5, Double.parseDouble(amount));
                    pstmt.executeUpdate();

                    ctx.result("{\"success\": true}");
                }
            } catch (Exception e) {
                e.printStackTrace();
                ctx.result("{\"success\": false}");
            }
        });

        // 9. API TO FETCH DRIVER PROFILE DETAILS
        app.get("/api/driverProfile", ctx -> {
            String driverId = ctx.queryParam("driverId");

            // Updated to match your exact database column: license_number
            String query = "SELECT driver_id, driver_name, license_number, license_expiry, phone FROM drivers WHERE driver_id = ?";

            try (java.sql.Connection conn = com.fleetmanagement.db.DatabaseConnection.getInstance().getConnection();
                 java.sql.PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, driverId);
                java.sql.ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    ctx.contentType("application/json");
                    ctx.result(String.format(
                            "{\"success\": true, \"driverId\": \"%s\", \"driverName\": \"%s\", \"licenseNo\": \"%s\", \"expiryDate\": \"%s\", \"phone\": \"%s\"}",
                            rs.getString("driver_id"),
                            rs.getString("driver_name"),
                            rs.getString("license_number") != null ? rs.getString("license_number") : "N/A",
                            rs.getString("license_expiry") != null ? rs.getString("license_expiry") : "N/A",
                            rs.getString("phone") != null ? rs.getString("phone") : "N/A"
                    ));
                } else {
                    ctx.result("{\"success\": false}");
                }
            } catch (Exception e) {
                e.printStackTrace();
                ctx.result("{\"success\": false}");
            }
        });

        // 10. API TO REPORT AN INCIDENT (POST METHOD WITH PHOTO)
        app.post("/api/reportIncident", ctx -> {
            String tripId = ctx.formParam("tripId");
            String driverId = ctx.formParam("driverId");
            String type = ctx.formParam("type");
            String desc = ctx.formParam("desc");
            String photoData = ctx.formParam("photoData"); // Catches the Base64 image string

            String query = "INSERT INTO incidents (trip_id, driver_id, incident_type, description, reported_at, status, photo_data) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, 'Open', ?)";

            try (java.sql.Connection conn = com.fleetmanagement.db.DatabaseConnection.getInstance().getConnection();
                 java.sql.PreparedStatement pstmt = conn.prepareStatement(query)) {

                pstmt.setString(1, tripId);
                pstmt.setString(2, driverId);
                pstmt.setString(3, type);
                pstmt.setString(4, desc);
                pstmt.setString(5, photoData != null ? photoData : "");
                pstmt.executeUpdate();

                ctx.result("{\"success\": true}");
            } catch (Exception e) {
                e.printStackTrace();
                ctx.result("{\"success\": false}");
            }
        });

        // 11. API TO CONFIRM DELIVERY (WITH TRANSACTION)
        app.post("/api/confirmDelivery", ctx -> {
            String tripId = ctx.formParam("tripId");
            String receiver = ctx.formParam("receiver");
            String quantity = ctx.formParam("quantity");
            String status = ctx.formParam("status");
            String remarks = ctx.formParam("remarks");
            String photoData = ctx.formParam("photoData");

            String insertQuery = "INSERT INTO delivery_reports (trip_id, receiver_name, delivered_quantity, delivery_status, feedback_remarks, pod_photo_path, reported_at) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
            String updateTripQuery = "UPDATE trips SET status = 'Completed' WHERE trip_id = ?";

            try (java.sql.Connection conn = com.fleetmanagement.db.DatabaseConnection.getInstance().getConnection()) {
                conn.setAutoCommit(false); // Start transaction

                try (java.sql.PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
                     java.sql.PreparedStatement updateStmt = conn.prepareStatement(updateTripQuery)) {

                    insertStmt.setString(1, tripId);
                    insertStmt.setString(2, receiver);
                    insertStmt.setDouble(3, quantity != null && !quantity.isEmpty() ? Double.parseDouble(quantity) : 0.0);
                    insertStmt.setString(4, status);
                    insertStmt.setString(5, remarks);
                    insertStmt.setString(6, photoData != null ? photoData : "");
                    insertStmt.executeUpdate();

                    updateStmt.setString(1, tripId);
                    updateStmt.executeUpdate();

                    conn.commit(); // Save both changes
                    ctx.result("{\"success\": true}");
                } catch (Exception e) {
                    conn.rollback(); // Cancel if anything fails
                    e.printStackTrace();
                    ctx.result("{\"success\": false}");
                }
            } catch (Exception e) {
                e.printStackTrace();
                ctx.result("{\"success\": false}");
            }
        });
        ReceiverDAO receiverDAO = new ReceiverDAO();

// Receiver Login API Endpoint
        app.post("/api/receiver/login", ctx -> {
            // Parse JSON request body sent from the web frontend
            ReceiverLoginRequest req = ctx.bodyAsClass(ReceiverLoginRequest.class);

            boolean isValid = receiverDAO.validateReceiver(req.getReceiverId(), req.getPassword());

            if (isValid) {
                ctx.json(Map.of("success", true, "message", "Login successful"));
            } else {
                ctx.status(401).json(Map.of("success", false, "message", "Invalid ID or Password"));
            }
        });

        app.get("/api/receiver/shipments", ctx -> {
            String receiverId = ctx.queryParam("id");

            // Quick SQL to get trips assigned to this receiver
            String query = "SELECT trip_id, source, destination, status, expected_date FROM trips WHERE receiver_id = ?";
            List<Map<String, Object>> shipments = new ArrayList<>();
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {

                pstmt.setString(1, receiverId);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    Map<String, Object> trip = new java.util.HashMap<>();
                    trip.put("tripId", rs.getString("trip_id"));
                    trip.put("source", rs.getString("source"));
                    trip.put("destination", rs.getString("destination"));
                    trip.put("status", rs.getString("status"));
                    trip.put("expectedDate", rs.getString("expected_date"));
                    shipments.add(trip);
                }
                ctx.json(Map.of("success", true, "data", shipments));
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).json(Map.of("success", false, "message", "Database error"));
            }
        });

        // Fetch Specific Trip Tracking Details API
        app.get("/api/receiver/track", ctx -> {
            String tripId = ctx.queryParam("tripId");

            Map<String, Object> responseData = new HashMap<>();
            List<Map<String, Object>> checkpoints = new ArrayList<>();

            // Queries to fetch trip data and its checkpoints
            String tripQuery = "SELECT source, destination, status FROM trips WHERE trip_id = ?";
            String checkpointQuery = "SELECT location_name, status, updated_at FROM checkpoints WHERE trip_id = ? ORDER BY id ASC";

            try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
                // 1. Fetch Trip Data
                try (PreparedStatement pt = conn.prepareStatement(tripQuery)) {
                    pt.setString(1, tripId);
                    ResultSet rs = pt.executeQuery();
                    if (rs.next()) {
                        responseData.put("tripId", tripId);
                        responseData.put("source", rs.getString("source"));
                        responseData.put("destination", rs.getString("destination"));
                        responseData.put("status", rs.getString("status"));
                    }
                }

                // 2. Fetch Checkpoints
                try (PreparedStatement pc = conn.prepareStatement(checkpointQuery)) {
                    pc.setString(1, tripId);
                    ResultSet rs = pc.executeQuery();
                    while (rs.next()) {
                        Map<String, Object> cp = new HashMap<>();
                        cp.put("location", rs.getString("location_name"));
                        cp.put("status", rs.getString("status"));
                        cp.put("time", rs.getString("updated_at"));
                        checkpoints.add(cp);
                    }
                }
                responseData.put("checkpoints", checkpoints);

                // 3. Mocked Driver & Vehicle Data (Update with JOINs later if needed)
                responseData.put("driverName", "Assigned Driver");
                responseData.put("vehicleInfo", "Heavy Truck (Pending Assignment)");

                ctx.json(Map.of("success", true, "data", responseData));
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).json(Map.of("success", false, "message", "Database error"));
            }
        });

        // 5. LAUNCH JAVAFX UI
        launch(args);
    }
}

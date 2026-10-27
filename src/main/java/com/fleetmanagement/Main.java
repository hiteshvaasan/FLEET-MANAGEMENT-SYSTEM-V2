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

    private static String or(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    public static void main(String[] args) {

        // 1. START JAVALIN WEB SERVER ON PORT 8081
        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public", io.javalin.http.staticfiles.Location.CLASSPATH);
            config.http.maxRequestSize = 20_000_000L; // 20 MB, so base64 photos fit
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

        // 7. API TO FETCH FULL TRIP, VEHICLE, CARGO AND RECEIVER DETAILS
        app.get("/api/tripDetails", ctx -> {
            String tripId = ctx.queryParam("tripId");

            String query = "SELECT t.vehicle_id, t.receiver_id, " +
                    "v.registration_number, v.model, v.manufacturer, v.capacity_kg, v.fuel_type, " +
                    "c.goods_type, c.quantity, c.weight_kg, c.description, " +
                    "r.name AS receiver_name, r.company_name, r.phone AS receiver_phone, r.address AS receiver_address " +
                    "FROM trips t " +
                    "LEFT JOIN vehicles v ON t.vehicle_id = v.vehicle_id " +
                    "LEFT JOIN cargo c ON t.trip_id = c.trip_id " +
                    "LEFT JOIN receivers r ON t.receiver_id = r.receiver_id " +
                    "WHERE t.trip_id = ?";

            try (java.sql.Connection conn = com.fleetmanagement.db.DatabaseConnection.getInstance().getConnection();
                 java.sql.PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, tripId);
                java.sql.ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    String manufacturer = or(rs.getString("manufacturer"), "");
                    String model = or(rs.getString("model"), "Standard Model");

                    Map<String, Object> res = new HashMap<>();
                    res.put("success", true);
                    res.put("vehicleName", (manufacturer + " " + model).trim());
                    res.put("regNo", or(rs.getString("registration_number"), "N/A"));
                    res.put("fuelType", or(rs.getString("fuel_type"), "Diesel"));
                    res.put("totalCap", String.format("%.0f kg", rs.getDouble("capacity_kg")));
                    res.put("goodsType", or(rs.getString("goods_type"), "General Cargo"));
                    res.put("goodsCap", String.format("%.0f kg (Qty: %d)", rs.getDouble("weight_kg"), rs.getInt("quantity")));

                    // receiver information
                    res.put("receiverId", or(rs.getString("receiver_id"), "Not Assigned"));
                    res.put("receiverName", or(rs.getString("receiver_name"), "Not Assigned"));
                    res.put("receiverCompany", or(rs.getString("company_name"), "N/A"));
                    res.put("receiverPhone", or(rs.getString("receiver_phone"), "N/A"));
                    res.put("receiverAddress", or(rs.getString("receiver_address"), "N/A"));

                    ctx.json(res);
                } else {
                    ctx.json(Map.of("success", false));
                }
            } catch (Exception e) {
                e.printStackTrace();
                ctx.json(Map.of("success", false));
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

            // The subquery now strictly checks if the receiver explicitly added their status
            String query = "SELECT trip_id, vehicle_id, source, destination, status, expected_delivery_date, " +
                    "(SELECT COUNT(*) FROM delivery_reports WHERE trip_id = trips.trip_id AND receiver_status IS NOT NULL) > 0 AS feedback_submitted " +
                    "FROM trips WHERE receiver_id = ?";

            List<Map<String, Object>> shipments = new ArrayList<>();

            try (java.sql.Connection conn = com.fleetmanagement.db.DatabaseConnection.getInstance().getConnection();
                 java.sql.PreparedStatement pstmt = conn.prepareStatement(query)) {

                pstmt.setString(1, receiverId);
                java.sql.ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    Map<String, Object> trip = new java.util.HashMap<>();
                    trip.put("tripId", rs.getString("trip_id"));
                    trip.put("vehicleId", rs.getString("vehicle_id"));
                    trip.put("source", rs.getString("source"));
                    trip.put("destination", rs.getString("destination"));
                    trip.put("status", rs.getString("status"));
                    trip.put("expectedDate", rs.getString("expected_delivery_date"));

                    // This will now be FALSE if the receiver hasn't submitted their form yet
                    trip.put("feedbackSubmitted", rs.getBoolean("feedback_submitted"));

                    shipments.add(trip);
                }
                ctx.json(Map.of("success", true, "data", shipments));
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).json(Map.of("success", false, "message", "Database error"));
            }
        });


        // Fetch Specific Trip Tracking Details API
        // Fetch Specific Trip Tracking Details API
        // Fetch Specific Trip Tracking Details API
        app.get("/api/receiver/track", ctx -> {
            String tripId = ctx.queryParam("tripId");

            Map<String, Object> responseData = new HashMap<>();
            List<Map<String, Object>> checkpoints = new ArrayList<>();

            // FIXED: Added JOIN with drivers and vehicles tables to get the real name and vehicle details
            String tripQuery = "SELECT t.source, t.destination, t.status, t.vehicle_id, d.driver_name " +
                    "FROM trips t LEFT JOIN drivers d ON t.driver_id = d.driver_id " +
                    "WHERE t.trip_id = ?";

            String checkpointQuery = "SELECT location_name, status, actual_time FROM trip_checkpoints WHERE trip_id = ? ORDER BY sequence_num ASC";

            try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
                // 1. Fetch Trip, Driver, & Vehicle Data
                try (PreparedStatement pt = conn.prepareStatement(tripQuery)) {
                    pt.setString(1, tripId);
                    ResultSet rs = pt.executeQuery();
                    if (rs.next()) {
                        responseData.put("tripId", tripId);
                        responseData.put("source", rs.getString("source"));
                        responseData.put("destination", rs.getString("destination"));
                        responseData.put("status", rs.getString("status"));

                        // Pull real driver name (fallback if unassigned)
                        String driverName = rs.getString("driver_name");
                        responseData.put("driverName", driverName != null ? driverName : "Unassigned");

                        // Pull real vehicle ID
                        String vehicleId = rs.getString("vehicle_id");
                        responseData.put("vehicleInfo", vehicleId != null ? vehicleId : "Pending Assignment");
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
                        String actualTime = rs.getString("actual_time");
                        cp.put("time", actualTime != null ? actualTime : "Pending");
                        checkpoints.add(cp);
                    }
                }
                responseData.put("checkpoints", checkpoints);

                ctx.json(Map.of("success", true, "data", responseData));
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).json(Map.of("success", false, "message", "Database error"));
            }
        });


        app.get("/api/receiver/details", ctx -> {
            String receiverId = ctx.queryParam("id");

            // Change table/column names if they differ slightly in your database
            String query = "SELECT receiver_id, name, company_name, email, phone FROM receivers WHERE receiver_id = ?";

            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {

                pstmt.setString(1, receiverId);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    Map<String, Object> details = new java.util.HashMap<>();
                    details.put("receiverId", rs.getString("receiver_id"));
                    details.put("name", rs.getString("name"));
                    details.put("companyName", rs.getString("company_name"));
                    details.put("email", rs.getString("email"));
                    details.put("phone", rs.getString("phone"));

                    ctx.json(Map.of("success", true, "data", details));
                } else {
                    ctx.json(Map.of("success", false, "message", "Receiver not found"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).json(Map.of("success", false, "message", "Database error"));
            }
        });

        // 1. Endpoint to fetch Driver and Vehicle details for a trip
        app.get("/api/trip/details", ctx -> {
            String tripId = ctx.queryParam("tripId");
            String query = "SELECT t.vehicle_id, t.status, d.driver_name " +
                    "FROM trips t LEFT JOIN drivers d ON t.driver_id = d.driver_id " +
                    "WHERE t.trip_id = ?";

            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, tripId);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    Map<String, Object> data = new java.util.HashMap<>();
                    data.put("driverName", rs.getString("driver_name") != null ? rs.getString("driver_name") : "Unassigned");
                    data.put("vehicleId", rs.getString("vehicle_id"));
                    data.put("status", rs.getString("status"));
                    ctx.json(Map.of("success", true, "data", data));
                } else {
                    ctx.json(Map.of("success", false, "message", "Trip not found"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).json(Map.of("success", false, "message", "Database error"));
            }
        });

        app.get("/api/trip/checkpoints", ctx -> {
            String tripId = ctx.queryParam("tripId");
            String query = "SELECT sequence_num, location_name, expected_time, actual_time, status " +
                    "FROM trip_checkpoints WHERE trip_id = ? ORDER BY sequence_num ASC";

            List<Map<String, Object>> checkpoints = new ArrayList<>();
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, tripId);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    Map<String, Object> cp = new java.util.HashMap<>();
                    cp.put("sequence", rs.getInt("sequence_num"));
                    cp.put("location", rs.getString("location_name"));
                    cp.put("expectedTime", rs.getString("expected_time"));
                    cp.put("actualTime", rs.getString("actual_time"));
                    cp.put("status", rs.getString("status"));
                    checkpoints.add(cp);
                }
                ctx.json(Map.of("success", true, "data", checkpoints));
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).json(Map.of("success", false, "message", "Database error"));
            }
        });
        // Fetch Specific Trip Tracking Details API
        app.get("/api/receiver/tracking", ctx -> {
            String tripId = ctx.queryParam("tripId");

            Map<String, Object> responseData = new HashMap<>();
            List<Map<String, Object>> checkpoints = new ArrayList<>();

            // Queries to fetch trip data and its checkpoints
            String tripQuery = "SELECT source, destination, status FROM trips WHERE trip_id = ?";
            // FIXED: Changed 'checkpoints' to your actual table name 'trip_checkpoints'
            String checkpointQuery = "SELECT location_name, status, actual_time FROM trip_checkpoints WHERE trip_id = ? ORDER BY sequence_num ASC";
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
                        cp.put("time", rs.getString("actual_time") != null ? rs.getString("actual_time") : "Pending");
                        checkpoints.add(cp);
                    }
                }
                responseData.put("checkpoints", checkpoints);

                // 3. Mocked Driver & Vehicle Data
                // 3. Real Driver & Vehicle Data
                String driverQuery = "SELECT t.vehicle_id, d.name AS driver_name " +
                        "FROM trips t LEFT JOIN drivers d ON t.driver_id = d.driver_id " +
                        "WHERE t.trip_id = ?";
                String driverName = "Unassigned";
                String vehicleInfo = "Not assigned";
                try (PreparedStatement pd = conn.prepareStatement(driverQuery)) {
                    pd.setString(1, tripId);
                    ResultSet rs = pd.executeQuery();
                    if (rs.next()) {
                        if (rs.getString("driver_name") != null) driverName = rs.getString("driver_name");
                        if (rs.getString("vehicle_id") != null) vehicleInfo = rs.getString("vehicle_id");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();   // shows the real SQL error in your IntelliJ console
                }
                responseData.put("driverName", driverName);
                responseData.put("vehicleInfo", vehicleInfo);
                ctx.json(Map.of("success", true, "data", responseData));
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).json(Map.of("success", false, "message", "Database error"));
            }
        });

        // API to handle receiver feedback submission
        app.post("/api/receiver/confirmDelivery", ctx -> {
            try {
                Map<String, Object> body = ctx.bodyAsClass(Map.class);

                String tripId     = body.get("tripId")    != null ? body.get("tripId").toString()    : null;
                String status     = body.get("status")    != null ? body.get("status").toString()    : "Delivered";
                String remarks    = body.get("remarks")   != null ? body.get("remarks").toString()   : "";
                String photoData  = body.get("photoData") != null ? body.get("photoData").toString() : "";
                String person     = body.get("receiver")  != null ? body.get("receiver").toString()  : "";
                int rating   = body.get("rating")   != null ? Integer.parseInt(body.get("rating").toString())   : 0;
                int quantity = (body.get("quantity") != null && !body.get("quantity").toString().isEmpty())
                        ? Integer.parseInt(body.get("quantity").toString()) : 0;

                if (tripId == null || tripId.isEmpty()) {
                    ctx.status(400).json(Map.of("success", false, "message", "Missing tripId"));
                    return;
                }

                try (Connection conn = DatabaseConnection.getInstance().getConnection()) {

                    // Block double submission
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT 1 FROM delivery_reports WHERE trip_id = ? AND receiver_status IS NOT NULL LIMIT 1")) {
                        ps.setString(1, tripId);
                        if (ps.executeQuery().next()) {
                            ctx.status(409).json(Map.of("success", false, "message", "Feedback already submitted for this trip"));
                            return;
                        }
                    }

                    // Try to update the driver's row first
                    String updateSql = "UPDATE delivery_reports SET receiver_status = ?, receiver_notes = ?, " +
                            "receiver_photo_path = ?, receiver_rating = ?, receiver_person = ?, receiver_quantity = ? " +
                            "WHERE trip_id = ?";
                    int updated;
                    try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                        ps.setString(1, status);
                        ps.setString(2, remarks);
                        ps.setString(3, photoData);
                        ps.setInt(4, rating);
                        ps.setString(5, person);
                        ps.setInt(6, quantity);
                        ps.setString(7, tripId);
                        updated = ps.executeUpdate();
                    }

                    // No driver row yet -> insert one
                    if (updated == 0) {
                        String insertSql = "INSERT INTO delivery_reports (trip_id, receiver_status, receiver_notes, " +
                                "receiver_photo_path, receiver_rating, receiver_person, receiver_quantity) VALUES (?,?,?,?,?,?,?)";
                        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                            ps.setString(1, tripId);
                            ps.setString(2, status);
                            ps.setString(3, remarks);
                            ps.setString(4, photoData);
                            ps.setInt(5, rating);
                            ps.setString(6, person);
                            ps.setInt(7, quantity);
                            ps.executeUpdate();
                        }
                    }
                    ctx.json(Map.of("success", true));
                }
            } catch (Exception e) {
                e.printStackTrace(); // the real cause shows in the IntelliJ console
                ctx.status(500).json(Map.of("success", false,
                        "message", e.getMessage() != null ? e.getMessage() : "Server error"));
            }
        });

// Used by the feedback page to lock itself if feedback was already given
        app.get("/api/receiver/feedbackStatus", ctx -> {
            String tripId = ctx.queryParam("tripId");
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT COUNT(*) FROM delivery_reports WHERE trip_id = ? AND receiver_status IS NOT NULL")) {
                ps.setString(1, tripId);
                ResultSet rs = ps.executeQuery();
                boolean submitted = rs.next() && rs.getInt(1) > 0;
                ctx.json(Map.of("success", true, "submitted", submitted));
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).json(Map.of("success", false, "submitted", false));
            }
        });

        // API to fetch incidents for a specific trip
        app.get("/api/trip/incidents", ctx -> {
            String tripId = ctx.queryParam("tripId");
            // Make sure 'photo_url' matches your database column name for incident photos
            String query = "SELECT incident_type, description, status, reported_at, photo_url FROM incidents WHERE trip_id = ?";

            List<Map<String, Object>> incidents = new ArrayList<>();
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, tripId);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    Map<String, Object> inc = new HashMap<>();
                    inc.put("type", rs.getString("incident_type"));
                    inc.put("description", rs.getString("description"));
                    inc.put("status", rs.getString("status"));
                    inc.put("reportedAt", rs.getString("reported_at"));
                    inc.put("photoUrl", rs.getString("photo_url")); // Pass photo path to frontend
                    incidents.add(inc);
                }
                ctx.json(Map.of("success", true, "data", incidents));
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).json(Map.of("success", false, "message", "Database error"));
            }
        });

        // API to handle new order requests from receivers
        app.post("/api/receiver/requestOrder", ctx -> {
            try {
                Map<String, Object> body = ctx.bodyAsClass(Map.class);
                String receiverId = (String) body.get("receiverId");
                String pickupAddress = (String) body.get("pickupAddress");
                String deliveryAddress = (String) body.get("deliveryAddress");
                String pickupDate = (String) body.get("pickupDate");
                String cargoType = (String) body.get("cargoType");
                String weightObj = body.get("cargoWeight").toString();
                String remarks = (String) body.get("remarks");

                // Inserts request as 'Pending Approval' for the admin module to review later
                String query = "INSERT INTO order_requests (receiver_id, pickup_location, delivery_location, pickup_date, cargo_type, weight, remarks, status, requested_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, 'Pending Approval', CURRENT_TIMESTAMP)";

                try (Connection conn = DatabaseConnection.getInstance().getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(query)) {

                    pstmt.setString(1, receiverId);
                    pstmt.setString(2, pickupAddress);
                    pstmt.setString(3, deliveryAddress);
                    pstmt.setString(4, pickupDate);
                    pstmt.setString(5, cargoType);
                    pstmt.setDouble(6, Double.parseDouble(weightObj));
                    pstmt.setString(7, remarks != null ? remarks : "");

                    pstmt.executeUpdate();
                    ctx.json(Map.of("success", true, "message", "Request submitted successfully"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).json(Map.of("success", false, "message", "Database error"));
            }
        });

        // API to fetch receiver profile settings
        app.get("/api/receiver/settings", ctx -> {
            String receiverId = ctx.queryParam("id");
            String query = "SELECT receiver_id, name, company_name, email, phone, address FROM receivers WHERE receiver_id = ?";

            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, receiverId);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("receiverId", rs.getString("receiver_id"));
                    data.put("name", rs.getString("name"));
                    data.put("companyName", rs.getString("company_name"));
                    data.put("email", rs.getString("email"));
                    data.put("phone", rs.getString("phone"));
                    data.put("address", rs.getString("address"));
                    ctx.json(Map.of("success", true, "data", data));
                } else {
                    ctx.status(404).json(Map.of("success", false, "message", "Receiver not found"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).json(Map.of("success", false, "message", "Database error"));
            }
        });

// API to update receiver profile settings
        app.post("/api/receiver/updateSettings", ctx -> {
            try {
                Map<String, Object> body = ctx.bodyAsClass(Map.class);
                String receiverId = (String) body.get("receiverId");
                String name = (String) body.get("name");
                String companyName = (String) body.get("companyName");
                String email = (String) body.get("email");
                String phone = (String) body.get("phone");
                String address = (String) body.get("address");

                String query = "UPDATE receivers SET name = ?, company_name = ?, email = ?, phone = ?, address = ? WHERE receiver_id = ?";

                try (Connection conn = DatabaseConnection.getInstance().getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setString(1, name);
                    pstmt.setString(2, companyName);
                    pstmt.setString(3, email);
                    pstmt.setString(4, phone);
                    pstmt.setString(5, address);
                    pstmt.setString(6, receiverId);

                    pstmt.executeUpdate();
                    ctx.json(Map.of("success", true, "message", "Settings updated successfully"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).json(Map.of("success", false, "message", "Database error"));
            }
        });
        // Add this route with your other app.get routes to serve files directly
        app.get("/uploads/{filename}", ctx -> {
            String filename = ctx.pathParam("filename");
            java.io.File file = new java.io.File("uploads/" + filename);
            if (file.exists()) {
                ctx.result(new java.io.FileInputStream(file));
            } else {
                ctx.status(404);
            }
        });
        // 5. LAUNCH JAVAFX UI
        launch(args);
    }
}

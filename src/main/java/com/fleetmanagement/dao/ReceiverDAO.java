package com.fleetmanagement.dao;

import com.fleetmanagement.db.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ReceiverDAO {

    public boolean validateReceiver(String receiverId, String password) {
        String query = "SELECT * FROM receivers WHERE receiver_id = ? AND password = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, receiverId);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();

            // Returns true if a matching record exists
            return rs.next();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
package com.fleetmanagement.dao;

import com.fleetmanagement.db.DatabaseConnection;
import com.fleetmanagement.model.OrderRequest;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderRequestDAO {

    public List<OrderRequest> getAllRequests() {
        List<OrderRequest> list = new ArrayList<>();
        String sql = "SELECT o.request_id, o.receiver_id, r.name AS receiver_name, r.company_name, r.phone, " +
                "o.pickup_location, o.delivery_location, o.pickup_date, o.cargo_type, o.weight, o.remarks, " +
                "o.status, o.requested_at, o.admin_remarks, o.reviewed_at " +
                "FROM order_requests o LEFT JOIN receivers r ON o.receiver_id = r.receiver_id " +
                "ORDER BY o.request_id DESC";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                OrderRequest o = new OrderRequest();
                o.setRequestId(rs.getInt("request_id"));
                o.setReceiverId(rs.getString("receiver_id"));
                o.setReceiverName(rs.getString("receiver_name"));
                o.setCompanyName(rs.getString("company_name"));
                o.setPhone(rs.getString("phone"));
                o.setPickupLocation(rs.getString("pickup_location"));
                o.setDeliveryLocation(rs.getString("delivery_location"));
                o.setPickupDate(rs.getString("pickup_date"));
                o.setCargoType(rs.getString("cargo_type"));
                o.setWeight(rs.getDouble("weight"));
                o.setRemarks(rs.getString("remarks"));
                String st = rs.getString("status");
                o.setStatus(st != null ? st : "Pending Approval");
                o.setRequestedAt(rs.getString("requested_at"));
                o.setAdminRemarks(rs.getString("admin_remarks"));
                o.setReviewedAt(rs.getString("reviewed_at"));
                list.add(o);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean updateDecision(int requestId, String status, String adminRemarks) {
        String sql = "UPDATE order_requests SET status = ?, admin_remarks = ?, reviewed_at = CURRENT_TIMESTAMP WHERE request_id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, adminRemarks);
            ps.setInt(3, requestId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteRequest(int requestId) {
        String sql = "DELETE FROM order_requests WHERE request_id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, requestId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteAllRequests() {
        String sql = "DELETE FROM order_requests";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            return ps.executeUpdate() >= 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
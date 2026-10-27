package com.fleetmanagement.dao;

import com.fleetmanagement.db.DatabaseConnection;
import com.fleetmanagement.model.OrderProof;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderProofDAO {

    private Integer nullableInt(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

    /** Table data - photos are intentionally NOT loaded here (they are huge). */
    public List<OrderProof> getAllProofs() {
        List<OrderProof> list = new ArrayList<>();
        String sql = "SELECT dr.report_id, dr.trip_id, t.driver_id, d.driver_name, t.vehicle_id, t.source, t.destination, " +
                "t.receiver_id, dr.receiver_name, dr.delivered_quantity, dr.delivery_status, dr.feedback_remarks, " +
                "dr.reported_at, dr.receiver_status, dr.receiver_notes, dr.receiver_person, dr.receiver_rating, " +
                "dr.receiver_quantity, dr.admin_status, dr.admin_remarks, dr.admin_reviewed_at " +
                "FROM delivery_reports dr " +
                "LEFT JOIN trips t ON dr.trip_id = t.trip_id " +
                "LEFT JOIN drivers d ON t.driver_id = d.driver_id " +
                "ORDER BY dr.report_id DESC";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                OrderProof p = new OrderProof();
                p.setReportId(rs.getInt("report_id"));
                p.setTripId(rs.getString("trip_id"));
                p.setDriverId(rs.getString("driver_id"));
                p.setDriverName(rs.getString("driver_name"));
                p.setVehicleId(rs.getString("vehicle_id"));
                p.setSource(rs.getString("source"));
                p.setDestination(rs.getString("destination"));
                p.setReceiverId(rs.getString("receiver_id"));
                p.setDriverReceiverName(rs.getString("receiver_name"));
                p.setDriverQty(nullableInt(rs, "delivered_quantity"));
                p.setDriverStatus(rs.getString("delivery_status"));
                p.setDriverRemarks(rs.getString("feedback_remarks"));
                p.setReportedAt(rs.getString("reported_at"));
                p.setReceiverStatus(rs.getString("receiver_status"));
                p.setReceiverNotes(rs.getString("receiver_notes"));
                p.setReceiverPerson(rs.getString("receiver_person"));
                p.setReceiverRating(nullableInt(rs, "receiver_rating"));
                p.setReceiverQty(nullableInt(rs, "receiver_quantity"));
                String as = rs.getString("admin_status");
                p.setAdminStatus(as != null ? as : "Pending Review");
                p.setAdminRemarks(rs.getString("admin_remarks"));
                p.setAdminReviewedAt(rs.getString("admin_reviewed_at"));
                list.add(p);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** Returns {driverPhoto, receiverPhoto} for one report. */
    public String[] getPhotos(int reportId) {
        String sql = "SELECT pod_photo_path, receiver_photo_path FROM delivery_reports WHERE report_id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reportId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return new String[]{rs.getString(1), rs.getString(2)};
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new String[]{null, null};
    }

    public boolean updateAdminDecision(int reportId, String status, String remarks) {
        String sql = "UPDATE delivery_reports SET admin_status = ?, admin_remarks = ?, admin_reviewed_at = CURRENT_TIMESTAMP WHERE report_id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, remarks);
            ps.setInt(3, reportId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteReport(int reportId) {
        String sql = "DELETE FROM delivery_reports WHERE report_id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reportId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteAllReports() {
        String sql = "DELETE FROM delivery_reports";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            return ps.executeUpdate() >= 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
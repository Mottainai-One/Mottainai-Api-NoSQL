package com.mottainai.mongoapi.security;
import java.sql.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OperationalMembership {
    private final String url, user, password;
    public OperationalMembership(@Value("${NOSQL_AUTH_DB_URL:}") String url,
            @Value("${NOSQL_AUTH_DB_USER:}") String user, @Value("${NOSQL_AUTH_DB_PASSWORD:}") String password) {
        this.url=url; this.user=user; this.password=password;
    }
    public boolean isActiveOwner(String uid, long userId, long companyId, java.util.UUID sessionId) {
        if (url.isBlank() || user.isBlank() || password.isBlank()) return false;
        try (Connection c = DriverManager.getConnection(url, user, password)) {
            c.setAutoCommit(false);
            try (PreparedStatement init = c.prepareStatement("SELECT mottainai.fn_bootstrap_staff_context(?, 'FIREBASE_UID')")) {
                init.setString(1, uid);
                try (ResultSet rs = init.executeQuery()) { if (!rs.next() || !rs.getBoolean(1)) return false; }
            }
            String sql = """
                SELECT 1 FROM mottainai.app_user u
                JOIN mottainai.employee e ON e.employee_id=u.employee_id
                JOIN mottainai.employee_role r ON r.role_id=e.role_id
                JOIN mottainai.retail_store s ON s.store_id=e.store_id
                JOIN mottainai.company c ON c.company_id=s.company_id
                WHERE u.user_id=mottainai.fn_get_current_user_id() AND u.firebase_uid=?
                  AND u.user_id=? AND s.company_id=? AND r.permission_level=100
                  AND EXISTS (SELECT 1 FROM mottainai.staff_session ss
                      WHERE ss.session_id=? AND ss.user_id=u.user_id
                        AND ss.revoked_at IS NULL AND ss.refresh_expires_at > CURRENT_TIMESTAMP)
                  AND u.active AND e.active AND r.active AND s.active AND c.active
                  AND u.deleted_at IS NULL AND e.deleted_at IS NULL AND r.deleted_at IS NULL
                  AND s.deleted_at IS NULL AND c.deleted_at IS NULL
                """;
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, uid); ps.setLong(2, userId); ps.setLong(3, companyId); ps.setObject(4, sessionId);
                try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
            }
        } catch (SQLException ex) { return false; }
    }
}

package com.mottainai.mongoapi.security;

import static org.junit.jupiter.api.Assertions.*;
import java.sql.*;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Executes the membership query against PostgreSQL with a non-superuser and RLS. */
@Testcontainers
class OperationalMembershipIT {
    @Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    private static final UUID SESSION = UUID.fromString("12345678-1234-1234-1234-123456789abc");
    private OperationalMembership membership;
    private Connection admin;

    @BeforeAll static void createSchema() throws Exception {
        try (Connection c = DriverManager.getConnection(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword()); Statement s=c.createStatement()) {
            s.execute("CREATE ROLE nosql_test LOGIN PASSWORD 'test-only'");
            s.execute("CREATE SCHEMA mottainai");
            s.execute("CREATE TABLE mottainai.company(company_id int primary key,active boolean,deleted_at timestamp)");
            s.execute("CREATE TABLE mottainai.retail_store(store_id int primary key,company_id int,active boolean,deleted_at timestamp)");
            s.execute("CREATE TABLE mottainai.employee_role(role_id int primary key,permission_level int,active boolean,deleted_at timestamp)");
            s.execute("CREATE TABLE mottainai.employee(employee_id int primary key,store_id int,role_id int,active boolean,deleted_at timestamp)");
            s.execute("CREATE TABLE mottainai.app_user(user_id int primary key,employee_id int,firebase_uid text,active boolean,deleted_at timestamp)");
            s.execute("CREATE TABLE mottainai.staff_session(session_id uuid primary key,user_id int,revoked_at timestamptz,refresh_expires_at timestamptz)");
            s.execute("CREATE FUNCTION mottainai.fn_get_current_user_id() RETURNS int LANGUAGE sql STABLE AS $$ SELECT NULLIF(current_setting('app.current_user_id',true),'')::int $$");
            s.execute("""
                CREATE FUNCTION mottainai.fn_bootstrap_staff_context(uid text, kind text) RETURNS boolean
                LANGUAGE plpgsql SECURITY DEFINER SET search_path = mottainai, pg_temp AS $$
                DECLARE id int; BEGIN
                    SELECT user_id INTO id FROM app_user WHERE firebase_uid=uid AND active AND deleted_at IS NULL;
                    IF id IS NULL OR kind <> 'FIREBASE_UID' THEN RETURN false; END IF;
                    PERFORM set_config('app.current_user_id',id::text,true); RETURN true;
                END $$
                """);
            s.execute("GRANT USAGE ON SCHEMA mottainai TO nosql_test");
            s.execute("GRANT SELECT ON ALL TABLES IN SCHEMA mottainai TO nosql_test");
            s.execute("ALTER TABLE mottainai.staff_session ENABLE ROW LEVEL SECURITY");
            s.execute("CREATE POLICY own_session ON mottainai.staff_session TO nosql_test USING (user_id=mottainai.fn_get_current_user_id())");
        }
    }
    @BeforeEach void seed() throws Exception {
        admin=DriverManager.getConnection(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword());
        try (Statement s=admin.createStatement()) {
            s.execute("TRUNCATE mottainai.staff_session,mottainai.app_user,mottainai.employee,mottainai.employee_role,mottainai.retail_store,mottainai.company");
            s.execute("INSERT INTO mottainai.company VALUES (7,true,null)");
            s.execute("INSERT INTO mottainai.retail_store VALUES (2,7,true,null)");
            s.execute("INSERT INTO mottainai.employee_role VALUES (3,100,true,null)");
            s.execute("INSERT INTO mottainai.employee VALUES (5,2,3,true,null)");
            s.execute("INSERT INTO mottainai.app_user VALUES (4,5,'uid',true,null)");
            s.execute("INSERT INTO mottainai.staff_session VALUES ('"+SESSION+"',4,null,current_timestamp+interval '1 hour')");
        }
        membership=new OperationalMembership(postgres.getJdbcUrl(),"nosql_test","test-only");
    }
    @AfterEach void close() throws Exception { admin.close(); }
    @Test void allowsActiveOwnerAndLiveSession() { assertTrue(membership.isActiveOwner("uid",4,7,SESSION)); }
    @Test void rejectsWrongCompanyAndSession() {
        assertFalse(membership.isActiveOwner("uid",4,8,SESSION));
        assertFalse(membership.isActiveOwner("uid",4,7,UUID.randomUUID()));
    }
    @Test void rejectsRevokedSession() throws Exception {
        try (Statement s=admin.createStatement()) { s.execute("UPDATE mottainai.staff_session SET revoked_at=current_timestamp"); }
        assertFalse(membership.isActiveOwner("uid",4,7,SESSION));
    }
    @Test void rejectsInactiveUserAndInsufficientRole() throws Exception {
        try (Statement s=admin.createStatement()) { s.execute("UPDATE mottainai.employee_role SET permission_level=80"); }
        assertFalse(membership.isActiveOwner("uid",4,7,SESSION));
        try (Statement s=admin.createStatement()) { s.execute("UPDATE mottainai.employee_role SET permission_level=100"); s.execute("UPDATE mottainai.app_user SET active=false"); }
        assertFalse(membership.isActiveOwner("uid",4,7,SESSION));
    }
}

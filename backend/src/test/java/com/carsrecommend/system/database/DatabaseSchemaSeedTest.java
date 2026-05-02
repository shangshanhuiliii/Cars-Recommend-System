package com.carsrecommend.system.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

class DatabaseSchemaSeedTest {

    @Test
    void schemaAndSeedDataSupportStageOneRequirements() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:cars_stage1;MODE=MySQL;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1",
                "sa",
                "")) {
            loadScripts(connection);

            assertEquals(1, count(connection, "SELECT COUNT(*) FROM app_user WHERE id = 1 AND username = 'demo_user'"));
            assertEquals(1, count(connection, "SELECT COUNT(*) FROM admin WHERE id = 1 AND username = 'demo_admin'"));

            assertEquals(120, count(connection, "SELECT COUNT(*) FROM car_model"));
            assertEquals(120, count(connection, "SELECT COUNT(*) FROM car_param"));
            assertEquals(3, count(connection, "SELECT COUNT(DISTINCT body_type) FROM car_model"));
            assertEquals(4, count(connection, "SELECT COUNT(DISTINCT energy_type) FROM car_model"));
            assertEquals(0, count(connection, "SELECT COUNT(*) FROM car_model WHERE energy_type = '新能源'"));
            assertEquals(0, count(connection,
                    "SELECT COUNT(*) FROM car_model WHERE energy_type NOT IN ('燃油', '纯电', '插混', '增程')"));
            assertEquals(0, count(connection,
                    "SELECT COUNT(*) FROM car_param p LEFT JOIN car_model c ON p.car_id = c.id WHERE c.id IS NULL"));
            assertEquals(0, count(connection,
                    "SELECT COUNT(*) FROM car_model c LEFT JOIN car_param p ON c.id = p.car_id WHERE p.car_id IS NULL"));

            assertTrue(count(connection, "SELECT COUNT(*) FROM car_model WHERE body_type = 'SUV'") >= 40);
            assertTrue(count(connection, "SELECT COUNT(*) FROM car_model WHERE body_type = '轿车'") >= 40);
            assertTrue(count(connection, "SELECT COUNT(*) FROM car_model WHERE body_type = 'MPV'") >= 20);
            assertTrue(count(connection, "SELECT COUNT(*) FROM car_model WHERE energy_type = '燃油'") >= 30);
            assertTrue(count(connection, "SELECT COUNT(*) FROM car_model WHERE energy_type = '纯电'") >= 30);
            assertTrue(count(connection, "SELECT COUNT(*) FROM car_model WHERE energy_type = '插混'") >= 25);
            assertTrue(count(connection, "SELECT COUNT(*) FROM car_model WHERE energy_type = '增程'") >= 15);
            assertTrue(count(connection, "SELECT COUNT(DISTINCT brand) FROM car_model") >= 25);

            assertTrue(count(connection, "SELECT COUNT(*) FROM car_model WHERE guide_price < 80000") > 0);
            assertTrue(count(connection, "SELECT COUNT(*) FROM car_model WHERE guide_price BETWEEN 80000 AND 120000") > 0);
            assertTrue(count(connection, "SELECT COUNT(*) FROM car_model WHERE guide_price BETWEEN 120000 AND 180000") > 0);
            assertTrue(count(connection, "SELECT COUNT(*) FROM car_model WHERE guide_price BETWEEN 180000 AND 250000") > 0);
            assertTrue(count(connection, "SELECT COUNT(*) FROM car_model WHERE guide_price BETWEEN 250000 AND 400000") > 0);
            assertTrue(count(connection, "SELECT COUNT(*) FROM car_model WHERE guide_price > 400000") > 0);
            assertTrue(count(connection, "SELECT COUNT(*) FROM car_model WHERE seats = 4") > 0);
            assertTrue(count(connection, "SELECT COUNT(*) FROM car_model WHERE seats = 5") > 0);
            assertTrue(count(connection, "SELECT COUNT(*) FROM car_model WHERE seats = 6") > 0);
            assertTrue(count(connection, "SELECT COUNT(*) FROM car_model WHERE seats = 7") > 0);

            assertColumnSelectable(connection,
                    "SELECT body_types, energy_types, min_seats, scenes, factor_weights, excluded_car_ids "
                            + "FROM user_demand WHERE 1 = 0");
            assertColumnSelectable(connection, "SELECT recommend_status FROM recommend_record WHERE 1 = 0");
            assertColumnSelectable(connection, "SELECT tags FROM recommend_item WHERE 1 = 0");
            assertColumnSelectable(connection,
                    "SELECT total_score, price_score, space_score, safety_score, energy_score, intelligence_score, "
                            + "comfort_score, power_score, reputation_score, popularity_score, match_level, "
                            + "reason_text, weakness_text FROM recommend_item WHERE 1 = 0");

            assertEquals(0, count(connection, "SELECT COUNT(*) FROM car_feature_score"));
            assertEquals(0, count(connection, "SELECT COUNT(*) FROM recommend_record"));
            assertEquals(0, count(connection, "SELECT COUNT(*) FROM recommend_item"));

            execute(connection,
                    "INSERT INTO user_demand (user_id, energy_types) VALUES (1, '[\"新能源\"]')");
            assertEquals(1, count(connection, "SELECT COUNT(*) FROM user_demand WHERE energy_types LIKE '%新能源%'"));
            assertThrows(SQLException.class,
                    () -> execute(connection, "SELECT body_type FROM user_demand WHERE 1 = 0"));
            assertThrows(SQLException.class,
                    () -> execute(connection, "SELECT energy_type FROM user_demand WHERE 1 = 0"));
            assertThrows(SQLException.class,
                    () -> execute(connection, "SELECT scene FROM user_demand WHERE 1 = 0"));
            assertThrows(SQLException.class,
                    () -> execute(connection, "SELECT focus_factors FROM user_demand WHERE 1 = 0"));

            assertThrows(SQLException.class, () -> execute(connection,
                    "INSERT INTO car_model (brand, series, model_name, guide_price, body_type, energy_type, "
                            + "seats, launch_year, sales_volume, user_rating, audit_status) VALUES "
                            + "('测试', '无效动力', '无效新能源车型', 100000, 'SUV', '新能源', 5, 2026, 0, 0, 'APPROVED')"));
        }
    }

    private void loadScripts(Connection connection) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setSqlScriptEncoding("UTF-8");
        populator.addScript(new ClassPathResource("db/schema.sql"));
        populator.addScript(new ClassPathResource("db/seed-data.sql"));
        populator.execute(new SingleConnectionDataSource(connection, true));
    }

    private int count(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private void assertColumnSelectable(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}

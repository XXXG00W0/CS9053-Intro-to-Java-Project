package chat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public class ServerDatabase {
    private static final String URL = "jdbc:sqlite:server.db";
    private static final String DB_NAME = "authentication";
    // private static final String USER = "admin";
    // private static final String PASSWORD = "pswd";
    private Connection conn;
    private PreparedStatement pstmt = null;
    private static final Logger logger = Logger.getLogger(ChatClientView.class.getName());

    public ServerDatabase() throws SQLException, ClassNotFoundException {
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection(URL);
        } catch (SQLException | ClassNotFoundException e) {
            logger.severe("Cannot establish connection with database:\n" + e.getMessage());
            throw e;
        }

        Statement stmt = conn.createStatement();
        String sql = "CREATE TABLE IF NOT EXISTS " + DB_NAME + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT UNIQUE NOT NULL, " +
                "password TEXT NOT NULL);";
        stmt.execute(sql);
        logger.info("Database created");
    }

    public boolean checkUserExists(String username) {
        String sql = "SELECT COUNT(id) FROM " + DB_NAME + " WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet result = pstmt.executeQuery()) {
                if (result.next()) {
                    return result.getInt(1) == 1;
                }
            }
        } catch (SQLException sqle) {
            logger.warning("Looking up username " + username + " gets exception:\n" + sqle.getMessage());
        }
        return false;
    }

    public Boolean checkUsernameAndPassword(String username, String password) {
        String sql = "SELECT COUNT(id) FROM " + DB_NAME + " WHERE username = ? AND password = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            try (ResultSet result = pstmt.executeQuery()) {
                if (result.next()) {
                    return result.getInt(1) > 0;
                }
            }
        } catch (SQLException sqle) {
            logger.warning("Looking up username " + username + " and password gets exception:\n" + sqle.getMessage());
        }
        return false;
    }

    public boolean createUser(String username, String password) {
        boolean success = false;
        if (checkUserExists(username)) {
            logger.warning("Username" + username + "already exists");
            return success;
        }
        try {
            conn = DriverManager.getConnection(URL);
            String sql = "INSERT INTO authentication (username, password) VALUES (?, ?)";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            logger.info("Successfully create username: " + username + " with password: " + password);
            success = true;
        } catch (SQLException sqle) {
            logger.warning("Creating username " + username + "gets exception:\n" + sqle.getMessage());
        } finally {
            try {
                if (pstmt != null)
                    pstmt.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException sqle) {
            }
            ;
        }
        return success;
    }

    public boolean updateUsername(String username, String password) {
        return false;
    }

    public boolean updatePassword(String username, String password) {
        return false;
    }

    public boolean deleteUser(String username, String password) {
        return false;
    }

    public String getURL() {
        return URL;
    }

    public String getDbName() {
        return DB_NAME;
    }

}

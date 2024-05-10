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
        if (checkUserExists(username)) {
            logger.warning("Username " + username + " already exists");
            return false;
        }

        String sql = "INSERT INTO authentication (username, password) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            logger.info("Successfully created username: " + username + " with password: " + password);
            return true;
        } catch (SQLException sqle) {
            logger.warning("Creating username " + username + " gets exception:\n" + sqle.getMessage());
            return false;
        }
    }

    public boolean updateUsername(String oldUsername, String password, String newUsername) {
        // Verify if old username and password is in the database 
        if (!checkUsernameAndPassword(oldUsername, password)) {
            logger.info("Username and password do not match");
            return false;
        }

        String sql = "UPDATE authentication SET username = ? WHERE username = ? AND password = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newUsername);
            pstmt.setString(2, oldUsername);
            pstmt.setString(3, password);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                logger.info("Username successfully updated from " + oldUsername + " to " + newUsername);
                return true;
            } else {
                logger.info("No rows affected.");
                return false;
            }
        } catch (SQLException sqle) {
            logger.warning("Updating username encountered an exception: " + sqle.getMessage());
            return false;
        }
    }

    public boolean updatePassword(String username, String oldPassword, String newPassword) {
        // Verify if old username and password is in the database 
        if (!checkUsernameAndPassword(username, oldPassword)) {
            logger.info("Username and password do not match");
            return false;
        }

        String sql = "UPDATE authentication SET password = ? WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newPassword); // to-do Bcrypt encryption
            pstmt.setString(2, username);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                logger.info("Password successfully updated for username: " + username);
                return true;
            } else {
                logger.info("No rows affected.");
                return false;
            }
        } catch (SQLException sqle) {
            logger.warning("Updating password encountered an exception: " + sqle.getMessage());
            return false;
        }
    }

    public boolean deleteUser(String username, String password) {
        // Verify if old username and password is in the database 
        if (!checkUsernameAndPassword(username, password)) {
            logger.info("Username or password is incorrect");
            return false;
        }

        String sql = "DELETE FROM authentication WHERE username = ? AND password = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                logger.info("User successfully deleted: " + username);
                return true;
            } else {
                logger.info("No rows affected. User not found or wrong password.");
                return false;
            }
        } catch (SQLException sqle) {
            logger.warning("Error deleting user " + username + ": " + sqle.getMessage());
            return false;
        }
    }

    public String getURL() {
        return URL;
    }

    public String getDbName() {
        return DB_NAME;
    }

    public boolean deleteTable() {
        // equivalent to TRUNCATE
        String sql = "DELETE FROM "+DB_NAME+";";
        try (Connection conn = DriverManager.getConnection(URL);
                Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            System.out.println("Table has been deleted successfully.");
            return true;
        } catch (SQLException e) {
            logger.warning("SQL Error while truncating db: " + e.getMessage());
        }
        return false;
    }

}

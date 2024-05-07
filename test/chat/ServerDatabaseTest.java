import chat.ServerDatabase;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ServerDatabaseTest {

    // public ServerDatabaseTest(){
    // ServerDatabase db = new ServerDatabase();
    // }
    ServerDatabase db;
    ResultSet result;

    public void deleteDbFile() {
        try {
            Files.delete(Paths.get("server.db"));
            System.out.println("File deleted successfully.");
        } catch (IOException e) {
            System.out.println("An error occurred while deleting the file.");
            e.printStackTrace();
        }
    }

    @Before
    public void runBefore() throws ClassNotFoundException, SQLException {
        deleteDbFile();
        db = new ServerDatabase();
    }

    @Test
    public void constructorTest() throws SQLException {
        ResultSet result;
        Connection con = DriverManager.getConnection(db.getURL());
        String sql = "SELECT * FROM " + db.getDbName() + ";";
        PreparedStatement pstmt = con.prepareStatement(sql);
        result = pstmt.executeQuery();
        assertFalse(result.next());
        pstmt.close();
        con.close();
    }

    @Test
    public void createUserTest() throws SQLException {
        String username = "ABC";
        String password = "123";
        db.createUser(username, password);
        Connection con = DriverManager.getConnection(db.getURL());
        String sql = "SELECT * FROM " + db.getDbName() + " WHERE username = ?;";
        PreparedStatement pstmt = con.prepareStatement(sql);
        pstmt.setString(1, username);
        result = pstmt.executeQuery();
        assertTrue(result.next());
        pstmt.close();
        con.close();
    }

    @Test
    public void createDuplicateUserTest() throws SQLException {
        String username1 = "abc";
        String username2 = "ABC";
        String password1 = "123";
        String password2 = "123";
        db.createUser(username1, password1);
        Connection con = DriverManager.getConnection(db.getURL());
        String sql = "SELECT * FROM " + db.getDbName() + " WHERE username = ?;";
        PreparedStatement pstmt = con.prepareStatement(sql);
        pstmt.setString(1, username1);
        result = pstmt.executeQuery();
        assertTrue(result.next());
        pstmt.close();
        con.close();
    }

    @Test
    public void checkUserExistsTest() {
        // ServerDatabase db = new ServerDatabase();
        String username = "abc";
        String password = "12123";
        assertFalse(db.checkUserExists(username));
        db.createUser(username, password);
        assertTrue(db.checkUserExists(username));
    }

    @Test
    public void checkUsernameAndPasswordTest(){
        String username1 = "abc";
        String username2 = "ABC";
        String password1 = "123";
        String password2 = "234";
        db.createUser(username1, password1);
        assertTrue(db.checkUsernameAndPassword(username1, password1));
        db.createUser(username2, password2);
        assertTrue(db.checkUsernameAndPassword(username2, password2));
        assertFalse(db.checkUsernameAndPassword(username1, password2));
        assertFalse(db.checkUsernameAndPassword(username2, password1));

    }

}

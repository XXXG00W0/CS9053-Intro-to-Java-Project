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
        // deleteDbFile();
        // db = new ServerDatabase();
            
        db = new ServerDatabase();
        if (db != null)
            db.deleteTable();
        
    }

    @Test
    public void constructorTest() throws SQLException, ClassNotFoundException {
        ResultSet result;
        db = new ServerDatabase();
        Connection con = DriverManager.getConnection(db.getURL());
        String sql = "SELECT * FROM " + db.getDbName() + ";";
        PreparedStatement pstmt = con.prepareStatement(sql);
        result = pstmt.executeQuery();
        assertFalse(result.next());
        pstmt.close();
        con.close();
    }
    @Test
    public void deleteTableTest(){
        assertTrue(db.deleteTable());
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
        String username = "def";
        String password = "123";
        assertFalse(db.checkUserExists(username));
        db.createUser(username, password);
        assertTrue(db.checkUserExists(username));
    }

    @Test
    public void checkUsernameAndPasswordTest(){
        String username1 = "ghi";
        String username2 = "GHI";
        String password1 = "123";
        String password2 = "234";
        assertTrue(db.createUser(username1, password1));
        assertTrue(db.checkUsernameAndPassword(username1, password1));
        assertTrue(db.createUser(username2, password2));
        assertTrue(db.checkUsernameAndPassword(username2, password2));
        // duplicate username
        assertFalse(db.checkUsernameAndPassword(username1, password2));
        // duplicate username
        assertFalse(db.checkUsernameAndPassword(username2, password1));
    }

    @Test
    public void updateUsernameTest(){
        String username1 = "ghi";
        String username2 = "GHI";
        String password1 = "123";
        String password2 = "234";
        db.createUser(username1, password1);
        // Wrong password
        assertFalse(db.updateUsername(username1, password2, username2));
        // Correct password
        assertTrue(db.updateUsername(username1, password1, username2));
        assertTrue(db.checkUsernameAndPassword(username2, password1));
    }

    @Test
    public void updatePasswordTest(){
        String username1 = "ghi";
        String username2 = "GHI";
        String password1 = "123";
        String password2 = "234";
        db.createUser(username2, password2);        
        // Wrong password
        assertFalse(db.updatePassword(username1, password1, password2));
        // correct password
        assertTrue(db.updatePassword(username2, password2, password1));
        assertTrue(db.checkUsernameAndPassword(username2, password1));
    }

    @Test
    public void deleteUserTest(){
        String username1 = "ghi";
        String username2 = "GHI";
        String password1 = "123";
        String password2 = "234";
        db.createUser(username2, password2);        
        // Wrong password
        assertFalse(db.deleteUser(username2, password1));
        // Not deleted from table
        assertTrue(db.checkUserExists(username2));
        // correct password
        assertTrue(db.deleteUser(username2, password2));
        // deleted from table
        assertFalse(db.checkUserExists(username2));
    }

}

package chat;

public class User {

    private String username;
    private String password;

    public User(String username, String password){
        this.username = username;
        this.password = password;
    }

    // public User(String rawString) throws IllegalArgumentException{
    //     String[] stringList = rawString.split(" ");
    //     if (stringList.length != 3){
    //         throw new IllegalArgumentException("\""+rawString +"\" is not a valid login string");
    //     }
    //     String username  = stringList[1];
    //     String password  = stringList[2];
    //     this.username = username;
    //     this.password = password;
    // }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    
}

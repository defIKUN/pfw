import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class UpdatePassword {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/river_detection?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai";
        String username = "root";
        String password = "root";
        
        try {
            Connection conn = DriverManager.getConnection(url, username, password);
            Statement stmt = conn.createStatement();
            
            String sql = "UPDATE user SET password = '$2a$10$Yr8bioSdHx5RPvkanj204ut80Yvlpp1SVK1/1L6gGWLISxEwuu64y' WHERE username = 'admin'";
            
            int rowsAffected = stmt.executeUpdate(sql);
            System.out.println("Rows affected: " + rowsAffected);
            
            stmt.close();
            conn.close();
            System.out.println("Password updated successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
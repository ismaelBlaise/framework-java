package scan;

import jakarta.servlet.http.HttpServletRequest;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DbUtil {
    private HttpServletRequest httpServletRequest;

    public DbUtil(HttpServletRequest request) {
        this.httpServletRequest = request;
    }

    public Connection getConnection() {
        Connection connection = null;
        Properties properties = new Properties();
        FileInputStream fis = null;

        try {
            String configPath = httpServletRequest.getServletContext().getRealPath("/") + "WEB-INF/application.properties";
            fis = new FileInputStream(configPath);
            properties.load(fis);

            String url = properties.getProperty("db.url");
            String username = properties.getProperty("db.username");
            String password = properties.getProperty("db.password");

            Class.forName(properties.getProperty("db.driver"));
            connection = DriverManager.getConnection(url, username, password);
        } catch (IOException | SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la connexion à la base de données");
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return connection;
    }
}
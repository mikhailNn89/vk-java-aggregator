import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import org.eclipse.jetty.server.Server;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class VkApplication {
    private final static String PROPERTIES_FILE = "config.properties";
    private static Properties properties;
    private static VkApiClient apiClient;
    private static UserActor userActor;
    private static GroupActor groupActor;

    private static void readProperties() throws FileNotFoundException {
        InputStream inputStream = VkApplication.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE);
        if (inputStream == null)
            throw new FileNotFoundException("Property file '" + PROPERTIES_FILE + "' is not found in the classpath");
        try {
            properties = new Properties();
            properties.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("Incorrect properties file");
        }
    }

    private static int abs(int n) { return Math.abs(n); }

    private static int getIntProperty(Properties properties, String name) {
        return Integer.parseInt(properties.getProperty(name));
    }

    private static void authorization() {
        apiClient = new VkApiClient(new HttpTransportClient());
        int userId = getIntProperty(properties, "userId");
        String userToken = properties.getProperty("userToken");
        userActor = new UserActor(userId, userToken);
        int groupId = abs(getIntProperty(properties, "publicId"));
        String groupToken = properties.getProperty("groupToken");
        groupActor = new GroupActor(groupId, groupToken);
    }

    public static void main(String[] args)  throws Exception  {
        readProperties();
        authorization();
        Aggregator ag = new Aggregator(apiClient,
                                       userActor,
                                       properties);
        MessageBot mb = new MessageBot(apiClient,
                                       groupActor,
                                       getIntProperty(properties, "userId"),
                                       properties.getProperty("confirmationCode"),
                                       getIntProperty(properties, "serverId"));
        mb.setAggregator(ag);

        int assPort = Integer.valueOf(System.getenv("PORT"));
        Server server = new Server(assPort);
        server.setHandler(mb);
        server.start();
        server.join();
    }
}

import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.utils.DomainResolved;

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

    private static void authorization() {
        apiClient = new VkApiClient(new HttpTransportClient());

        int userId = Integer.parseInt(properties.getProperty("userId"));
        String userToken = properties.getProperty("userToken");
        userActor = new UserActor(userId, userToken);

        int groupId = Integer.parseInt(properties.getProperty("groupId"));
        String groupToken = properties.getProperty("groupToken");
        groupActor = new GroupActor(groupId, groupToken);
    }

    public static void main(String[] args)  throws Exception  {
        readProperties();
        authorization();
        Aggregator ag = new Aggregator(apiClient, userActor);
        ag.aggregateContent();
        //PhotoHandler ph = new PhotoHandler(apiClient, userActor);
        //ph.photo3();
        //ph.photo1();
        //ph.calcNum();
        //ph.deleteAllPhoto();
        //WallHandler wh = new WallHandler(apiClient, userActor);
        //wh.clear();
        //wh.getNum();



        int y = 2;
    }

}

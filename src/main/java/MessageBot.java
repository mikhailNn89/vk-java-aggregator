import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Random;

public class MessageBot extends AbstractHandler {
    private VkApiClient apiClient;
    private UserActor userActor;
    private GroupActor groupActor;
    private Aggregator aggregator;
    private final static int adminId = 431432761;

    private final Random random = new Random();
    private final static String CONFIRMATION_TYPE = "confirmation";
    private final static String MESSAGE_TYPE = "message_new";
    private final static String OK_BODY = "ok";
    private final String confirmationCode;
    private final Gson gson;
    private ArrayList<JsonObject> objList;

    MessageBot(VkApiClient apiClient, UserActor userActor, GroupActor groupActor, String confirmationCode) {
        this.apiClient = apiClient;
        this.userActor = userActor;
        this.groupActor = groupActor;
        this.confirmationCode = confirmationCode;
        this.gson = new GsonBuilder().create();
        this.objList = new ArrayList<>();
    }

    public void setAggregator(Aggregator aggregator) { this.aggregator = aggregator; }

    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response)
                       throws IOException, ServletException {
        if (!"POST".equalsIgnoreCase(request.getMethod())) { throw new ServletException("This method is unsupported"); }
        try {
            Reader reader = request.getReader();
            JsonObject requestJson = gson.fromJson(reader, JsonObject.class);
            String type = requestJson.get("type").getAsString();
            if (type == null || type.isEmpty()) throw new ServletException("No type in json");

            final String responseBody;
            switch (type) {
                case CONFIRMATION_TYPE:
                    sendResponse(confirmationCode, baseRequest, response);
                    break;
                case MESSAGE_TYPE:
                    sendResponse(OK_BODY, baseRequest, response);
                    JsonObject object = requestJson.getAsJsonObject("object");
                    handleMessage(object);
                    objList.add(object);
                    break;
                default:
                    sendResponse(OK_BODY, baseRequest, response);
                    break;
            }
        } catch (JsonParseException e) {
            throw new ServletException("Incorrect json", e);
        }
    }

    private void sendResponse(String responseBody,
                                Request baseRequest,
                                HttpServletResponse response)
            throws IOException, ServletException {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
        response.getWriter().println(responseBody);
    }

    private void sendMessage(int userId, String str) {
        try {
            apiClient.messages().send(groupActor).message(str).userId(userId).randomId(random.nextInt()).execute();
        } catch (ApiException e) {
            ;
        } catch (ClientException e) {
            ;
        }
    }

    private void handleMessage(JsonObject object) {
        if (objList.contains(object)) {
            return;
        }

        int userId = object.getAsJsonPrimitive("from_id").getAsInt();
        if (userId != adminId) {
            sendMessage(userId, "Hello user from MessageBot.");
            return;
        }

        sendMessage(userId, "Hello admin.");
        //sendMessage(userId, object.toString());
        String text = object.getAsJsonPrimitive("text").getAsString();
        switch (text) {
            case "#fw":
                aggregator.fillWall();
                sendMessage(userId,"Wall is filled.");
                break;
            case "#fp":
                aggregator.fillPhoto();
                sendMessage(userId,"Photos are filled.");
                break;
            case "#fl":
                aggregator.fillLinks();
                sendMessage(userId,"Links are filled.");
                break;
            case "#cw":
                aggregator.clearWall();
                sendMessage(userId,"Wall is cleared.");
                break;
            case "#cp":
                aggregator.clearPhoto();
                sendMessage(userId,"Photos are cleared.");
                break;
            case "#cl":
                aggregator.clearLinks();
                sendMessage(userId,"Links are cleared.");
                break;
            default:
                sendMessage(userId,"Command isn't supported.");
                break;
        }
    }
}

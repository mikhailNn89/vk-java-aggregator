import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.base.responses.OkResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Random;

public class MessageBot extends AbstractHandler {
    private VkApiClient apiClient;
    private GroupActor groupActor;
    private Aggregator aggregator;
    private int adminId;
    private final static long intermDelay = 100;

    private final Random random = new Random();
    private final static String CONFIRMATION_TYPE = "confirmation";
    private final static String MESSAGE_TYPE = "message_new";
    private final static String OK_BODY = "ok";
    private final String confirmationCode;
    private final Gson gson;
    private ArrayList<JsonObject> objList;

    MessageBot(VkApiClient apiClient,
               GroupActor groupActor,
               int adminId,
               String confirmationCode,
               int serverId) {
        this.apiClient = apiClient;
        this.groupActor = groupActor;
        this.adminId = adminId;
        this.confirmationCode = confirmationCode;
        this.gson = new GsonBuilder().create();
        this.objList = new ArrayList<>();
        try {
            setCallbackSettings(serverId);
            Thread.sleep(intermDelay);
        } catch (Exception e) {
            ;
        }
    }

    public void setAggregator(Aggregator aggregator) { this.aggregator = aggregator; }

    private void setCallbackSettings(int serverId) throws Exception {
        OkResponse ok = apiClient.
                         groups().
setCallbackSettings(groupActor, serverId).
                 messageNew(true).
              messageReply(false).
              messageAllow(false).
               messageDeny(false).
               messageEdit(false).
                  photoNew(false).
                  audioNew(false).
                  videoNew(false).
              wallReplyNew(false).
             wallReplyEdit(false).
           wallReplyDelete(false).
   	      wallReplyRestore(false).
               wallPostNew(false).
                wallRepost(false).
              boardPostNew(false).
             boardPostEdit(false).
          boardPostRestore(false).
           boardPostDelete(false).
           photoCommentNew(false).
          photoCommentEdit(false).
        photoCommentDelete(false).
       photoCommentRestore(false).
           videoCommentNew(false).
          videoCommentEdit(false).
        videoCommentDelete(false).
       videoCommentRestore(false).
          marketCommentNew(false).
         marketCommentEdit(false).
       marketCommentDelete(false).
      marketCommentRestore(false).
               pollVoteNew(false).
                 groupJoin(false).
                groupLeave(false).
       groupChangeSettings(false).
          groupChangePhoto(false).
		 groupOfficersEdit(false).
				        execute();
    }

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

            switch (type) {
                case CONFIRMATION_TYPE:
                    sendResponse(confirmationCode, baseRequest, response);
                    break;
                case MESSAGE_TYPE:
                    JsonObject object = requestJson.getAsJsonObject("object");
                    objList.add(object);
                    sendResponse(OK_BODY, baseRequest, response);
                    try {
                        handleMessage(object);
                    } catch (Exception e) {
                        ;
                    }
                    break;
                default:
                    sendResponse(OK_BODY, baseRequest, response);
                    break;
            }
        } catch (JsonParseException e) {
            throw new ServletException("Incorrect json", e);
        }
    }

    private static void sendResponse(String responseBody,
                                     Request baseRequest,
                                     HttpServletResponse response)
            throws IOException {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
        response.getWriter().println(responseBody);
    }

    private void sendMessage(int userId, String str) {
        try {
            int res = apiClient.messages().send(groupActor).message(str).userId(userId).randomId(random.nextInt()).execute();
        } catch (ApiException e) {
            ;
        } catch (ClientException e) {
            ;
        }
    }

    private void handleMessage(JsonObject object) throws Exception {
        int count = 0;
        for (JsonObject o : objList) {
            if (o.equals(object)) {
                count++;
            }
        }
        if (count != 1) {
            return;
        }

        int userId = object.getAsJsonPrimitive("from_id").getAsInt();
        if (userId != adminId) {
            sendMessage(userId, "Hello user from MessageBot.");
            return;
        }

        sendMessage(userId, "Hello admin.");
        String text = object.getAsJsonPrimitive("text").getAsString();
        text = text.trim();
        Thread.sleep(intermDelay);
        switch (text) {
            case "#fw":
                aggregator.fillWall();
                Thread.sleep(intermDelay);
                sendMessage(userId,"Wall is filled.");
                break;
            case "#fp":
                aggregator.fillPhoto();
                Thread.sleep(intermDelay);
                sendMessage(userId,"Photos are filled.");
                break;
            case "#fl":
                aggregator.fillLinks();
                Thread.sleep(intermDelay);
                sendMessage(userId,"Links are filled.");
                break;
            case "#cw":
                aggregator.clearWall();
                Thread.sleep(intermDelay);
                sendMessage(userId,"Wall is cleared.");
                break;
            case "#cp":
                aggregator.clearPhoto();
                Thread.sleep(intermDelay);
                sendMessage(userId,"Photos are cleared.");
                break;
            case "#cl":
                aggregator.clearLinks();
                Thread.sleep(intermDelay);
                sendMessage(userId,"Links are cleared.");
                break;
            default:
                String[] split = text.split("#sg");
                if (split.length == 2) {
                    int grVar = Integer.parseInt(split[split.length-1].trim());
                    aggregator.setGroupsVar(grVar);
                    sendMessage(userId,"Variant #"+grVar+" is selected.");
                }
                else {
                    sendMessage(userId, "Command isn't supported.");
                }
                break;
        }
    }
}

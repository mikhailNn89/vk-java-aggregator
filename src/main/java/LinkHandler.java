import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.objects.base.responses.OkResponse;
import com.vk.api.sdk.objects.groups.GroupFull;
import com.vk.api.sdk.objects.groups.GroupLink;
import com.vk.api.sdk.objects.groups.LinksItem;
import com.vk.api.sdk.queries.groups.GroupField;

import java.util.ArrayList;
import java.util.List;

public class LinkHandler {
    private VkApiClient apiClient;
    private UserActor userActor;
    private final static long cycleDelay = 250;
    private final static long intermDelay = 100;

    LinkHandler(VkApiClient apiClient, UserActor userActor) {
        this.apiClient = apiClient;
        this.userActor = userActor;
    }

    private static int abs(int n) {
        return Math.abs(n);
    }

    private List<LinksItem> getAllLinks(int groupId) throws Exception {
        String str = Integer.toString(abs(groupId));
        List<GroupFull> list1 = apiClient.groups().getById(userActor).groupId(str).fields(GroupField.LINKS).execute();
        return list1.get(0).getLinks();
    }

    private void deleteLink(int groupId, LinksItem link) throws Exception {
        OkResponse ok = apiClient.groups().deleteLink(userActor, abs(groupId), link.getId()).execute();
    }

    public void deleteAllLinks(int groupId) throws Exception {
        List<LinksItem> list = getAllLinks(groupId);
        if ((list == null) || list.isEmpty()) {
            return;
        }
        Thread.sleep(intermDelay);
        for (LinksItem l : list) {
            deleteLink(groupId, l);
            Thread.sleep(cycleDelay);
        }
    }

    private GroupLink addLink(int groupId, String link, String text) throws Exception {
        //return apiClient.groups().addLink(userActor, abs(groupId), link).text(text).execute();
        return apiClient.groups().addLink(userActor, abs(groupId), link).execute();
    }

    public List<GroupLink> addLinkList(int groupId, List<String> linkList) throws Exception {
        List<GroupLink> list = new ArrayList<>();
        for (String s : linkList) {
            list.add(addLink(groupId, s, ""));
            Thread.sleep(cycleDelay);
        }
        return list;
    }
}

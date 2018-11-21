import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.objects.base.responses.OkResponse;
import com.vk.api.sdk.objects.groups.GroupFull;
import com.vk.api.sdk.objects.groups.GroupLink;
import com.vk.api.sdk.objects.groups.LinksItem;
import com.vk.api.sdk.queries.groups.GroupField;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class LinkHandler {
    private VkApiClient apiClient;
    private UserActor userActor;

    LinkHandler(VkApiClient apiClient, UserActor userActor) {
        this.apiClient = apiClient;
        this.userActor = userActor;
    }

    private int abs(int n) {
        return Math.abs(n);
    }

    public List<LinksItem> getAllLinks(int ownerId) throws Exception {
        String str = Integer.toString(abs(ownerId));
        List<GroupFull> list1 = apiClient.groups().getById(userActor).groupId(str).fields(GroupField.LINKS).execute();
        return list1.get(0).getLinks();
    }

    private void deleteLink(int ownerId, LinksItem link) throws Exception {
        OkResponse ok = apiClient.groups().deleteLink(userActor, abs(ownerId), link.getId()).execute();
    }

    public void deleteAllLinks(int ownerId) throws Exception {
        List<LinksItem> list = getAllLinks(ownerId);
        if ((list == null) || list.isEmpty()) {
            return;
        }
        Thread.sleep(400);
        for (LinksItem l : list) {
            deleteLink(ownerId, l);
            Thread.sleep(400);
        }
    }

    private GroupLink addLink(int ownerId, String link, String text) throws Exception {
        //return apiClient.groups().addLink(userActor, abs(ownerId), link).text(text).execute();
        return apiClient.groups().addLink(userActor, abs(ownerId), link).execute();
    }

    public List<GroupLink> addLinkList(int ownerId, List<String> linkList) throws Exception {
        List<GroupLink> list = new ArrayList<>();
        for (String s : linkList) {
            list.add(addLink(ownerId, s, ""));
            Thread.sleep(250);
        }
        /*
        int len = linkList.size();
        for (int idx = 0; idx < len; idx++) {
            list.add(addLink(ownerId, linkList.get(idx), textList.get(idx)));
        }
        */
        return list;
    }
}

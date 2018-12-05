import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.objects.base.responses.OkResponse;
import com.vk.api.sdk.objects.wall.WallPostFull;
import com.vk.api.sdk.objects.wall.responses.GetByIdExtendedResponse;
import com.vk.api.sdk.objects.wall.responses.GetExtendedResponse;
import com.vk.api.sdk.objects.wall.responses.RepostResponse;
import com.vk.api.sdk.queries.wall.WallGetFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class WallHandler {
    private VkApiClient apiClient;
    private UserActor userActor;
    private int maxNumPost;
    private final static long cycleDelay = 250;
    private final static long intermDelay = 100;

    WallHandler(VkApiClient apiClient,
                UserActor userActor,
                int maxNumPost) {
        this.apiClient = apiClient;
        this.userActor = userActor;
        this.maxNumPost = maxNumPost;
    }

    private static int abs(int n) { return Math.abs(n); }

    private static int divCeil(int x1, int x2) {
        double div = ((double)x1) / ((double)x2);
        return (int)(Math.ceil(div));
    }

    private void delete(WallPostFull post) throws Exception {
        OkResponse ok = apiClient.wall().delete(userActor).ownerId(post.getOwnerId()).postId(post.getId()).execute();
    }

    public void clearWall(int groupId) throws Exception {
        List<WallPostFull> list = getExtended(groupId);
        if ((list == null) || list.isEmpty()) {
            return;
        }
        Thread.sleep(intermDelay);
        for (WallPostFull p : list) {
            delete(p);
            Thread.sleep(cycleDelay);
        }
    }

    private List<WallPostFull> getExtended(int groupId) throws Exception {
        int offsetPost = 0, countPost = 100;
        int numIter = divCeil(maxNumPost, countPost), countIter = 0;
        List<WallPostFull> list1 = new ArrayList<>();
        while (true) {
            GetExtendedResponse resp = apiClient.
                                          wall().
                          getExtended(userActor).
                                ownerId(groupId).
                              offset(offsetPost).
                                count(countPost).
                       filter(WallGetFilter.ALL).
                                       execute();
            List<WallPostFull> list2 = resp.getItems();
            if ((list2 == null) || list2.isEmpty()) {
                break;
            }
            list1.addAll(list2);
            offsetPost += countPost;
            countIter++;
            if ((list2.size() < countPost) || (countIter >= numIter)) {
                break;
            }
            Thread.sleep(cycleDelay);
        }
        return list1;
    }

    public List<WallPostFull> getAllSorted(int groupId) throws Exception {
        List<WallPostFull> list = getExtended(groupId);
        sortPost(list);
        return list;
    }

    private WallPostFull getByIdExtended(int groupId, int postId) throws Exception {
        String str = groupId + "_" + postId;
        GetByIdExtendedResponse resp = apiClient.wall().getByIdExtended(userActor, str).copyHistoryDepth(2).execute();
        return resp.getItems().get(0);
    }

    private void repost(WallPostFull post, int groupId2) throws Exception {
        int groupId1 = post.getOwnerId(), postId = post.getId();
        String str = "wall" + groupId1 + "_" + postId;

        String str1 = "likes: " + post.getLikes().getCount();
        String str2 = "comments: " + post.getComments().getCount();
        String str3 = "reposts: " + post.getReposts().getCount();
        int sum = post.getLikes().getCount() + post.getComments().getCount() + post.getReposts().getCount();
        String str4 = "sum: " + sum;
        //String strFinal = str1 + "\n" + str2 + "\n" + str3 + "\n" + str4;
        String strFinal = str1 + ", " + str2 + ", " + str3 + ", " + str4;

        RepostResponse resp = apiClient.wall().repost(userActor, str).message(strFinal).groupId(abs(groupId2)).execute();
    }

    public void repostList(List<WallPostFull> list, int groupId2) throws Exception {
        for (WallPostFull p : list) {
            repost(p, groupId2);
            Thread.sleep(cycleDelay);
        }
    }

    private static int getPostMetric(WallPostFull post) {
        return post.getLikes().getCount() + post.getComments().getCount() + post.getReposts().getCount();
    }

    public static void sortPost(List<WallPostFull> list) {
        Collections.sort(list, new Comparator<WallPostFull>() {
            @Override
            public int compare(WallPostFull post1, WallPostFull post2) {
                return getPostMetric(post2) - getPostMetric(post1);
            }
        });
    }
}

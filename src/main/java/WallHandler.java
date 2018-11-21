import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.objects.base.responses.OkResponse;
import com.vk.api.sdk.objects.photos.PhotoFull;
import com.vk.api.sdk.objects.wall.WallPostFull;
import com.vk.api.sdk.objects.wall.responses.GetByIdExtendedResponse;
import com.vk.api.sdk.objects.wall.responses.GetExtendedResponse;
import com.vk.api.sdk.objects.wall.responses.GetResponse;
import com.vk.api.sdk.objects.wall.responses.RepostResponse;
import com.vk.api.sdk.queries.wall.WallGetFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class WallHandler {
    private VkApiClient apiClient;
    private UserActor userActor;
    private static final int maxPostNum = 200;

    WallHandler(VkApiClient apiClient, UserActor userActor) {
        this.apiClient = apiClient;
        this.userActor = userActor;
    }

    private int abs(int n) {
        return Math.abs(n);
    }

    private int divCeil(int x1, int x2) {
        double div = ((double)x1) / ((double)x2);
        return (int)(Math.ceil(div));
    }

    private void delete(WallPostFull post) throws Exception {
        OkResponse ok = apiClient.wall().delete(userActor).ownerId(post.getOwnerId()).postId(post.getId()).execute();
    }

    public void clearWall(int ownerId) throws Exception {
        List<WallPostFull> list = getExtended(ownerId);
        if ((list == null) || list.isEmpty()) {
            return;
        }
        for (WallPostFull p : list) {
            delete(p);
            Thread.sleep(300);
        }
    }

    public List<WallPostFull> getExtended(int ownerId) throws Exception {
        int offsetPost = 0, countPost = 100;
        int numIter = divCeil(maxPostNum, countPost), countIter = 0;
        List<WallPostFull> list1 = new ArrayList<>();
        while (true) {
            GetExtendedResponse resp = apiClient.
                                          wall().
                          getExtended(userActor).
                                ownerId(ownerId).
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
            Thread.sleep(200);
        }
        return list1;
    }

    public List<WallPostFull> getAllSorted(int ownerId) throws Exception {
        List<WallPostFull> list = getExtended(ownerId);
        sortPost(list);
        return list;
    }

    private WallPostFull getByIdExtended(int ownerId, int postId) throws Exception {
        String str = ownerId + "_" + postId;
        GetByIdExtendedResponse resp = apiClient.wall().getByIdExtended(userActor, str).copyHistoryDepth(2).execute();
        return resp.getItems().get(0);
    }

    private void repost(WallPostFull post, int ownerId2) throws Exception {
        int ownerId1 = post.getOwnerId(), postId = post.getId();
        String str = "wall" + ownerId1 + "_" + postId;

        String str1 = "Likes: " + post.getLikes().getCount();
        String str2 = "Comments: " + post.getComments().getCount();
        String str3 = "Reposts: " + post.getReposts().getCount();
        int sum = post.getLikes().getCount() + post.getComments().getCount() + post.getReposts().getCount();
        String str4 = "Sum: " + sum;
        String strFinal = str1 + "\n" + str2 + "\n" + str3 + "\n" + str4;

        RepostResponse resp = apiClient.wall().repost(userActor, str).message(strFinal).groupId(abs(ownerId2)).execute();
    }

    public void repostList(List<WallPostFull> list, int ownerId2) throws Exception {
        for (WallPostFull p : list) {
            repost(p, ownerId2);
            Thread.sleep(200);
        }
    }

    private int getPostMetric(WallPostFull post) {
        return post.getLikes().getCount() + post.getComments().getCount() + post.getReposts().getCount();
    }

    public void sortPost(List<WallPostFull> list) {
        Collections.sort(list, new Comparator<WallPostFull>() {
            @Override
            public int compare(WallPostFull post1, WallPostFull post2) {
                return getPostMetric(post2) - getPostMetric(post1);
            }
        });
    }

    public void clear() throws Exception {
        /*
        GetResponse gr11 = apiClient.wall().get(userActor).ownerId(-170303225).count(20).execute();
        List<WallPostFull> wlist = gr11.getItems();
        for (WallPostFull x : wlist) {
            OkResponse ok1 = apiClient.wall().delete(userActor).ownerId(-170303225).postId(x.getId()).execute();
            Thread.sleep(500);
        }
        */
        clearWall(-170303225);
    }

    public void getNum()  throws Exception {
        int pid = -26493942;
        int mypid = -170303225;
        int pNum = 4;
        GetExtendedResponse resp1 = apiClient.wall().getExtended(userActor).ownerId(pid).offset(0).count(pNum).execute();
        List<WallPostFull> list1 = resp1.getItems();
        List<WallPostFull> list2 = new ArrayList<>(list1);

        Collections.sort(list1, new Comparator<WallPostFull>() {
            @Override
            public int compare(WallPostFull o1, WallPostFull o2) {
                return o2.getLikes().getCount() - o1.getLikes().getCount();
            }
        });

        Collections.sort(list2, new Comparator<WallPostFull>() {
            @Override
            public int compare(WallPostFull o1, WallPostFull o2) {
                return o2.getComments().getCount() - o1.getComments().getCount();
            }
        });


        for (WallPostFull x : list1) {
            String str = "wall" + x.getOwnerId() + "_" + x.getId();
            apiClient.wall().repost(userActor, str).message("Repost").groupId(-mypid).execute();
            Thread.sleep(600);
        }

        clear();

        int x = 3;
    }
}

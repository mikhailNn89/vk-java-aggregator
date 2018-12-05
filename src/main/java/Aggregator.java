import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.objects.photos.Photo;
import com.vk.api.sdk.objects.photos.PhotoAlbumFull;
import com.vk.api.sdk.objects.photos.PhotoFull;
import com.vk.api.sdk.objects.photos.PhotoUpload;
import com.vk.api.sdk.objects.utils.DomainResolved;
import com.vk.api.sdk.objects.wall.WallPostFull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class Aggregator {
    private VkApiClient apiClient;
    private UserActor userActor;
    private PhotoHandler photoHandler;
    private WallHandler wallHandler;
    private LinkHandler linkHandler;
    private int publicId;
    private int numAggrPostPerGroup;
    private int numAggrPhotoPerGroup;
    private int groupsVar;
    private final static long cycleDelay = 250;
    private final static long intermDelay = 100;

    Aggregator(VkApiClient apiClient,
               UserActor userActor,
               Properties properties) {
        this.apiClient = apiClient;
        this.userActor = userActor;
        this.photoHandler = new PhotoHandler(apiClient,
                                             userActor,
                                             getIntProperty(properties, "maxNumPhoto1"),
                                             getIntProperty(properties, "maxNumPhoto2"),
                                             getIntProperty(properties, "maxNumAlbum"));
        this.wallHandler = new WallHandler(apiClient,
                                           userActor,
                                           getIntProperty(properties, "maxNumPost"));
        this.linkHandler = new LinkHandler(apiClient, userActor);
        this.groupsVar = 1;
        this.publicId = getIntProperty(properties, "publicId");
        this.numAggrPostPerGroup = getIntProperty(properties, "numAggrPostPerGroup");
        this.numAggrPhotoPerGroup = getIntProperty(properties, "numAggrPhotoPerGroup");
    }

    private static int getIntProperty(Properties properties, String name) {
        return Integer.parseInt(properties.getProperty(name));
    }

    public void setGroupsVar(int groupsVar) {
        this.groupsVar = groupsVar;
    }

    private int resolveScreenName(String name) throws Exception {
        DomainResolved res = apiClient.utils().resolveScreenName(userActor, name).execute();
        int id = res.getObjectId();
        if (res.getType().getValue().equalsIgnoreCase("group")) {
            id = -id;
        }
        return id;
    }

    private static String getGroupName(String url) {
        String[] split = url.split("/");
        return split[split.length-1];
    }

    private static String[] diffSplit(String name) {
        List<String> diff = new ArrayList<>();
        diff.add("public");
        diff.add("club");
        diff.add("group");
        diff.add("event");
        String[] split = null;
        for (String s : diff) {
            split = name.split(s);
            if (split.length == 2) {
                return split;
            }
        }
        return split;
    }

    private int getGroupId(String url) throws Exception {
        String name = getGroupName(url);
        String[] split = diffSplit(name);
        if (split.length == 1) {
            return resolveScreenName(name);
        }
        else if (split.length == 2) {
            int id = Integer.parseInt(split[split.length-1]);
            if (id > 0) {
                return -id;
            }
            else {
                return id;
            }
        }
        return 0;
    }

    private List<Integer> getGroupIdList(List<String> list) throws Exception {
        List<Integer> list1 = new ArrayList<>();
        for (String s : list) {
            list1.add(getGroupId(s));
            Thread.sleep(cycleDelay);
        }
        return list1;
    }

    private PhotoAlbumFull getAlbumByTitle(int publicIdCurr, String title) throws Exception {
        List<PhotoAlbumFull> list = photoHandler.getAlbums(publicIdCurr);
        for (PhotoAlbumFull a : list) {
            if (a.getTitle().compareToIgnoreCase(title) == 0) {
                return a;
            }
        }
        return null;
    }

    private PhotoAlbumFull getMainAlbum(int publicIdCurr) throws Exception {
        return getAlbumByTitle(publicIdCurr, "Для загрузки");
    }

    private PhotoAlbumFull getDelAlbum(int publicIdCurr) throws Exception {
        return getAlbumByTitle(publicIdCurr, "Удаленные");
    }

    private void checkAlbum(int publicIdCurr, String title) throws Exception {
        PhotoAlbumFull album = getAlbumByTitle(publicIdCurr, title);
        Thread.sleep(intermDelay);
        if (album == null) {
            photoHandler.createAlbum(title, publicIdCurr);
        }
    }

    private void checkAlbums(int publicIdCurr) throws Exception {
        checkAlbum(publicIdCurr, "Основной альбом");
        Thread.sleep(intermDelay);
        checkAlbum(publicIdCurr, "Для загрузки");
        Thread.sleep(intermDelay);
        checkAlbum(publicIdCurr, "Удаленные");
    }

    private List<PhotoFull> preparePhoto(List<Integer> idList) throws Exception {
        List<PhotoFull> list1 = new ArrayList<>(), list2;
        for (Integer i : idList) {
            list2 = photoHandler.getAllSorted(i);
            if (list2.size() > numAggrPhotoPerGroup) {
                list1.addAll(list2.subList(0,numAggrPhotoPerGroup));
            }
            else {
                list1.addAll(list2);
            }
            Thread.sleep(cycleDelay);
        }
        PhotoHandler.sortPhoto(list1);
        return list1;
    }

    private List<WallPostFull> preparePost(List<Integer> idList) throws Exception {
        List<WallPostFull> list1 = new ArrayList<>(), list2;
        for (Integer i : idList) {
            list2 = wallHandler.getAllSorted(i);
            if (list2.size() > numAggrPostPerGroup) {
                list1.addAll(list2.subList(0,numAggrPostPerGroup));
            }
            else {
                list1.addAll(list2);
            }
            Thread.sleep(cycleDelay);
        }
        WallHandler.sortPost(list1);
        return list1;
    }

    private List<String> getUrlList() {
        List<String> urlList = new ArrayList<>();
        if (groupsVar == 1) {
            urlList.add("https://vk.com/novgorod_52");
            urlList.add("https://vk.com/club134454330");
            urlList.add("https://vk.com/public79858706");
        }
        else if (groupsVar == 2) {
            urlList.add("https://vk.com/typical_nn");
            urlList.add("https://vk.com/nndaytoday");
        }
        else {
            urlList.add("https://vk.com/interestingnn");
            urlList.add("https://vk.com/newsnnru");
        }
        return urlList;
    }

    private List<Integer> getIdList() throws Exception {
        List<String> urlList = getUrlList();
        return getGroupIdList(urlList);
    }

    public void fillWall() {
        try {
            wallHandler.clearWall(publicId);
            Thread.sleep(intermDelay);
            List<Integer> idList = getIdList();
            Thread.sleep(intermDelay);
            List<WallPostFull> list2 = preparePost(idList);
            Collections.reverse(list2);
            Thread.sleep(cycleDelay);
            wallHandler.repostList(list2, publicId);
        } catch (Exception e) {
            ;
        }
    }

    public void fillPhoto() {
        try {
            checkAlbums(publicId);
            Thread.sleep(intermDelay);
            photoHandler.deleteAllPhoto(publicId);
            Thread.sleep(intermDelay);
            List<Integer> idList = getIdList();
            Thread.sleep(intermDelay);

            List<PhotoAlbumFull> albumList = new ArrayList<>();
            albumList.add(getMainAlbum(publicId));
            Thread.sleep(intermDelay);
            albumList.add(getDelAlbum(publicId));
            Thread.sleep(intermDelay);
            int albumId = albumList.get(0).getId();
            PhotoUpload photoUpload = photoHandler.getUploadServer(publicId, albumId);
            Thread.sleep(cycleDelay);

            List<PhotoFull> list1 = preparePhoto(idList);
            Collections.reverse(list1);
            Thread.sleep(cycleDelay);
            List<Photo> lp = photoHandler.uploadPhotoList(list1, albumList.get(0), photoUpload, list1.get(list1.size()-1));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void fillLinks() {
        try {
            linkHandler.deleteAllLinks(publicId);
            Thread.sleep(intermDelay);
            List<String> urlList = getUrlList();
            Thread.sleep(intermDelay);
            linkHandler.addLinkList(publicId, urlList);
        } catch (Exception e) {
            ;
        }
    }

    public void clearWall() {
        try {
            wallHandler.clearWall(publicId);
        } catch (Exception e) {
            ;
        }
    }

    public void clearPhoto() {
        try {
            photoHandler.deleteAllPhoto(publicId);
        } catch (Exception e) {
            ;
        }
    }

    public void clearLinks() {
        try {
            linkHandler.deleteAllLinks(publicId);
        } catch (Exception e) {
            ;
        }
    }
}

import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.objects.groups.GroupLink;
import com.vk.api.sdk.objects.groups.LinksItem;
import com.vk.api.sdk.objects.photos.Photo;
import com.vk.api.sdk.objects.photos.PhotoAlbumFull;
import com.vk.api.sdk.objects.photos.PhotoFull;
import com.vk.api.sdk.objects.photos.PhotoUpload;
import com.vk.api.sdk.objects.utils.DomainResolved;
import com.vk.api.sdk.objects.wall.WallPostFull;
import org.apache.commons.collections4.IterableGet;
import org.omg.CORBA.INTERNAL;
import org.omg.PortableInterceptor.INACTIVE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Aggregator {
    private VkApiClient apiClient;
    private UserActor userActor;
    private PhotoHandler photoHandler;
    private WallHandler wallHandler;
    private LinkHandler linkHandler;
    private final static int myGroupId = -170303225;
    private final static int numPostPerGroup = 3;
    private final static int numPhotoPerGroup = 7;

    Aggregator(VkApiClient apiClient, UserActor userActor) {
        this.apiClient = apiClient;
        this.userActor = userActor;
        this.photoHandler = new PhotoHandler(apiClient, userActor);
        this.wallHandler = new WallHandler(apiClient, userActor);
        this.linkHandler = new LinkHandler(apiClient, userActor);
    }

    private int resolveScreenName(String name) throws Exception {
        DomainResolved res = apiClient.utils().resolveScreenName(userActor, name).execute();
        int id = res.getObjectId();
        if (res.getType().getValue().equalsIgnoreCase("group")) {
            id = -id;
        }
        return id;
    }

    private String getGroupName(String url) {
        String[] split = url.split("/");
        return split[split.length-1];
    }

    private int getGroupId(String url) throws Exception {
        String name = getGroupName(url);
        String[] split = name.split("public");
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
            Thread.sleep(400);
        }
        return list1;
    }

    private PhotoAlbumFull getAlbumByTitle(int myGroupId, String title) throws Exception {
        List<PhotoAlbumFull> list = photoHandler.getAlbums(myGroupId);
        for (PhotoAlbumFull a : list) {
            if (a.getTitle().compareToIgnoreCase(title) == 0) {
                return a;
            }
        }
        return null;
    }

    private PhotoAlbumFull getMainAlbum(int myGroupId) throws Exception {
        return getAlbumByTitle(myGroupId, "Для загрузки");
    }

    private PhotoAlbumFull getDelAlbum(int myGroupId) throws Exception {
        return getAlbumByTitle(myGroupId, "Удаленные");
    }

    private List<PhotoFull> preparePhoto(List<Integer> idList) throws Exception {
        List<PhotoFull> list1 = new ArrayList<>(), list2;
        for (Integer i : idList) {
            list2 = photoHandler.getAllSorted(i);
            if (list2.size() > numPhotoPerGroup) {
                list1.addAll(list2.subList(0,numPhotoPerGroup));
            }
            else {
                list1.addAll(list2);
            }
        }
        photoHandler.sortPhoto(list1);
        return list1;
    }

    private List<WallPostFull> preparePost(List<Integer> idList) throws Exception {
        List<WallPostFull> list1 = new ArrayList<>(), list2;
        for (Integer i : idList) {
            list2 = wallHandler.getAllSorted(i);
            if (list2.size() > numPostPerGroup) {
                list1.addAll(list2.subList(0,numPostPerGroup));
            }
            else {
                list1.addAll(list2);
            }
        }
        wallHandler.sortPost(list1);
        return list1;
    }

    private List<Integer> getIdList() throws Exception {
        List<String> urlList = new ArrayList<>();
        urlList.add("https://vk.com/public79858706");
        urlList.add("https://vk.com/typical_nn");
        urlList.add("https://vk.com/overhearnn");
        Thread.sleep(300);
        return getGroupIdList(urlList);
    }

    public void fillWall() {
        try {
            wallHandler.clearWall(myGroupId);
            Thread.sleep(100);
            List<Integer> idList = getIdList();
            Thread.sleep(100);
            List<WallPostFull> list2 = preparePost(idList);
            Collections.reverse(list2);
            Thread.sleep(100);
            wallHandler.repostList(list2, myGroupId);
            Thread.sleep(100);
        } catch (Exception e) {
            ;
        }
    }

    public void fillPhoto() {
        try {
            photoHandler.deleteAllPhoto2(myGroupId);
            Thread.sleep(100);
            List<Integer> idList = getIdList();
            Thread.sleep(100);

            List<PhotoAlbumFull> albumList = new ArrayList<>();
            albumList.add(getMainAlbum(myGroupId));
            Thread.sleep(100);
            albumList.add(getDelAlbum(myGroupId));
            int albumId = albumList.get(0).getId();
            PhotoUpload photoUpload = photoHandler.getUploadServer(myGroupId, albumId);
            Thread.sleep(100);

            List<PhotoFull> list1 = preparePhoto(idList);
            Thread.sleep(100);
            Collections.reverse(list1);

            Thread.sleep(100);
            List<Photo> lp = photoHandler.uploadPhotoList(list1, albumList.get(0), photoUpload, list1.get(list1.size()-1));
            Thread.sleep(100);
        } catch (Exception e) {
            ;
        }
    }

    public void fillLinks() {
        try {
            linkHandler.deleteAllLinks(myGroupId);
            Thread.sleep(100);

            List<String> urlList = new ArrayList<>();
            urlList.add("https://vk.com/public79858706");
            urlList.add("https://vk.com/typical_nn");
            urlList.add("https://vk.com/overhearnn");
            Thread.sleep(300);
            List<Integer> idList = getGroupIdList(urlList);
            Thread.sleep(100);
            linkHandler.addLinkList(myGroupId, urlList);
            Thread.sleep(100);
        } catch (Exception e) {
            ;
        }
    }

    public void clearWall() {
        try {
            wallHandler.clearWall(myGroupId);
            Thread.sleep(100);
        } catch (Exception e) {
            ;
        }
    }

    public void clearPhoto() {
        try {
            photoHandler.deleteAllPhoto2(myGroupId);
            Thread.sleep(100);
        } catch (Exception e) {
            ;
        }
    }

    public void clearLinks() {
        try {
            linkHandler.deleteAllLinks(myGroupId);
            Thread.sleep(100);
        } catch (Exception e) {
            ;
        }
    }

    public void aggregateContent() throws Exception {
        photoHandler.deleteAllPhoto2(myGroupId);
        Thread.sleep(100);
        wallHandler.clearWall(myGroupId);
        Thread.sleep(100);
        linkHandler.deleteAllLinks(myGroupId);

        List<PhotoAlbumFull> albumList = new ArrayList<>();
        albumList.add(getMainAlbum(myGroupId));
        Thread.sleep(100);
        albumList.add(getDelAlbum(myGroupId));
        int albumId = albumList.get(0).getId();
        PhotoUpload photoUpload = photoHandler.getUploadServer(myGroupId, albumId);
        Thread.sleep(300);

        List<String> urlList = new ArrayList<>();
        urlList.add("https://vk.com/public79858706");
        urlList.add("https://vk.com/typical_nn");
        urlList.add("https://vk.com/overhearnn");
        Thread.sleep(300);
        List<Integer> idList = getGroupIdList(urlList);
        Thread.sleep(100);
        linkHandler.addLinkList(myGroupId, urlList);
        Thread.sleep(100);

        List<PhotoFull> list1 = preparePhoto(idList);
        Thread.sleep(100);
        List<WallPostFull> list2 = preparePost(idList);

        Collections.reverse(list1);
        Collections.reverse(list2);

        Thread.sleep(100);
        List<Photo> lp = photoHandler.uploadPhotoList(list1, albumList.get(0), photoUpload, list1.get(list1.size()-1));
        Thread.sleep(100);
        wallHandler.repostList(list2, myGroupId);
    }

    public void aggregateGroups2() throws Exception {

        //int id = -26493942;
        int id = -25714310;
        List<PhotoAlbumFull> albumList = new ArrayList<>();
        albumList.add(getMainAlbum(myGroupId));
        albumList.add(getDelAlbum(myGroupId));
        int albumId = albumList.get(0).getId();
        PhotoUpload photoUpload = photoHandler.getUploadServer(myGroupId, albumId);

        photoHandler.deleteAlbumPhoto(albumList.get(0));
        Thread.sleep(500);

        List<PhotoFull> photoList3 = photoHandler.getAllSorted(id);
        List<PhotoFull> photoList4 = photoList3.subList(0,10);
        List<Photo> lp = photoHandler.uploadPhotoList(photoList4, albumList.get(0), photoUpload, photoList4.get(photoList4.size()-1));
        photoHandler.movePhotoList(lp, albumList.get(1));

        List<WallPostFull> list33 = wallHandler.getExtended(id);
        List<WallPostFull> list44 = list33.subList(0,1);
        wallHandler.repostList(list44, myGroupId);
    }
}

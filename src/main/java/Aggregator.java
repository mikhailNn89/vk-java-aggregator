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

public class Aggregator {
    private VkApiClient apiClient;
    private UserActor userActor;
    private PhotoHandler photoHandler;
    private WallHandler wallHandler;
    private final static int myGroupId = -170303225;

    Aggregator(VkApiClient apiClient, UserActor userActor) {
        this.apiClient = apiClient;
        this.userActor = userActor;
        this.photoHandler = new PhotoHandler(apiClient, userActor);
        this.wallHandler = new WallHandler(apiClient, userActor);
    }

    private int resolveScreenName(String name) throws Exception {
        DomainResolved res = apiClient.utils().resolveScreenName(userActor, name).execute();
        int id = res.getObjectId();
        if (res.getType().getValue().equalsIgnoreCase("group")) {
            id = -id;
        }
        return id;
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

    public void aggregateGroups() throws Exception {
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
        Collections.reverse(photoList4);

        List<Photo> lp = photoHandler.uploadPhotoList(photoList4, albumList.get(0), photoUpload, photoList4.get(photoList4.size()-1));
        photoHandler.movePhotoList(lp, albumList.get(1));
        photoHandler.deleteAllPhoto(myGroupId);



        wallHandler.clearWall(myGroupId);
        List<WallPostFull> list33 = wallHandler.getExtended2(id);
        List<WallPostFull> list44 = list33.subList(0,1);
        Collections.reverse(list44);

        wallHandler.repostList(list44, myGroupId);
        wallHandler.clearWall(myGroupId);

    }

}

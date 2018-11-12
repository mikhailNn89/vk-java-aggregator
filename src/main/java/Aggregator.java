import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.objects.photos.PhotoAlbumFull;
import com.vk.api.sdk.objects.photos.PhotoFull;
import com.vk.api.sdk.objects.photos.PhotoUpload;
import com.vk.api.sdk.objects.utils.DomainResolved;

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

    public void aggregateGroups() throws Exception {
        int id = -26493942;
        int count = 0;

        photoHandler.deleteAllPhoto(myGroupId);
        wallHandler.clearWall(myGroupId);

        List<PhotoAlbumFull> albumList = photoHandler.getAlbums(myGroupId);
        int albumId = albumList.get(0).getId();
        PhotoUpload photoUpload = photoHandler.getUploadServer(myGroupId, albumId);

        List<PhotoFull> photoList1 = photoHandler.getAllSorted(id);
        List<PhotoFull> photoList2 = photoList1.subList(0,1);
        photoHandler.sortPhotoRev(photoList2);
        photoHandler.uploadPhotoList(photoList2, myGroupId, albumId, photoUpload);

        photoHandler.deleteAllPhoto(myGroupId);

        int y = 7;
    }

}

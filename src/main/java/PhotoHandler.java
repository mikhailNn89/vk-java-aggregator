import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.objects.base.responses.OkResponse;
import com.vk.api.sdk.objects.photos.*;
import com.vk.api.sdk.objects.photos.responses.GetAlbumsResponse;
import com.vk.api.sdk.objects.photos.responses.GetAllExtendedResponse;
import com.vk.api.sdk.objects.photos.responses.GetExtendedResponse;
import com.vk.api.sdk.objects.photos.responses.PhotoUploadResponse;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PhotoHandler {
    private VkApiClient apiClient;
    private UserActor userActor;

    PhotoHandler(VkApiClient apiClient, UserActor userActor) {
        this.apiClient = apiClient;
        this.userActor = userActor;
    }

    private int abs(int n) {
        return Math.abs(n);
    }

    private void createAlbum() {};

    private void delete(int ownerId, int photoId) throws Exception {
        OkResponse ok = apiClient.photos().delete(userActor, photoId).ownerId(ownerId).execute();
    }

    private List<PhotoFull> getExtended(int ownerId, String albumId) throws Exception {
        GetExtendedResponse resp = apiClient.photos().getExtended(userActor).ownerId(ownerId).albumId(albumId).rev(true).photoSizes(true).offset(0).count(1000).execute();
        return resp.getItems();
    }

    public List<PhotoAlbumFull> getAlbums(int ownerId) throws Exception {
        GetAlbumsResponse resp = apiClient.photos().getAlbums(userActor).ownerId(ownerId).offset(0).needCovers(true).photoSizes(true).execute();
        return resp.getItems();
    }

    private int getAlbumsCount(int ownerId) throws Exception {
        return apiClient.photos().getAlbumsCount(userActor).groupId(abs(ownerId)).execute();
    }

    private List<PhotoFull> getAllExtended(int ownerId) throws Exception {
        GetAllExtendedResponse resp = apiClient.photos().getAllExtended(userActor).ownerId(ownerId).offset(0).count(200).photoSizes(true).noServiceAlbums(true).execute();
        List<PhotoFullXtrRealOffset> list1 = resp.getItems();
        List<PhotoFull> list2 = new ArrayList<>();
        for (PhotoFullXtrRealOffset p : list1) {
            list2.add(getByIdExtended(p.getOwnerId(), p.getId()));
            Thread.sleep(500);
        }
        return list2;
    }

    private List<PhotoFull> getAllExtended2(int ownerId) throws Exception {
        List<PhotoAlbumFull> list1 = getAlbums(ownerId);
        List<PhotoFull> list2 = new ArrayList<>();
        for (PhotoAlbumFull a : list1) {
            list2.addAll(getExtended(ownerId, a.getId().toString()));
        }
        return list2;
    }

    public List<PhotoFull> getAllSorted(int ownerId) throws Exception {
        List<PhotoFull> list = getAllExtended2(ownerId);
        sortPhoto(list);
        return list;
    }

    public List<PhotoFull> uploadPhotoList(List<PhotoFull> list1, int ownerId, int albumId, PhotoUpload photoUpload) throws Exception {
        List<PhotoFull> list2 = new ArrayList<>();
        for (PhotoFull p : list1) {
            list2.add(uploadPhoto(p, ownerId, albumId, photoUpload));
            Thread.sleep(500);
        }
        return list2;
    }

    private PhotoFull getByIdExtended(int ownerId, int photoId) throws Exception {
        String str = ownerId + "_" + photoId;
        List<PhotoFull> list = apiClient.photos().getByIdExtended(userActor, str).photoSizes(true).execute();
        return list.get(0);
    }

    public PhotoUpload getUploadServer(int ownerId, int albumId) throws Exception {
        return apiClient.photos().getUploadServer(userActor).groupId(abs(ownerId)).albumId(albumId).execute();
    }

    private void makeCover(int ownerId, int albumId, int photoId) throws Exception {
        OkResponse ok = apiClient.photos().makeCover(userActor, photoId).ownerId(ownerId).albumId(albumId).execute();
    }

    private PhotoUploadResponse upload(File file, String url) throws Exception {
        return apiClient.upload().photo(url, file).execute();
    }

    private PhotoFull save(int ownerId, int albumId, int server, String photosList, String hash, String caption) throws Exception {
        List<Photo> list1 = apiClient.photos().save(userActor).groupId(abs(ownerId)).albumId(albumId).server(server).photosList(photosList).hash(hash).caption(caption).execute();
        List<PhotoFull> list2 = new ArrayList<>();
        for (Photo p : list1) {
            list2.add(getByIdExtended(p.getOwnerId(), p.getId()));
        }
        return list2.get(0);
    }

    private void downloadFile(String name, String url) throws Exception {
        BufferedInputStream inputStream = new BufferedInputStream(new URL(url).openStream());
        FileOutputStream fileOutputStream = new FileOutputStream(name);
        byte dataBuffer[] = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(dataBuffer, 0, 1024)) != -1) {
            fileOutputStream.write(dataBuffer, 0, bytesRead);
        }
        inputStream.close();
        fileOutputStream.close();
    }

    private PhotoFull uploadPhoto(PhotoFull photo, int ownerId, int albumId, PhotoUpload photoUpload) throws Exception {
        int len = photo.getSizes().size();
        String photoUrl = photo.getSizes().get(len-1).getSrc();
        String[] split = photoUrl.split("/");
        String photoName = split[split.length-1];
        downloadFile(photoName, photoUrl);

        File file = new File(photoName);
        PhotoUploadResponse resp = upload(file, photoUpload.getUploadUrl());
        boolean del = file.delete();

        String str1 = "Likes: " + photo.getLikes().getCount();
        String str2 = "Comments: " + photo.getComments().getCount();
        String str3 = "Reposts: " + photo.getReposts().getCount();
        int sum = photo.getLikes().getCount() + photo.getComments().getCount() + photo.getReposts().getCount();
        String str4 = "Sum: " + sum;
        String strFinal = str1 + "\n" + str2 + "\n" + str3 + "\n" + str4;

        return save(ownerId, albumId, resp.getServer(), resp.getPhotosList(), resp.getHash(), strFinal);
    }

    private int getPhotoMetric(PhotoFull photo) {
        return photo.getLikes().getCount() + photo.getComments().getCount() + photo.getReposts().getCount();
    }

    public void sortPhoto(List<PhotoFull> list) {
        Collections.sort(list, new Comparator<PhotoFull>() {
            @Override
            public int compare(PhotoFull photo1, PhotoFull photo2) {
                return getPhotoMetric(photo2) - getPhotoMetric(photo1);
            }
        });
    }

    public void sortPhotoRev(List<PhotoFull> list) {
        Collections.sort(list, new Comparator<PhotoFull>() {
            @Override
            public int compare(PhotoFull photo1, PhotoFull photo2) {
                return getPhotoMetric(photo1) - getPhotoMetric(photo2);
            }
        });
    }

    public void deleteAllPhoto(int ownerId) throws Exception {
        List<PhotoAlbumFull> list1 = getAlbums(ownerId);
        List<PhotoFull> list2;
        for (PhotoAlbumFull a : list1) {
            list2 = getExtended(ownerId, a.getId().toString());
            for (PhotoFull p : list2) {
                delete(ownerId, p.getId());
            }
        }
    }





    public void photo2() throws Exception {
        PhotoFull photo = getByIdExtended(-16108331, 412045111);
        PhotoUpload photoUpload = getUploadServer(170303225, 256293027);
        uploadPhoto(photo, 170303225, 256293027, photoUpload);
    }

    public void photo3() throws Exception {
        int pid = -26493942;

        List<PhotoAlbumFull> list1 = getAlbums(pid);
        int count = getAlbumsCount(pid);
        List<PhotoFull> list2 = getAllExtended(pid);
        sortPhoto(list2);

        int u = 23;
    }


    public void photo1() throws Exception {
        List<PhotoFull> lph = apiClient.photos().getByIdExtended(userActor, "-16108331_412045111").photoSizes(true).execute();
        int len = lph.get(0).getSizes().size();
        String str2 = lph.get(0).getSizes().get(len-1).getSrc();

        String[] strsp = str2.split("/");
        String fileName2 = strsp[strsp.length - 1];

        BufferedInputStream in = new BufferedInputStream(new URL(str2).openStream());
        FileOutputStream fileOutputStream = new FileOutputStream(fileName2);
        byte dataBuffer[] = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
            fileOutputStream.write(dataBuffer, 0, bytesRead);
        }
        in.close();
        fileOutputStream.close();

        File f1 = new File(fileName2);
        PhotoUpload pup1 = apiClient.photos().getUploadServer(userActor).albumId(256293027).groupId(170303225).execute();

        //for (int i = 0; i < 1; i++) {
            PhotoUploadResponse resp999 = apiClient.upload().photo(pup1.getUploadUrl(), f1).execute();
            List<Photo> list = apiClient.photos().save(userActor).albumId(256293027).groupId(170303225).server(resp999.getServer()).photosList(resp999.getPhotosList()).hash(resp999.getHash()).caption("Now").execute();
        //}
        boolean del = f1.delete();
    }

    public void calcNum() throws Exception {
        int pid = -26493942;
        GetAlbumsResponse resp1 = apiClient.photos().getAlbums(userActor).ownerId(pid).execute();
        List<PhotoAlbumFull> list1 = resp1.getItems();
        int albNum = apiClient.photos().getAlbumsCount(userActor).groupId(-pid).execute();

        // All photo info
        List<GetExtendedResponse> list2 = new ArrayList<>();
        for (PhotoAlbumFull x : list1) {
            list2.add(apiClient.photos().getExtended(userActor).ownerId(pid).albumId(x.getId().toString()).photoSizes(true).count(1000).execute());
        }

        //GetExtendedResponse r; r.

        // Info w/o comments
        GetAllExtendedResponse resp2 = apiClient.photos().getAllExtended(userActor).ownerId(pid).count(200).photoSizes(true).noServiceAlbums(true).execute();
        List<PhotoFullXtrRealOffset> list3 = resp2.getItems();

        Collections.sort(list3, new Comparator<PhotoFullXtrRealOffset>() {
            @Override
            public int compare(PhotoFullXtrRealOffset o1, PhotoFullXtrRealOffset o2) {
                return o2.getLikes().getCount() - o1.getLikes().getCount();
            }
        });

        List<PhotoFullXtrRealOffset> list4 = new ArrayList<>(list3);
        Collections.sort(list4, new Comparator<PhotoFullXtrRealOffset>() {
            @Override
            public int compare(PhotoFullXtrRealOffset o1, PhotoFullXtrRealOffset o2) {
                return o2.getReposts().getCount() - o1.getReposts().getCount();
            }
        });
    }

    public void deleteAllPhoto2() throws Exception {
        int mypid = -170303225;
        GetAlbumsResponse resp1 = apiClient.photos().getAlbums(userActor).ownerId(mypid).execute();
        List<PhotoAlbumFull> list1 = resp1.getItems();
        int albNum = apiClient.photos().getAlbumsCount(userActor).groupId(-mypid).execute();

        GetExtendedResponse resp;
        List<PhotoFull> list2;
        for (PhotoAlbumFull x : list1) {
            resp = apiClient.photos().getExtended(userActor).ownerId(mypid).albumId(x.getId().toString()).photoSizes(true).count(1000).execute();
            list2 = resp.getItems();
            for (PhotoFull y : list2) {
                OkResponse okr = apiClient.photos().delete(userActor, y.getId()).ownerId(y.getOwnerId()).execute();
            }
        }

        //int w = -27532693;
        //com.vk.api.sdk.objects.video.responses.GetAlbumsResponse resp33 = apiClient.videos().getAlbums(userActor).ownerId(w).count(100).offset(0).execute();
        //com.vk.api.sdk.objects.video.responses.GetExtendedResponse resp44 = apiClient.videos().getExtended(userActor).ownerId(w).albumId(resp33.getItems().get(0).getId()).count(100).offset(0).execute();
    }

}

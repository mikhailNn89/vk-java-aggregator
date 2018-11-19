import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.objects.base.responses.OkResponse;
import com.vk.api.sdk.objects.photos.*;
import com.vk.api.sdk.objects.photos.responses.GetAlbumsResponse;
import com.vk.api.sdk.objects.photos.responses.GetAllExtendedResponse;
import com.vk.api.sdk.objects.photos.responses.GetExtendedResponse;
import com.vk.api.sdk.objects.photos.responses.PhotoUploadResponse;
import org.apache.commons.io.FileUtils;

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
    private static final int maxPhotoNum = 20000;
    private static final int maxAlbumNum = 50;

    PhotoHandler(VkApiClient apiClient, UserActor userActor) {
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

    private void delete(PhotoFull photo) throws Exception {
        OkResponse ok = apiClient.photos().delete(userActor, photo.getId()).ownerId(photo.getOwnerId()).execute();
    }

    private void makeCover(int ownerId, int albumId, int photoId) throws Exception {
        OkResponse ok = apiClient.photos().makeCover(userActor, photoId).ownerId(ownerId).albumId(albumId).execute();
    }

    public void movePhotoList(List<Photo> list, PhotoAlbumFull album) throws Exception {
        for (Photo p : list) {
            OkResponse ok = apiClient.photos().move(userActor, album.getId(), p.getId()).ownerId(p.getOwnerId()).execute();
            Thread.sleep(250);
        }
    }

    private List<PhotoFull> getExtended(PhotoAlbumFull album) throws Exception {
        int ownerId = album.getOwnerId();
        String albumId = album.getId().toString();
        int offsetPhoto = 0, countPhoto = 1000;
        int numIter = divCeil(maxPhotoNum, countPhoto), countIter = 0;
        List<PhotoFull> list1 = new ArrayList<>();
        while (true) {
            GetExtendedResponse resp = apiClient.
                                        photos().
                          getExtended(userActor).
                                ownerId(ownerId).
                                albumId(albumId).
                                       rev(true).
                                photoSizes(true).
                                  offset(offsetPhoto).
                                    count(countPhoto).
                                       execute();
            List<PhotoFull> list2 = resp.getItems();
            if ((list2 == null) || list2.isEmpty()) {
                break;
            }
            list1.addAll(list2);
            offsetPhoto += countPhoto;
            countIter++;
            if ((list2.size() < countPhoto) || (countIter >= numIter)) {
                break;
            }
            Thread.sleep(250);
        }
        return list1;
    }

    public List<PhotoFull> getAllExtended(int ownerId) throws Exception {
        List<PhotoAlbumFull> list1 = getAlbums(ownerId);
        sortAlbum(list1);
        List<PhotoAlbumFull> list2;
        if (list1.size() <= maxAlbumNum) {
            list2 = list1;
        }
        else {
            list2 = list1.subList(0,maxAlbumNum);
        }
        List<PhotoFull> list3 = new ArrayList<>();
        for (PhotoAlbumFull a : list2) {
            list3.addAll(getExtended(a));
            Thread.sleep(250);
        }
        return list3;
    }

    public List<PhotoFull> getAllSorted(int ownerId) throws Exception {
        List<PhotoFull> list = getAllExtended(ownerId);
        sortPhoto(list);
        return list;
    }

    public List<PhotoAlbumFull> getAlbums(int ownerId) throws Exception {
        GetAlbumsResponse resp = apiClient.photos().getAlbums(userActor).ownerId(ownerId).offset(0).needCovers(true).photoSizes(true).execute();
        return resp.getItems();
    }

    private int getAlbumsCount(int ownerId) throws Exception {
        return apiClient.photos().getAlbumsCount(userActor).groupId(abs(ownerId)).execute();
    }

    private PhotoFull getByIdExtended(int ownerId, int photoId) throws Exception {
        String str = ownerId + "_" + photoId;
        List<PhotoFull> list = apiClient.photos().getByIdExtended(userActor, str).photoSizes(true).execute();
        return list.get(0);
    }

    private List<PhotoFull> getByIdExtendedList(int ownerId, List<Integer> photoIdList) throws Exception {
        List<String> strList = new ArrayList<>();
        for (Integer id : photoIdList) {
            strList.add(ownerId + "_" + id);
        }
        return apiClient.photos().getByIdExtended(userActor, strList).photoSizes(true).execute();
    }

    public PhotoUpload getUploadServer(int ownerId, int albumId) throws Exception {
        return apiClient.photos().getUploadServer(userActor).groupId(abs(ownerId)).albumId(albumId).execute();
    }

    private PhotoUploadResponse upload(File file, String url) throws Exception {
        return apiClient.upload().photo(url, file).execute();
    }

    private Photo save(int ownerId, int albumId, int server, String photosList, String hash, String caption) throws Exception {
        List<Photo> list1 = apiClient.
                             photos().
                      save(userActor).
                groupId(abs(ownerId)).
                     albumId(albumId).
                       server(server).
               photosList(photosList).
                           hash(hash).
                     caption(caption).
                            execute();
        return list1.get(0);
        //return getByIdExtended(list1.get(0).getOwnerId(), list1.get(0).getId());
    }

    private void downloadFile(String name, String url) throws Exception {
        BufferedInputStream inputStream = new BufferedInputStream(new URL(url).openStream());
        FileOutputStream fileOutputStream = new FileOutputStream(name);
        int len = 1024;
        byte dataBuffer[] = new byte[len];
        int bytesRead;
        while ((bytesRead = inputStream.read(dataBuffer, 0, len)) != -1) {
            fileOutputStream.write(dataBuffer, 0, bytesRead);
        }
        inputStream.close();
        fileOutputStream.close();
    }

    private void downloadFile2(String name, String urlStr) throws Exception {
        URL url = new URL(urlStr);
        File file1 = new File(name);
        FileUtils.copyURLToFile(url, file1);
    }

    private Photo uploadPhoto(PhotoFull photo, int ownerId, int albumId, PhotoUpload photoUpload) throws Exception {
        List<PhotoSizes> list0 = photo.getSizes();
        sortPhotoSize(list0);
        String photoUrl = list0.get(0).getSrc();
        /*
        int len = photo.getSizes().size();
        String photoUrl = photo.getSizes().get(len-1).getSrc();
        */
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

    public List<Photo> uploadPhotoList(List<PhotoFull> list1, PhotoAlbumFull album, PhotoUpload photoUpload, PhotoFull photoCover) throws Exception {
        int ownerId = album.getOwnerId(), albumId = album.getId();
        List<Photo> list2 = new ArrayList<>();
        for (PhotoFull p : list1) {
            list2.add(uploadPhoto(p, ownerId, albumId, photoUpload));
            if ((photoCover != null) && (photoCover == p)) {
                Photo p1 = list2.get(list2.size()-1);
                makeCover(p1.getOwnerId(), p1.getAlbumId(), p1.getId());
            }
            Thread.sleep(200);
        }
        return list2;
    }

    private int getPhotoMetric(PhotoFull photo) {
        return photo.getLikes().getCount() + photo.getComments().getCount() + photo.getReposts().getCount();
    }

    private int getAlbumMetric(PhotoAlbumFull album) {
        return album.getSize();
    }

    private int getPhotoSizeMetric(PhotoSizes photoSizes) {
        return photoSizes.getHeight() * photoSizes.getWidth();
    }

    public void sortPhoto(List<PhotoFull> list) {
        Collections.sort(list, new Comparator<PhotoFull>() {
            @Override
            public int compare(PhotoFull photo1, PhotoFull photo2) {
                return getPhotoMetric(photo2) - getPhotoMetric(photo1);
            }
        });
    }

    public void sortAlbum(List<PhotoAlbumFull> list) {
        Collections.sort(list, new Comparator<PhotoAlbumFull>() {
            @Override
            public int compare(PhotoAlbumFull album1, PhotoAlbumFull album2) {
                return getAlbumMetric(album2) - getAlbumMetric(album1);
            }
        });
    }

    public void sortPhotoSize(List<PhotoSizes> list) {
        Collections.sort(list, new Comparator<PhotoSizes>() {
            @Override
            public int compare(PhotoSizes photoSizes1, PhotoSizes photoSizes2) {
                return getPhotoSizeMetric(photoSizes2) - getPhotoSizeMetric(photoSizes1);
            }
        });
    }

    public void deleteAllPhoto(int ownerId) throws Exception {
        List<PhotoAlbumFull> list1 = getAlbums(ownerId);
        List<PhotoFull> list2;
        for (PhotoAlbumFull a : list1) {
            list2 = getExtended(a);
            for (PhotoFull p : list2) {
                delete(p);
                Thread.sleep(300);
            }
        }
    }

    public void deleteAlbumPhoto(PhotoAlbumFull album) throws Exception {
        List<PhotoFull> list = getExtended(album);
        for (PhotoFull p : list) {
            delete(p);
            Thread.sleep(300);
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
            List<Photo> list = apiClient.photos().save(userActor).albumId(256293027).groupId(170303225).server(resp999.getServer()).photosList(resp999.getPhotosList()).hash(resp999.getHash()).caption("Now").execute();//}
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
    }

    public void deleteAllPhoto2() {
        //int w = -27532693;
        //com.vk.api.sdk.objects.video.responses.GetAlbumsResponse resp33 = apiClient.videos().getAlbums(userActor).ownerId(w).count(100).offset(0).execute();
        //com.vk.api.sdk.objects.video.responses.GetExtendedResponse resp44 = apiClient.videos().getExtended(userActor).ownerId(w).albumId(resp33.getItems().get(0).getId()).count(100).offset(0).execute();
    }
}

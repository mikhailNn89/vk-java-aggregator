import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.objects.base.responses.OkResponse;
import com.vk.api.sdk.objects.photos.*;
import com.vk.api.sdk.objects.photos.responses.GetAlbumsResponse;
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
    private int maxNumPhoto1;
    private int maxNumPhoto2;
    private int maxNumAlbum;
    private final static long cycleDelay = 250;
    private final static long intermDelay = 100;

    PhotoHandler(VkApiClient apiClient,
                 UserActor userActor,
                 int maxNumPhoto1,
                 int maxNumPhoto2,
                 int maxNumAlbum) {
        this.apiClient = apiClient;
        this.userActor = userActor;
        this.maxNumPhoto1 = maxNumPhoto1;
        this.maxNumPhoto2 = maxNumPhoto2;
        this.maxNumAlbum = maxNumAlbum;
    }

    private static int abs(int n) { return Math.abs(n); }

    private static int divCeil(int x1, int x2) {
        double div = ((double)x1) / ((double)x2);
        return (int)(Math.ceil(div));
    }

    private void delete(PhotoFull photo) throws Exception {
        OkResponse ok = apiClient.photos().delete(userActor, photo.getId()).ownerId(photo.getOwnerId()).execute();
    }

    private void clearAlbumByDelete(PhotoAlbumFull album) throws Exception {
        OkResponse ok = apiClient.
                         photos().
      deleteAlbum(userActor, album.getId()).
      groupId(abs(album.getOwnerId())).
                        execute();
        Thread.sleep(intermDelay);
        PhotoAlbumFull albumNew = apiClient.
                                   photos().
   createAlbum(userActor, album.getTitle()).
                groupId(abs(album.getOwnerId())).
                            description("").
                         privacyView("all").
                      privacyComment("all").
                   uploadByAdminsOnly(true).
                     commentsDisabled(true).
                                  execute();
    }

    public void createAlbum(String title, int groupId) throws Exception {
        PhotoAlbumFull albumNew = apiClient.
                                  photos().
                                  createAlbum(userActor, title).
                                  groupId(abs(groupId)).
                                  description("").
                                  privacyView("all").
                                  privacyComment("all").
                                  uploadByAdminsOnly(true).
                                  commentsDisabled(true).
                                  execute();
    }

    private void makeCover(int groupId, int albumId, int photoId) throws Exception {
        OkResponse ok = apiClient.photos().makeCover(userActor, photoId).ownerId(groupId).albumId(albumId).execute();
    }

    public void movePhotoList(List<Photo> list, PhotoAlbumFull album) throws Exception {
        for (Photo p : list) {
            OkResponse ok = apiClient.photos().move(userActor, album.getId(), p.getId()).ownerId(p.getOwnerId()).execute();
            Thread.sleep(cycleDelay);
        }
    }

    private List<PhotoFull> getExtended(PhotoAlbumFull album) throws Exception {
        int groupId = album.getOwnerId();
        String albumId = album.getId().toString();
        int offsetPhoto = 0, countPhoto = 1000;
        int numIter = divCeil(maxNumPhoto1, countPhoto), countIter = 0;
        List<PhotoFull> list1 = new ArrayList<>();
        while (true) {
            GetExtendedResponse resp = apiClient.
                                        photos().
                          getExtended(userActor).
                                ownerId(groupId).
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
            Thread.sleep(cycleDelay);
        }
        return list1;
    }

    private List<PhotoFull> getAllExtended(int groupId) throws Exception {
        List<PhotoAlbumFull> list1 = getAlbums(groupId);
        sortAlbum(list1);
        List<PhotoAlbumFull> list2;
        if (list1.size() <= maxNumAlbum) {
            list2 = list1;
        }
        else {
            list2 = list1.subList(0,maxNumAlbum);
        }
        Thread.sleep(intermDelay);
        List<PhotoFull> list3 = new ArrayList<>();
        for (PhotoAlbumFull a : list2) {
            if (filtAlbum(a)) {
                continue;
            }
            list3.addAll(getExtended(a));
            if (list3.size() >= maxNumPhoto2) {
                break;
            }
            Thread.sleep(cycleDelay);
        }
        return list3;
    }

    private static boolean filtAlbum(PhotoAlbumFull album) {
        boolean filt = false;
        if (album.getSize() == 0) {
            return true;
        }
        String title = album.getTitle().toLowerCase();
        List<String> list = new ArrayList<>();
        list.add("победите");
        list.add("розыгрыш");
        list.add("девуш");
        list.add("мисс");
        list.add("girl");
        list.add("boy");
        list.add("гороско");
        list.add("наши");
        list.add("подписч");
        list.add("cигн");
        list.add("конкурс");
        list.add("авто");
        list.add("общая");
        list.add("барахолка");
        list.add("дети");
        list.add("детск");
        list.add("без");
        list.add(".");
        list.add("основн");
        for (String s : list) {
            if (title.contains(s.toLowerCase())) {
                filt = true;
                break;
            }
        }
        return filt;
    }

    public List<PhotoFull> getAllSorted(int groupId) throws Exception {
        List<PhotoFull> list = getAllExtended(groupId);
        sortPhoto(list);
        return list;
    }

    public List<PhotoAlbumFull> getAlbums(int groupId) throws Exception {
        GetAlbumsResponse resp = apiClient.photos().getAlbums(userActor).ownerId(groupId).offset(0).needCovers(true).photoSizes(true).execute();
        return resp.getItems();
    }

    private int getAlbumsCount(int groupId) throws Exception {
        return apiClient.photos().getAlbumsCount(userActor).groupId(abs(groupId)).execute();
    }

    private PhotoFull getByIdExtended(int groupId, int photoId) throws Exception {
        String str = groupId + "_" + photoId;
        List<PhotoFull> list = apiClient.photos().getByIdExtended(userActor, str).photoSizes(true).execute();
        return list.get(0);
    }

    private List<PhotoFull> getByIdExtendedList(int groupId, List<Integer> photoIdList) throws Exception {
        List<String> strList = new ArrayList<>();
        for (Integer id : photoIdList) {
            strList.add(groupId + "_" + id);
        }
        return apiClient.photos().getByIdExtended(userActor, strList).photoSizes(true).execute();
    }

    public PhotoUpload getUploadServer(int groupId, int albumId) throws Exception {
        return apiClient.photos().getUploadServer(userActor).groupId(abs(groupId)).albumId(albumId).execute();
    }

    private PhotoUploadResponse upload(File file, String url) throws Exception {
        return apiClient.upload().photo(url, file).execute();
    }

    private Photo save(int groupId, int albumId, int server, String photosList, String hash, String caption) throws Exception {
        List<Photo> list1 = apiClient.
                             photos().
                      save(userActor).
                groupId(abs(groupId)).
                     albumId(albumId).
                       server(server).
               photosList(photosList).
                           hash(hash).
                     caption(caption).
                            execute();
        return list1.get(0);
    }

    private static void downloadFile(String name, String url) throws Exception {
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

    private static void downloadFile2(String name, String urlStr) throws Exception {
        URL url = new URL(urlStr);
        File file1 = new File(name);
        FileUtils.copyURLToFile(url, file1);
    }

    private Photo uploadPhoto(PhotoFull photo, int groupId, int albumId, PhotoUpload photoUpload) throws Exception {
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
        Thread.sleep(intermDelay);

        String str1 = "likes: " + photo.getLikes().getCount();
        String str2 = "comments: " + photo.getComments().getCount();
        String str3 = "reposts: " + photo.getReposts().getCount();
        int sum = photo.getLikes().getCount() + photo.getComments().getCount() + photo.getReposts().getCount();
        String str4 = "sum: " + sum;
        //String strFinal = str1 + "\n" + str2 + "\n" + str3 + "\n" + str4;
        String strFinal = str1 + ", " + str2 + ", " + str3 + ", " + str4;

        return save(groupId, albumId, resp.getServer(), resp.getPhotosList(), resp.getHash(), strFinal);
    }

    public List<Photo> uploadPhotoList(List<PhotoFull> list1, PhotoAlbumFull album, PhotoUpload photoUpload, PhotoFull photoCover) throws Exception {
        int groupId = album.getOwnerId(), albumId = album.getId();
        List<Photo> list2 = new ArrayList<>();
        for (PhotoFull p : list1) {
            list2.add(uploadPhoto(p, groupId, albumId, photoUpload));
            if ((photoCover != null) && (photoCover == p)) {
                Thread.sleep(cycleDelay);
                Photo p1 = list2.get(list2.size()-1);
                makeCover(p1.getOwnerId(), p1.getAlbumId(), p1.getId());
            }
            Thread.sleep(cycleDelay);
        }
        return list2;
    }

    private static int getPhotoMetric(PhotoFull photo) {
        return photo.getLikes().getCount() + photo.getComments().getCount() + photo.getReposts().getCount();
    }

    private static int getAlbumMetric(PhotoAlbumFull album) {
        return album.getSize();
    }

    private static int getPhotoSizeMetric(PhotoSizes photoSizes) {
        return photoSizes.getHeight() * photoSizes.getWidth();
    }

    public static void sortPhoto(List<PhotoFull> list) {
        Collections.sort(list, new Comparator<PhotoFull>() {
            @Override
            public int compare(PhotoFull photo1, PhotoFull photo2) {
                return getPhotoMetric(photo2) - getPhotoMetric(photo1);
            }
        });
    }

    private static void sortAlbum(List<PhotoAlbumFull> list) {
        Collections.sort(list, new Comparator<PhotoAlbumFull>() {
            @Override
            public int compare(PhotoAlbumFull album1, PhotoAlbumFull album2) {
                return getAlbumMetric(album2) - getAlbumMetric(album1);
            }
        });
    }

    private static void sortPhotoSize(List<PhotoSizes> list) {
        Collections.sort(list, new Comparator<PhotoSizes>() {
            @Override
            public int compare(PhotoSizes photoSizes1, PhotoSizes photoSizes2) {
                return getPhotoSizeMetric(photoSizes2) - getPhotoSizeMetric(photoSizes1);
            }
        });
    }

    public void deleteAllPhoto(int groupId) throws Exception {
        List<PhotoAlbumFull> list1 = getAlbums(groupId);
        if ((list1 == null) || list1.isEmpty()) {
            return;
        }
        Thread.sleep(intermDelay);
        List<PhotoFull> list2;
        for (PhotoAlbumFull a : list1) {
            if (a.getSize() == 0) {
                continue;
            }
            list2 = getExtended(a);
            if ((list2 == null) || list2.isEmpty()) {
                Thread.sleep(cycleDelay);
                continue;
            }
            Thread.sleep(cycleDelay);
            for (PhotoFull p : list2) {
                delete(p);
                Thread.sleep(cycleDelay);
            }
        }
    }

    public void deleteAllPhoto2(int groupId) throws Exception {
        List<PhotoAlbumFull> list1 = getAlbums(groupId);
        if ((list1 == null) || list1.isEmpty()) {
            return;
        }
        Thread.sleep(intermDelay);
        for (PhotoAlbumFull a : list1) {
            if (a.getSize() == 0) {
                continue;
            }
            if (a.getTitle().equalsIgnoreCase("Основной альбом")) {
                deleteAlbumPhoto(a);
            }
            else {
                clearAlbumByDelete(a);
            }
            Thread.sleep(cycleDelay);
        }
    }

    private void deleteAlbumPhoto(PhotoAlbumFull album) throws Exception {
        List<PhotoFull> list = getExtended(album);
        Thread.sleep(intermDelay);
        for (PhotoFull p : list) {
            delete(p);
            Thread.sleep(cycleDelay);
        }
    }
}

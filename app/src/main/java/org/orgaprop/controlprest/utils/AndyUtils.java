package org.orgaprop.controlprest.utils;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Size;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

public class AndyUtils {

    public static final int PERMISSION_REQUEST = 100;
    public static final int LAPS_TIME_TEST_CONNECT = 10000;

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if( connectivityManager != null ) {
            NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());

            if( networkCapabilities != null ) {
                return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
            }
        }

        return false;
    }

    public static String putBitmapToGallery(Context context, Bitmap bitmap, String imageName) {
        String result = "";
        OutputStream os;

        if( Build.VERSION.SDK_INT < 29 ) {
            String imagesPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/OrgaProp";
            File fileDir = new File(imagesPath);

            if( !fileDir.exists() ) {
                fileDir.mkdirs();
            }

            File image = new File(imagesPath, imageName);

            if( imageName.equals("sig1.png") || imageName.equals("sig2.png") ) {
                if( image.exists() ) {
                    image.delete();
                }
            }

            try {
                os = Files.newOutputStream(image.toPath());

                bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                os.close();

                result = image.getAbsolutePath();

                MediaScannerConnection.scanFile(context, new String[] { image.toString() }, null, (path, uri) -> {});
            } catch( IOException e ) {
                e.printStackTrace();
            }
        } else {
            ContentValues values = new ContentValues();

            values.put(MediaStore.Images.Media.MIME_TYPE, "images/png");
            values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis()/1000);
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
            values.put(MediaStore.Images.Media.DISPLAY_NAME, imageName);
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES+"/OrgaProp");
            values.put(MediaStore.Images.Media.IS_PENDING, true);

            Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            if( uri != null ) {
                try {
                    os = context.getContentResolver().openOutputStream(uri);

                    if( os != null ) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                        os.close();

                        values.put(MediaStore.Images.Media.IS_PENDING, false);

                        context.getContentResolver().update(uri, values, null, null);

                        result = uri.toString();
                    }
                } catch( IOException e ) {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }
    public static Bitmap getBitmapFromGallery(Context context, String imagePath, Size size) {
        Bitmap bitmap = null;

        if( Build.VERSION.SDK_INT < 29 ) {
            bitmap = BitmapFactory.decodeFile(imagePath);
        } else {
            Uri uri = Uri.parse(imagePath);

            try {
                bitmap = context.getContentResolver().loadThumbnail(uri, size, null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return bitmap;
    }
    public static String bitmapToString(Bitmap bitmap){
        ByteArrayOutputStream baos = new  ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100, baos);
        byte[] arr = baos.toByteArray();

        return Base64.encodeToString(arr, Base64.DEFAULT);
    }
    public static Bitmap StringToBitMap(String image){
        try{
            byte[] encodeByte = Base64.decode(image,Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);

            return bitmap;
        }catch(Exception e){
            e.getMessage();
            return null;
        }
    }

    public static String getFolderPath(Context context) {
        String folderPath = null;

        if (isSdPresent()) {
            try {
                File sdPath = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "OrgaProp/");

                if( !sdPath.exists() ) {
                    sdPath.mkdirs();
                }

                folderPath = sdPath.getAbsolutePath();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                File cacheDir = new File(context.getCacheDir(),"OrgaProp/");

                if( !cacheDir.exists() ) {
                    cacheDir.mkdirs();
                }

                folderPath = cacheDir.getAbsolutePath();
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        return folderPath;
    }

    private static boolean isSdPresent() {
        return android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
    }

    public static int convertDpsToInt(Context context, int dps) {
        final float scale = context.getResources().getDisplayMetrics().density;

        return (int) (dps * scale + 0.5f);
    }

}

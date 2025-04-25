package org.orgaprop.controlprest.utils;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;

public class AndyUtils {

    private static final String TAG = "AndyUtils";

    public static final int PERMISSION_REQUEST = 100;
    public static final int LAPS_TIME_TEST_CONNECT = 10000;


    /**
     * Vérifie si une connexion réseau est disponible
     * @param context Contexte de l'application
     * @return true si une connexion est disponible, false sinon
     */
    public static boolean isNetworkAvailable(Context context) {
        if (context == null) {
            Log.e(TAG, "Le contexte est null dans isNetworkAvailable");
            return false;
        }

        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            if( connectivityManager != null ) {
                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());

                if( networkCapabilities != null ) {
                    boolean hasWifi = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
                    boolean hasCellular = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);

                    boolean isAvailable = hasWifi || hasCellular;
                    Log.d(TAG, "État du réseau - WiFi: " + hasWifi + ", Cellulaire: " + hasCellular);
                    Toast.makeText(context, "État du réseau - WiFi: " + hasWifi + ", Cellulaire: " + hasCellular, Toast.LENGTH_SHORT).show();

                    return isAvailable;
                } else {
                    Log.e(TAG, "Aucune capacité réseau détectée");
                    Toast.makeText(context, "Aucune capacité réseau détectée", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e(TAG, "ConnectivityManager est null");
                Toast.makeText(context, "ConnectivityManager est null", Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permission ACCESS_NETWORK_STATE manquante", e);
            Toast.makeText(context, "Permission ACCESS_NETWORK_STATE manquante", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Exception lors de la vérification du réseau", e);
            Toast.makeText(context, "Exception lors de la vérification du réseau", Toast.LENGTH_SHORT).show();
        }

        return false;
    }

    /**
     * Enregistre un bitmap dans la galerie
     * @param context Contexte de l'application
     * @param bitmap Bitmap à enregistrer
     * @param imageName Nom de l'image
     * @return Chemin de l'image enregistrée, ou chaîne vide en cas d'erreur
     */
    public static String putBitmapToGallery(Context context, Bitmap bitmap, String imageName) {
        if (context == null) {
            Log.e(TAG, "Le contexte est null dans putBitmapToGallery");
            Toast.makeText(context, "Le contexte est null dans putBitmapToGallery", Toast.LENGTH_SHORT).show();
            return "";
        }

        if (bitmap == null) {
            Log.e(TAG, "Le bitmap est null dans putBitmapToGallery");
            Toast.makeText(context, "Le bitmap est null dans putBitmapToGallery", Toast.LENGTH_SHORT).show();
            return "";
        }

        if (imageName == null || imageName.isEmpty()) {
            Log.e(TAG, "Le nom de l'image est null ou vide dans putBitmapToGallery");
            Toast.makeText(context, "Le nom de l'image est null ou vide dans putBitmapToGallery", Toast.LENGTH_SHORT).show();
            imageName = "image_" + System.currentTimeMillis() + ".png";
        }

        String result = "";

        try {
            if( Build.VERSION.SDK_INT < 29 ) {
                String imagesPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/OrgaProp";
                File fileDir = new File(imagesPath);

                if( !fileDir.exists() ) {
                    boolean dirCreated = fileDir.mkdirs();
                    if (!dirCreated) {
                        Log.e(TAG, "Impossible de créer le répertoire: " + imagesPath);
                        Toast.makeText(context, "Impossible de créer le répertoire: " + imagesPath, Toast.LENGTH_SHORT).show();
                        return "";
                    }
                }

                File image = new File(imagesPath, imageName);

                if( imageName.equals("sig1.png") || imageName.equals("sig2.png") ) {
                    if( image.exists() ) {
                        boolean deleted = image.delete();
                        if (!deleted) {
                            Log.w(TAG, "Impossible de supprimer l'image existante: " + image.getAbsolutePath());
                            Toast.makeText(context, "Impossible de supprimer l'image existante: " + image.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                OutputStream os = null;
                try {
                    os = Files.newOutputStream(image.toPath());

                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                    os.close();

                    result = image.getAbsolutePath();

                    MediaScannerConnection.scanFile(
                        context,
                        new String[] { image.toString() },
                        null,
                        (path, uri) -> {
                            Log.d(TAG, "Image scannée: " + path);
                            Toast.makeText(context, "Image scannée: " + path, Toast.LENGTH_SHORT).show();
                        }
                    );
                } catch (NoSuchFileException e) {
                    Log.e(TAG, "Fichier introuvable pour l'écriture", e);
                    Toast.makeText(context, "Fichier introuvable pour l'écriture", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Log.e(TAG, "Erreur d'E/S lors de l'écriture du bitmap", e);
                    Toast.makeText(context, "Erreur d'E/S lors de l'écriture du bitmap", Toast.LENGTH_SHORT).show();
                } finally {
                    if (os != null) {
                        try {
                            os.close();
                        } catch (IOException e) {
                            Log.e(TAG, "Erreur lors de la fermeture du flux de sortie", e);
                            Toast.makeText(context, "Erreur lors de la fermeture du flux de sortie", Toast.LENGTH_SHORT).show();
                        }
                    }
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
                    OutputStream os = null;
                    try {
                        os = context.getContentResolver().openOutputStream(uri);

                        if( os != null ) {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);

                            values.clear();
                            values.put(MediaStore.Images.Media.IS_PENDING, 0);
                            context.getContentResolver().update(uri, values, null, null);

                            result = uri.toString();
                        } else {
                            Log.e(TAG, "OutputStream est null après ouverture");
                            Toast.makeText(context, "OutputStream est null après ouverture", Toast.LENGTH_SHORT).show();
                        }
                    } catch (FileNotFoundException e) {
                        Log.e(TAG, "Fichier non trouvé pour l'URI: " + uri, e);
                    } finally {
                        if (os != null) {
                            try {
                                os.close();
                            } catch (IOException e) {
                                Log.e(TAG, "Erreur lors de la fermeture du flux de sortie", e);
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "Impossible de créer l'URI pour l'image");
                    Toast.makeText(context, "Impossible de créer l'URI pour l'image", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permission de stockage manquante", e);
            Toast.makeText(context, "Permission de stockage manquante", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Exception lors de l'enregistrement du bitmap", e);
            Toast.makeText(context, "Exception lors de l'enregistrement du bitmap", Toast.LENGTH_SHORT).show();
        }

        return result;
    }

    /**
     * Récupère un bitmap depuis la galerie
     * @param context Contexte de l'application
     * @param imagePath Chemin de l'image
     * @param size Taille souhaitée
     * @return Bitmap chargé ou null si échec
     */
    public static Bitmap getBitmapFromGallery(Context context, String imagePath, Size size) {
        if (context == null) {
            Log.e(TAG, "Le contexte est null dans getBitmapFromGallery");
            Toast.makeText(context, "Le contexte est null dans getBitmapFromGallery", Toast.LENGTH_SHORT).show();
            return null;
        }

        if (imagePath == null || imagePath.isEmpty()) {
            Log.e(TAG, "Le chemin de l'image est null ou vide");
            Toast.makeText(context, "Le chemin de l'image est null ou vide", Toast.LENGTH_SHORT).show();
            return null;
        }

        if (size == null) {
            Log.e(TAG, "La taille est null");
            Toast.makeText(context, "La taille est null", Toast.LENGTH_SHORT).show();
            return null;
        }

        Bitmap bitmap = null;

        try {
            if (Build.VERSION.SDK_INT < 29) {
                File imageFile = new File(imagePath);
                if (!imageFile.exists()) {
                    Log.e(TAG, "Le fichier n'existe pas: " + imagePath);
                    Toast.makeText(context, "Le fichier n'existe pas: " + imagePath, Toast.LENGTH_SHORT).show();
                    return null;
                }

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;

                BitmapFactory.decodeFile(imagePath, options);

                int imageHeight = options.outHeight;
                int imageWidth = options.outWidth;

                int scaleFactor = Math.max(1, Math.min(
                        imageWidth / size.getWidth(),
                        imageHeight / size.getHeight()));

                options.inJustDecodeBounds = false;
                options.inSampleSize = scaleFactor;

                bitmap = BitmapFactory.decodeFile(imagePath, options);
            } else {
                Uri uri = Uri.parse(imagePath);
                bitmap = context.getContentResolver().loadThumbnail(uri, size, null);
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Fichier non trouvé: " + imagePath, e);
            Toast.makeText(context, "Fichier non trouvé: " + imagePath, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(TAG, "Erreur d'E/S lors du chargement du bitmap", e);
            Toast.makeText(context, "Erreur d'E/S lors du chargement du bitmap", Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Log.e(TAG, "Permission manquante pour accéder à l'image", e);
            Toast.makeText(context, "Permission manquante pour accéder à l'image", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Exception lors du chargement du bitmap", e);
            Toast.makeText(context, "Exception lors du chargement du bitmap", Toast.LENGTH_SHORT).show();
        }

        return bitmap;
    }

    /**
     * Convertit un bitmap en chaîne encodée en Base64
     * @param bitmap Bitmap à convertir
     * @return Chaîne encodée en Base64 ou null si échec
     */
    public static String bitmapToString(Context context, Bitmap bitmap) {
        if (bitmap == null) {
            Log.e(TAG, "Le bitmap est null dans bitmapToString");
            Toast.makeText(context, "Le bitmap est null dans bitmapToString", Toast.LENGTH_SHORT).show();
            return null;
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            boolean success = bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);

            if (!success) {
                Log.e(TAG, "Échec de la compression du bitmap");
                Toast.makeText(context, "Échec de la compression du bitmap", Toast.LENGTH_SHORT).show();
                return null;
            }

            byte[] arr = baos.toByteArray();
            return Base64.encodeToString(arr, Base64.DEFAULT);
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "Mémoire insuffisante pour la conversion du bitmap", e);
            Toast.makeText(context, "Mémoire insuffisante pour la conversion du bitmap", Toast.LENGTH_SHORT).show();
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Exception lors de la conversion du bitmap en chaîne", e);
            Toast.makeText(context, "Exception lors de la conversion du bitmap en chaîne", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    /**
     * Convertit une chaîne encodée en Base64 en bitmap
     * @param image Chaîne encodée en Base64
     * @return Bitmap décodé ou null si échec
     */
    public static Bitmap StringToBitMap(Context context, String image) {
        if (image == null || image.isEmpty()) {
            Log.e(TAG, "La chaîne image est null ou vide");
            Toast.makeText(context, "La chaîne image est null ou vide", Toast.LENGTH_SHORT).show();
            return null;
        }

        try {
            byte[] encodeByte = Base64.decode(image, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "La chaîne ne contient pas de données Base64 valides", e);
            Toast.makeText(context, "La chaîne ne contient pas de données Base64 valides", Toast.LENGTH_SHORT).show();
            return null;
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "Mémoire insuffisante pour le décodage du bitmap", e);
            Toast.makeText(context, "Mémoire insuffisante pour le décodage du bitmap", Toast.LENGTH_SHORT).show();
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Exception lors de la conversion de la chaîne en bitmap", e);
            Toast.makeText(context, "Exception lors de la conversion de la chaîne en bitmap", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    /**
     * Obtient le chemin du dossier d'application
     * @param context Contexte de l'application
     * @return Chemin du dossier ou null si échec
     */
    public static String getFolderPath(Context context) {
        if (context == null) {
            Log.e(TAG, "Le contexte est null dans getFolderPath");
            Toast.makeText(context, "Le contexte est null dans getFolderPath", Toast.LENGTH_SHORT).show();
            return null;
        }

        String folderPath = null;

        try {
            if (isSdPresent()) {
                File sdPath = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "OrgaProp/");

                if (!sdPath.exists()) {
                    boolean created = sdPath.mkdirs();
                    if (!created) {
                        Log.e(TAG, "Impossible de créer le dossier dans le stockage externe: " + sdPath.getAbsolutePath());
                        Toast.makeText(context, "Impossible de créer le dossier dans le stockage externe: " + sdPath.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                    }
                }

                folderPath = sdPath.getAbsolutePath();
            } else {
                File cacheDir = new File(context.getCacheDir(), "OrgaProp/");

                if (!cacheDir.exists()) {
                    boolean created = cacheDir.mkdirs();
                    if (!created) {
                        Log.e(TAG, "Impossible de créer le dossier dans le cache: " + cacheDir.getAbsolutePath());
                        Toast.makeText(context, "Impossible de créer le dossier dans le cache: " + cacheDir.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                    }
                }

                folderPath = cacheDir.getAbsolutePath();
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permissions insuffisantes pour le stockage", e);
            Toast.makeText(context, "Permissions insuffisantes pour le stockage", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Exception lors de la récupération du chemin du dossier", e);
            Toast.makeText(context, "Exception lors de la récupération du chemin du dossier", Toast.LENGTH_SHORT).show();
        }

        return folderPath;
    }

    /**
     * Vérifie si une carte SD est présente et accessible
     * @return true si une carte SD est disponible, false sinon
     */
    private static boolean isSdPresent() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    /**
     * Convertit des dp en pixels
     * @param context Contexte de l'application
     * @param dps Valeur en dp à convertir
     * @return Valeur équivalente en pixels
     */
    public static int convertDpsToInt(Context context, int dps) {
        if (context == null) {
            Log.e(TAG, "Le contexte est null dans convertDpsToInt");
            Toast.makeText(context, "Le contexte est null dans convertDpsToInt", Toast.LENGTH_SHORT).show();
            return dps;
        }

        try {
            final float scale = context.getResources().getDisplayMetrics().density;
            return (int) (dps * scale + 0.5f);
        } catch (Exception e) {
            Log.e(TAG, "Exception lors de la conversion dp en pixels", e);
            Toast.makeText(context, "Exception lors de la conversion dp en pixels", Toast.LENGTH_SHORT).show();
            return dps;
        }
    }

}

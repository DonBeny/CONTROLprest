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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

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

                    FirebaseCrashlytics.getInstance().log("Connectivité réseau - WiFi: " + hasWifi + ", Cellulaire: " + hasCellular);
                    Log.d(TAG, "État du réseau - WiFi: " + hasWifi + ", Cellulaire: " + hasCellular);

                    return isAvailable;
                } else {
                    FirebaseCrashlytics.getInstance().log("Aucune capacité réseau détectée");
                    Log.e(TAG, "Aucune capacité réseau détectée");
                    ToastManager.showError("Aucune capacité réseau détectée");
                }
            } else {
                FirebaseCrashlytics.getInstance().log("ConnectivityManager est null");
                Log.e(TAG, "ConnectivityManager est null");
                ToastManager.showError("ConnectivityManager est null");
            }
        } catch (SecurityException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            Log.e(TAG, "Permission ACCESS_NETWORK_STATE manquante", e);
            ToastManager.showError("Permission ACCESS_NETWORK_STATE manquante");
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            Log.e(TAG, "Exception lors de la vérification du réseau", e);
            ToastManager.showError("Exception lors de la vérification du réseau");
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
            ToastManager.showError("Le contexte est null dans putBitmapToGallery");
            return "";
        }

        if (bitmap == null) {
            Log.e(TAG, "Le bitmap est null dans putBitmapToGallery");
            ToastManager.showError("Le bitmap est null dans putBitmapToGallery");
            return "";
        }

        if (imageName == null || imageName.isEmpty()) {
            Log.e(TAG, "Le nom de l'image est null ou vide dans putBitmapToGallery");
            ToastManager.showError("Le nom de l'image est null ou vide dans putBitmapToGallery");
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
                        ToastManager.showError("Impossible de créer le répertoire: " + imagesPath);
                        return "";
                    }
                }

                File image = new File(imagesPath, imageName);

                if( imageName.equals("sig1.png") || imageName.equals("sig2.png") ) {
                    if( image.exists() ) {
                        boolean deleted = image.delete();
                        if (!deleted) {
                            Log.w(TAG, "Impossible de supprimer l'image existante: " + image.getAbsolutePath());
                            ToastManager.showError("Impossible de supprimer l'image existante: " + image.getAbsolutePath());
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
                            ToastManager.showError("Image scannée: " + path);
                        }
                    );
                } catch (NoSuchFileException e) {
                    Log.e(TAG, "Fichier introuvable pour l'écriture", e);
                    ToastManager.showError("Fichier introuvable pour l'écriture");
                } catch (IOException e) {
                    Log.e(TAG, "Erreur d'E/S lors de l'écriture du bitmap", e);
                    ToastManager.showError("Erreur d'E/S lors de l'écriture du bitmap");
                } finally {
                    if (os != null) {
                        try {
                            os.close();
                        } catch (IOException e) {
                            Log.e(TAG, "Erreur lors de la fermeture du flux de sortie", e);
                            ToastManager.showError("Erreur lors de la fermeture du flux de sortie");
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
                            ToastManager.showError("OutputStream est null après ouverture");
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
                    ToastManager.showError("Impossible de créer l'URI pour l'image");
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permission de stockage manquante", e);
            ToastManager.showError("Permission de stockage manquante");
        } catch (Exception e) {
            Log.e(TAG, "Exception lors de l'enregistrement du bitmap", e);
            ToastManager.showError("Exception lors de l'enregistrement du bitmap");
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
            ToastManager.showError("Le contexte est null dans getBitmapFromGallery");
            return null;
        }

        if (imagePath == null || imagePath.isEmpty()) {
            Log.e(TAG, "Le chemin de l'image est null ou vide");
            ToastManager.showError("Le chemin de l'image est null ou vide");
            return null;
        }

        if (size == null) {
            Log.e(TAG, "La taille est null");
            ToastManager.showError("La taille est null");
            return null;
        }

        Bitmap bitmap = null;

        try {
            if (Build.VERSION.SDK_INT < 29) {
                File imageFile = new File(imagePath);
                if (!imageFile.exists()) {
                    Log.e(TAG, "Le fichier n'existe pas: " + imagePath);
                    ToastManager.showError("Le fichier n'existe pas: " + imagePath);
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
            ToastManager.showError("Fichier non trouvé: " + imagePath);
        } catch (IOException e) {
            Log.e(TAG, "Erreur d'E/S lors du chargement du bitmap", e);
            ToastManager.showError("Erreur d'E/S lors du chargement du bitmap");
        } catch (SecurityException e) {
            Log.e(TAG, "Permission manquante pour accéder à l'image", e);
            ToastManager.showError("Permission manquante pour accéder à l'image");
        } catch (Exception e) {
            Log.e(TAG, "Exception lors du chargement du bitmap", e);
            ToastManager.showError("Exception lors du chargement du bitmap");
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
            ToastManager.showError("Le bitmap est null dans bitmapToString");
            return null;
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            boolean success = bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);

            if (!success) {
                Log.e(TAG, "Échec de la compression du bitmap");
                ToastManager.showError("Échec de la compression du bitmap");
                return null;
            }

            byte[] arr = baos.toByteArray();
            return Base64.encodeToString(arr, Base64.DEFAULT);
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "Mémoire insuffisante pour la conversion du bitmap", e);
            ToastManager.showError("Mémoire insuffisante pour la conversion du bitmap");
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Exception lors de la conversion du bitmap en chaîne", e);
            ToastManager.showError("Exception lors de la conversion du bitmap en chaîne");
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
            ToastManager.showError("La chaîne image est null ou vide");
            return null;
        }

        try {
            byte[] encodeByte = Base64.decode(image, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "La chaîne ne contient pas de données Base64 valides", e);
            ToastManager.showError("La chaîne ne contient pas de données Base64 valides");
            return null;
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "Mémoire insuffisante pour le décodage du bitmap", e);
            ToastManager.showError("Mémoire insuffisante pour le décodage du bitmap");
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Exception lors de la conversion de la chaîne en bitmap", e);
            ToastManager.showError("Exception lors de la conversion de la chaîne en bitmap");
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
            ToastManager.showError("Le contexte est null dans getFolderPath");
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
                        ToastManager.showError("Impossible de créer le dossier dans le stockage externe: " + sdPath.getAbsolutePath());
                    }
                }

                folderPath = sdPath.getAbsolutePath();
            } else {
                File cacheDir = new File(context.getCacheDir(), "OrgaProp/");

                if (!cacheDir.exists()) {
                    boolean created = cacheDir.mkdirs();
                    if (!created) {
                        Log.e(TAG, "Impossible de créer le dossier dans le cache: " + cacheDir.getAbsolutePath());
                        ToastManager.showError("Impossible de créer le dossier dans le cache: " + cacheDir.getAbsolutePath());
                    }
                }

                folderPath = cacheDir.getAbsolutePath();
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permissions insuffisantes pour le stockage", e);
            ToastManager.showError("Permissions insuffisantes pour le stockage");
        } catch (Exception e) {
            Log.e(TAG, "Exception lors de la récupération du chemin du dossier", e);
            ToastManager.showError("Exception lors de la récupération du chemin du dossier");
        }

        return folderPath;
    }

    /**
     * Vérifie si une carte SD est présente et accessible
     * @return true si une carte SD est disponible, false sinon
     */
    private static boolean isSdPresent() {
        boolean isPresent = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        FirebaseCrashlytics.getInstance().log("Carte SD présente: " + isPresent);
        return isPresent;
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
            ToastManager.showError("Le contexte est null dans convertDpsToInt");
            return dps;
        }

        try {
            final float scale = context.getResources().getDisplayMetrics().density;
            return (int) (dps * scale + 0.5f);
        } catch (Exception e) {
            Log.e(TAG, "Exception lors de la conversion dp en pixels", e);
            ToastManager.showError("Exception lors de la conversion dp en pixels");
            return dps;
        }
    }

}

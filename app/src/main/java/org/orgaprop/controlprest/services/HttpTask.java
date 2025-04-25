package org.orgaprop.controlprest.services;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.util.Log;

import org.orgaprop.controlprest.controllers.activities.LoginActivity;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;

public class HttpTask {

//********* PRIVATE VARIABLES

    private Context context;
    private static final String TAG = "HttpTask";
    private final ExecutorService executorService;

//********* STATIC VARIABLES


    private static final String HTTP_ADRESS_SERVER = "https://www.orgaprop.org/cs/app/";
    //private static final String HTTP_ADRESS_SERVER = "https://www.benysoftware.fr/cs/app/";

    public static final String HTTP_TASK_ACT_CONEX = "conex";
    public static final String HTTP_TASK_CBL_TEST = "test";
    public static final String HTTP_TASK_CBL_OK = "ok";
    public static final String HTTP_TASK_CBL_NO = "no";
    public static final String HTTP_TASK_CBL_ROBOT = "robot";
    public static final String HTTP_TASK_CBL_MAIL = "mail";

    public static final String HTTP_TASK_ACT_LIST = "list";
    public static final String HTTP_TASK_CBL_AGCS = "agcs";
    public static final String HTTP_TASK_CBL_GRPS = "grps";
    public static final String HTTP_TASK_CBL_RSDS = "rsds";

    public static final String HTTP_TASK_ACT_FICH = "fich";
    public static final String HTTP_TASK_ACT_GRILL = "grill";
    public static final String HTTP_TASK_ACT_SAVE = "save";
    public static final String HTTP_TASK_ACT_SEND = "send";
    public static final String HTTP_TASK_ACT_SEARCH = "search";
    public static final String HTTP_TASK_ACT_SIGNATURE = "sign";
    public static final String HTTP_TASK_ACT_SUPPR = "suppr";

    public static final int TIME_OUT = 10;
    public static final int RETRY_DELAY_MS = 1000;
    public static final int CONNECTION_TIMEOUT_MS = 15000;
    public static final int READ_TIMEOUT_MS = 20000;

//********* CONSTRUCTORS

    public HttpTask(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Le contexte ne peut pas être null");
        }
        this.context = context;
        this.executorService = Executors.newCachedThreadPool();
    }

    /**
     * Ferme l'executor service lors de la destruction de l'objet
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            }
        } finally {
            super.finalize();
        }
    }

//********* PUBLIC VARIABLES

    /**
     * Exécute une tâche HTTP avec tentatives de reconnexion
     *
     * @param params Paramètres de la requête:
     *        params[0] = action, params[1] = cible, params[2] = paramètres GET, params[3] = paramètres POST
     * @return CompletableFuture contenant la réponse du serveur
     */
    public CompletableFuture<String> executeHttpTask(String... params) {
        return CompletableFuture.supplyAsync(() -> {
            if (params == null || params.length < 4) {
                Log.e(TAG, "Paramètres insuffisants pour executeHttpTask");
                return "0Paramètres insuffisants";
            }

            String paramsAct = params[0];
            String paramsCbl = params[1];
            String paramsGet = params[2];
            String paramsPost = params[3];

            if (paramsAct == null || paramsAct.isEmpty() || paramsCbl == null || paramsCbl.isEmpty()) {
                Log.e(TAG, "Paramètres 'act' ou 'cbl' manquants");
                return "0Paramètres manquants (action ou cible)";
            }
            if (paramsGet == null) paramsGet = "";
            if (paramsPost == null) paramsPost = "";

            if (!isNetworkAvailable()) {
                Log.e(TAG, "Pas de connexion réseau disponible");
                return "0Aucune connexion réseau disponible";
            }

            int retryCount = 0;
            String result = null;
            Exception lastException = null;

            while ( retryCount < TIME_OUT ) {
                HttpsURLConnection urlConnection = null;

                try {
                    String stringUrl = HTTP_ADRESS_SERVER + LoginActivity.ACCESS_CODE + ".php";
                    stringUrl += "?act=" + paramsAct;
                    stringUrl += "&cbl=" + paramsCbl;

                    if (!paramsGet.isEmpty()) {
                        stringUrl += "&" + paramsGet;
                    }

                    Log.d(TAG, "Tentative de connexion à: " + stringUrl);
                    URL url = new URL(stringUrl);

                    urlConnection = (HttpsURLConnection) url.openConnection();
                    urlConnection.setReadTimeout(READ_TIMEOUT_MS);
                    urlConnection.setConnectTimeout(CONNECTION_TIMEOUT_MS);
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setDoInput(true);
                    urlConnection.setDoOutput(true);
                    urlConnection.setUseCaches(false);

                    urlConnection.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
                    urlConnection.setRequestProperty("Pragma", "no-cache");
                    urlConnection.setRequestProperty("Expires", "0");

                    if (!paramsPost.isEmpty()) {
                        try (
                                OutputStream outputStream = urlConnection.getOutputStream();
                                BufferedWriter bufferedWriter = new BufferedWriter(
                                        new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))
                        ) {
                            bufferedWriter.write(paramsPost);
                            bufferedWriter.flush();
                        }
                    }

                    int responseCode = urlConnection.getResponseCode();
                    Log.d(TAG, "Code de réponse HTTP: " + responseCode);

                    if (responseCode == HttpsURLConnection.HTTP_OK) {
                        try (InputStream in = new BufferedInputStream(urlConnection.getInputStream())) {
                            result = readStream(in);

                            // Vérifier si la réponse est valide
                            if (result == null || result.isEmpty()) {
                                Log.w(TAG, "Réponse vide du serveur");
                                result = "0Réponse vide du serveur";
                            } else {
                                Log.d(TAG, "Réponse reçue avec succès");
                                return result; // Succès, sortir de la boucle
                            }
                        }
                    } else {
                        // Gérer les différents codes d'erreur HTTP
                        String errorMessage;

                        switch (responseCode) {
                            case HttpsURLConnection.HTTP_NOT_FOUND:
                                errorMessage = "0Service non trouvé (404)";
                                break;
                            case HttpsURLConnection.HTTP_INTERNAL_ERROR:
                                errorMessage = "0Erreur interne du serveur (500)";
                                break;
                            case HttpsURLConnection.HTTP_UNAVAILABLE:
                                errorMessage = "0Service temporairement indisponible (503)";
                                break;
                            case HttpsURLConnection.HTTP_UNAUTHORIZED:
                                errorMessage = "0Non autorisé (401)";
                                break;
                            case HttpsURLConnection.HTTP_FORBIDDEN:
                                errorMessage = "0Accès interdit (403)";
                                break;
                            default:
                                errorMessage = "0Erreur HTTP: " + responseCode;
                        }

                        // Lire le message d'erreur si disponible
                        try (InputStream errorStream = urlConnection.getErrorStream()) {
                            if (errorStream != null) {
                                String errorBody = readStream(errorStream);
                                Log.e(TAG, "Corps de la réponse d'erreur: " + errorBody);
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Erreur lors de la lecture du flux d'erreur", e);
                        }

                        Log.e(TAG, errorMessage);
                        result = errorMessage;
                    }
                } catch (SocketTimeoutException e) {
                    lastException = e;
                    Log.e(TAG, "Timeout lors de la connexion au serveur", e);
                    result = "0Temps d'attente dépassé";
                } catch (UnknownHostException e) {
                    lastException = e;
                    Log.e(TAG, "Hôte inconnu", e);
                    result = "0Serveur non trouvé";
                } catch (SSLException e) {
                    lastException = e;
                    Log.e(TAG, "Erreur SSL", e);
                    result = "0Erreur de sécurité lors de la connexion";
                } catch (IOException e) {
                    lastException = e;
                    Log.e(TAG, "Erreur d'E/S", e);
                    result = "0Erreur de communication";
                } catch (Exception e) {
                    lastException = e;
                    Log.e(TAG, "Exception inattendue", e);
                    result = "0Erreur inattendue: " + e.getMessage();
                } finally {
                    if (urlConnection != null) {
                        try {
                            urlConnection.disconnect();
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur lors de la déconnexion", e);
                        }
                    }
                }

                retryCount++;

                if (retryCount < TIME_OUT) {
                    Log.d(TAG, "Tentative " + retryCount + "/" + TIME_OUT + ", attente avant nouvelle tentative");
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        Log.e(TAG, "Interruption pendant l'attente entre tentatives", ie);
                    }
                }
            }

            if (lastException != null) {
                Log.e(TAG, "Toutes les tentatives ont échoué avec la dernière exception:", lastException);
            }

            return result;
        });
    }

//********* PRIVATE VARIABLES

    private String readStream(InputStream in) {
        if (in == null) {
            return null;
        }

        StringBuilder result = new StringBuilder();
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;

            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }

            if (result.length() > 0 && result.charAt(result.length() - 1) == '\n') {
                result.setLength(result.length() - 1);
            }

            return result.toString();
        } catch (IOException e) {
            Log.e(TAG, "Erreur lors de la lecture du flux", e);
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(TAG, "Erreur lors de la fermeture du lecteur", e);
                }
            }
        }
    }

    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            if (connectivityManager == null) {
                Log.e(TAG, "ConnectivityManager est null");
                return false;
            }

            NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());

            if (networkCapabilities == null) {
                Log.e(TAG, "Aucune capacité réseau détectée");
                return false;
            }

            boolean hasWifi = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
            boolean hasCellular = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);

            boolean isAvailable = hasWifi || hasCellular;
            Log.d(TAG, "État du réseau - WiFi: " + hasWifi + ", Cellulaire: " + hasCellular);

            return isAvailable;
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la vérification de la disponibilité du réseau", e);
            return false;
        }
    }

}

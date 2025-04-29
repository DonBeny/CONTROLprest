package org.orgaprop.controlprest.services;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.util.Log;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

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

import org.orgaprop.controlprest.controllers.activities.LoginActivity;



public class HttpTask {

//********* PRIVATE VARIABLES

    private Context context;
    private static final String TAG = "HttpTask";
    private final ExecutorService executorService;
    private FirebaseCrashlytics crashlytics;
    private FirebaseAnalytics analytics;

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
        try {
            if (context == null) {
                throw new IllegalArgumentException("Le contexte ne peut pas être null");
            }
            this.context = context;
            this.executorService = Executors.newCachedThreadPool();

            // Initialiser Crashlytics
            this.crashlytics = FirebaseCrashlytics.getInstance();
            crashlytics.log("HttpTask initialisé");

            analytics = FirebaseAnalytics.getInstance(context);

            Bundle screenViewParams = new Bundle();
            screenViewParams.putString(FirebaseAnalytics.Param.SCREEN_NAME, "HttpTask");
            screenViewParams.putString(FirebaseAnalytics.Param.SCREEN_CLASS, "HttpTask");
            analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, screenViewParams);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'initialisation de HttpTask", e);
            FirebaseCrashlytics.getInstance();
            FirebaseCrashlytics.getInstance().recordException(e);

            if (context != null) {
                analytics = FirebaseAnalytics.getInstance(context);

                Bundle screenViewParams = new Bundle();
                screenViewParams.putString(FirebaseAnalytics.Param.SCREEN_NAME, "HttpTask");
                screenViewParams.putString(FirebaseAnalytics.Param.SCREEN_CLASS, "HttpTask");
                analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, screenViewParams);
            }

            throw e;
        }
    }

    /**
     * Ferme l'executor service lors de la destruction de l'objet
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            crashlytics.log("HttpTask finalize called");
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            }
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur lors de la finalisation de HttpTask", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "finalize_exception");
            errorParams.putString("class", "HttpTask");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("finalize_app_error", errorParams);
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
            try {
                if (params == null || params.length < 4) {
                    crashlytics.log("Paramètres insuffisants pour executeHttpTask");
                    Log.e(TAG, "Paramètres insuffisants pour executeHttpTask");

                    Bundle errorParams = new Bundle();
                    errorParams.putString("error_type", "insufficient_parameters");
                    errorParams.putString("class", "HttpTask");
                    analytics.logEvent("executeHttpTask_insufficient_parameters", errorParams);

                    return "0Paramètres insuffisants";
                }

                String paramsAct = params[0];
                String paramsCbl = params[1];
                String paramsGet = params[2];
                String paramsPost = params[3];

                crashlytics.setCustomKey("httpTaskAct", paramsAct);
                crashlytics.setCustomKey("httpTaskCbl", paramsCbl);
                crashlytics.log("executeHttpTask: " + paramsAct + "/" + paramsCbl);

                Bundle httpTaskParams = new Bundle();
                httpTaskParams.putString("action", paramsAct);
                httpTaskParams.putString("cible", paramsCbl);
                analytics.logEvent("executeHttpTask", httpTaskParams);

                if (paramsAct.isEmpty() || paramsCbl.isEmpty()) {
                    crashlytics.log("Paramètres 'act' ou 'cbl' manquants");
                    Log.e(TAG, "Paramètres 'act' ou 'cbl' manquants");

                    Bundle errorParams = new Bundle();
                    errorParams.putString("error_type", "missing_parameters");
                    errorParams.putString("class", "HttpTask");
                    analytics.logEvent("executeHttpTask_missing_parameters", errorParams);

                    return "0Paramètres manquants (action ou cible)";
                }
                if (paramsGet == null) paramsGet = "";
                if (paramsPost == null) paramsPost = "";

                if (!isNetworkAvailable()) {
                    crashlytics.log("Pas de connexion réseau disponible");
                    Log.e(TAG, "Pas de connexion réseau disponible");

                    Bundle errorParams = new Bundle();
                    errorParams.putString("error_type", "no_network");
                    errorParams.putString("class", "HttpTask");
                    analytics.logEvent("executeHttpTask_no_network", errorParams);

                    return "0Aucune connexion réseau disponible";
                }

                int retryCount = 0;
                String result = null;
                Exception lastException = null;

                while (retryCount < TIME_OUT) {
                    HttpsURLConnection urlConnection = null;

                    try {
                        String stringUrl = HTTP_ADRESS_SERVER + LoginActivity.ACCESS_CODE + ".php";
                        stringUrl += "?act=" + paramsAct;
                        stringUrl += "&cbl=" + paramsCbl;

                        if (!paramsGet.isEmpty()) {
                            stringUrl += "&" + paramsGet;
                        }

                        crashlytics.log("Tentative de connexion à: " + stringUrl + " (Tentative " + (retryCount + 1) + "/" + TIME_OUT + ")");
                        Log.d(TAG, "Tentative de connexion à: " + stringUrl);

                        Bundle httpTaskParams2 = new Bundle();
                        httpTaskParams2.putString("url", stringUrl);
                        analytics.logEvent("executeHttpTask_url", httpTaskParams2);

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
                            crashlytics.log("Envoi de données POST");
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

                        crashlytics.log("Code de réponse HTTP: " + responseCode);
                        Log.d(TAG, "Code de réponse HTTP: " + responseCode);

                        Bundle httpTaskParams3 = new Bundle();
                        httpTaskParams3.putString("code", String.valueOf(responseCode));
                        analytics.logEvent("executeHttpTask_response_code", httpTaskParams3);

                        if (responseCode == HttpsURLConnection.HTTP_OK) {
                            try (InputStream in = new BufferedInputStream(urlConnection.getInputStream())) {
                                result = readStream(in);

                                // Vérifier si la réponse est valide
                                if (result == null || result.isEmpty()) {
                                    crashlytics.log("Réponse vide du serveur");
                                    Log.w(TAG, "Réponse vide du serveur");

                                    Bundle errorParams = new Bundle();
                                    errorParams.putString("error_type", "empty_response");
                                    errorParams.putString("class", "HttpTask");
                                    analytics.logEvent("executeHttpTask_empty_response", errorParams);

                                    result = "0Réponse vide du serveur";
                                } else {
                                    crashlytics.log("Réponse reçue avec succès: " + (result.length() > 100 ? result.substring(0, 100) + "..." : result));
                                    Log.d(TAG, "Réponse reçue avec succès");

                                    Bundle httpTaskParams4 = new Bundle();
                                    httpTaskParams4.putString("response", result);
                                    analytics.logEvent("executeHttpTask_response", httpTaskParams4);

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
                                    crashlytics.log("Corps de la réponse d'erreur: " + errorBody);
                                    Log.e(TAG, "Corps de la réponse d'erreur: " + errorBody);

                                    Bundle httpTaskParams5 = new Bundle();
                                    httpTaskParams5.putString("error_body", errorBody);
                                    analytics.logEvent("executeHttpTask_error_body", httpTaskParams5);
                                }
                            } catch (IOException e) {
                                crashlytics.recordException(e);
                                Log.e(TAG, "Erreur lors de la lecture du flux d'erreur", e);

                                Bundle errorParams = new Bundle();
                                errorParams.putString("error_type", "error_reading_error_stream");
                                errorParams.putString("class", "HttpTask");
                                analytics.logEvent("executeHttpTask_error_stream", errorParams);
                            }

                            crashlytics.log(errorMessage);
                            Log.e(TAG, errorMessage);

                            Bundle errorParams = new Bundle();
                            errorParams.putString("error_type", "http_error");
                            errorParams.putString("class", "HttpTask");
                            errorParams.putString("error_message", errorMessage);
                            analytics.logEvent("executeHttpTask_http_error", errorParams);

                            result = errorMessage;
                        }
                    } catch (SocketTimeoutException e) {
                        lastException = e;

                        crashlytics.recordException(e);
                        crashlytics.log("Timeout lors de la connexion au serveur: " + e.getMessage());
                        Log.e(TAG, "Timeout lors de la connexion au serveur", e);

                        Bundle errorParams = new Bundle();
                        errorParams.putString("error_type", "timeout");
                        errorParams.putString("class", "HttpTask");
                        errorParams.putString("error_message", e.getMessage());
                        analytics.logEvent("executeHttpTask_timeout", errorParams);

                        result = "0Temps d'attente dépassé";
                    } catch (UnknownHostException e) {
                        lastException = e;

                        crashlytics.recordException(e);
                        crashlytics.log("Hôte inconnu: " + e.getMessage());
                        Log.e(TAG, "Hôte inconnu", e);

                        Bundle errorParams = new Bundle();
                        errorParams.putString("error_type", "unknown_host");
                        errorParams.putString("class", "HttpTask");
                        errorParams.putString("error_message", e.getMessage());
                        analytics.logEvent("executeHttpTask_unknown_host", errorParams);

                        result = "0Serveur non trouvé";
                    } catch (SSLException e) {
                        lastException = e;

                        crashlytics.recordException(e);
                        crashlytics.log("Erreur SSL: " + e.getMessage());
                        Log.e(TAG, "Erreur SSL", e);

                        Bundle errorParams = new Bundle();
                        errorParams.putString("error_type", "ssl_error");
                        errorParams.putString("class", "HttpTask");
                        errorParams.putString("error_message", e.getMessage());
                        analytics.logEvent("executeHttpTask_ssl_error", errorParams);

                        result = "0Erreur de sécurité lors de la connexion";
                    } catch (IOException e) {
                        lastException = e;

                        crashlytics.recordException(e);
                        crashlytics.log("Erreur d'E/S: " + e.getMessage());
                        Log.e(TAG, "Erreur d'E/S", e);

                        Bundle errorParams = new Bundle();
                        errorParams.putString("error_type", "io_error");
                        errorParams.putString("class", "HttpTask");
                        errorParams.putString("error_message", e.getMessage());
                        analytics.logEvent("executeHttpTask_io_error", errorParams);

                        result = "0Erreur de communication";
                    } catch (Exception e) {
                        lastException = e;

                        crashlytics.recordException(e);
                        crashlytics.log("Exception inattendue: " + e.getMessage());
                        Log.e(TAG, "Exception inattendue", e);

                        Bundle errorParams = new Bundle();
                        errorParams.putString("error_type", "unexpected_exception");
                        errorParams.putString("class", "HttpTask");
                        errorParams.putString("error_message", e.getMessage());
                        analytics.logEvent("executeHttpTask_unexpected_exception", errorParams);

                        result = "0Erreur inattendue: " + e.getMessage();
                    } finally {
                        if (urlConnection != null) {
                            try {
                                urlConnection.disconnect();
                            } catch (Exception e) {
                                crashlytics.recordException(e);
                                Log.e(TAG, "Erreur lors de la déconnexion", e);

                                Bundle errorParams = new Bundle();
                                errorParams.putString("error_type", "disconnect_error");
                                errorParams.putString("class", "HttpTask");
                                errorParams.putString("error_message", e.getMessage());
                                analytics.logEvent("executeHttpTask_disconnect_error", errorParams);
                            }
                        }
                    }

                    retryCount++;

                    if (retryCount < TIME_OUT) {
                        crashlytics.log("Tentative " + retryCount + "/" + TIME_OUT + ", attente avant nouvelle tentative");
                        Log.d(TAG, "Tentative " + retryCount + "/" + TIME_OUT + ", attente avant nouvelle tentative");

                        Bundle httpTaskParams6 = new Bundle();
                        httpTaskParams6.putString("retry", String.valueOf(retryCount));
                        analytics.logEvent("executeHttpTask_retry", httpTaskParams6);

                        try {
                            Thread.sleep(RETRY_DELAY_MS);
                        } catch (InterruptedException ie) {
                            crashlytics.recordException(ie);
                            Thread.currentThread().interrupt();
                            Log.e(TAG, "Interruption pendant l'attente entre tentatives", ie);

                            Bundle errorParams = new Bundle();
                            errorParams.putString("error_type", "sleep_error");
                            errorParams.putString("class", "HttpTask");
                            errorParams.putString("error_message", ie.getMessage());
                            analytics.logEvent("executeHttpTask_sleep_error", errorParams);
                        }
                    }
                }

                if (lastException != null) {
                    crashlytics.log("Toutes les tentatives ont échoué avec la dernière exception: " + lastException.getMessage());
                    Log.e(TAG, "Toutes les tentatives ont échoué avec la dernière exception:", lastException);

                    Bundle errorParams = new Bundle();
                    errorParams.putString("error_type", "last_exception");
                    errorParams.putString("class", "HttpTask");
                    errorParams.putString("error_message", lastException.getMessage());
                    analytics.logEvent("executeHttpTask_last_exception", errorParams);
                }

                return result;
            } catch (Exception e) {
                crashlytics.recordException(e);
                Log.e(TAG, "Erreur non gérée dans executeHttpTask", e);

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "unexpected_exception");
                errorParams.putString("class", "HttpTask");
                errorParams.putString("error_message", e.getMessage());
                analytics.logEvent("executeHttpTask_unexpected_exception", errorParams);

                return "0Erreur non gérée: " + e.getMessage();
            }
        });
    }

//********* PRIVATE VARIABLES

    private String readStream(InputStream in) {
        if (in == null) {
            crashlytics.log("Stream d'entrée null dans readStream");
            Log.e(TAG, "Stream d'entrée null dans readStream");

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "null_stream");
            errorParams.putString("class", "HttpTask");
            analytics.logEvent("readStream_null_stream", errorParams);

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

            crashlytics.log("Stream lu avec succès, taille: " + result.length() + " octets");
            return result.toString();
        } catch (IOException e) {
            crashlytics.recordException(e);
            crashlytics.log("Erreur lors de la lecture du flux: " + e.getMessage());
            Log.e(TAG, "Erreur lors de la lecture du flux", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "read_error");
            errorParams.putString("class", "HttpTask");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("readStream_read_error", errorParams);

            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    crashlytics.recordException(e);
                    Log.e(TAG, "Erreur lors de la fermeture du lecteur", e);

                    Bundle errorParams = new Bundle();
                    errorParams.putString("error_type", "close_error");
                    errorParams.putString("class", "HttpTask");
                    errorParams.putString("error_message", e.getMessage());
                    analytics.logEvent("readStream_close_error", errorParams);
                }
            }
        }
    }

    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            if (connectivityManager == null) {
                crashlytics.log("ConnectivityManager est null");
                Log.e(TAG, "ConnectivityManager est null");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "null_connectivity_manager");
                errorParams.putString("class", "HttpTask");
                analytics.logEvent("isNetworkAvailable_connectivity_manager", errorParams);

                return false;
            }

            NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());

            if (networkCapabilities == null) {
                crashlytics.log("Aucune capacité réseau détectée");
                Log.e(TAG, "Aucune capacité réseau détectée");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "null_network_capabilities");
                errorParams.putString("class", "HttpTask");
                analytics.logEvent("isNetworkAvailable_network_capabilities", errorParams);

                return false;
            }

            boolean hasWifi = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
            boolean hasCellular = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);

            boolean isAvailable = hasWifi || hasCellular;
            crashlytics.log("État du réseau - WiFi: " + hasWifi + ", Cellulaire: " + hasCellular);
            Log.d(TAG, "État du réseau - WiFi: " + hasWifi + ", Cellulaire: " + hasCellular);

            return isAvailable;
        } catch (Exception e) {
            crashlytics.recordException(e);
            crashlytics.log("Erreur lors de la vérification de la disponibilité du réseau: " + e.getMessage());
            Log.e(TAG, "Erreur lors de la vérification de la disponibilité du réseau", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "network_check_error");
            errorParams.putString("class", "HttpTask");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("isNetworkAvailable_network_check_error", errorParams);

            return false;
        }
    }

}

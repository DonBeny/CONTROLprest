package org.orgaprop.controlprest.services;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
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

    private final Context context;
    private static final String TAG = "HttpTask";
    private final ExecutorService executorService;
    private final FirebaseCrashlytics crashlytics;
    private final FirebaseAnalytics analytics;

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

        // Initialiser Crashlytics et Analytics
        this.crashlytics = FirebaseCrashlytics.getInstance();
        this.analytics = FirebaseAnalytics.getInstance(context);

        logInfo("HttpTask initialisé", "init");
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
                // Validation des paramètres
                if (!validateParams(params)) {
                    return "0Paramètres insuffisants ou invalides";
                }

                String paramsAct = params[0];
                String paramsCbl = params[1];
                String paramsGet = params[2] != null ? params[2] : "";
                String paramsPost = params[3] != null ? params[3] : "";

                crashlytics.setCustomKey("httpTaskAct", paramsAct);
                crashlytics.setCustomKey("httpTaskCbl", paramsCbl);

                logInfo("Exécution de la requête HTTP: " + paramsAct + "/" + paramsCbl, "http_request");

                // Vérification de la connexion réseau
                if (!isNetworkAvailable()) {
                    logWarning("Pas de connexion réseau disponible", "no_network");
                    return "0Aucune connexion réseau disponible";
                }

                // Effectuer les tentatives de connexion
                return performHttpRequest(paramsAct, paramsCbl, paramsGet, paramsPost);
            } catch (Exception e) {
                logException(e, "unexpected_exception", "Erreur non gérée dans executeHttpTask");
                return "0Erreur non gérée: " + e.getMessage();
            }
        }, executorService);
    }

    /**
     * Ferme proprement l'executorService
     */
    public void shutdown() {
        try {
            logInfo("Arrêt de HttpTask", "shutdown");

            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();

                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                    logWarning("ExecutorService forcé à s'arrêter", "force_shutdown");
                }
            }
        } catch (Exception e) {
            logException(e, "shutdown_error", "Erreur lors de l'arrêt de l'ExecutorService");
        }
    }

//********* PRIVATE VARIABLES

    /**
     * Valide les paramètres de la requête HTTP
     */
    private boolean validateParams(String[] params) {
        if (params == null || params.length < 4) {
            logWarning("Paramètres insuffisants pour executeHttpTask", "insufficient_parameters");
            return false;
        }

        String paramsAct = params[0];
        String paramsCbl = params[1];

        if (paramsAct == null || paramsAct.isEmpty() || paramsCbl == null || paramsCbl.isEmpty()) {
            logWarning("Paramètres 'act' ou 'cbl' manquants", "missing_parameters");
            return false;
        }

        return true;
    }

    /**
     * Effectue la requête HTTP avec gestion des tentatives
     */
    private String performHttpRequest(String paramsAct, String paramsCbl, String paramsGet, String paramsPost) {
        int retryCount = 0;
        String result = null;
        Exception lastException = null;

        while (retryCount < TIME_OUT) {
            HttpsURLConnection urlConnection = null;

            try {
                // Construction de l'URL avec Uri.Builder pour éviter les problèmes d'encodage
                Uri.Builder uriBuilder = Uri.parse(HTTP_ADRESS_SERVER + LoginActivity.ACCESS_CODE + ".php").buildUpon();
                uriBuilder.appendQueryParameter("act", paramsAct);
                uriBuilder.appendQueryParameter("cbl", paramsCbl);

                // Ajout des paramètres GET supplémentaires
                if (!paramsGet.isEmpty()) {
                    // Séparation des paramètres GET et ajout au builder
                    String[] getParams = paramsGet.split("&");
                    for (String param : getParams) {
                        String[] keyValue = param.split("=", 2);
                        if (keyValue.length == 2) {
                            uriBuilder.appendQueryParameter(keyValue[0], keyValue[1]);
                        }
                    }
                }

                String stringUrl = uriBuilder.build().toString();
                logInfo("Tentative de connexion à: " + stringUrl + " (Tentative " + (retryCount + 1) + "/" + TIME_OUT + ")", "connection_attempt");

                // Configuration de la connexion
                urlConnection = setupConnection(stringUrl);

                // Envoi des données POST si nécessaire
                if (!paramsPost.isEmpty()) {
                    sendPostData(urlConnection, paramsPost);
                }

                // Lecture de la réponse
                int responseCode = urlConnection.getResponseCode();
                logInfo("Code de réponse HTTP: " + responseCode, "response_code");

                if (responseCode == HttpsURLConnection.HTTP_OK) {
                    // Lecture du flux de réponse dans une méthode séparée avec gestion de ressources
                    try (InputStream in = new BufferedInputStream(urlConnection.getInputStream())) {
                        result = readStream(in);

                        // Vérifier si la réponse est valide
                        if (result == null || result.isEmpty()) {
                            logWarning("Réponse vide du serveur", "empty_response");
                            result = "0Réponse vide du serveur";
                        } else {
                            logInfo("Réponse reçue avec succès", "success_response");
                            return result; // Succès, sortir de la boucle
                        }
                    }
                } else {
                    // Gestion des erreurs HTTP
                    result = handleHttpError(urlConnection, responseCode);
                }
            } catch (Exception e) {
                lastException = e;
                result = handleConnectionException(e);
            } finally {
                // Fermeture sécurisée de la connexion
                closeConnection(urlConnection);
            }

            retryCount++;

            if (retryCount < TIME_OUT) {
                logInfo("Tentative " + retryCount + "/" + TIME_OUT + ", attente avant nouvelle tentative", "retry");

                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    logException(ie, "sleep_error", "Interruption pendant l'attente entre tentatives");
                    Thread.currentThread().interrupt();
                }
            }
        }

        if (lastException != null) {
            logWarning("Toutes les tentatives ont échoué avec la dernière exception: " + lastException.getMessage(), "all_attempts_failed");
        }

        return result;
    }

    /**
     * Configure la connexion HTTPS
     */
    private HttpsURLConnection setupConnection(String stringUrl) throws IOException {
        URL url = new URL(stringUrl);
        HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();

        urlConnection.setReadTimeout(READ_TIMEOUT_MS);
        urlConnection.setConnectTimeout(CONNECTION_TIMEOUT_MS);
        urlConnection.setRequestMethod("POST");
        urlConnection.setDoInput(true);
        urlConnection.setDoOutput(true);
        urlConnection.setUseCaches(false);

        urlConnection.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
        urlConnection.setRequestProperty("Pragma", "no-cache");
        urlConnection.setRequestProperty("Expires", "0");

        return urlConnection;
    }

    /**
     * Envoie les données POST
     */
    private void sendPostData(HttpsURLConnection urlConnection, String paramsPost) throws IOException {
        logInfo("Envoi de données POST", "post_data");

        OutputStream outputStream = null;
        BufferedWriter bufferedWriter = null;

        try {
            outputStream = urlConnection.getOutputStream();
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
            bufferedWriter.write(paramsPost);
            bufferedWriter.flush();
        } finally {
            closeQuietly(bufferedWriter);
            closeQuietly(outputStream);
        }
    }

    /**
     * Gère les erreurs HTTP
     */
    private String handleHttpError(HttpsURLConnection urlConnection, int responseCode) {
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

        // Lecture du corps de la réponse d'erreur
        String errorBody = readErrorBody(urlConnection);
        if (errorBody != null && !errorBody.isEmpty()) {
            logWarning("Corps de la réponse d'erreur: " + errorBody, "error_body");
        }

        logWarning(errorMessage, "http_error");
        return errorMessage;
    }

    /**
     * Lit le corps de la réponse d'erreur
     */
    private String readErrorBody(HttpsURLConnection urlConnection) {
        InputStream errorStream = null;
        try {
            errorStream = urlConnection.getErrorStream();
            if (errorStream != null) {
                return readStream(errorStream);
            }
        } catch (Exception e) {
            logException(e, "error_reading_error_stream", "Erreur lors de la lecture du flux d'erreur");
        } finally {
            closeQuietly(errorStream);
        }
        return null;
    }

    /**
     * Gère les exceptions lors de la connexion
     */
    private String handleConnectionException(Exception e) {
        if (e instanceof SocketTimeoutException) {
            logException(e, "timeout", "Timeout lors de la connexion au serveur");
            return "0Temps d'attente dépassé";
        } else if (e instanceof UnknownHostException) {
            logException(e, "unknown_host", "Hôte inconnu");
            return "0Serveur non trouvé";
        } else if (e instanceof SSLException) {
            logException(e, "ssl_error", "Erreur SSL");
            return "0Erreur de sécurité lors de la connexion";
        } else if (e instanceof IOException) {
            logException(e, "io_error", "Erreur d'E/S");
            return "0Erreur de communication";
        } else {
            logException(e, "unexpected_exception", "Exception inattendue");
            return "0Erreur inattendue: " + e.getMessage();
        }
    }

    /**
     * Ferme la connexion HTTP de manière sécurisée
     */
    private void closeConnection(HttpsURLConnection urlConnection) {
        if (urlConnection != null) {
            try {
                urlConnection.disconnect();
            } catch (Exception e) {
                logException(e, "disconnect_error", "Erreur lors de la déconnexion");
            }
        }
    }

    /**
     * Lit le flux d'entrée
     */
    private String readStream(InputStream in) {
        if (in == null) {
            logWarning("Stream d'entrée null dans readStream", "null_stream");
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

            logInfo("Stream lu avec succès, taille: " + result.length() + " octets", "stream_read");
            return result.toString();
        } catch (IOException e) {
            logException(e, "read_error", "Erreur lors de la lecture du flux");
            return null;
        } finally {
            closeQuietly(reader);
        }
    }



    /**
     * Ferme silencieusement une ressource Closeable
     */
    private void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                logException(e, "close_error", "Erreur lors de la fermeture d'une ressource");
            }
        }
    }

    /**
     * Vérifie si une connexion réseau est disponible
     */
    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            if (connectivityManager == null) {
                logWarning("ConnectivityManager est null", "null_connectivity_manager");
                return false;
            }

            NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());

            if (networkCapabilities == null) {
                logWarning("Aucune capacité réseau détectée", "null_network_capabilities");
                return false;
            }

            boolean hasWifi = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
            boolean hasCellular = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);

            boolean isAvailable = hasWifi || hasCellular;
            logInfo("État du réseau - WiFi: " + hasWifi + ", Cellulaire: " + hasCellular, "network_state");

            return isAvailable;
        } catch (Exception e) {
            logException(e, "network_check_error", "Erreur lors de la vérification de la disponibilité du réseau");
            return false;
        }
    }

//********* LOGGING METHODS

    private void logInfo(String message, String infoType) {
        try {
            crashlytics.log("INFO: " + message);
            Log.i(TAG, message);

            Bundle params = new Bundle();
            params.putString("info_type", infoType);
            params.putString("class", "HttpTask");
            params.putString("info_message", message);
            if (analytics != null) {
                analytics.logEvent("app_info", params);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in logInfo: " + e.getMessage());
        }
    }

    private void logWarning(String message, String warningType) {
        try {
            crashlytics.log("WARNING: " + message);
            Log.w(TAG, message);

            Bundle params = new Bundle();
            params.putString("warning_type", warningType);
            params.putString("class", "HttpTask");
            params.putString("warning_message", message);
            if (analytics != null) {
                analytics.logEvent("app_warning", params);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in logWarning: " + e.getMessage());
        }
    }

    private void logError(String message, String errorType) {
        try {
            crashlytics.log("ERROR: " + message);
            Log.e(TAG, message);

            Bundle params = new Bundle();
            params.putString("error_type", errorType);
            params.putString("class", "HttpTask");
            params.putString("error_message", message);
            if (analytics != null) {
                analytics.logEvent("app_error", params);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in logError: " + e.getMessage());
        }
    }

    private void logException(Exception e, String errorType, String context) {
        try {
            crashlytics.recordException(e);
            String errorMessage = e.getMessage() != null ? e.getMessage() : "No message";
            crashlytics.log("EXCEPTION: " + context + ": " + errorMessage);
            Log.e(TAG, context + ": " + errorMessage, e);

            Bundle params = new Bundle();
            params.putString("error_type", errorType);
            params.putString("class", "HttpTask");
            params.putString("error_message", errorMessage);
            params.putString("error_context", context);
            if (analytics != null) {
                analytics.logEvent("app_exception", params);
            }
        } catch (Exception loggingEx) {
            Log.e(TAG, "Error in logException: " + loggingEx.getMessage());
        }
    }

}

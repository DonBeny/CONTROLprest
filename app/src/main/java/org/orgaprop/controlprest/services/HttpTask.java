package org.orgaprop.controlprest.services;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.orgaprop.controlprest.R;
import org.orgaprop.controlprest.controllers.activities.LoginActivity;
import org.orgaprop.controlprest.utils.AndyUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.net.ssl.HttpsURLConnection;

public class HttpTask {

//********* PRIVATE VARIABLES

    private Context context;

//********* STATIC VARIABLES

    private static final String TAG = "HttpTask";
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

//********* CONSTRUCTORS

    public HttpTask(Context context) {
        this.context = context;
    }

//********* PUBLIC VARIABLES

    public CompletableFuture<String> executeHttpTask(String... params) {
        return CompletableFuture.supplyAsync(() -> {
            int retryCount = 0;

            while ( retryCount < TIME_OUT ) {
                if( !isNetworkAvailable() ) {
                    return "No internet connection";
                }

                String stringUrl = HTTP_ADRESS_SERVER;
                String paramsAct = params[0];
                String paramsCbl = params[1];
                String paramsGet = params[2];
                String paramsPost = params[3];

                if( !paramsAct.isEmpty() && !paramsCbl.isEmpty() ) {
                    URL url;
                    HttpsURLConnection urlConnection = null;
                    String result = null;

                    stringUrl += LoginActivity.ACCESS_CODE + ".php";
                    stringUrl += "?act=" + paramsAct;
                    stringUrl += "&cbl=" + paramsCbl;

                    if( !paramsGet.isEmpty() ) {
                        stringUrl += "&" + paramsGet;
                    }

                    try {
                        url = new URL(stringUrl);

                        urlConnection = (HttpsURLConnection) url.openConnection();

                        urlConnection.setReadTimeout(AndyUtils.LAPS_TIME_TEST_CONNECT);
                        urlConnection.setConnectTimeout(AndyUtils.LAPS_TIME_TEST_CONNECT);
                        urlConnection.setRequestMethod("POST");
                        urlConnection.setDoInput(true);
                        urlConnection.setDoOutput(true);

                        try (
                            OutputStream outputStream = urlConnection.getOutputStream();
                            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))
                        ) {
                            bufferedWriter.write(paramsPost);
                        }

                        int responseCode = urlConnection.getResponseCode();

                        if( responseCode == HttpsURLConnection.HTTP_OK ) {
                            try ( InputStream in = new BufferedInputStream(urlConnection.getInputStream()) ) {
                                result = readStream(in);
                            }
                        }

                        return result;
                    } catch (IOException e) {
                        retryCount++;

                        try {
                            Thread.sleep(RETRY_DELAY_MS);
                        } catch (InterruptedException ie) {
                            //Log.d(TAG, functionName+"InterruptedException occurred: " + ie.getMessage(), ie);
                        }
                    } finally {
                        if (urlConnection != null) {
                            urlConnection.disconnect();
                        }
                    }
                } else {
                    return "0Parametres manquants !!!";
                }
            }

            return "0Request timed out";
        });
    }

//********* PRIVATE VARIABLES

    private String readStream(InputStream in) {
        StringBuilder result = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;

        try {
            while( ( line = reader.readLine() ) != null) {
                result.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace(); // Gestion de l'erreur, tu peux aussi retourner un message d'erreur
        } finally {
            try {
                reader.close(); // Assurez-vous de fermer le BufferedReader
            } catch (IOException e) {
                e.printStackTrace(); // Gérer l'exception lors de la fermeture
            }
        }

        return result.toString().trim(); // Retourne le résultat sous forme de chaîne, sans saut de ligne final
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());

            if( networkCapabilities != null ) {
                return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
            }
        }

        return false;
    }

}

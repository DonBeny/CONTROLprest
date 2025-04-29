package org.orgaprop.controlprest.controllers.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.StringTokenizer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.orgaprop.controlprest.R;
import org.orgaprop.controlprest.databinding.ActivityLoginBinding;
import org.orgaprop.controlprest.services.HttpTask;
import org.orgaprop.controlprest.utils.AndyUtils;
import org.orgaprop.controlprest.utils.ToastManager;
import org.orgaprop.controlprest.services.PreferencesManager;

public class LoginActivity extends AppCompatActivity {

//********* PRIVATE VARIABLES

    private static final String TAG = "LoginActivity";

    private static LoginActivity mLoginActivity;

    private SharedPreferences preferences;
    private PreferencesManager preferencesManager;
    private boolean isFirst;
    private AtomicBoolean isConnecting;
    private String userName;
    private String password;
    private FirebaseCrashlytics crashlytics;
    private FirebaseAnalytics analytics;

//********* PUBLIC VARIABLES

    public static final int VERSION = 1;

    public static String idMbr;
    public static String adrMac;
    public static String id_client;

    public static String phoneName = Build.MANUFACTURER + " " + Build.DEVICE;
    public static String phoneModel = Build.MODEL;
    public static String phoneBuild = Build.FINGERPRINT;

    public static final String PREF_NAME_APPLI = "ControlPrest";
    public static final String PREF_KEY_MBR = "mbr";
    public static final String PREF_KEY_PWD = "pwd";
    public static final String PREF_KEY_CLT = "clt";

    public static final int UPDATE_REQUEST_CODE = 100;

    public static final String ACCESS_CODE = "controlprest";

//********* WIDGETS

    private ActivityLoginBinding binding;

//********* CONSTRUCTORS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            crashlytics = FirebaseCrashlytics.getInstance();
            crashlytics.setCustomKey("deviceModel", Build.MODEL);
            crashlytics.setCustomKey("deviceManufacturer", Build.MANUFACTURER);
            crashlytics.log("LoginActivity démarrée");

            analytics = FirebaseAnalytics.getInstance(this);

            Bundle screenViewParams = new Bundle();
            screenViewParams.putString(FirebaseAnalytics.Param.SCREEN_NAME, "Login");
            screenViewParams.putString(FirebaseAnalytics.Param.SCREEN_CLASS, "LoginActivity");
            analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, screenViewParams);

            binding = ActivityLoginBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            // Initialiser les variables
            mLoginActivity = this;
            isConnecting = new AtomicBoolean(false);

            preferences = getSharedPreferences(PREF_NAME_APPLI, MODE_PRIVATE);
            preferencesManager = PreferencesManager.getInstance(this);
            preferencesManager.initializeDefaultPrefsIfNeeded();

            userName = preferences.getString(PREF_KEY_MBR, "");
            password = preferences.getString(PREF_KEY_PWD, "");
            idMbr = "new";
            adrMac = Build.FINGERPRINT;
            isFirst = true;
            id_client = null;

            // Configuration de l'UI
            EditText mUserName = binding.loginActivityUsernameTxt;
            EditText mUserPwd = binding.loginActivityPasswordTxt;

            mUserName.setText(userName);
            mUserPwd.setText(password);

            crashlytics.setCustomKey("userName", userName.isEmpty() ? "empty" : "set");
            crashlytics.setCustomKey("phoneName", phoneName);
            crashlytics.setCustomKey("phoneModel", phoneModel);

            Bundle loginParams = new Bundle();
            loginParams.putString("class", "LoginActivity");
            loginParams.putString("phone_name", phoneName);
            loginParams.putString("phone_model", phoneModel);
            analytics.logEvent("onCreate_login_params", loginParams);

            // Vérifier les permissions essentielles
            checkEssentialPermissions();
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur lors de l'initialisation de LoginActivity", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "app_error");
            errorParams.putString("class", "LoginActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("onCreate_app_error", errorParams);

            ToastManager.showError(getString(R.string.erreur_lors_de_l_initialisation_de_l_application));
            finish();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();

        try {
            crashlytics.log("onResume LoginActivity");

            if (!isConnecting.get()) {
                testIdentified();
            }
            showWait(false);
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur dans onResume", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "app_error");
            errorParams.putString("class", "LoginActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("onResume_app_error", errorParams);

            ToastManager.showError(getString(R.string.erreur_lors_du_d_marrage_de_l_application));
            showWait(false);
        }
    }
    @Override
    protected void onPostResume() {
        super.onPostResume();

        try {
            crashlytics.log("onPostResume LoginActivity");

            if (!isFirst) {
                if (idMbr.equals("new")) {
                    EditText mUserName = binding.loginActivityUsernameTxt;
                    EditText mUserPwd = binding.loginActivityPasswordTxt;

                    mUserName.setText("");
                    mUserPwd.setText("");

                    preferencesManager.setMbrId("new");

                    openConexion();
                } else {
                    openDeco();
                }
            }
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur dans onPostResume", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "app_error");
            errorParams.putString("class", "LoginActivity");
            errorParams.putString("method", "onPostResume");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("onResume_app_error", errorParams);

            ToastManager.showError(getString(R.string.erreur_lors_du_d_marrage_de_l_application));
        }
    }

//********* SURCHARGES

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        try {
            crashlytics.log("onRequestPermissionsResult: " + requestCode);

            if (requestCode == AndyUtils.PERMISSION_REQUEST) {
                // Vérifier les permissions réseau
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    String msg = getString(R.string.mess_bad_permission_internet);

                    crashlytics.log("Permission refusée: " + permissions[0]);
                    Log.e(TAG, msg);

                    Bundle errorParams = new Bundle();
                    errorParams.putString("error_type", "permission_error");
                    errorParams.putString("class", "LoginActivity");
                    errorParams.putString("error_message", msg);
                    analytics.logEvent("onRequestPermissionsResult_network_error", errorParams);

                    ToastManager.showError(msg);
                    finish();
                    return;
                }

                // Vérifier les permissions de stockage si demandées
                if (grantResults.length > 1 && grantResults[1] != PackageManager.PERMISSION_GRANTED) {
                    String msg = getString(R.string.mess_bad_permission_write);
                    crashlytics.log("Permission refusée: " + permissions[1]);
                    Log.e(TAG, msg);

                    Bundle errorParams = new Bundle();
                    errorParams.putString("error_type", "permission_error");
                    errorParams.putString("class", "LoginActivity");
                    errorParams.putString("error_message", msg);
                    analytics.logEvent("onRequestPermissionsResult_write_error", errorParams);

                    ToastManager.showError(msg);
                }
            }
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur lors du traitement des résultats de permission", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "permission_error");
            errorParams.putString("class", "LoginActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("onRequestPermissionsResult_app_error", errorParams);

            ToastManager.showError(getString(R.string.erreur_lors_du_traitement_des_r_sultats_de_permission));
        }
    }

//********* PUBLIC FUNCTIONS

    public static LoginActivity getInstance() {
        return mLoginActivity;
    }

    public void loginActivityActions(View v) {
        if (v == null || v.getTag() == null) {
            crashlytics.log("loginActivityActions: View or tag null");
            Log.e(TAG, "Vue ou tag null dans loginActivityActions");

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "view_error");
            errorParams.putString("class", "LoginActivity");
            errorParams.putString("error_message", "View or tag null");
            analytics.logEvent("loginActivityActions_view_error", errorParams);

            return;
        }

        try {
            String tag = v.getTag().toString();

            crashlytics.log("loginActivityActions: " + tag);

            switch (tag) {
                case "on":
                    connectMbr();
                    break;
                case "off":
                    deconectMbr();
                    break;
                case "robot":
                    requestConexion(HttpTask.HTTP_TASK_CBL_ROBOT);
                    break;
                case "mail":
                    requestConexion(HttpTask.HTTP_TASK_CBL_MAIL);
                    break;
                case "rgpd":
                    openWebPage();
                    break;
                default:
                    crashlytics.log("Tag non reconnu: " + tag);
                    Log.w(TAG, "Tag non reconnu: " + tag);

                    Bundle errorParams = new Bundle();
                    errorParams.putString("error_type", "tag_error");
                    errorParams.putString("class", "LoginActivity");
                    errorParams.putString("method", "loginActivityActions");
                    errorParams.putString("error_message", "Tag non reconnu: " + tag);
                    analytics.logEvent("app_error", errorParams);

                    break;
            }
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur lors du traitement de l'action: " + e.getMessage(), e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "action_error");
            errorParams.putString("class", "LoginActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("loginActivityActions_action_error", errorParams);
        }
    }

//********* PRIVATE FUNCTIONS

    private void checkEssentialPermissions() {
        try {
            crashlytics.log("checkEssentialPermissions called");

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.INTERNET,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, AndyUtils.PERMISSION_REQUEST);
            }
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur lors de la vérification des permissions", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "permission_error");
            errorParams.putString("class", "LoginActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("checkEssentialPermissions_app_error", errorParams);

            ToastManager.showError(getString(R.string.erreur_lors_de_la_v_rification_des_permissions));
        }
    }

    private void connectMbr() {
        if (isConnecting.getAndSet(true)) {
            String msg = getString(R.string.une_tentative_de_connexion_est_d_j_en_cours);

            crashlytics.log(msg);
            Log.d(TAG, msg);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "connection_error");
            errorParams.putString("class", "LoginActivity");
            errorParams.putString("error_message", msg);
            analytics.logEvent("connectMbr_connection_error", errorParams);

            ToastManager.showError(msg);
            return;
        }

        try {
            crashlytics.log("connectMbr called");

            EditText mUserName = binding.loginActivityUsernameTxt;
            EditText mUserPwd = binding.loginActivityPasswordTxt;
            CheckBox mCheckBox = binding.loginActivityRgpdChx;

            userName = mUserName.getText().toString().trim();
            password = mUserPwd.getText().toString().trim();

            if (userName.isEmpty() || password.isEmpty()) {
                String msg = getString(R.string.mess_err_conex);

                isConnecting.set(false);

                crashlytics.log(msg);
                Log.d(TAG, msg);

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "connection_error");
                errorParams.putString("class", "LoginActivity");
                errorParams.putString("error_message", msg);
                analytics.logEvent("connectMbr_auth_error", errorParams);

                ToastManager.showShort(msg);

                return;
            }

            if (!mCheckBox.isChecked()) {
                isConnecting.set(false);

                crashlytics.log("Erreur de connexion: non accepté RGPD");
                Log.d(TAG, "Erreur de connexion: non accepté RGPD");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "rgpd_error");
                errorParams.putString("class", "LoginActivity");
                errorParams.putString("error_message", "Non accepté RGPD");
                analytics.logEvent("connectMbr_rgpd_error", errorParams);

                ToastManager.showShort(getString(R.string.mess_err_rgpd));

                return;
            }

            hideKeyboard();

            if (!AndyUtils.isNetworkAvailable(this)) {
                isConnecting.set(false);

                crashlytics.log("Erreur de connexion: pas de connexion réseau");
                Log.d(TAG, "Erreur de connexion: pas de connexion réseau");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "connection_error");
                errorParams.putString("class", "LoginActivity");
                errorParams.putString("error_message", "Pas de connexion réseau");
                analytics.logEvent("connectMbr_network_error", errorParams);

                ToastManager.showShort(getString(R.string.mess_conextion_lost));

                return;
            }

            showWait(true);
            crashlytics.log("Préparation de la requête HTTP");

            try {
                String mac = Build.FINGERPRINT;
                String encodedPwd = URLEncoder.encode(password, "utf-8");
                String stringGet = "version=" + VERSION + "&phone=" +
                        URLEncoder.encode(phoneName, "utf-8") + "&model=" +
                        URLEncoder.encode(phoneModel, "utf-8") + "&build=" +
                        URLEncoder.encode(phoneBuild, "utf-8");
                String stringPost = "psd=" + URLEncoder.encode(userName, "utf-8") +
                        "&pwd=" + encodedPwd + "&mac=" +
                        URLEncoder.encode(mac, "utf-8");

                HttpTask task = new HttpTask(this);

                crashlytics.log("Exécution de la requête de connexion");

                CompletableFuture<String> futureResult = task.executeHttpTask(
                        HttpTask.HTTP_TASK_ACT_CONEX,
                        HttpTask.HTTP_TASK_CBL_OK,
                        stringGet,
                        stringPost);

                futureResult.thenAccept(result -> {
                    try {
                        if (result == null) {
                            isConnecting.set(false);

                            crashlytics.log("Erreur de communication avec le serveur");
                            Log.e(TAG, "Erreur de communication avec le serveur");

                            runOnUiThread(() -> {
                                ToastManager.showError(getString(R.string.erreur_de_communication_avec_le_serveur));
                                showWait(false);
                            });

                            return;
                        }

                        crashlytics.log("Réponse reçue: " + (result.startsWith("1") ? "Succès" : "Échec"));
                        Log.d(TAG, "Réponse reçue: " + result);

                        Bundle loginParams = new Bundle();
                        loginParams.putString("class", "LoginActivity");
                        loginParams.putString("result", result);
                        analytics.logEvent("connectMbr_login_result", loginParams);

                        if (result.startsWith("1")) {
                            startAppli(result.substring(1));
                        } else {
                            String errorMessage = result.length() > 1 ? result.substring(1) : "Erreur de connexion";

                            isConnecting.set(false);

                            crashlytics.log("Erreur de connexion: " + errorMessage);
                            Log.e(TAG, errorMessage);

                            Bundle errorParams = new Bundle();
                            errorParams.putString("error_type", "connection_error");
                            errorParams.putString("class", "LoginActivity");
                            errorParams.putString("error_message", errorMessage);
                            analytics.logEvent("connectMbr_connection_error", errorParams);

                            runOnUiThread(() -> {
                                ToastManager.showError(errorMessage);
                                showWait(false);
                            });
                        }
                    } catch (Exception e) {
                        isConnecting.set(false);

                        crashlytics.recordException(e);
                        Log.e(TAG, "Erreur lors du traitement de la réponse", e);

                        Bundle errorParams = new Bundle();
                        errorParams.putString("error_type", "connection_error");
                        errorParams.putString("class", "LoginActivity");
                        errorParams.putString("error_message", e.getMessage());
                        analytics.logEvent("connectMbr_connection_error", errorParams);

                        runOnUiThread(() -> {
                            ToastManager.showError(getString(R.string.erreur_lors_du_traitement_de_la_r_ponse));
                            showWait(false);
                        });
                    }
                }).exceptionally(ex -> {
                    isConnecting.set(false);

                    crashlytics.recordException(ex);
                    Log.e(TAG, "Exception lors de la connexion", ex);

                    Bundle errorParams = new Bundle();
                    errorParams.putString("error_type", "connection_error");
                    errorParams.putString("class", "LoginActivity");
                    errorParams.putString("error_message", ex.getMessage());
                    analytics.logEvent("connectMbr_connection_error", errorParams);

                    runOnUiThread(() -> {
                        ToastManager.showError(getString(R.string.mess_timeout));
                        showWait(false);
                    });
                    return null;
                });
            } catch (UnsupportedEncodingException e) {
                isConnecting.set(false);

                crashlytics.recordException(e);
                Log.e(TAG, "Erreur d'encodage des paramètres", e);

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "connection_error");
                errorParams.putString("class", "LoginActivity");
                errorParams.putString("error_message", e.getMessage());
                analytics.logEvent("connectMbr_connection_error", errorParams);

                ToastManager.showError(getString(R.string.erreur_d_encodage_des_param_tres));
                showWait(false);
            } catch (Exception e) {
                isConnecting.set(false);

                crashlytics.recordException(e);
                Log.e(TAG, "Erreur lors de la connexion", e);

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "connection_error");
                errorParams.putString("class", "LoginActivity");
                errorParams.putString("error_message", e.getMessage());
                analytics.logEvent("connectMbr_connection_error", errorParams);

                ToastManager.showError(getString(R.string.erreur_lors_de_la_tentative_de_connexion));
                showWait(false);
            }
        } catch (Exception e) {
            isConnecting.set(false);

            crashlytics.recordException(e);
            Log.e(TAG, "Erreur générale dans connectMbr", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "connection_error");
            errorParams.putString("class", "LoginActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("connectMbr_connection_error", errorParams);

            ToastManager.showError(getString(R.string.erreur_lors_de_la_connexion));
            showWait(false);
        }
    }
    private void deconectMbr() {
        if (isConnecting.getAndSet(true)) {
            crashlytics.log("Une opération est déjà en cours");
            Log.d(TAG, "Une opération est déjà en cours");

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "connection_error");
            errorParams.putString("class", "LoginActivity");
            errorParams.putString("error_message", "Une opération est déjà en cours");
            analytics.logEvent("deconectMbr_connection_error", errorParams);

            ToastManager.showShort(getString(R.string.une_op_ration_est_d_j_en_cours));
            return;
        }

        crashlytics.log("deconectMbr called");
        showWait(true);

        try {
            String mac = Build.FINGERPRINT;
            String stringGet = "version=" + VERSION + "&phone=" +
                    URLEncoder.encode(phoneName, "utf-8") + "&model=" +
                    URLEncoder.encode(phoneModel, "utf-8") + "&build=" +
                    URLEncoder.encode(phoneBuild, "utf-8");
            String stringPost = "mbr=" + idMbr + "&mac=" + URLEncoder.encode(mac, "utf-8");

            HttpTask task = new HttpTask(this);

            crashlytics.log("Exécution de la requête de déconnexion");

            CompletableFuture<String> futureResult = task.executeHttpTask(
                    HttpTask.HTTP_TASK_ACT_CONEX,
                    HttpTask.HTTP_TASK_CBL_NO,
                    stringGet,
                    stringPost);

            futureResult.thenAccept(result -> {
                try {
                    if (result == null) {
                        String msg = getString(R.string.erreur_de_communication_avec_le_serveur);

                        isConnecting.set(false);

                        crashlytics.log(msg);
                        Log.e(TAG, msg);

                        Bundle errorParams = new Bundle();
                        errorParams.putString("error_type", "connection_error");
                        errorParams.putString("class", "LoginActivity");
                        errorParams.putString("error_message", msg);
                        analytics.logEvent("deconectMbr_connection_error", errorParams);

                        runOnUiThread(() -> {
                            ToastManager.showShort(msg);
                            showWait(false);
                            isConnecting.set(false);
                        });

                        return;
                    }

                    boolean success = result.startsWith("1");
                    crashlytics.log("Déconnexion réussie: " + success);
                    Log.d(TAG, "Déconnexion réussie: " + success);

                    Bundle loginParams = new Bundle();
                    loginParams.putString("class", "LoginActivity");
                    loginParams.putString("result", result);
                    analytics.logEvent("deconectMbr_login_result", loginParams);

                    String message = success ? "Déconnexion réussie" : (result.length() > 1 ? result.substring(1) : "Erreur de déconnexion");

                    isConnecting.set(false);

                    // Réinitialiser les préférences même en cas d'erreur
                    resetUserPreferences();

                    runOnUiThread(() -> {
                        ToastManager.showShort(message);
                        finish();
                    });
                } catch (Exception e) {
                    isConnecting.set(false);

                    crashlytics.recordException(e);
                    Log.e(TAG, "Erreur lors du traitement de la déconnexion", e);

                    Bundle errorParams = new Bundle();
                    errorParams.putString("error_type", "connection_error");
                    errorParams.putString("class", "LoginActivity");
                    errorParams.putString("error_message", e.getMessage());
                    analytics.logEvent("deconectMbr_connection_error", errorParams);

                    resetUserPreferences();
                    runOnUiThread(() -> {
                        ToastManager.showError(getString(R.string.erreur_lors_de_la_d_connexion));
                        finish();
                    });
                }
            }).exceptionally(ex -> {
                isConnecting.set(false);

                crashlytics.recordException(ex);
                Log.e(TAG, "Exception lors de la déconnexion", ex);

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "connection_error");
                errorParams.putString("class", "LoginActivity");
                errorParams.putString("error_message", ex.getMessage());
                analytics.logEvent("deconectMbr_connection_error", errorParams);

                resetUserPreferences();
                runOnUiThread(() -> {
                    ToastManager.showError(getString(R.string.mess_timeout));
                    finish();
                });
                return null;
            });
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur lors de la déconnexion", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "connection_error");
            errorParams.putString("class", "LoginActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("deconectMbr_app_error", errorParams);

            resetUserPreferences();
            ToastManager.showError(getString(R.string.erreur_lors_de_la_d_connexion));
            showWait(false);
            isConnecting.set(false);
            finish();
        }
    }

    private void resetUserPreferences() {
        try {
            crashlytics.log("resetUserPreferences called");

            EditText mUserName = binding.loginActivityUsernameTxt;
            EditText mUserPwd = binding.loginActivityPasswordTxt;

            mUserName.setText("");
            mUserPwd.setText("");

            idMbr = "new";
            isFirst = true;

            // Effacer les informations enregistrées
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.apply();

            // Réinitialiser les préférences gérées par PreferencesManager
            preferencesManager.setMbrId("new");
            preferencesManager.setAdrMac("new");
            preferencesManager.setAgency("");
            preferencesManager.setGroup("");
            preferencesManager.setResidence("");
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur lors de la réinitialisation des préférences", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "preferences_error");
            errorParams.putString("class", "LoginActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("resetUserPreferences_app_error", errorParams);

            ToastManager.showError(getString(R.string.erreur_lors_de_la_r_initialisation_des_pr_f_rences));
        }
    }

    private void requestConexion(String m) {
        try {
            crashlytics.log("requestConexion: " + m);
            Log.d(TAG, "requestConexion: " + m);

            Intent intent = new Intent(this, GetMailActivity.class);
            intent.putExtra(GetMailActivity.GET_MAIL_ACTIVITY_TYPE, m);
            startActivity(intent);
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur lors de la requête de connexion", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "connection_error");
            errorParams.putString("class", "LoginActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("requestConexion_app_error", errorParams);

            ToastManager.showError(getString(R.string.erreur_lors_de_l_ouverture_de_l_cran_de_demande));
        }
    }

    private void openWebPage() {
        try {
            crashlytics.log("openWebPage called");

            String url = "https://www.orgaprop.org/ress/protectDonneesPersonnelles.html";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur lors de l'ouverture de la page web", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "ui_error");
            errorParams.putString("class", "LoginActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("openWebPage_app_error", errorParams);

            ToastManager.showError(getString(R.string.impossible_d_ouvrir_la_page_web));
        }
    }

    private void startAppli(String result) {
        try {
            crashlytics.log("startAppli called");

            if (result == null || result.isEmpty()) {
                isConnecting.set(false);

                crashlytics.log("Réponse vide du serveur");
                Log.e(TAG, "Réponse vide du serveur");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "exec_error");
                errorParams.putString("class", "LoginActivity");
                errorParams.putString("error_message", "Réponse vide du serveur");
                analytics.logEvent("startAppli_result_error", errorParams);

                runOnUiThread(() -> {
                    ToastManager.showError(getString(R.string.r_ponse_invalide_du_serveur));
                    showWait(false);
                });
                return;
            }

            StringTokenizer tokenizer = new StringTokenizer(result, "#");

            if (tokenizer.countTokens() < 5) {
                isConnecting.set(false);

                crashlytics.log("Réponse incomplète du serveur: " + result);
                Log.e(TAG, "Réponse incomplète du serveur: " + result);

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "exec_error");
                errorParams.putString("class", "LoginActivity");
                errorParams.putString("error_message", "Réponse incomplète du serveur: " + result);
                analytics.logEvent("startAppli_result_error", errorParams);

                runOnUiThread(() -> {
                    ToastManager.showError(getString(R.string.r_ponse_incompl_te_du_serveur));
                    showWait(false);
                });
                return;
            }

            int version = Integer.parseInt(tokenizer.nextToken());
            crashlytics.log("Version serveur: " + version);

            if (version == VERSION) {
                idMbr = tokenizer.nextToken();
                adrMac = tokenizer.nextToken();
                id_client = tokenizer.nextToken();
                isFirst = false;

                crashlytics.setUserId(idMbr);
                crashlytics.setCustomKey("id_client", id_client);
                crashlytics.log("Connexion réussie, idMbr: " + idMbr);

                Bundle loginParams = new Bundle();
                loginParams.putString("error_type", "exec_error");
                loginParams.putString("class", "LoginActivity");
                loginParams.putString("method", "startAppli");
                loginParams.putString("id_mbr", idMbr);
                analytics.logEvent("startAppli_login_result", loginParams);

                // Enregistrer les informations de l'utilisateur
                preferencesManager.setMbrId(idMbr);
                preferencesManager.setAdrMac(adrMac);

                // Conserver les identifiants pour la reconnexion
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(PREF_KEY_MBR, userName);
                editor.putString(PREF_KEY_PWD, password);
                editor.putString(PREF_KEY_CLT, id_client);
                editor.apply();

                isConnecting.set(false);

                // Démarrer l'activité de sélection
                String agencesData = tokenizer.nextToken();

                crashlytics.log("Lancement de SelectActivity");
                Log.d(TAG, "Lancement de SelectActivity");

                Intent intent = new Intent(this, SelectActivity.class);
                intent.putExtra(SelectActivity.SELECT_ACTIVITY_EXTRA, agencesData);
                startActivity(intent);
            } else {
                isConnecting.set(false);

                crashlytics.log("Version incompatible: " + version + " vs " + VERSION);
                Log.w(TAG, "Version incompatible: " + version + " vs " + VERSION);

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "exec_error");
                errorParams.putString("class", "LoginActivity");
                errorParams.putString("error_message", "Version incompatible: " + version + " vs " + VERSION);
                analytics.logEvent("startAppli_version_error", errorParams);

                runOnUiThread(this::openVersion);
            }
        } catch (NumberFormatException e) {
            isConnecting.set(false);

            crashlytics.recordException(e);
            Log.e(TAG, "Format de version invalide", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "exec_error");
            errorParams.putString("class", "LoginActivity");
            errorParams.putString("error_message", "Format de version invalide");
            analytics.logEvent("startAppli_version_error", errorParams);

            runOnUiThread(() -> {
                ToastManager.showError(getString(R.string.format_de_r_ponse_invalide));
                showWait(false);
            });
        } catch (Exception e) {
            isConnecting.set(false);

            crashlytics.recordException(e);
            Log.e(TAG, "Erreur lors du démarrage de l'application", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "exec_error");
            errorParams.putString("class", "LoginActivity");
            errorParams.putString("method", "startAppli");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("startAppli_app_error", errorParams);

            runOnUiThread(() -> {
                ToastManager.showError(getString(R.string.erreur_lors_du_d_marrage_de_l_application));
                showWait(false);
            });
        }
    }
    private void testIdentified() {
        try {
            crashlytics.log("testIdentified called");

            if (!AndyUtils.isNetworkAvailable(this)) {
                crashlytics.log("Aucune connexion réseau disponible");
                Log.d(TAG, "Aucune connexion réseau disponible");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "connection_error");
                errorParams.putString("class", "LoginActivity");
                errorParams.putString("error_message", "Aucune connexion réseau disponible");
                analytics.logEvent("testIdentified_no_network", errorParams);

                ToastManager.showShort(getString(R.string.mess_conextion_lost));
                openConexion();
                return;
            }

            showWait(true);

            idMbr = preferencesManager.getMbrId();
            adrMac = Build.FINGERPRINT;

            if (idMbr == null || idMbr.isEmpty() || idMbr.equals("new") || adrMac == null || adrMac.isEmpty()) {
                return;
            }

            String stringGet = "version=" + VERSION;
            String stringPost = "mbr=" + idMbr + "&mac=" + adrMac;

            HttpTask task = new HttpTask(LoginActivity.this);

            crashlytics.log("Exécution de la requête testIdentified");
            crashlytics.setCustomKey("id_mbr", idMbr);
            crashlytics.setCustomKey("adr_mac", adrMac);

            Log.d(TAG, "Exécution de la requête testIdentified");
            Log.d(TAG, "id_mbr: " + idMbr);
            Log.d(TAG, "adr_mac: " + adrMac);

            Bundle loginParams = new Bundle();
            loginParams.putString("error_type", "connection_error");
            loginParams.putString("class", "LoginActivity");
            loginParams.putString("id_mbr", idMbr);
            loginParams.putString("adr_mac", adrMac);
            analytics.logEvent("testIdentified_login_result", loginParams);

            CompletableFuture<String> futureResult = task.executeHttpTask(
                    HttpTask.HTTP_TASK_ACT_CONEX,
                    HttpTask.HTTP_TASK_CBL_TEST,
                    stringGet,
                    stringPost);

            futureResult.thenAccept(result -> {
                try {
                    crashlytics.log("Résultat de testIdentified: " + result);
                    Log.d(TAG, "Résultat de testIdentified: " + result);

                    Bundle resultParams = new Bundle();
                    resultParams.putString("result", result);
                    analytics.logEvent("testIdentified_result", resultParams);

                    if (result != null && result.startsWith("1")) {
                        isConnecting.set(true);

                        crashlytics.log("Session valide, redirection vers l'application");
                        Log.d(TAG, "Session valide, redirection vers l'application");

                        startAppli(result.substring(1));
                    } else {
                        if (result != null && !result.equals("0")) {
                            String errorMessage = result.substring(1);

                            isConnecting.set(false);

                            crashlytics.log("Erreur testIdentified: " + errorMessage);
                            Log.e(TAG, "Erreur testIdentified: " + errorMessage);

                            Bundle errorParams = new Bundle();
                            errorParams.putString("error_type", "connection_error");
                            errorParams.putString("class", "LoginActivity");
                            errorParams.putString("error_message", errorMessage);
                            analytics.logEvent("testIdentified_error", errorParams);

                            runOnUiThread(() -> {
                                ToastManager.showShort(errorMessage);
                            });
                        }
                        runOnUiThread(() -> {
                            openConexion();
                            showWait(false);
                        });
                    }
                } catch (Exception e) {
                    isConnecting.set(false);

                    crashlytics.recordException(e);
                    Log.e(TAG, "Erreur lors du traitement du résultat de testIdentified", e);

                    Bundle errorParams = new Bundle();
                    errorParams.putString("error_type", "connection_error");
                    errorParams.putString("class", "LoginActivity");
                    errorParams.putString("error_message", e.getMessage());
                    analytics.logEvent("testIdentified_result_error", errorParams);

                    runOnUiThread(() -> {
                        ToastManager.showError(getString(R.string.erreur_lors_du_traitement_du_r_sultat));
                        openConexion();
                        showWait(false);
                    });
                }
            }).exceptionally(ex -> {
                isConnecting.set(false);

                crashlytics.recordException(ex);
                Log.e(TAG, "Exception lors de testIdentified", ex);

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "connection_error");
                errorParams.putString("class", "LoginActivity");
                errorParams.putString("error_message", ex.getMessage());
                analytics.logEvent("testIdentified_connection_error", errorParams);

                runOnUiThread(() -> {
                    ToastManager.showError(getString(R.string.mess_timeout));
                    openConexion();
                    showWait(false);
                });
                return null;
            });
        } catch (Exception e) {
            isConnecting.set(false);

            crashlytics.recordException(e);
            Log.e(TAG, "Erreur dans testIdentified", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "connection_error");
            errorParams.putString("class", "LoginActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("testIdentified_app_error", errorParams);

            runOnUiThread(() -> {
                ToastManager.showError("Erreur dans testIdentified");
                openConexion();
                showWait(false);
            });
        }
    }

    private void openConexion() {
        try {
            crashlytics.log("openConexion called");

            ConstraintLayout mLayoutConnect = binding.loginActivityConnectLyt;
            LinearLayout mLayoutDeco = binding.loginActivityDecoLyt;
            LinearLayout mLayoutVersion = binding.loginActivityVersionLyt;
            EditText mUserName = binding.loginActivityUsernameTxt;
            EditText mUserPwd = binding.loginActivityPasswordTxt;

            idMbr = "new";
            isConnecting.set(false);

            mUserName.setText(userName);
            mUserPwd.setText(password);

            mLayoutDeco.setVisibility(View.GONE);
            mLayoutVersion.setVisibility(View.GONE);
            mLayoutConnect.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur dans openConexion", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "ui_error");
            errorParams.putString("class", "LoginActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("openConexion_app_error", errorParams);
        }
    }
    private void openDeco() {
        try {
            crashlytics.log("openDeco called");

            ConstraintLayout mLayoutConnect = binding.loginActivityConnectLyt;
            LinearLayout mLayoutDeco = binding.loginActivityDecoLyt;
            LinearLayout mLayoutVersion = binding.loginActivityVersionLyt;

            mLayoutConnect.setVisibility(View.GONE);
            mLayoutVersion.setVisibility(View.GONE);
            mLayoutDeco.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur dans openDeco", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "ui_error");
            errorParams.putString("class", "LoginActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("openDeco_app_error", errorParams);
        }
    }
    private void openVersion() {
        try {
            crashlytics.log("openVersion called");

            ConstraintLayout mLayoutConnect = binding.loginActivityConnectLyt;
            LinearLayout mLayoutDeco = binding.loginActivityDecoLyt;
            LinearLayout mLayoutVersion = binding.loginActivityVersionLyt;

            mLayoutConnect.setVisibility(View.GONE);
            mLayoutDeco.setVisibility(View.GONE);
            mLayoutVersion.setVisibility(View.VISIBLE);
            showWait(false);
            isConnecting.set(false);
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur dans openVersion", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "ui_error");
            errorParams.putString("class", "LoginActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("openVersion_app_error", errorParams);
        }
    }

    private void showWait(Boolean b) {
        try {
            crashlytics.log("showWait called");

            pl.droidsonroids.gif.GifImageView mWaitImg = binding.loginActivityWaitImg;

            runOnUiThread(() -> {
                try {
                    mWaitImg.setVisibility(b ? View.VISIBLE : View.INVISIBLE);
                } catch (Exception e) {
                    crashlytics.recordException(e);
                    Log.e(TAG, "Erreur dans l'UI de showWait", e);

                    Bundle errorParams = new Bundle();
                    errorParams.putString("error_type", "ui_error");
                    errorParams.putString("class", "LoginActivity");
                    errorParams.putString("error_message", e.getMessage());
                    analytics.logEvent("showWait_ui_error", errorParams);
                }
            });
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur dans showWait", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "ui_error");
            errorParams.putString("class", "LoginActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("showWait_app_error", errorParams);
        }
    }

    private void hideKeyboard() {
        try {
            crashlytics.log("hideKeyboard called");

            View currentFocus = getCurrentFocus();
            if (currentFocus != null) {
                InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                }
            }
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur lors de la fermeture du clavier", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "ui_error");
            errorParams.putString("class", "LoginActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("hideKeyboard_app_error", errorParams);
        }
    }

}

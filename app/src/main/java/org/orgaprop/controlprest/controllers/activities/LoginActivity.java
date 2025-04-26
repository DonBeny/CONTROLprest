package org.orgaprop.controlprest.controllers.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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

import org.orgaprop.controlprest.R;
import org.orgaprop.controlprest.databinding.ActivityLoginBinding;
import org.orgaprop.controlprest.services.HttpTask;
import org.orgaprop.controlprest.utils.AndyUtils;
import org.orgaprop.controlprest.utils.ToastManager;
import org.orgaprop.controlprest.services.PreferencesManager;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.StringTokenizer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

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

            // Vérifier les permissions essentielles
            checkEssentialPermissions();
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'initialisation de LoginActivity", e);
            ToastManager.showShort("Erreur lors de l'initialisation de l'application");
            finish();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();

        try {
            if (!isConnecting.get()) {
                testIdentified();
            }
            showWait(false);
        } catch (Exception e) {
            Log.e(TAG, "Erreur dans onResume", e);
            ToastManager.showShort("Erreur dans onResume");
            showWait(false);
        }
    }
    @Override
    protected void onPostResume() {
        super.onPostResume();

        try {
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
            Log.e(TAG, "Erreur dans onPostResume", e);
            ToastManager.showShort("Erreur dans onPostResume");
        }
    }

//********* SURCHARGES

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        try {
            if (requestCode == AndyUtils.PERMISSION_REQUEST) {
                // Vérifier les permissions réseau
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    String msg = getString(R.string.mess_bad_permission_internet);
                    Log.e(TAG, msg);
                    ToastManager.showShort(msg);
                    // TODO : Ajouter un delay
                    finish();
                    return;
                }

                // Vérifier les permissions de stockage si demandées
                if (grantResults.length > 1 && grantResults[1] != PackageManager.PERMISSION_GRANTED) {
                    String msg = getString(R.string.mess_bad_permission_write);
                    Log.e(TAG, msg);
                    ToastManager.showShort(msg);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du traitement des résultats de permission", e);
            ToastManager.showShort("Erreur lors du traitement des résultats de permission");
        }
    }

//********* PUBLIC FUNCTIONS

    public static LoginActivity getInstance() {
        return mLoginActivity;
    }

    public void loginActivityActions(View v) {
        if (v == null || v.getTag() == null) {
            Log.e(TAG, "Vue ou tag null dans loginActivityActions");
            return;
        }

        try {
            String tag = v.getTag().toString();

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
                    Log.w(TAG, "Tag non reconnu: " + tag);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du traitement de l'action: " + e.getMessage(), e);
            ToastManager.showShort("Erreur lors du traitement de l'action");
        }
    }

//********* PRIVATE FUNCTIONS

    private void checkEssentialPermissions() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.INTERNET,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, AndyUtils.PERMISSION_REQUEST);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la vérification des permissions", e);
            ToastManager.showShort("Erreur lors de la vérification des permissions");
        }
    }

    private void connectMbr() {
        if (isConnecting.getAndSet(true)) {
            Log.d(TAG, "Une tentative de connexion est déjà en cours");
            ToastManager.showShort("Une tentative de connexion est déjà en cours");
            return;
        }

        try {
            EditText mUserName = binding.loginActivityUsernameTxt;
            EditText mUserPwd = binding.loginActivityPasswordTxt;
            CheckBox mCheckBox = binding.loginActivityRgpdChx;

            userName = mUserName.getText().toString().trim();
            password = mUserPwd.getText().toString().trim();

            if (userName.isEmpty() || password.isEmpty()) {
                Log.d(TAG, "Erreur de connexion: champs vides");
                ToastManager.showShort(getString(R.string.mess_err_conex));
                isConnecting.set(false);
                return;
            }

            if (!mCheckBox.isChecked()) {
                Log.d(TAG, "Erreur de connexion: non accepté RGPD");
                ToastManager.showShort(getString(R.string.mess_err_rgpd));
                isConnecting.set(false);
                return;
            }

            hideKeyboard();

            if (!AndyUtils.isNetworkAvailable(this)) {
                Log.d(TAG, "Erreur de connexion: pas de connexion réseau");
                ToastManager.showShort(getString(R.string.mess_conextion_lost));
                isConnecting.set(false);
                return;
            }

            showWait(true);

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
                CompletableFuture<String> futureResult = task.executeHttpTask(
                        HttpTask.HTTP_TASK_ACT_CONEX,
                        HttpTask.HTTP_TASK_CBL_OK,
                        stringGet,
                        stringPost);

                futureResult.thenAccept(result -> {
                    try {
                        if (result == null) {
                            runOnUiThread(() -> {
                                Log.e(TAG, "Erreur de communication avec le serveur");
                                ToastManager.showShort("Erreur de communication avec le serveur");
                                showWait(false);
                                isConnecting.set(false);
                            });
                            return;
                        }

                        if (result.startsWith("1")) {
                            startAppli(result.substring(1));
                        } else {
                            String errorMessage = result.length() > 1 ? result.substring(1) : "Erreur de connexion";
                            runOnUiThread(() -> {
                                Log.e(TAG, errorMessage);
                                ToastManager.showShort(errorMessage);
                                showWait(false);
                                isConnecting.set(false);
                            });
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur lors du traitement de la réponse", e);
                        runOnUiThread(() -> {
                            ToastManager.showShort("Erreur lors du traitement de la réponse");
                            showWait(false);
                            isConnecting.set(false);
                        });
                    }
                }).exceptionally(ex -> {
                    Log.e(TAG, "Exception lors de la connexion", ex);
                    runOnUiThread(() -> {
                        ToastManager.showShort(getString(R.string.mess_timeout));
                        showWait(false);
                        isConnecting.set(false);
                    });
                    return null;
                });
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Erreur d'encodage des paramètres", e);
                ToastManager.showShort("Erreur d'encodage des paramètres");
                showWait(false);
                isConnecting.set(false);
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la connexion", e);
                ToastManager.showShort("Erreur lors de la tentative de connexion");
                showWait(false);
                isConnecting.set(false);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur générale dans connectMbr", e);
            ToastManager.showShort("Erreur lors de la connexion");
            showWait(false);
            isConnecting.set(false);
        }
    }
    private void deconectMbr() {
        if (isConnecting.getAndSet(true)) {
            Log.d(TAG, "Une opération est déjà en cours");
            ToastManager.showShort("Une opération est déjà en cours");
            return;
        }

        showWait(true);

        try {
            String mac = Build.FINGERPRINT;
            String stringGet = "version=" + VERSION + "&phone=" +
                    URLEncoder.encode(phoneName, "utf-8") + "&model=" +
                    URLEncoder.encode(phoneModel, "utf-8") + "&build=" +
                    URLEncoder.encode(phoneBuild, "utf-8");
            String stringPost = "mbr=" + idMbr + "&mac=" + URLEncoder.encode(mac, "utf-8");

            HttpTask task = new HttpTask(this);
            CompletableFuture<String> futureResult = task.executeHttpTask(
                    HttpTask.HTTP_TASK_ACT_CONEX,
                    HttpTask.HTTP_TASK_CBL_NO,
                    stringGet,
                    stringPost);

            futureResult.thenAccept(result -> {
                try {
                    if (result == null) {
                        runOnUiThread(() -> {
                            Log.e(TAG, "Erreur de communication avec le serveur");
                            ToastManager.showShort("Erreur de communication avec le serveur");
                            showWait(false);
                            isConnecting.set(false);
                        });
                        return;
                    }

                    boolean success = result.startsWith("1");
                    String message = success ? "Déconnexion réussie" : (result.length() > 1 ? result.substring(1) : "Erreur de déconnexion");

                    // Réinitialiser les préférences même en cas d'erreur
                    resetUserPreferences();

                    runOnUiThread(() -> {
                        Log.d(TAG, message);
                        ToastManager.showShort(message);
                        finish();
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Erreur lors du traitement de la déconnexion", e);
                    resetUserPreferences();
                    runOnUiThread(() -> {
                        ToastManager.showShort("Erreur lors de la déconnexion");
                        finish();
                    });
                }
            }).exceptionally(ex -> {
                Log.e(TAG, "Exception lors de la déconnexion", ex);
                resetUserPreferences();
                runOnUiThread(() -> {
                    ToastManager.showShort(getString(R.string.mess_timeout));
                    finish();
                });
                return null;
            });
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la déconnexion", e);
            resetUserPreferences();
            ToastManager.showShort("Erreur lors de la déconnexion");
            showWait(false);
            isConnecting.set(false);
            finish();
        }
    }

    private void resetUserPreferences() {
        try {
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
            Log.e(TAG, "Erreur lors de la réinitialisation des préférences", e);
            ToastManager.showShort("Erreur lors de la réinitialisation des préférences");
        }
    }

    private void requestConexion(String m) {
        try {
            Intent intent = new Intent(this, GetMailActivity.class);
            intent.putExtra(GetMailActivity.GET_MAIL_ACTIVITY_TYPE, m);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la requête de connexion", e);
            ToastManager.showShort("Erreur lors de l'ouverture de l'écran de demande");
        }
    }

    private void openWebPage() {
        try {
            String url = "https://www.orgaprop.org/ress/protectDonneesPersonnelles.html";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'ouverture de la page web", e);
            ToastManager.showShort("Impossible d'ouvrir la page web");
        }
    }

    private void startAppli(String result) {
        try {
            if (result == null || result.isEmpty()) {
                Log.e(TAG, "Réponse vide du serveur");
                runOnUiThread(() -> {
                    ToastManager.showShort("Réponse invalide du serveur");
                    showWait(false);
                    isConnecting.set(false);
                });
                return;
            }

            StringTokenizer tokenizer = new StringTokenizer(result, "#");

            if (tokenizer.countTokens() < 5) {
                Log.e(TAG, "Réponse incomplète du serveur: " + result);
                runOnUiThread(() -> {
                    ToastManager.showShort("Réponse incomplète du serveur");
                    showWait(false);
                    isConnecting.set(false);
                });
                return;
            }

            int version = Integer.parseInt(tokenizer.nextToken());

            if (version == VERSION) {
                idMbr = tokenizer.nextToken();
                adrMac = tokenizer.nextToken();
                id_client = tokenizer.nextToken();
                isFirst = false;

                // Enregistrer les informations de l'utilisateur
                preferencesManager.setMbrId(idMbr);
                preferencesManager.setAdrMac(adrMac);

                // Conserver les identifiants pour la reconnexion
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(PREF_KEY_MBR, userName);
                editor.putString(PREF_KEY_PWD, password);
                editor.putString(PREF_KEY_CLT, id_client);
                editor.apply();

                // Démarrer l'activité de sélection
                String agencesData = tokenizer.nextToken();
                Intent intent = new Intent(this, SelectActivity.class);
                intent.putExtra(SelectActivity.SELECT_ACTIVITY_EXTRA, agencesData);
                startActivity(intent);
            } else {
                Log.w(TAG, "Version incompatible: " + version + " vs " + VERSION);
                runOnUiThread(this::openVersion);
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Format de version invalide", e);
            runOnUiThread(() -> {
                ToastManager.showShort("Format de réponse invalide");
                showWait(false);
                isConnecting.set(false);
            });
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du démarrage de l'application", e);
            runOnUiThread(() -> {
                ToastManager.showShort("Erreur lors du démarrage de l'application");
                showWait(false);
                isConnecting.set(false);
            });
        }
    }
    private void testIdentified() {
        try {
            if (!AndyUtils.isNetworkAvailable(this)) {
                Log.d(TAG, "Aucune connexion réseau disponible");
                ToastManager.showShort(getString(R.string.mess_conextion_lost));
                openConexion();
                return;
            }

            showWait(true);

            idMbr = preferencesManager.getMbrId();
            adrMac = Build.FINGERPRINT;

            String stringGet = "version=" + VERSION;
            String stringPost = "mbr=" + idMbr + "&mac=" + adrMac;

            HttpTask task = new HttpTask(LoginActivity.this);
            CompletableFuture<String> futureResult = task.executeHttpTask(
                    HttpTask.HTTP_TASK_ACT_CONEX,
                    HttpTask.HTTP_TASK_CBL_TEST,
                    stringGet,
                    stringPost);

            futureResult.thenAccept(result -> {
                try {
                    if (result != null && result.startsWith("1")) {
                        isConnecting.set(true);
                        startAppli(result.substring(1));
                    } else {
                        if (result != null && !result.equals("0")) {
                            String errorMessage = result.substring(1);
                            Log.e(TAG, errorMessage);
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
                    Log.e(TAG, "Erreur lors du traitement du résultat de testIdentified", e);
                    runOnUiThread(() -> {
                        ToastManager.showShort("Erreur lors du traitement du résultat de testIdentified");
                        openConexion();
                        showWait(false);
                    });
                }
            }).exceptionally(ex -> {
                Log.e(TAG, "Exception lors de testIdentified", ex);
                runOnUiThread(() -> {
                    ToastManager.showShort(getString(R.string.mess_timeout));
                    openConexion();
                    showWait(false);
                });
                return null;
            });
        } catch (Exception e) {
            Log.e(TAG, "Erreur dans testIdentified", e);
            runOnUiThread(() -> {
                ToastManager.showShort("Erreur dans testIdentified");
                openConexion();
                showWait(false);
            });
        }
    }

    private void openConexion() {
        try {
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
            Log.e(TAG, "Erreur dans openConexion", e);
            ToastManager.showShort("Erreur dans openConexion");
        }
    }
    private void openDeco() {
        try {
            ConstraintLayout mLayoutConnect = binding.loginActivityConnectLyt;
            LinearLayout mLayoutDeco = binding.loginActivityDecoLyt;
            LinearLayout mLayoutVersion = binding.loginActivityVersionLyt;

            mLayoutConnect.setVisibility(View.GONE);
            mLayoutVersion.setVisibility(View.GONE);
            mLayoutDeco.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Log.e(TAG, "Erreur dans openDeco", e);
            ToastManager.showShort("Erreur dans openDeco");
        }
    }
    private void openVersion() {
        try {
            ConstraintLayout mLayoutConnect = binding.loginActivityConnectLyt;
            LinearLayout mLayoutDeco = binding.loginActivityDecoLyt;
            LinearLayout mLayoutVersion = binding.loginActivityVersionLyt;

            mLayoutConnect.setVisibility(View.GONE);
            mLayoutDeco.setVisibility(View.GONE);
            mLayoutVersion.setVisibility(View.VISIBLE);
            showWait(false);
            isConnecting.set(false);
        } catch (Exception e) {
            Log.e(TAG, "Erreur dans openVersion", e);
            ToastManager.showShort("Erreur dans openVersion");
        }
    }

    private void showWait(Boolean b) {
        try {
            pl.droidsonroids.gif.GifImageView mWaitImg = binding.loginActivityWaitImg;

            runOnUiThread(() -> mWaitImg.setVisibility(b ? View.VISIBLE : View.INVISIBLE));
        } catch (Exception e) {
            Log.e(TAG, "Erreur dans showWait", e);
            ToastManager.showShort("Erreur dans showWait");
        }
    }

    private void hideKeyboard() {
        try {
            View currentFocus = getCurrentFocus();
            if (currentFocus != null) {
                InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la fermeture du clavier", e);
            ToastManager.showShort("Erreur lors de la fermeture du clavier");
        }
    }

}

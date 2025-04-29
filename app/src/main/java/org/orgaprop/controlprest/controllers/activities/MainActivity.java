package org.orgaprop.controlprest.controllers.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.tasks.Task;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.orgaprop.controlprest.BuildConfig;
import org.orgaprop.controlprest.R;
import org.orgaprop.controlprest.databinding.ActivityMainBinding;
import org.orgaprop.controlprest.utils.AndyUtils;
import org.orgaprop.controlprest.utils.ToastManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {

//********* PRIVATE VARIABLES

    private static final String TAG = "MainActivity";
    private TextView mVersion;
    private boolean isStarted;
    private final AtomicBoolean isCheckingUpdate = new AtomicBoolean(false);
    private final AtomicBoolean permissionsChecked = new AtomicBoolean(false);
    private ExecutorService executorService;
    private AppUpdateManager appUpdateManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private AlertDialog myDialog;
    private FirebaseCrashlytics crashlytics;
    private FirebaseAnalytics analytics;

//********* PUBLIC VARIABLES

    public static final int SPLASH_SCREEN_DELAY = 3000;
    public static final int UPDATE_REQUEST_CODE = 100;

//********* WIDGETS

    private ActivityMainBinding binding;
    private ActivityResultLauncher<IntentSenderRequest> updateLauncher;

//********* CONSTRUCTORS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            crashlytics = FirebaseCrashlytics.getInstance();
            crashlytics.setCustomKey("deviceModel", Build.MODEL);
            crashlytics.setCustomKey("deviceManufacturer", Build.MANUFACTURER);
            crashlytics.setCustomKey("appVersion", BuildConfig.VERSION_NAME);
            crashlytics.log("Application démarrée");

            analytics = FirebaseAnalytics.getInstance(this);

            Bundle screenViewParams = new Bundle();
            screenViewParams.putString(FirebaseAnalytics.Param.SCREEN_NAME, "Main");
            screenViewParams.putString(FirebaseAnalytics.Param.SCREEN_CLASS, "MainActivity");
            analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, screenViewParams);

            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            mVersion = binding.mainActivityVersionTxt;
            executorService = Executors.newSingleThreadExecutor();

            // Initialiser le launcher pour les mises à jour
            updateLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartIntentSenderForResult(),
                    result -> handelUpdateResult(result.getResultCode())
            );

            // Afficher la version
            putVersion();

            // Vérifier les permissions essentielles avant de continuer
            checkPermissions();

        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur lors de l'initialisation de MainActivity", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "app_init_error");
            errorParams.putString("class", "MainActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("onCreate_app_error", errorParams);

            showErrorAndFinish(getString(R.string.erreur_lors_de_l_initialisation_de_l_application));
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        try {
            crashlytics.log("onResume called");
            // Si les permissions sont OK et qu'une mise à jour n'est pas en cours
            if (permissionsChecked.get() && !isCheckingUpdate.get() && !isStarted) {
                checkUpdate();
            }
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur dans onResume", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "app_init_error");
            errorParams.putString("class", "MainActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("onResume_app_error", errorParams);

            ToastManager.showError(getString(R.string.erreur_lors_du_d_marrage_de_l_application));
        }
    }
    @Override
    protected void onDestroy() {
        try {
            crashlytics.log("onDestroy called");
            // Nettoyer les ressources
            shutdownExecutor();

            // Libérer l'AppUpdateManager si nécessaire
            if (appUpdateManager != null) {
                appUpdateManager = null;
            }
            if (myDialog != null && myDialog.isShowing()) {
                myDialog.dismiss();
            }
            myDialog = null;
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur dans onDestroy", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "app_init_error");
            errorParams.putString("class", "MainActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("onDestroy_app_error", errorParams);

            ToastManager.showError(getString(R.string.erreur_lors_de_la_fermeture_de_l_application));
        } finally {
            super.onDestroy();
        }
    }

//********** SURCHARGES

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        try {
            if (requestCode == AndyUtils.PERMISSION_REQUEST) {
                boolean allPermissionsGranted = true;

                // Vérifier chaque permission
                for (int i = 0; i < permissions.length && i < grantResults.length; i++) {
                    String permission = permissions[i];
                    int result = grantResults[i];

                    if (result != PackageManager.PERMISSION_GRANTED) {
                        allPermissionsGranted = false;
                        crashlytics.log("Permission refusée: " + permission);

                        // Messages d'erreur spécifiques selon les permissions
                        if (Manifest.permission.ACCESS_NETWORK_STATE.equals(permission) ||
                                Manifest.permission.INTERNET.equals(permission)) {
                            Log.e(TAG, getString(R.string.mess_bad_permission_internet));

                            Bundle errorParams = new Bundle();
                            errorParams.putString("error_type", "bad_permission");
                            errorParams.putString("class", "MainActivity");
                            errorParams.putString("permission", "ACCESS_NETWORK_STATE");
                            analytics.logEvent("onPermissionResult_permission_error", errorParams);

                            showErrorAndFinish(getString(R.string.mess_bad_permission_internet));
                            return;
                        } else if (Manifest.permission.NFC.equals(permission)) {
                            Log.e(TAG, getString(R.string.mess_bad_nfc));

                            Bundle errorParams = new Bundle();
                            errorParams.putString("error_type", "bad_permission");
                            errorParams.putString("class", "MainActivity");
                            errorParams.putString("permission", "NFC");
                            analytics.logEvent("onPermissionResult_permission_error", errorParams);

                            showErrorAndFinish(getString(R.string.mess_bad_nfc));
                            return;
                        }
                    }
                }

                // Si toutes les permissions sont accordées, vérifier le NFC
                if (allPermissionsGranted) {
                    checkNfcAvailability();
                }
            }
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur lors du traitement des résultats de permission", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "permission_result_error");
            errorParams.putString("class", "MainActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("onRequestPermissionsResult_app_error", errorParams);

            showErrorAndFinish(getString(R.string.erreur_lors_de_la_v_rification_des_permissions));
        }
    }

//********** PRIVATE FUNCTIONS

    /**
     * Gère le résultat de la mise à jour de l'application
     */
    private void handelUpdateResult(int resultCode) {
        try {
            crashlytics.log("handelUpdateResult: " + resultCode);

            isCheckingUpdate.set(false);

            if (resultCode == RESULT_OK) {
                // La mise à jour a réussi, l'application va redémarrer automatiquement
                Log.i(TAG, "Mise à jour réussie");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "update_success");
                errorParams.putString("class", "MainActivity");
                analytics.logEvent("onResult_update_success", errorParams);
            } else if (resultCode == RESULT_CANCELED) {
                // L'utilisateur a annulé, continuer normalement
                Log.w(TAG, "Mise à jour annulée par l'utilisateur");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "update_canceled");
                errorParams.putString("class", "MainActivity");
                analytics.logEvent("onResult_update_canceled", errorParams);

                ToastManager.showError(getString(R.string.mise_jour_annul_e_par_l_utilisateur));
                startLoginActivity();
            } else {
                // Échec de la mise à jour
                Log.e(TAG, "Échec de la mise à jour: " + resultCode);

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "update_failed");
                errorParams.putString("class", "MainActivity");
                errorParams.putString("result_code", String.valueOf(resultCode));
                analytics.logEvent("onResult_update_failed", errorParams);

                ToastManager.showError(getString(R.string.chec_de_la_mise_jour_l_application_continuera_avec_la_version_actuelle));
                startLoginActivity();
            }
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur lors du traitement du résultat de mise à jour", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "update_result_error");
            errorParams.putString("class", "MainActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("onResult_app_error", errorParams);

            ToastManager.showError(getString(R.string.erreur_lors_du_traitement_du_r_sultat_de_mise_jour));
            startLoginActivity();
        }
    }

    /**
     * Vérifie que les permissions nécessaires sont accordées
     */
    private void checkPermissions() {
        try {
            crashlytics.log("checkPermissions called");

            List<String> permissionsNeeded = new ArrayList<>();

            // Vérifier les permissions essentielles
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.ACCESS_NETWORK_STATE);
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.INTERNET);
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.NFC) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.NFC);
            }

            // Demander les permissions si nécessaire
            if (!permissionsNeeded.isEmpty()) {
                ActivityCompat.requestPermissions(this,
                        permissionsNeeded.toArray(new String[0]),
                        AndyUtils.PERMISSION_REQUEST);
            } else {
                // Toutes les permissions sont déjà accordées
                permissionsChecked.set(true);
                checkNfcAvailability();
            }
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur lors de la vérification des permissions", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "permission_check_error");
            errorParams.putString("class", "MainActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("checkPermissions_app_error", errorParams);

            showErrorAndFinish(getString(R.string.erreur_lors_de_la_v_rification_des_permissions));
        }
    }

    /**
     * Vérifie que le NFC est disponible et activé
     */
    private void checkNfcAvailability() {
        try {
            crashlytics.log("checkNfcAvailability called");

            NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);

            if (nfcAdapter == null) {
                // Le périphérique ne supporte pas le NFC
                crashlytics.log("NFC non supporté par l'appareil");
                Log.e(TAG, "NFC non supporté par l'appareil");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "no_nfc");
                errorParams.putString("class", "MainActivity");
                analytics.logEvent("checkNfcAvailability_no_nfc", errorParams);

                showErrorDialog(getString(R.string.mess_bad_nfc), true);
                return;
            }

            if (!nfcAdapter.isEnabled()) {
                // Le NFC est désactivé, demander à l'utilisateur de l'activer
                crashlytics.log("NFC désactivé");
                Log.e(TAG, "NFC désactivé");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "nfc_disabled");
                errorParams.putString("class", "MainActivity");
                analytics.logEvent("checkNfcAvailability_nfc_disabled", errorParams);

                showNfcEnableDialog();
                return;
            }

            // NFC disponible et activé, continuer
            crashlytics.log("NFC disponible et activé");

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "nfc_available");
            errorParams.putString("class", "MainActivity");
            analytics.logEvent("checkNfcAvailability_nfc_available", errorParams);

            checkUpdate();
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur lors de la vérification du NFC", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "nfc_check_error");
            errorParams.putString("class", "MainActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("checkNfcAvailability_app_error", errorParams);

            showErrorDialog(getString(R.string.erreur_lors_de_la_v_rification_du_nfc), false);
        }
    }

    /**
     * Affiche un dialogue pour activer le NFC
     */
    private void showNfcEnableDialog() {
        try {
            crashlytics.log("showNfcEnableDialog called");

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("NFC désactivé")
                    .setMessage(getString(R.string.mess_bad_nfc))
                    .setCancelable(false)
                    .setPositiveButton("Activer NFC", (dialog, which) -> {
                        try {
                            Intent intentNfc = new Intent(Settings.ACTION_NFC_SETTINGS);
                            startActivity(intentNfc);
                        } catch (Exception e) {
                            crashlytics.recordException(e);
                            Log.e(TAG, "Erreur lors de l'ouverture des paramètres NFC", e);

                            Bundle errorParams = new Bundle();
                            errorParams.putString("error_type", "nfc_settings_error");
                            errorParams.putString("class", "MainActivity");
                            errorParams.putString("error_message", e.getMessage());
                            analytics.logEvent("showNfcEnableDialog_nfc_settings_error", errorParams);

                            showErrorAndFinish("Impossible d'ouvrir les paramètres NFC");
                        }
                    })
                    .setNegativeButton("Quitter", (dialog, which) -> finish());

            builder.create().show();
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur lors de l'affichage du dialogue NFC", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "nfc_dialog_error");
            errorParams.putString("class", "MainActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("showNfcEnableDialog_app_error", errorParams);

            showErrorAndFinish(getString(R.string.mess_bad_nfc));
        }
    }

    /**
     * Met à jour l'affichage de la version
     */
    private void putVersion() {
        try {
            String versionName = "Version : " + BuildConfig.VERSION_NAME;
            if (mVersion != null) {
                mVersion.setText(versionName);
            }
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur lors de l'affichage de la version", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "version_error");
            errorParams.putString("class", "MainActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("putVersion_app_error", errorParams);
        }
    }

    /**
     * Vérifie si une mise à jour est disponible
     */
    private void checkUpdate() {
        try {
            crashlytics.log("checkUpdate called");

            // Éviter les vérifications simultanées
            if (isCheckingUpdate.getAndSet(true)) {
                crashlytics.log("Une vérification de mise à jour est déjà en cours");
                Log.d(TAG, "Une vérification de mise à jour est déjà en cours");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "update_check_in_progress");
                errorParams.putString("class", "MainActivity");
                analytics.logEvent("checkUpdate_in_progress", errorParams);

                return;
            }

            // Version sans risque - démarre toujours l'app après un délai
            Runnable startAppRunnable = this::startLoginActivity;
            mainHandler.postDelayed(startAppRunnable, SPLASH_SCREEN_DELAY);

            // Tenter la vérification de mise à jour en parallèle
            if (isFromPlayStore()) {
                crashlytics.log("Application installée depuis Play Store");

                try {
                    // Initialiser le gestionnaire de mise à jour
                    appUpdateManager = AppUpdateManagerFactory.create(this);

                    executorService.execute(() -> {
                        try {
                            crashlytics.log("Vérification de la connexion réseau");

                            if (!AndyUtils.isNetworkAvailable(MainActivity.this)) {
                                crashlytics.log("Pas de connexion réseau disponible");
                                Log.w(TAG, "Pas de connexion réseau disponible");

                                Bundle errorParams = new Bundle();
                                errorParams.putString("error_type", "no_network");
                                errorParams.putString("class", "MainActivity");
                                analytics.logEvent("checkUpdate_no_network", errorParams);

                                return; // On sort simplement, startLoginActivity sera appelé par le handler
                            }

                            crashlytics.log("Récupération des infos de mise à jour");

                            Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

                            appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
                                try {
                                    crashlytics.log("UpdateAvailability: " + appUpdateInfo.updateAvailability());

                                    if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                                            appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                                        crashlytics.log("Mise à jour disponible et autorisée");

                                        // Annuler le démarrage automatique car on va lancer la mise à jour
                                        mainHandler.removeCallbacksAndMessages(null);

                                        // Démarrer la mise à jour
                                        startUpdateFlow(appUpdateInfo);
                                    }
                                    // Pour tous les autres cas, l'app démarrera normalement via le handler
                                } catch (Exception e) {
                                    crashlytics.recordException(e);
                                    Log.e(TAG, "Erreur dans onSuccess de appUpdateInfoTask", e);

                                    Bundle errorParams = new Bundle();
                                    errorParams.putString("error_type", "update_info_error");
                                    errorParams.putString("class", "MainActivity");
                                    errorParams.putString("error_message", e.getMessage());
                                    analytics.logEvent("checkUpdate_app_error", errorParams);

                                    isCheckingUpdate.set(false);
                                }
                            }).addOnFailureListener(e -> {
                                crashlytics.recordException(e);
                                crashlytics.log("Échec de la vérification: " + e.getMessage());
                                Log.e(TAG, "Échec de la vérification de mise à jour", e);

                                Bundle errorParams = new Bundle();
                                errorParams.putString("error_type", "update_check_error");
                                errorParams.putString("class", "MainActivity");
                                errorParams.putString("error_message", e.getMessage());
                                analytics.logEvent("checkUpdate_app_error", errorParams);

                                isCheckingUpdate.set(false);
                            });
                        } catch (Exception e) {
                            crashlytics.recordException(e);
                            Log.e(TAG, "Erreur dans le thread de vérification", e);

                            Bundle errorParams = new Bundle();
                            errorParams.putString("error_type", "update_check_thread_error");
                            errorParams.putString("class", "MainActivity");
                            errorParams.putString("error_message", e.getMessage());
                            analytics.logEvent("checkUpdate_app_error", errorParams);

                            isCheckingUpdate.set(false);
                        }
                    });
                } catch (Exception e) {
                    crashlytics.recordException(e);
                    Log.e(TAG, "Erreur lors de l'initialisation de la mise à jour", e);

                    Bundle errorParams = new Bundle();
                    errorParams.putString("error_type", "update_init_error");
                    errorParams.putString("class", "MainActivity");
                    errorParams.putString("error_message", e.getMessage());
                    analytics.logEvent("checkUpdate_app_error", errorParams);

                    isCheckingUpdate.set(false);
                }
            } else {
                crashlytics.log("Application non installée depuis Play Store");
                Log.i(TAG, "Application non installée depuis Play Store");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "not_from_play_store");
                errorParams.putString("class", "MainActivity");
                analytics.logEvent("checkUpdate_not_from_play_store", errorParams);

                isCheckingUpdate.set(false);
            }
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur générale dans checkUpdate", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "update_check_general_error");
            errorParams.putString("class", "MainActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("checkUpdate_app_error", errorParams);

            isCheckingUpdate.set(false);
            mainHandler.removeCallbacksAndMessages(null);
            mainHandler.post(this::startLoginActivity);
        }
    }

    /**
     * Démarre le flux de mise à jour
     */
    private void startUpdateFlow(AppUpdateInfo updateInfo) {
        try {
            crashlytics.log("startUpdateFlow called");

            isStarted = true;

            // Configuration de la mise à jour immédiate
            AppUpdateOptions updateOptions = AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build();

            // Lancer le flux de mise à jour
            appUpdateManager.startUpdateFlowForResult(
                    updateInfo,
                    updateLauncher,
                    updateOptions
            );
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur lors du démarrage du flux de mise à jour", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "update_start_error");
            errorParams.putString("class", "MainActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("checkUpdate_app_error", errorParams);

            isCheckingUpdate.set(false);
            mainHandler.postDelayed(this::startLoginActivity, SPLASH_SCREEN_DELAY);
        }
    }

    /**
     * Démarre l'activité de connexion
     */
    private void startLoginActivity() {
        try {
            if (isFinishing() || isDestroyed()) {
                crashlytics.log("startLoginActivity: activité en cours de fermeture");
                Log.w(TAG, "Tentative de démarrer LoginActivity alors que MainActivity est en train de se terminer");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "activity_closing");
                errorParams.putString("class", "MainActivity");
                analytics.logEvent("startLoginActivity_activity_closing", errorParams);

                return;
            }

            crashlytics.log("Démarrage de LoginActivity");
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur lors du démarrage de LoginActivity", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "start_login_activity_error");
            errorParams.putString("class", "MainActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("startLoginActivity_app_error", errorParams);

            showErrorAndFinish(getString(R.string.erreur_lors_du_d_marrage_de_l_application));
        }
    }

    /**
     * Vérifie si l'application est installée depuis le Play Store
     */
    private boolean isFromPlayStore() {
        try {
            crashlytics.log("isFromPlayStore called");

            String installer = getPackageManager().getInstallerPackageName(getPackageName());
            boolean isFromStore = installer != null && installer.equals("com.android.vending");

            crashlytics.log("isFromPlayStore: " + isFromStore + ", installer: " + installer);

            return isFromStore;
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur lors de la vérification de la source d'installation", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "play_store_check_error");
            errorParams.putString("class", "MainActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("isFromPlayStore_app_error", errorParams);

            return false;
        }
    }

    /**
     * Ferme l'ExecutorService proprement
     */
    private void shutdownExecutor() {
        try {
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
                try {
                    if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
                        executorService.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    crashlytics.recordException(e);
                    executorService.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur lors de la fermeture de l'ExecutorService", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "executor_shutdown_error");
            errorParams.putString("class", "MainActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("shutdownExecutor_app_error", errorParams);
        }
    }

    /**
     * Affiche une erreur et termine l'activité
     */
    private void showErrorAndFinish(String message) {
        try {
            crashlytics.log("showErrorAndFinish: " + message);
            Log.e(TAG, "Erreur fatale: " + message);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "fatal_error");
            errorParams.putString("class", "MainActivity");
            errorParams.putString("error_message", message);
            analytics.logEvent("showErrorAndFinish_app_error", errorParams);

            ToastManager.showError(message);

            mainHandler.postDelayed(this::startLoginActivity, SPLASH_SCREEN_DELAY);
            finish();
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur lors de l'affichage du message d'erreur", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "error_message_error");
            errorParams.putString("class", "MainActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("showErrorAndFinish_app_error", errorParams);

            ToastManager.showError(message);

            mainHandler.postDelayed(this::startLoginActivity, SPLASH_SCREEN_DELAY);
            finish();
        }
    }

    /**
     * Affiche un dialogue d'erreur
     */
    private void showErrorDialog(String message, boolean fatal) {
        try {
            crashlytics.log("showErrorDialog: " + message + ", fatal: " + fatal);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Erreur")
                    .setMessage(message)
                    .setCancelable(false);

            if (fatal) {
                // Erreur fatale, l'application doit se terminer
                builder.setPositiveButton("Fermer", (dialog, which) -> finish());
            } else {
                // Erreur non fatale, l'utilisateur peut continuer
                builder.setPositiveButton("Continuer", (dialog, which) -> { checkUpdate(); })
                        .setNegativeButton("Quitter", (dialog, which) -> finish());
            }

            myDialog = builder.create();
            myDialog.show();
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur lors de l'affichage du dialogue d'erreur", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "dialog_error");
            errorParams.putString("class", "MainActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("showErrorDialog_app_error", errorParams);

            ToastManager.showError(message);

            if (fatal) {
                mainHandler.postDelayed(this::startLoginActivity, SPLASH_SCREEN_DELAY);
                finish();
            } else {
                checkUpdate();
            }
        }
    }

}

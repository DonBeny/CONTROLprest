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
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;

import org.orgaprop.controlprest.BuildConfig;
import org.orgaprop.controlprest.R;
import org.orgaprop.controlprest.databinding.ActivityMainBinding;
import org.orgaprop.controlprest.utils.AndyUtils;

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
            Log.e(TAG, "Erreur lors de l'initialisation de MainActivity", e);
            showErrorAndFinish("Erreur lors de l'initialisation de l'application");
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        try {
            // Si les permissions sont OK et qu'une mise à jour n'est pas en cours
            if (permissionsChecked.get() && !isCheckingUpdate.get() && !isStarted) {
                checkUpdate();
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur dans onResume", e);
            Toast.makeText(MainActivity.this, "Erreur dans onResume", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        try {
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
            Log.e(TAG, "Erreur dans onDestroy", e);
            Toast.makeText(MainActivity.this, "Erreur dans onDestroy", Toast.LENGTH_LONG).show();
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

                        // Messages d'erreur spécifiques selon les permissions
                        if (Manifest.permission.ACCESS_NETWORK_STATE.equals(permission) ||
                                Manifest.permission.INTERNET.equals(permission)) {
                            Log.e(TAG, getString(R.string.mess_bad_permission_internet));
                            showErrorAndFinish(getString(R.string.mess_bad_permission_internet));
                            return;
                        } else if (Manifest.permission.NFC.equals(permission)) {
                            Log.e(TAG, getString(R.string.mess_bad_nfc));
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
            Log.e(TAG, "Erreur lors du traitement des résultats de permission", e);
            showErrorAndFinish("Erreur lors de la vérification des permissions");
        }
    }

//********** PRIVATE FUNCTIONS

    /**
     * Gère le résultat de la mise à jour de l'application
     */
    private void handelUpdateResult(int resultCode) {
        try {
            isCheckingUpdate.set(false);

            if (resultCode == RESULT_OK) {
                // La mise à jour a réussi, l'application va redémarrer automatiquement
                Log.i(TAG, "Mise à jour réussie");
                Toast.makeText(MainActivity.this, "Mise à jour réussie", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                // L'utilisateur a annulé, continuer normalement
                Log.w(TAG, "Mise à jour annulée par l'utilisateur");
                Toast.makeText(MainActivity.this, "Mise à jour annulée par l'utilisateur", Toast.LENGTH_SHORT).show();
                startLoginActivity();
            } else {
                // Échec de la mise à jour
                Log.e(TAG, "Échec de la mise à jour: " + resultCode);
                Toast.makeText(MainActivity.this, "Échec de la mise à jour, l'application continuera avec la version actuelle", Toast.LENGTH_SHORT).show();
                startLoginActivity();
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du traitement du résultat de mise à jour", e);
            Toast.makeText(MainActivity.this, "Erreur lors du traitement du résultat de mise à jour", Toast.LENGTH_SHORT).show();
            startLoginActivity();
        }
    }

    /**
     * Vérifie que les permissions nécessaires sont accordées
     */
    private void checkPermissions() {
        try {
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
            Log.e(TAG, "Erreur lors de la vérification des permissions", e);
            showErrorAndFinish("Erreur lors de la vérification des permissions");
        }
    }

    /**
     * Vérifie que le NFC est disponible et activé
     */
    private void checkNfcAvailability() {
        try {
            NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);

            if (nfcAdapter == null) {
                // Le périphérique ne supporte pas le NFC
                showErrorDialog(getString(R.string.mess_bad_nfc), true);
                return;
            }

            if (!nfcAdapter.isEnabled()) {
                // Le NFC est désactivé, demander à l'utilisateur de l'activer
                showNfcEnableDialog();
                return;
            }

            // NFC disponible et activé, continuer
            checkUpdate();

        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la vérification du NFC", e);
            showErrorDialog("Erreur lors de la vérification du NFC", false);
        }
    }

    /**
     * Affiche un dialogue pour activer le NFC
     */
    private void showNfcEnableDialog() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("NFC désactivé")
                    .setMessage(getString(R.string.mess_bad_nfc))
                    .setCancelable(false)
                    .setPositiveButton("Activer NFC", (dialog, which) -> {
                        try {
                            Intent intentNfc = new Intent(Settings.ACTION_NFC_SETTINGS);
                            startActivity(intentNfc);
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur lors de l'ouverture des paramètres NFC", e);
                            showErrorAndFinish("Impossible d'ouvrir les paramètres NFC");
                        }
                    })
                    .setNegativeButton("Quitter", (dialog, which) -> finish());

            builder.create().show();
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'affichage du dialogue NFC", e);
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
            Log.e(TAG, "Erreur lors de l'affichage de la version", e);
            Toast.makeText(MainActivity.this, "Erreur lors de l'affichage de la version", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Vérifie si une mise à jour est disponible
     */
    private void checkUpdate() {
        try {
            // Éviter les vérifications simultanées
            if (isCheckingUpdate.getAndSet(true)) {
                Log.d(TAG, "Une vérification de mise à jour est déjà en cours");
                Toast.makeText(MainActivity.this, "Une vérification de mise à jour est déjà en cours", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isFromPlayStore()) {
                // Initialiser le gestionnaire de mise à jour
                appUpdateManager = AppUpdateManagerFactory.create(this);

                executorService.execute(() -> {
                    try {
                        if (!AndyUtils.isNetworkAvailable(MainActivity.this)) {
                            Log.w(TAG, "Pas de connexion réseau disponible pour vérifier les mises à jour");
                            mainHandler.post(() -> {
                                Toast.makeText(MainActivity.this,
                                        "Récupérez une connexion pour utiliser l'application.",
                                        Toast.LENGTH_SHORT).show();
                                isCheckingUpdate.set(false);
                            });
                            return;
                        }

                        mainHandler.postDelayed(this::startLoginActivity, SPLASH_SCREEN_DELAY);

                        /*Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

                        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
                            try {
                                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                                    Log.i(TAG, "Mise à jour disponible");

                                    mainHandler.post(() -> {
                                        Toast.makeText(MainActivity.this, "Mise à jour disponible.", Toast.LENGTH_SHORT).show();
                                    });

                                    startUpdateFlow(appUpdateInfo);
                                } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                                    Log.i(TAG, "Mise à jour déjà en cours");

                                    mainHandler.post(() -> {
                                        Toast.makeText(MainActivity.this, "Mise à jour déjà en cours.", Toast.LENGTH_SHORT).show();
                                    });

                                    startUpdateFlow(appUpdateInfo);
                                } else {
                                    Log.i(TAG, "Aucune mise à jour disponible");

                                    mainHandler.post(() -> {
                                        Toast.makeText(MainActivity.this, "Aucune mise à jour disponible.", Toast.LENGTH_SHORT).show();
                                    });

                                    // Continuer avec un délai pour l'écran de démarrage
                                    mainHandler.postDelayed(this::startLoginActivity, SPLASH_SCREEN_DELAY);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Erreur lors du traitement de l'info de mise à jour", e);

                                mainHandler.post(() -> {
                                    Toast.makeText(MainActivity.this, "Erreur lors du traitement de l'info de mise à jour.", Toast.LENGTH_SHORT).show();
                                });

                                mainHandler.postDelayed(this::startLoginActivity, SPLASH_SCREEN_DELAY);
                            }
                        }).addOnFailureListener(e -> {
                            Log.e(TAG, "Échec de la vérification de mise à jour", e);

                            mainHandler.post(() -> {
                                Toast.makeText(MainActivity.this, "Échec de la vérification de mise à jour.", Toast.LENGTH_SHORT).show();
                            });

                            mainHandler.postDelayed(this::startLoginActivity, SPLASH_SCREEN_DELAY);
                            isCheckingUpdate.set(false);
                        });*/
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur lors de la vérification de mise à jour", e);
                        mainHandler.post(() -> {
                            Toast.makeText(MainActivity.this, "Erreur lors de la vérification des mises à jour", Toast.LENGTH_SHORT).show();
                            isCheckingUpdate.set(false);
                            mainHandler.postDelayed(this::startLoginActivity, SPLASH_SCREEN_DELAY);
                        });
                    }
                });
            } else {
                // Application non installée depuis le Play Store
                Log.i(TAG, "Application non installée depuis le Play Store, aucune vérification de mise à jour");
                Toast.makeText(MainActivity.this, "Application non installée depuis le Play Store, aucune vérification de mise à jour", Toast.LENGTH_SHORT).show();
                isCheckingUpdate.set(false);
                mainHandler.postDelayed(this::startLoginActivity, SPLASH_SCREEN_DELAY);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la vérification des mises à jour", e);
            Toast.makeText(MainActivity.this, "Erreur lors de la vérification des mises à jour", Toast.LENGTH_SHORT).show();
            isCheckingUpdate.set(false);
            mainHandler.postDelayed(this::startLoginActivity, SPLASH_SCREEN_DELAY);
        }
    }

    /**
     * Démarre le flux de mise à jour
     */
    private void startUpdateFlow(AppUpdateInfo updateInfo) {
        try {
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
            Log.e(TAG, "Erreur lors du démarrage du flux de mise à jour", e);
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
                Log.w(TAG, "Tentative de démarrer LoginActivity alors que MainActivity est en train de se terminer");
                return;
            }

            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du démarrage de LoginActivity", e);
            showErrorAndFinish("Erreur lors du démarrage de l'application");
        }
    }

    /**
     * Vérifie si l'application est installée depuis le Play Store
     */
    private boolean isFromPlayStore() {
        try {
            String installer = getPackageManager().getInstallerPackageName(getPackageName());
            return installer != null && installer.equals("com.android.vending");
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la vérification de la source d'installation", e);
            Toast.makeText(MainActivity.this, "Erreur lors de la vérification de la source d'installation", Toast.LENGTH_SHORT).show();
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
                    executorService.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la fermeture de l'ExecutorService", e);
            Toast.makeText(MainActivity.this, "Erreur lors de la fermeture de l'ExecutorService", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Affiche une erreur et termine l'activité
     */
    private void showErrorAndFinish(String message) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            Log.e(TAG, "Erreur fatale: " + message);
            mainHandler.postDelayed(this::startLoginActivity, SPLASH_SCREEN_DELAY);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'affichage du message d'erreur", e);
            Toast.makeText(this, "Erreur lors de l'affichage du message d'erreur", Toast.LENGTH_LONG).show();
            mainHandler.postDelayed(this::startLoginActivity, SPLASH_SCREEN_DELAY);
            finish();
        }
    }

    /**
     * Affiche un dialogue d'erreur
     */
    private void showErrorDialog(String message, boolean fatal) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Erreur")
                    .setMessage(message)
                    .setCancelable(false);

            if (fatal) {
                // Erreur fatale, l'application doit se terminer
                builder.setPositiveButton("Fermer", (dialog, which) -> finish());
            } else {
                // Erreur non fatale, l'utilisateur peut continuer
                builder.setPositiveButton("Continuer", (dialog, which) -> {
                            checkUpdate();
                        })
                        .setNegativeButton("Quitter", (dialog, which) -> finish());
            }

            myDialog = builder.create();
            myDialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'affichage du dialogue d'erreur", e);
            Toast.makeText(this, "Erreur lors de l'affichage du dialogue d'erreur", Toast.LENGTH_LONG).show();

            if (fatal) {
                mainHandler.postDelayed(this::startLoginActivity, SPLASH_SCREEN_DELAY);
                finish();
            } else {
                checkUpdate();
            }
        }
    }

}
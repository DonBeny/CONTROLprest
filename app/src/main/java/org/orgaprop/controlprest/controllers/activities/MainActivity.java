package org.orgaprop.controlprest.controllers.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
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

import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

//********* PRIVATE VARIABLES

    private TextView mVersion;
    private boolean isStart;

    private static final int SPLASH_SCREEN_DELAY = 3000;

//********* PUBLIC VARIABLES

    public static final String TAG = "MainActivity";

    public static final int UPDATE_REQUEST_CODE = 100;

//********* WIDGETS

    private ActivityMainBinding binding;
    private ActivityResultLauncher<IntentSenderRequest> updateLauncher;

//********* CONSTRUCTORS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mVersion = binding.mainActivityVersionTxt;

        updateLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> handelUpdateResult(result.getResultCode())
        );

        checkPermissions();
        putVersion();
        checkUpdate();

    }

//********** SURCHARGES

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if( requestCode == AndyUtils.PERMISSION_REQUEST ) {
            if( grantResults[0] != PackageManager.PERMISSION_GRANTED ) {
                Toast.makeText(MainActivity.this, getResources().getString(R.string.mess_bad_permission_internet), Toast.LENGTH_LONG).show();
                finish();
            }
            if( grantResults[1] != PackageManager.PERMISSION_GRANTED ) {
                Toast.makeText(MainActivity.this, getResources().getString(R.string.mess_bad_nfc), Toast.LENGTH_LONG).show();
                finish();
            } else {
                NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);

                if( nfcAdapter == null ) {
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.mess_bad_nfc), Toast.LENGTH_LONG).show();
                    finish();
                } else if( !nfcAdapter.isEnabled() ) {
                    Intent intentNfc = new Intent(Settings.ACTION_NFC_SETTINGS);

                    startActivity(intentNfc);
                }
            }
        }
    }

//********** PRIVATE FUNCTIONS

    private void handelUpdateResult(int resultCode) {
        if( resultCode != RESULT_OK ) {
            startLoginActivity();
        } else {
            Toast.makeText(MainActivity.this, "Échec de la mise à jour !!!", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkPermissions() {
        if( ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.INTERNET,
                    Manifest.permission.NFC
            }, AndyUtils.PERMISSION_REQUEST);
        } else {
            NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);

            if( nfcAdapter == null ) {
                Toast.makeText(MainActivity.this, getResources().getString(R.string.mess_bad_nfc), Toast.LENGTH_LONG).show();
            } else if( !nfcAdapter.isEnabled() ) {
                Intent intentNfc = new Intent(Settings.ACTION_NFC_SETTINGS);

                startActivity(intentNfc);
            }
        }
    }
    private void putVersion() {
        String versionName = "Version : " + BuildConfig.VERSION_NAME;

        mVersion.setText(versionName);
    }
    private void checkUpdate() {
        if (isFromPlayStore()) {
            Executors.newSingleThreadExecutor().execute(() -> {
                Looper.prepare();

                if( AndyUtils.isNetworkAvailable(MainActivity.this) ) {
                    AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(MainActivity.this);
                    Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

                    appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
                        if( appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE ) {
                            isStart = false;

                            AppUpdateOptions appUpdateOptions = AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build();

                            appUpdateManager.startUpdateFlowForResult(
                                    appUpdateInfo,
                                    updateLauncher,
                                    appUpdateOptions
                            );
                        } else if( appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS ) {
                            isStart = false;

                            AppUpdateOptions appUpdateOptions = AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build();

                            appUpdateManager.startUpdateFlowForResult(
                                    appUpdateInfo,
                                    updateLauncher,
                                    appUpdateOptions
                            );
                        } else {
                            new Handler().postDelayed(this::startLoginActivity, SPLASH_SCREEN_DELAY);
                        }
                    }).addOnFailureListener(e -> {
                        //Log.e(TAG, functionName + "Task failed", e);
                    });
                } else {
                    Toast.makeText(MainActivity.this, "Récupérez une connexion pour utiliser l'application.", Toast.LENGTH_SHORT).show();
                }

                Looper.loop();
            });
        } else {
            new Handler().postDelayed(this::startLoginActivity, SPLASH_SCREEN_DELAY);
        }
    }
    private void startLoginActivity() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
    private boolean isFromPlayStore() {
        String installer = getPackageManager().getInstallerPackageName(getPackageName());
        return installer != null && installer.equals("com.android.vending");
    }

}
package org.orgaprop.controlprest.controllers.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.orgaprop.controlprest.R;
import org.orgaprop.controlprest.databinding.ActivityLoginBinding;
import org.orgaprop.controlprest.services.HttpTask;
import org.orgaprop.controlprest.services.Prefs;
import org.orgaprop.controlprest.utils.AndyUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.StringTokenizer;
import java.util.concurrent.CompletableFuture;

public class LoginActivity extends AppCompatActivity {

//********* PRIVATE VARIABLES

    private static LoginActivity mLoginActivity;

    private SharedPreferences Preferences;
    private Prefs prefs;
    private boolean isFirst;
    private boolean isConnected;
    private String userName;
    private String password;


//********* PUBLIC VARIABLES

    public static final int VERSION = 1;

    public static String idMbr;
    public static String adrMac;
    public static String id_client;

    public static String phoneName;
    public static String phoneModel;
    public static String phoneBuild;

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

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        EditText mUserName = binding.loginActivityUsernameTxt;
        EditText mUserPwd = binding.loginActivityPasswordTxt;

        mLoginActivity = this;

        Preferences = getSharedPreferences(PREF_NAME_APPLI, MODE_PRIVATE);
        prefs = new Prefs(this);

        userName = Preferences.getString(PREF_KEY_MBR, "");
        password = Preferences.getString(PREF_KEY_PWD, "");
        idMbr = "new";
        adrMac = "new";
        isFirst = true;
        isConnected = false;
        id_client = null;

        mUserName.setText(userName);
        mUserPwd.setText(password);
    }
    @Override
    protected void onResume() {
        super.onResume();

        if( !isConnected ) {
            testIdentified();
        }

        showWait(false);
    }
    @Override
    protected void onPostResume() {
        super.onPostResume();

        EditText mUserName = binding.loginActivityUsernameTxt;
        EditText mUserPwd = binding.loginActivityPasswordTxt;

        if( !isFirst ) {
            if( idMbr.equals("new") ) {
                mUserName.setText("");
                mUserPwd.setText("");

                prefs.setMbr("new");

                openConexion();
            } else {
                openDeco();
            }
        }
    }

//********* SURCHARGES

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == AndyUtils.PERMISSION_REQUEST) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(LoginActivity.this, getResources().getString(R.string.mess_bad_permission_internet), Toast.LENGTH_LONG).show();
                finish();
            }
            if (grantResults[1] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(LoginActivity.this, getResources().getString(R.string.mess_bad_permission_write), Toast.LENGTH_LONG).show();
            }
        }
    }

//********* PUBLIC FUNCTIONS

    public static LoginActivity getInstance() {
        return mLoginActivity;
    }

    public void loginActivityActions(View v) {
        String tag = v.getTag().toString();

        switch( tag ) {
            case "on": connectMbr(); break;
            case "off": deconectMbr(); break;
            case "robot": requestConexion(HttpTask.HTTP_TASK_CBL_ROBOT); break;
            case "mail": requestConexion(HttpTask.HTTP_TASK_CBL_MAIL); break;
            case "rgpd": openWebPage(); break;
        }
    }

//********* PRIVATE FUNCTIONS

    private void connectMbr() {
        EditText mUserName = binding.loginActivityUsernameTxt;
        EditText mUserPwd = binding.loginActivityPasswordTxt;
        CheckBox mCheckBox = binding.loginActivityRgpdChx;

        if( !isConnected ) {
            isConnected = true;

            if( ( !mUserName.getText().toString().isEmpty() ) && ( !mUserPwd.getText().toString().isEmpty() ) ) {
                userName = mUserName.getText().toString();
                password = mUserPwd.getText().toString();

                if( mCheckBox.isChecked() ) {
                    try {
                        String mac = Build.FINGERPRINT;
                        String pwd = URLEncoder.encode(password, "utf-8");
                        String stringGet = "version=" + VERSION + "&phone=" + phoneName + "&model=" + phoneModel + "&build=" + phoneBuild;
                        String stringPost = "psd=" + userName + "&pwd=" + pwd + "&mac=" + mac;

                        HttpTask task = new HttpTask(LoginActivity.this);
                        CompletableFuture<String> futureResult = task.executeHttpTask(HttpTask.HTTP_TASK_ACT_CONEX, HttpTask.HTTP_TASK_CBL_OK, stringGet, stringPost);

                        futureResult.thenAccept(result -> {
                            if( result.startsWith("1") ) {
                                startAppli(result.substring(1));
                            } else {
                                runOnUiThread(() -> {
                                    Toast.makeText(LoginActivity.this, result.substring(1), Toast.LENGTH_SHORT).show();

                                    showWait(false);
                                    isConnected = false;
                                });
                            }
                        }).exceptionally(ex -> {
                            runOnUiThread(() -> {
                                Toast.makeText(LoginActivity.this, getResources().getString(R.string.mess_timeout), Toast.LENGTH_SHORT).show();

                                showWait(false);
                                isConnected = false;
                            });

                            return null;
                        });
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();

                        showWait(false);
                        isConnected = false;
                    }
                } else {
                    Toast.makeText(LoginActivity.this, R.string.mess_err_rgpd, Toast.LENGTH_LONG).show();

                    showWait(false);
                    isConnected = false;
                }
            } else {
                Toast.makeText(LoginActivity.this, R.string.mess_err_conex, Toast.LENGTH_LONG).show();

                showWait(false);
                isConnected = false;
            }
        }
    }
    private void deconectMbr() {
        EditText mUserName = binding.loginActivityUsernameTxt;
        EditText mUserPwd = binding.loginActivityPasswordTxt;

        try {
            String mac = Build.FINGERPRINT;
            String pwd = URLEncoder.encode(password, "utf-8");
            String stringGet = "version=" + VERSION + "&phone=" + phoneName + "&model=" + phoneModel + "&build=" + phoneBuild;
            String stringPost = "psd=" + userName + "&pwd=" + pwd + "&mac=" + mac;

            HttpTask task = new HttpTask(LoginActivity.this);
            CompletableFuture<String> futureResult = task.executeHttpTask(HttpTask.HTTP_TASK_ACT_CONEX, HttpTask.HTTP_TASK_CBL_OK, stringGet, stringPost);

            futureResult.thenAccept(result -> {
                if( result.startsWith("1") ) {
                    mUserName.setText("");
                    mUserPwd.setText("");

                    idMbr = "new";
                    isFirst = true;
                    isConnected = false;

                    prefs.setMbr(idMbr);
                    prefs.setAgency("");
                    prefs.setGroup("");
                    prefs.setResidence("");

                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, result.substring(1), Toast.LENGTH_SHORT).show();

                    showWait(false);
                    isConnected = false;
                }
            }).exceptionally(ex -> {
                Toast.makeText(LoginActivity.this, getResources().getString(R.string.mess_timeout), Toast.LENGTH_SHORT).show();

                showWait(false);
                isConnected = false;

                return null;
            });
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();

            showWait(false);
            isConnected = false;
        }
    }
    private void requestConexion(String m) {
        Intent intent = new Intent(LoginActivity.this, GetMailActivity.class);

        intent.putExtra(GetMailActivity.GET_MAIL_ACTIVITY_TYPE, m);

        LoginActivity.this.startActivity(intent);
    }
    private void openWebPage() {
        String url = "https://www.orgaprop.org/ress/protectDonneesPersonnelles.html";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    private void startAppli(String result) {
        StringTokenizer tokenizer = new StringTokenizer(result, "#");
        int version = Integer.parseInt(tokenizer.nextToken());

        if( version == VERSION ) {
            idMbr = tokenizer.nextToken();
            adrMac = tokenizer.nextToken();
            id_client = tokenizer.nextToken();
            isFirst = false;

            prefs.setMbr(idMbr);
            prefs.setAdrMac(adrMac);

            SharedPreferences.Editor editor = Preferences.edit();

            editor.putString(PREF_KEY_MBR, userName);
            editor.putString(PREF_KEY_PWD, password);
            editor.putString(PREF_KEY_CLT, id_client);
            editor.apply();

            Intent intent = new Intent(LoginActivity.this, SelectActivity.class);

            intent.putExtra(SelectActivity.SELECT_ACTIVITY_EXTRA, tokenizer.nextToken());

            startActivity(intent);
        } else {
            runOnUiThread(this::openVersion);
        }
    }
    private void testIdentified() {
        if( ContextCompat.checkSelfPermission(LoginActivity.this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(LoginActivity.this, new String[]{
                    Manifest.permission.INTERNET,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, AndyUtils.PERMISSION_REQUEST);
        }

        prefs.getMbr(new Prefs.Callback<String>() {
            @Override
            public void onResult(String mbr) {
                idMbr = mbr;
                adrMac = Build.FINGERPRINT;

                String stringGet = "version=" + VERSION;
                String stringPost = "mbr=" + idMbr + "&mac=" + adrMac;

                HttpTask task = new HttpTask(LoginActivity.this);
                CompletableFuture<String> futureResult = task.executeHttpTask(HttpTask.HTTP_TASK_ACT_CONEX, HttpTask.HTTP_TASK_CBL_TEST, stringGet, stringPost);

                futureResult.thenAccept(result -> {
                    if( result.startsWith("1") ) {
                        isConnected = true;
                        startAppli(result.substring(1));
                    } else {
                        if (!result.equals("0")) {
                            Toast.makeText(LoginActivity.this, result.substring(1), Toast.LENGTH_SHORT).show();
                        }

                        openConexion();
                    }
                }).exceptionally(ex -> {
                    Toast.makeText(LoginActivity.this, getResources().getString(R.string.mess_timeout), Toast.LENGTH_SHORT).show();

                    openConexion();
                    return null;
                });
            }
        });
    }

    private void openConexion() {
        ConstraintLayout mLayoutConnect = binding.loginActivityConnectLyt;
        LinearLayout mLayoutDeco = binding.loginActivityDecoLyt;
        LinearLayout mLayoutVersion = binding.loginActivityVersionLyt;
        EditText mUserName = binding.loginActivityUsernameTxt;
        EditText mUserPwd = binding.loginActivityPasswordTxt;

        idMbr = "new";
        isConnected = false;

        mUserName.setText(userName);
        mUserPwd.setText(password);

        mLayoutDeco.setVisibility(View.GONE);
        mLayoutVersion.setVisibility(View.GONE);
        mLayoutConnect.setVisibility(View.VISIBLE);
    }
    private void openDeco() {
        ConstraintLayout mLayoutConnect = binding.loginActivityConnectLyt;
        LinearLayout mLayoutDeco = binding.loginActivityDecoLyt;
        LinearLayout mLayoutVersion = binding.loginActivityVersionLyt;

        mLayoutConnect.setVisibility(View.GONE);
        mLayoutVersion.setVisibility(View.GONE);
        mLayoutDeco.setVisibility(View.VISIBLE);
    }
    private void openVersion() {
        ConstraintLayout mLayoutConnect = binding.loginActivityConnectLyt;
        LinearLayout mLayoutDeco = binding.loginActivityDecoLyt;
        LinearLayout mLayoutVersion = binding.loginActivityVersionLyt;

        mLayoutConnect.setVisibility(View.GONE);
        mLayoutDeco.setVisibility(View.GONE);
        mLayoutVersion.setVisibility(View.VISIBLE);
    }

    private void showWait(Boolean b) {
        pl.droidsonroids.gif.GifImageView mWaitImg = binding.loginActivityWaitImg;

        if( b ) {
            mWaitImg.setVisibility(View.VISIBLE);
        } else {
            mWaitImg.setVisibility(View.INVISIBLE);
        }
    }

}
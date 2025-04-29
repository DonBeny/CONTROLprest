package org.orgaprop.controlprest.controllers.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import java.util.concurrent.CompletableFuture;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.orgaprop.controlprest.R;
import org.orgaprop.controlprest.databinding.ActivityGetMailBinding;
import org.orgaprop.controlprest.services.HttpTask;
import org.orgaprop.controlprest.utils.AndyUtils;
import org.orgaprop.controlprest.utils.ToastManager;



public class GetMailActivity extends AppCompatActivity {

//********* PRIVATE VARIABLES

    private String typeRequete;
    private boolean isRequestInProgress = false;
    private FirebaseCrashlytics crashlytics;
    private FirebaseAnalytics analytics;

//********* STATIC VARIABLES

    private static final String TAG = "GetMailActivity";

    public static final String GET_MAIL_ACTIVITY_TYPE = "type";

//********* WIDGETS

    private ActivityGetMailBinding binding;

//********* CONSTRUCTORS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            crashlytics = FirebaseCrashlytics.getInstance();
            crashlytics.setCustomKey("deviceModel", Build.MODEL);
            crashlytics.setCustomKey("deviceManufacturer", Build.MANUFACTURER);
            crashlytics.log("GetMailActivity démarrée");

            analytics = FirebaseAnalytics.getInstance(this);

            Bundle screenViewParams = new Bundle();
            screenViewParams.putString(FirebaseAnalytics.Param.SCREEN_NAME, "GetMail");
            screenViewParams.putString(FirebaseAnalytics.Param.SCREEN_CLASS, "GetMailActivity");
            analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, screenViewParams);

            binding = ActivityGetMailBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            EditText mEditText = binding.getMailActivityMailTxt;

            Intent intent = getIntent();

            if (intent == null) {
                crashlytics.log("Intent is null");
                Log.e(TAG, "Intent is null");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "null_intent");
                errorParams.putString("class", "GetMailActivity");
                errorParams.putString("screen", "GetMailActivity");
                analytics.logEvent("onCreate_intent", errorParams);

                finish();
                return;
            }

            typeRequete = intent.getStringExtra(GET_MAIL_ACTIVITY_TYPE);
            if (typeRequete == null) {
                crashlytics.log("Type de requête est null, utilisation de valeur par défaut");
                Log.e(TAG, "Type de requête est null");

                Bundle reqTypeParams = new Bundle();
                reqTypeParams.putString("issue", "null_request_type");
                reqTypeParams.putString("class", "GetMailActivity");
                reqTypeParams.putString("default_type", HttpTask.HTTP_TASK_CBL_MAIL);
                analytics.logEvent("onCreate_type_requete", reqTypeParams);

                typeRequete = HttpTask.HTTP_TASK_CBL_MAIL;
            }

            crashlytics.setCustomKey("typeRequete", typeRequete);

            Bundle typeParams = new Bundle();
            typeParams.putString("class", "GetMailActivity");
            typeParams.putString("request_type", typeRequete);
            analytics.logEvent("onCreate_mail_request_type", typeParams);

            mEditText.setOnEditorActionListener((textView, actionId, keyEvent) -> {
                try {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        crashlytics.log("Action IME_ACTION_DONE déclenchée");

                        Bundle keyboardParams = new Bundle();
                        keyboardParams.putString("class", "GetMailActivity");
                        keyboardParams.putString("action", "ime_done");
                        analytics.logEvent("onCreate_keyboard_action", keyboardParams);

                        if (mEditText.getText().length() > 0) {
                            hideKeyboard();
                            sendRequest();
                        }
                        return true;
                    }
                } catch (Exception e) {
                    crashlytics.recordException(e);
                    Log.e(TAG, "Exception dans onEditorActionListener: " + e.getMessage(), e);

                    Bundle errorParams = new Bundle();
                    errorParams.putString("error_type", "editor_action_exception");
                    errorParams.putString("class", "GetMailActivity");
                    errorParams.putString("error_message", e.getMessage());
                    analytics.logEvent("onCreate_editor_listener", errorParams);

                    String msg = getString(R.string.erreur_lors_de_l_envoi_de_la_demande);
                    ToastManager.showError(msg);
                }
                return false;
            });
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance();
            FirebaseCrashlytics.getInstance().recordException(e);

            try {
                FirebaseAnalytics.getInstance(this).logEvent("fatal_app_error", null);
            } catch (Exception analyticsEx) {
                Log.e(TAG, "Exception dans onCreate analytics: " + analyticsEx.getMessage(), analyticsEx);
            }

            String msg = getString(R.string.une_erreur_est_survenue_lors_de_l_initialisation);
            Log.e(TAG, "Exception dans onCreate: " + e.getMessage(), e);
            ToastManager.showError(msg);
            finish();
        }
    }

//********* PUBLIC FUNCTIONS

    public void getMailActivityActions(View v) {
        try {
            crashlytics.log("getMailActivityActions appelée");

            hideKeyboard();
            sendRequest();
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Exception dans getMailActivityActions: " + e.getMessage(), e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "action_exception");
            errorParams.putString("class", "GetMailActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("getMailActivityActions_error", errorParams);

            String msg = getString(R.string.erreur_lors_de_l_envoi_de_la_demande);
            ToastManager.showError(msg);
        }
    }

//********* PRIVATE FUNCTIONS

    private void hideKeyboard() {
        try {
            crashlytics.log("hideKeyboard appelée");

            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null && getCurrentFocus() != null) {
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Exception dans hideKeyboard: " + e.getMessage(), e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "keyboard_exception");
            errorParams.putString("class", "GetMailActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("hideKeyboard_error", errorParams);

            String msg = getString(R.string.erreur_lors_de_l_envoi_de_la_demande);
            ToastManager.showError(msg);
        }
    }

    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void sendRequest() {
        try {
            crashlytics.log("sendRequest appelée");

            if (isRequestInProgress) {
                crashlytics.log("Une requête est déjà en cours");
                Log.d(TAG, "Une requête est déjà en cours");
                return;
            }

            EditText mEditText = binding.getMailActivityMailTxt;
            String email = mEditText.getText().toString().trim();

            if (email.isEmpty()) {
                crashlytics.log("exception dans sendRequest: email empty");
                Log.d(TAG, "exception dans sendRequest: email empty");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "empty_email");
                errorParams.putString("class", "GetMailActivity");
                errorParams.putString("method", "sendRequest");
                analytics.logEvent("sendRequest_email_empty", errorParams);

                String msg = getString(R.string.veuillez_entrer_une_adresse_email);
                ToastManager.showError(msg);
                return;
            }

            if (!isValidEmail(email)) {
                crashlytics.log("exception dans sendRequest: email not valid ("+email+")");
                Log.d(TAG, "exception dans sendRequest: email not valid");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "invalid_email");
                errorParams.putString("class", "GetMailActivity");
                analytics.logEvent("sendRequest_email_invalid", errorParams);

                String msg = getString(R.string.veuillez_entrer_une_adresse_email_valide);
                ToastManager.showError(msg);
                return;
            }

            crashlytics.setCustomKey("emailLength", email.length());

            if (!AndyUtils.isNetworkAvailable(this)) {
                crashlytics.log("exception dans sendRequest: no network");
                Log.d(TAG, "exception dans sendRequest: no network");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "no_network");
                errorParams.putString("class", "GetMailActivity");
                analytics.logEvent("SendRequest_no_network", errorParams);

                String msg = getString(R.string.mess_conextion_lost);
                ToastManager.showError(msg);
                return;
            }

            isRequestInProgress = true;

            crashlytics.log("Préparation de la requête HTTP");

            String mail = "mail=" + mEditText.getText().toString();
            String cbl = (typeRequete.equals(HttpTask.HTTP_TASK_CBL_ROBOT)) ? HttpTask.HTTP_TASK_CBL_ROBOT : HttpTask.HTTP_TASK_CBL_MAIL;

            HttpTask task = new HttpTask(GetMailActivity.this);

            crashlytics.log("Envoi de la requête: " + cbl);

            CompletableFuture<String> futureResult = task.executeHttpTask(HttpTask.HTTP_TASK_ACT_CONEX, cbl, "", mail);

            futureResult.thenAccept(result -> {
                runOnUiThread(() -> {
                    try {
                        isRequestInProgress = false;
                        crashlytics.log("Réponse reçue");

                        if (result == null) {
                            crashlytics.log("result null");
                            Log.d(TAG, "exception dans sendRequest: result null");

                            Bundle errorParams = new Bundle();
                            errorParams.putString("error_type", "null_result");
                            errorParams.putString("class", "GetMailActivity");
                            analytics.logEvent("SendRequest_null_result", errorParams);

                            String msg = getString(R.string.erreur_de_communication_avec_le_serveur);
                            ToastManager.showError(msg);
                            return;
                        }

                        if( result.startsWith("0") ) {
                            crashlytics.log("Échec: " + (result.length() > 1 ? result.substring(1) : "inconnu"));
                            Log.d(TAG, "exception dans sendRequest: result error");

                            Bundle errorParams = new Bundle();
                            errorParams.putString("error_type", "error_result");
                            errorParams.putString("class", "GetMailActivity");
                            analytics.logEvent("SendRequest_error_result", errorParams);

                            String msg = result.substring(1);
                            ToastManager.showError(msg);

                            setResult(RESULT_CANCELED);
                        } else {
                            crashlytics.log("Succès: Email envoyé à " + email);
                            Log.d(TAG, "exception dans sendRequest: result success");

                            Bundle errorParams = new Bundle();
                            errorParams.putString("error_type", "success_result");
                            errorParams.putString("class", "GetMailActivity");
                            analytics.logEvent("sendRequest_success", errorParams);

                            String msg = getString(R.string.un_message_a_t_envoy) + " à "  + email;
                            ToastManager.showShort(msg);

                            setResult(RESULT_OK);
                        }

                        new Handler(Looper.getMainLooper()).postDelayed(this::finish, 2000);
                    } catch (Exception e) {
                        crashlytics.recordException(e);
                        Log.e(TAG, "Exception dans le traitement du résultat: " + e.getMessage(), e);

                        Bundle errorParams = new Bundle();
                        errorParams.putString("error_type", "result_processing_exception");
                        errorParams.putString("class", "GetMailActivity");
                        errorParams.putString("error_message", e.getMessage());
                        analytics.logEvent("sendRequest_result_processing", errorParams);

                        String msg = getString(R.string.erreur_lors_du_traitement_de_la_r_ponse);
                        ToastManager.showError(msg);

                        setResult(RESULT_CANCELED);
                        finish();
                    }
                });
            }).exceptionally(ex -> {
                runOnUiThread(() -> {
                    String msg = getString(R.string.erreur_de_communication_avec_le_serveur);

                    try {
                        isRequestInProgress = false;
                        crashlytics.recordException(ex);
                        crashlytics.log("Exception dans la requête HTTP");
                        Log.e(TAG, "Exception dans sendRequest: " + ex.getMessage(), ex);

                        Bundle errorParams = new Bundle();
                        errorParams.putString("error_type", "http_request_exception");
                        errorParams.putString("class", "GetMailActivity");
                        errorParams.putString("error_message", ex.getMessage());
                        analytics.logEvent("sendRequest_http_request", errorParams);

                        ToastManager.showError(msg);

                        setResult(RESULT_CANCELED);
                        new Handler(Looper.getMainLooper()).postDelayed(this::finish, 2000);
                    } catch (Exception e) {
                        if (crashlytics != null) {
                            crashlytics.recordException(e);
                        }
                        Log.e(TAG, "Exception dans exceptionally: " + e.getMessage(), e);

                        Bundle errorParams = new Bundle();
                        errorParams.putString("error_type", "exceptionally_exception");
                        errorParams.putString("class", "GetMailActivity");
                        errorParams.putString("error_message", e.getMessage());
                        analytics.logEvent("sendRequest_exceptionally", errorParams);

                        finish();
                    }
                });

                return null;
            });
        } catch (Exception e) {
            isRequestInProgress = false;

            crashlytics.recordException(e);
            Log.e(TAG, "Exception dans sendRequest: " + e.getMessage(), e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "send_request_exception");
            errorParams.putString("class", "GetMailActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("sendRequest_exception", errorParams);

            String msg = getString(R.string.erreur_lors_de_l_envoi_de_la_demande);
            ToastManager.showError(msg);
        }
    }

}

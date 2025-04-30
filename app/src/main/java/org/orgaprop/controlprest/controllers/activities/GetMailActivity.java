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
import java.util.regex.Pattern;

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
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final int MESSAGE_DISPLAY_DELAY = 3500; // Longer delay for user to read messages
    private static final String EMAIL_PATTERN = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[<>\"'&]");

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

            Log.i(TAG, "GetMailActivity démarrée");

            analytics = FirebaseAnalytics.getInstance(this);

            Bundle screenViewParams = new Bundle();
            screenViewParams.putString(FirebaseAnalytics.Param.SCREEN_NAME, "GetMail");
            screenViewParams.putString(FirebaseAnalytics.Param.SCREEN_CLASS, "GetMailActivity");
            analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, screenViewParams);

            binding = ActivityGetMailBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            EditText mEditText = binding.getMailActivityMailTxt;

            setupEditTextListeners(mEditText);

            Intent intent = getIntent();

            if (intent == null) {
                logError("Intent is null", "null_intent");
                showErrorAndFinish(getString(R.string.erreur_lors_de_l_initialisation_de_l_application));
                return;
            }

            typeRequete = intent.getStringExtra(GET_MAIL_ACTIVITY_TYPE);
            if (typeRequete == null) {
                logWarning("Request type is null, using default value", "null_request_type");
                typeRequete = HttpTask.HTTP_TASK_CBL_MAIL;
            }

            crashlytics.setCustomKey("typeRequete", typeRequete);

            Bundle typeParams = new Bundle();
            typeParams.putString("class", "GetMailActivity");
            typeParams.putString("request_type", typeRequete);
            analytics.logEvent("onCreate_mail_request_type", typeParams);

            if (!isValidRequestType(typeRequete)) {
                logError("Invalid request type: " + typeRequete, "invalid_request_type");
                showErrorAndFinish(getString(R.string.erreur_lors_de_l_initialisation_de_l_application));
                return;
            }
        } catch (Exception e) {
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            // Clean up resources
            handler.removeCallbacksAndMessages(null);
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy: " + e.getMessage(), e);
        }
    }

//********* PUBLIC FUNCTIONS

    public void getMailActivityActions(View v) {
        try {
            crashlytics.log("getMailActivityActions appelée");
            Log.i(TAG, "getMailActivityActions appelée");

            if (!isActivityValid()) {
                logWarning("Activity state invalid when handling action", "invalid_activity_state");
                return;
            }

            if (v == null || v.getTag() == null) {
                logWarning("View or tag is null", "null_view_or_tag");
                return;
            }

            String tag = v.getTag().toString();
            if (!"send".equals(tag)) {
                logWarning("Unknown view tag: " + tag, "unknown_tag");
                return;
            }

            hideKeyboard();
            sendRequest();
        } catch (Exception e) {
            logException(e, "action_exception", "Exception dans getMailActivityActions");

            String msg = getString(R.string.erreur_lors_de_l_envoi_de_la_demande);
            ToastManager.showError(msg);
        }
    }

//********* PRIVATE FUNCTIONS

    /**
     * Configure les listeners pour l'EditText
     */
    private void setupEditTextListeners(EditText editText) {
        try {
            editText.setOnEditorActionListener((textView, actionId, keyEvent) -> {
                try {
                    if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEND) {
                        crashlytics.log("Action IME_ACTION déclenché: " + actionId);
                        Log.d(TAG, "Action clavier détectée: " + actionId);

                        Bundle keyboardParams = new Bundle();
                        keyboardParams.putString("class", "GetMailActivity");
                        keyboardParams.putString("action", "ime_action");
                        keyboardParams.putInt("action_id", actionId);
                        analytics.logEvent("keyboard_action", keyboardParams);

                        if (editText.getText().length() > 0) {
                            hideKeyboard();
                            sendRequest();
                        }
                        return true;
                    }
                } catch (Exception e) {
                    logException(e, "editor_action_exception", "Exception dans onEditorActionListener");
                    // Même en cas d'erreur, on continue le traitement normal
                }
                return false;
            });

            editText.setOnFocusChangeListener((v, hasFocus) -> {
                try {
                    if (hasFocus) {
                        final InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
                        }
                    }
                } catch (Exception e) {
                    logException(e, "focus_change_exception", "Exception dans onFocusChangeListener");
                }
            });
        } catch (Exception e) {
            logException(e, "setup_listeners_exception", "Exception lors de la configuration des listeners");
        }
    }

    /**
     * Vérifie si le type de requête est valide
     */
    private boolean isValidRequestType(String type) {
        return HttpTask.HTTP_TASK_CBL_MAIL.equals(type) || HttpTask.HTTP_TASK_CBL_ROBOT.equals(type);
    }

    /**
     * Vérifie si l'activité est dans un état valide pour traiter des actions
     */
    private boolean isActivityValid() {
        return !isFinishing() && !isDestroyed();
    }

    private void hideKeyboard() {
        try {
            crashlytics.log("hideKeyboard appelée");

            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null && getCurrentFocus() != null) {
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                Log.d(TAG, "Clavier masqué avec succès");
            } else {
                Log.d(TAG, "Impossible de masquer le clavier - focus ou IMM null");
            }
        } catch (Exception e) {
            logException(e, "keyboard_exception", "Exception dans hideKeyboard");
        }
    }

    /**
     * Valide l'email avec des règles plus strictes
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }

        // Vérification standard du format de l'email
        boolean isValidFormat = Patterns.EMAIL_ADDRESS.matcher(email).matches();

        // Vérification supplémentaire contre les injections
        boolean containsSpecialChars = SPECIAL_CHARS_PATTERN.matcher(email).find();

        return isValidFormat && !containsSpecialChars;
    }

    /**
     * Envoie la requête au serveur après validation
     */
    private void sendRequest() {
        try {
            crashlytics.log("sendRequest appelée");

            if (isRequestInProgress) {
                logWarning("Une requête est déjà en cours", "request_in_progress");
                ToastManager.showShort(getString(R.string.une_op_ration_est_d_j_en_cours));
                return;
            }

            if (!isActivityValid()) {
                logWarning("Activity invalide lors de sendRequest", "invalid_activity_state");
                return;
            }

            EditText mEditText = binding.getMailActivityMailTxt;
            String email = mEditText.getText().toString().trim();

            if (email.isEmpty()) {
                logWarning("Email vide", "empty_email");
                ToastManager.showError(getString(R.string.veuillez_entrer_une_adresse_email));
                return;
            }

            if (!isValidEmail(email)) {
                logWarning("Email invalide: " + email, "invalid_email");
                ToastManager.showError(getString(R.string.veuillez_entrer_une_adresse_email_valide));
                return;
            }

            // Vérification du réseau
            if (!AndyUtils.isNetworkAvailable(getApplicationContext())) {
                logWarning("Pas de connexion réseau", "no_network");
                ToastManager.showError(getString(R.string.mess_conextion_lost));
                return;
            }

            isRequestInProgress = true;
            crashlytics.log("Préparation de la requête HTTP");
            Log.d(TAG, "Envoi d'une requête pour l'email: " + email);

            String mail = "mail=" + mEditText.getText().toString();
            String cbl = (typeRequete.equals(HttpTask.HTTP_TASK_CBL_ROBOT)) ? HttpTask.HTTP_TASK_CBL_ROBOT : HttpTask.HTTP_TASK_CBL_MAIL;

            HttpTask task = new HttpTask(GetMailActivity.this);

            crashlytics.log("Envoi de la requête: " + cbl);

            CompletableFuture<String> futureResult = task.executeHttpTask(HttpTask.HTTP_TASK_ACT_CONEX, cbl, "", mail);

            futureResult.thenAccept(result -> {
                runOnUiThread(() -> {
                    try {
                        isRequestInProgress = false;
                        crashlytics.log("Réponse reçue: " + (result != null ? result.substring(0, Math.min(50, result != null ? result.length() : 0)) + "..." : "null"));
                        Log.d(TAG, "Réponse du serveur reçue");

                        if (result == null) {
                            logError("Résultat null", "null_result");
                            showErrorAndFinish(getString(R.string.erreur_de_communication_avec_le_serveur));
                            return;
                        }

                        if (result.isEmpty()) {
                            logError("Résultat vide", "empty_result");
                            showErrorAndFinish(getString(R.string.erreur_de_communication_avec_le_serveur));
                            return;
                        }

                        if (result.startsWith("0")) {
                            String errorMessage = result.length() > 1 ? result.substring(1) : getString(R.string.erreur_lors_du_traitement_de_la_r_ponse);
                            logError("Échec: " + errorMessage, "error_result");
                            ToastManager.showError(errorMessage);
                            setResult(RESULT_CANCELED);
                        } else if (result.startsWith("1")) {
                            logInfo("Succès: Email envoyé à " + email, "success_result");
                            String msg = getString(R.string.un_message_a_t_envoy) + " à " + email;
                            ToastManager.showShort(msg);
                            setResult(RESULT_OK);
                        } else {
                            logError("Format de réponse inattendu: " + result, "unexpected_format");
                            ToastManager.showError(getString(R.string.erreur_lors_du_traitement_de_la_r_ponse));
                            setResult(RESULT_CANCELED);
                        }

                        finishWithDelay();
                    } catch (Exception e) {
                        isRequestInProgress = false;
                        logException(e, "result_processing_exception", "Exception dans le traitement du résultat");
                        showErrorAndFinish(getString(R.string.erreur_lors_du_traitement_de_la_r_ponse));
                    }
                });
            }).exceptionally(ex -> {
                runOnUiThread(() -> {
                    isRequestInProgress = false;
                    logException((Exception) ex, "http_request_exception", "Exception dans la requête HTTP");
                    showErrorAndFinish(getString(R.string.erreur_de_communication_avec_le_serveur));
                });
                return null;
            });
        } catch (Exception e) {
            isRequestInProgress = false;
            logException(e, "send_request_exception", "Exception dans sendRequest");
            ToastManager.showError(getString(R.string.erreur_lors_de_l_envoi_de_la_demande));
        }
    }

    /**
     * Termine l'activité après un délai
     */
    private void finishWithDelay() {
        try {
            // Using a longer delay to ensure the user can read the message
            handler.postDelayed(this::finish, MESSAGE_DISPLAY_DELAY);
        } catch (Exception e) {
            logException(e, "finish_delay_exception", "Exception lors de la fermeture avec délai");
            finish(); // Fallback to immediate finish
        }
    }

    /**
     * Affiche une erreur et termine l'activité
     */
    private void showErrorAndFinish(String message) {
        try {
            ToastManager.showError(message);
            finishWithDelay();
        } catch (Exception e) {
            logException(e, "show_error_finish_exception", "Exception lors de l'affichage de l'erreur");
            finish();
        }
    }

    //********* LOGGING METHODS

    private void logInfo(String message, String errorType) {
        try {
            crashlytics.log("INFO: " + message);
            Log.i(TAG, message);

            Bundle params = new Bundle();
            params.putString("info_type", errorType);
            params.putString("class", "GetMailActivity");
            params.putString("info_message", message);
            if (analytics != null) {
                analytics.logEvent("app_info", params);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in logInfo: " + e.getMessage());
        }
    }

    private void logWarning(String message, String errorType) {
        try {
            crashlytics.log("WARNING: " + message);
            Log.w(TAG, message);

            Bundle params = new Bundle();
            params.putString("warning_type", errorType);
            params.putString("class", "GetMailActivity");
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
            params.putString("class", "GetMailActivity");
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
            params.putString("class", "GetMailActivity");
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

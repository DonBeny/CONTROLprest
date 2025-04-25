package org.orgaprop.controlprest.controllers.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import org.orgaprop.controlprest.R;
import org.orgaprop.controlprest.databinding.ActivityGetMailBinding;
import org.orgaprop.controlprest.services.HttpTask;
import org.orgaprop.controlprest.utils.AndyUtils;

import java.util.concurrent.CompletableFuture;



public class GetMailActivity extends AppCompatActivity {

//********* PRIVATE VARIABLES

    private String typeRequete;
    private boolean isRequestInProgress = false;

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
            binding = ActivityGetMailBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            EditText mEditText = binding.getMailActivityMailTxt;

            Intent intent = getIntent();

            if (intent == null) {
                Log.e(TAG, "Intent is null");
                finish();
                return;
            }

            typeRequete = intent.getStringExtra(GET_MAIL_ACTIVITY_TYPE);
            if (typeRequete == null) {
                Log.e(TAG, "Type de requête est null");
                typeRequete = HttpTask.HTTP_TASK_CBL_MAIL;
            }

            mEditText.setOnEditorActionListener((textView, actionId, keyEvent) -> {
                try {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        if (mEditText.getText().length() > 0) {
                            hideKeyboard();
                            sendRequest();
                        }
                        return true;
                    }
                } catch (Exception e) {
                    String msg = getString(R.string.erreur_lors_de_l_envoi_de_la_demande) + " (listener)";
                    Log.e(TAG, "Exception dans onEditorActionListener: " + e.getMessage(), e);
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                }
                return false;
            });
        } catch (Exception e) {
            String msg = getString(R.string.une_erreur_est_survenue_lors_de_l_initialisation) + " (on create)";
            Log.e(TAG, "Exception dans onCreate: " + e.getMessage(), e);
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

//********* PUBLIC FUNCTIONS

    public void getMailActivityActions(View v) {
        try {
            hideKeyboard();
            sendRequest();
        } catch (Exception e) {
            String msg = getString(R.string.erreur_lors_de_l_envoi_de_la_demande) + " (actions)";
            Log.e(TAG, "Exception dans getMailActivityActions: " + e.getMessage(), e);
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        }
    }

//********* PRIVATE FUNCTIONS

    private void hideKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null && getCurrentFocus() != null) {
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }
        } catch (Exception e) {
            String msg = getString(R.string.erreur_lors_de_l_envoi_de_la_demande) + " (hideKeyboard)";
            Log.e(TAG, "Exception dans hideKeyboard: " + e.getMessage(), e);
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void sendRequest() {
        try {
            if (isRequestInProgress) {
                Log.d(TAG, "Une requête est déjà en cours");
                return;
            }

            EditText mEditText = binding.getMailActivityMailTxt;
            String email = mEditText.getText().toString().trim();

            if (email.isEmpty()) {
                String msg = getString(R.string.veuillez_entrer_une_adresse_email) + " (sendRequest)";
                Log.d(TAG, "exception dans sendRequest: email empty");
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isValidEmail(email)) {
                String msg = getString(R.string.veuillez_entrer_une_adresse_email_valide) + " (sendRequest)";
                Log.d(TAG, "exception dans sendRequest: email not valid");
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!AndyUtils.isNetworkAvailable(this)) {
                String msg = getString(R.string.mess_conextion_lost) + " (sendRequest)";
                Log.d(TAG, msg);
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                return;
            }

            isRequestInProgress = true;

            String mail = "mail=" + mEditText.getText().toString();
            String cbl = (typeRequete.equals(HttpTask.HTTP_TASK_CBL_ROBOT)) ? HttpTask.HTTP_TASK_CBL_ROBOT : HttpTask.HTTP_TASK_CBL_MAIL;

            HttpTask task = new HttpTask(GetMailActivity.this);
            CompletableFuture<String> futureResult = task.executeHttpTask(HttpTask.HTTP_TASK_ACT_CONEX, cbl, "", mail);

            futureResult.thenAccept(result -> {
                runOnUiThread(() -> {
                    try {
                        isRequestInProgress = false;

                        if (result == null) {
                            String msg = getString(R.string.erreur_de_communication_avec_le_serveur) + " (sendRequest::result httpTask)";
                            Log.d(TAG, "exception dans sendRequest: result null");
                            Toast.makeText(GetMailActivity.this, msg, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if( result.startsWith("0") ) {
                            String msg = result.substring(1) + " (sendRequest::result httpTask)";
                            Log.d(TAG, "exception dans sendRequest: result error");
                            Toast.makeText(GetMailActivity.this, msg, Toast.LENGTH_SHORT).show();

                            setResult(RESULT_CANCELED);
                        } else {
                            String msg = getString(R.string.un_message_a_t_envoy) + " à "  + email;
                            Log.d(TAG, "exception dans sendRequest: result success");
                            Toast.makeText(GetMailActivity.this, msg, Toast.LENGTH_SHORT).show();

                            setResult(RESULT_OK);
                        }

                        new Handler(Looper.getMainLooper()).postDelayed(this::finish, 2000);
                    } catch (Exception e) {
                        String msg = getString(R.string.erreur_lors_du_traitement_de_la_r_ponse) + " (sendRequest::result httpTask)";
                        Log.e(TAG, "Exception dans le traitement du résultat: " + e.getMessage(), e);
                        Toast.makeText(GetMailActivity.this, msg, Toast.LENGTH_SHORT).show();
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                });
            }).exceptionally(ex -> {
                runOnUiThread(() -> {
                    try {
                        isRequestInProgress = false;

                        String msg = getString(R.string.erreur_de_communication_avec_le_serveur) + " (sendRequest::result httpTask)";
                        Log.e(TAG, "Exception dans sendRequest: " + ex.getMessage(), ex);
                        Toast.makeText(GetMailActivity.this, msg, Toast.LENGTH_SHORT).show();

                        setResult(RESULT_CANCELED);
                        new Handler(Looper.getMainLooper()).postDelayed(this::finish, 2000);
                    } catch (Exception e) {
                        Log.e(TAG, "Exception dans exceptionally: " + e.getMessage(), e);
                        finish();
                    }
                });

                return null;
            });
        } catch (Exception e) {
            isRequestInProgress = false;
            String msg = getString(R.string.erreur_lors_de_l_envoi_de_la_demande) + " (sendRequest)";
            Log.e(TAG, "Exception dans sendRequest: " + e.getMessage(), e);
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        }
    }

}
package org.orgaprop.controlprest.controllers.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.inputmethod.EditorInfo;
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

//********* STATIC VARIABLES

    private static final String TAG = "GetMailActivity";

    public static final String GET_MAIL_ACTIVITY_TYPE = "type";

//********* WIDGETS

    private ActivityGetMailBinding binding;

//********* CONSTRUCTORS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityGetMailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        EditText mEditText = binding.getMailActivityMailTxt;

        Intent intent = getIntent();

        typeRequete = intent.getStringExtra(GET_MAIL_ACTIVITY_TYPE);

        mEditText.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if( mEditText.getText().length() > 0 ) {
                    sendRequest();
                }

                return true;
            }

            return false;
        });
    }

//********* PUBLIC FUNCTIONS

    public void getMailActivityActions(View v) {
        sendRequest();
    }

//********* PRIVATE FUNCTIONS

    private void sendRequest() {
        EditText mEditText = binding.getMailActivityMailTxt;

        if( !AndyUtils.isNetworkAvailable(this) ) {
            return;
        }

        String mail = "mail=" + mEditText.getText().toString();
        String cbl = (typeRequete.equals(HttpTask.HTTP_TASK_CBL_ROBOT)) ? HttpTask.HTTP_TASK_CBL_ROBOT : HttpTask.HTTP_TASK_CBL_MAIL;

        HttpTask task = new HttpTask(GetMailActivity.this);
        CompletableFuture<String> futureResult = task.executeHttpTask(HttpTask.HTTP_TASK_ACT_CONEX, cbl, "", mail);

        futureResult.thenAccept(result -> {
            runOnUiThread(() -> {
                if( result.startsWith("0") ) {
                    Toast.makeText(GetMailActivity.this, result.substring(1), Toast.LENGTH_SHORT).show();

                    setResult(RESULT_CANCELED);
                    new Handler(Looper.getMainLooper()).postDelayed(this::finish, 2000);
                } else {
                    String msg = "Un message a été envoyé";

                    Toast.makeText(GetMailActivity.this, msg, Toast.LENGTH_SHORT).show();

                    setResult(RESULT_OK);
                    new Handler(Looper.getMainLooper()).postDelayed(this::finish, 2000);
                }
            });
        }).exceptionally(ex -> {
            runOnUiThread(() -> {
                Toast.makeText(GetMailActivity.this, getResources().getString(R.string.mess_timeout), Toast.LENGTH_SHORT).show();

                setResult(RESULT_CANCELED);
                new Handler(Looper.getMainLooper()).postDelayed(this::finish, 2000);
            });

            return null;
        });
    }

}
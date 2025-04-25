package org.orgaprop.controlprest.controllers.activities;

import static org.orgaprop.controlprest.controllers.activities.SelectActivity.SELECT_ACTIVITY_RESULT;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.orgaprop.controlprest.R;
import org.orgaprop.controlprest.databinding.ActivitySearchBinding;
import org.orgaprop.controlprest.models.ListResidModel;
import org.orgaprop.controlprest.services.HttpTask;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.StringTokenizer;
import java.util.concurrent.CompletableFuture;

public class SearchActivity extends AppCompatActivity {

//********* STATIC VARIABLES

    public static final String SELECT_SEARCH_ACTIVITY_STR = "val";
    public static final String SELECT_SEARCH_ACTIVITY_RESULT_AGC = "agc";
    public static final String SELECT_SEARCH_ACTIVITY_RESULT_GRP = "grp";
    public static final String SELECT_SEARCH_ACTIVITY_RESULT_RSD = "rsd";

    public static final String SELECT_SEARCH_ACTIVITY_REQUEST = "search";
    public static final int SELECT_SEARCH_ACTIVITY_REQUEST_OK = 1;
    public static final int SELECT_SEARCH_ACTIVITY_REQUEST_CANCEL = 0;

//********* WIDGETS

    private ActivitySearchBinding binding;

//********* CONSTRUCTORS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Intent intent = getIntent();

        startLoading();

        try{
            String strSearch = URLEncoder.encode(intent.getStringExtra(SELECT_SEARCH_ACTIVITY_STR), "UTF-8");
            String stringPost = "mbr=" + LoginActivity.idMbr + "&val=" + strSearch;

            HttpTask task = new HttpTask(SearchActivity.this);
            CompletableFuture<String> futureResult = task.executeHttpTask(HttpTask.HTTP_TASK_ACT_SEARCH, "rsd", "", stringPost);

            futureResult.thenAccept(result -> {
                if( result.startsWith("1") ) {
                    makeView(result.substring(1));
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(SearchActivity.this, result.substring(1), Toast.LENGTH_SHORT).show();

                        setResult(RESULT_CANCELED);

                        new Handler(Looper.getMainLooper()).postDelayed(this::finish, 1000);
                    });
                }
            }).exceptionally(ex -> {
                runOnUiThread(() -> {
                    Toast.makeText(SearchActivity.this, getResources().getString(R.string.mess_timeout), Toast.LENGTH_SHORT).show();

                    setResult(RESULT_CANCELED);

                    new Handler(Looper.getMainLooper()).postDelayed(this::finish, 2000);
                });


                return null;
            });
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

//********* SURCHARGES



//********* PUBLIC FUNCTIONS

    public void searchActivityActions(View v) {
        String viewTag = v.getTag().toString();

        switch( viewTag ) {
            case "cancel": finishActivity(); break;
        }
    }

//********* PRIVATE FUNCTIONS

    private void finishActivity() {
        setResult(RESULT_CANCELED);
        finish();
    }

    private void makeView(String string) {
        LinearLayout mLayout = binding.searchActivityLyt;

        StringTokenizer tokenizer = new StringTokenizer(string, "£");

        if( tokenizer.countTokens() > 0 ) {
            while( tokenizer.hasMoreTokens() ) {
                StringTokenizer item = new StringTokenizer(tokenizer.nextToken(), "§");
                ListResidModel fiche = new ListResidModel();

                fiche.setAgc(item.nextToken());
                fiche.setGrp(item.nextToken());
                fiche.setId(Integer.parseInt(item.nextToken()));
                fiche.setVisited((item.nextToken().equals("1")));
                fiche.setRef(item.nextToken());
                fiche.setName(item.nextToken());
                fiche.setEntry(item.nextToken());
                fiche.setAdresse(item.nextToken());
                fiche.setCity(item.nextToken(), item.nextToken());

                View viewElement = LayoutInflater.from(SearchActivity.this).inflate(R.layout.resid_item, null);

                TextView textViewRef = viewElement.findViewById(R.id.fiche_search_resid_ref);
                TextView textViewName = viewElement.findViewById(R.id.fiche_search_resid_name);
                TextView textViewEntry = viewElement.findViewById(R.id.fiche_search_resid_entry);
                TextView textViewAdr = viewElement.findViewById(R.id.fiche_search_resid_adr);
                TextView textViewCity = viewElement.findViewById(R.id.fiche_search_resid_city);

                textViewRef.setText(fiche.getRef());
                textViewName.setText(fiche.getName());
                textViewEntry.setText(fiche.getEntry());
                textViewAdr.setText(fiche.getAdress());
                textViewCity.setText(fiche.getCity());

                if (fiche.isVisited()) {
                    viewElement.setBackground(AppCompatResources.getDrawable(this, R.drawable.button_enabled));
                } else {
                    viewElement.setBackground(AppCompatResources.getDrawable(this, R.drawable.button_standard));
                }

                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                layoutParams.setMargins(5, 5, 5, 5);

                viewElement.setOnClickListener(view -> {
                    Intent intent = new Intent();

                    intent.putExtra(SELECT_ACTIVITY_RESULT, SELECT_SEARCH_ACTIVITY_REQUEST);
                    intent.putExtra(SELECT_SEARCH_ACTIVITY_RESULT_AGC, fiche.getAgc());
                    intent.putExtra(SELECT_SEARCH_ACTIVITY_RESULT_GRP, fiche.getGrp());
                    intent.putExtra(SELECT_SEARCH_ACTIVITY_RESULT_RSD, Integer.toString(fiche.getId()));

                    setResult(RESULT_OK, intent);
                    finish();
                });

                runOnUiThread(() -> mLayout.addView(viewElement, layoutParams));
            }

            stopLoading();
        } else {
            Toast.makeText(SearchActivity.this, "Auncun résultat", Toast.LENGTH_SHORT).show();

            new Handler(Looper.getMainLooper()).postDelayed(this::finish, 2000);
        }
    }

    private void startLoading() {
        runOnUiThread(() -> {
            binding.searchActivityWaitImg.setVisibility(View.VISIBLE);
            binding.searchActivityScroll.setVisibility(View.GONE);
        });

    }

    private void stopLoading() {
        runOnUiThread(() -> {
            binding.searchActivityWaitImg.setVisibility(View.GONE);
            binding.searchActivityScroll.setVisibility(View.VISIBLE);
        });

    }

}
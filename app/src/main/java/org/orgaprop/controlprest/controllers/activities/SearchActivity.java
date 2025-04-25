package org.orgaprop.controlprest.controllers.activities;

import static org.orgaprop.controlprest.controllers.activities.SelectActivity.SELECT_ACTIVITY_RESULT;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
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

    private static final String TAG = "SearchActivity";
    private boolean isLoading = false;

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
        if (intent == null) {
            Log.e(TAG, "onCreate: Intent is null");
            showError("Invalid search request");
            return;
        }

        String searchValue = intent.getStringExtra(SELECT_SEARCH_ACTIVITY_STR);
        if (TextUtils.isEmpty(searchValue)) {
            Log.e(TAG, "onCreate: Search value is empty or null");
            showError("Search term is empty");
            return;
        }

        startLoading();
        performSearch(searchValue);
    }

//********* SURCHARGES

    @Override
    protected void onDestroy() {
        // Clean up resources if needed
        super.onDestroy();
    }

//********* PUBLIC FUNCTIONS

    public void searchActivityActions(View v) {
        if (v == null || v.getTag() == null) {
            Log.e(TAG, "searchActivityActions: View or tag is null");
            return;
        }

        String viewTag = v.getTag().toString();

        if (viewTag.equals("cancel")) {
            finishActivity();
        } else {
            Log.w(TAG, "searchActivityActions: Unknown tag: " + viewTag);
            Toast.makeText(this, "Unknown tag: " + viewTag, Toast.LENGTH_SHORT).show();
        }
    }

//********* PRIVATE FUNCTIONS

    private void performSearch(String searchValue) {
        try {
            String strSearch = URLEncoder.encode(searchValue, "UTF-8");
            if (TextUtils.isEmpty(LoginActivity.idMbr)) {
                Log.e(TAG, "performSearch: Member ID is null or empty");
                showError("Login information is missing");
                return;
            }

            String stringPost = "mbr=" + LoginActivity.idMbr + "&val=" + strSearch;

            HttpTask task = new HttpTask(SearchActivity.this);
            CompletableFuture<String> futureResult = task.executeHttpTask(
                    HttpTask.HTTP_TASK_ACT_SEARCH,
                    "rsd",
                    "",
                    stringPost
            );

            futureResult.thenAccept(result -> {
                if (result == null) {
                    Log.e(TAG, "performSearch: Server returned null response");
                    runOnUiThread(() -> {
                        showError("Server returned null response");
                    });
                    return;
                }

                if (result.startsWith("1")) {
                    try {
                        String data = result.substring(1);
                        if (TextUtils.isEmpty(data)) {
                            runOnUiThread(this::showNoResults);
                        } else {
                            makeView(data);
                        }
                    } catch (IndexOutOfBoundsException e) {
                        Log.e(TAG, "performSearch: Error parsing result", e);
                        runOnUiThread(() -> showError("Error parsing server response"));
                    }
                } else {
                    String errorMessage = result.length() > 1 ? result.substring(1) : "Unknown error";
                    runOnUiThread(() -> {
                        Log.e(TAG, "performSearch: Server returned error: " + errorMessage);
                        Toast.makeText(SearchActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        setResult(RESULT_CANCELED);
                        new Handler(Looper.getMainLooper()).postDelayed(this::finish, 3000);
                    });
                }
            }).exceptionally(ex -> {
                Log.e(TAG, "performSearch: Exception occurred", ex);
                runOnUiThread(() -> {
                    Toast.makeText(SearchActivity.this, getResources().getString(R.string.mess_timeout), Toast.LENGTH_SHORT).show();
                    setResult(RESULT_CANCELED);
                    new Handler(Looper.getMainLooper()).postDelayed(this::finish, 3000);
                });
                return null;
            });
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "performSearch: Encoding error", e);
            showError("Error encoding search term");
        } catch (Exception e) {
            Log.e(TAG, "performSearch: Unexpected error", e);
            showError("An unexpected error occurred");
        }
    }

    private void finishActivity() {
        if (isLoading) {
            // Don't allow finishing while loading
            return;
        }
        setResult(RESULT_CANCELED);
        finish();
    }

    private void makeView(String string) {
        if (TextUtils.isEmpty(string)) {
            runOnUiThread(this::showNoResults);
            return;
        }

        LinearLayout mLayout = binding.searchActivityLyt;

        try {
            StringTokenizer tokenizer = new StringTokenizer(string, "£");
            int itemCount = tokenizer.countTokens();

            if (itemCount > 0) {
                runOnUiThread(mLayout::removeAllViews);
                int processedItems = 0;

                while (tokenizer.hasMoreTokens()) {
                    try {
                        String token = tokenizer.nextToken();
                        if (TextUtils.isEmpty(token)) {
                            continue;
                        }

                        StringTokenizer item = new StringTokenizer(token, "§");
                        if (item.countTokens() < 10) { // Minimum number of expected tokens
                            Log.w(TAG, "makeView: Insufficient tokens in item data");
                            Toast.makeText(this, "Insufficient tokens in item data", Toast.LENGTH_SHORT).show();
                            continue;
                        }

                        ListResidModel fiche = new ListResidModel();
                        try {
                            fiche.setAgc(item.nextToken());
                            fiche.setGrp(item.nextToken());
                            fiche.setId(Integer.parseInt(item.nextToken()));
                            fiche.setVisited(item.nextToken().equals("1"));
                            fiche.setRef(item.nextToken());
                            fiche.setName(item.nextToken());
                            fiche.setEntry(item.nextToken());
                            fiche.setAdresse(item.nextToken());

                            String cp = item.hasMoreTokens() ? item.nextToken() : "";
                            String city = item.hasMoreTokens() ? item.nextToken() : "";
                            fiche.setCity(cp, city);

                            if (item.hasMoreTokens()) {
                                fiche.setLast(item.nextToken());
                            }
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "makeView: Error parsing numeric field", e);
                            Toast.makeText(this, "Error parsing numeric field", Toast.LENGTH_SHORT).show();
                            continue;
                        } catch (Exception e) {
                            Log.e(TAG, "makeView: Error setting fiche data", e);
                            Toast.makeText(this, "Error setting fiche data", Toast.LENGTH_SHORT).show();
                            continue;
                        }

                        createListItem(mLayout, fiche);
                        processedItems++;
                    } catch (Exception e) {
                        Log.e(TAG, "makeView: Error processing token", e);
                        Toast.makeText(this, "Error processing token", Toast.LENGTH_SHORT).show();
                    }
                }

                final int finalProcessedItems = processedItems;
                runOnUiThread(() -> {
                    if (finalProcessedItems == 0) {
                        showNoResults();
                    } else {
                        stopLoading();
                    }
                });
            } else {
                runOnUiThread(this::showNoResults);
            }
        } catch (Exception e) {
            Log.e(TAG, "makeView: Unexpected error", e);
            runOnUiThread(() -> showError("Error processing search results"));
        }
    }

    private void createListItem(LinearLayout layout, ListResidModel fiche) {
        runOnUiThread(() -> {
            try {
                View viewElement = LayoutInflater.from(SearchActivity.this).inflate(R.layout.resid_item, null);
                if (viewElement == null) {
                    Log.e(TAG, "createListItem: Failed to inflate view");
                    Toast.makeText(SearchActivity.this, "Failed to inflate view", Toast.LENGTH_SHORT).show();
                    return;
                }

                TextView textViewRef = viewElement.findViewById(R.id.fiche_search_resid_ref);
                TextView textViewName = viewElement.findViewById(R.id.fiche_search_resid_name);
                TextView textViewEntry = viewElement.findViewById(R.id.fiche_search_resid_entry);
                TextView textViewAdr = viewElement.findViewById(R.id.fiche_search_resid_adr);
                TextView textViewCity = viewElement.findViewById(R.id.fiche_search_resid_city);
                TextView textViewLast = viewElement.findViewById(R.id.fiche_search_resid_last);

                if (textViewRef != null) textViewRef.setText(fiche.getRef());
                if (textViewName != null) textViewName.setText(fiche.getName());
                if (textViewEntry != null) textViewEntry.setText(fiche.getEntry());
                if (textViewAdr != null) textViewAdr.setText(fiche.getAdress());
                if (textViewCity != null) textViewCity.setText(fiche.getCity());
                if (textViewLast != null) textViewLast.setText(fiche.getLast());

                if (fiche.isVisited()) {
                    viewElement.setBackground(AppCompatResources.getDrawable(this, R.drawable.button_enabled));
                } else {
                    viewElement.setBackground(AppCompatResources.getDrawable(this, R.drawable.button_standard));
                }

                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                layoutParams.setMargins(5, 5, 5, 5);

                viewElement.setOnClickListener(view -> {
                    if (isLoading) return;

                    Intent intent = new Intent();
                    intent.putExtra(SELECT_ACTIVITY_RESULT, SELECT_SEARCH_ACTIVITY_REQUEST);
                    intent.putExtra(SELECT_SEARCH_ACTIVITY_RESULT_AGC, fiche.getAgc());
                    intent.putExtra(SELECT_SEARCH_ACTIVITY_RESULT_GRP, fiche.getGrp());
                    intent.putExtra(SELECT_SEARCH_ACTIVITY_RESULT_RSD, Integer.toString(fiche.getId()));

                    setResult(RESULT_OK, intent);
                    finish();
                });

                layout.addView(viewElement, layoutParams);
            } catch (Exception e) {
                Log.e(TAG, "createListItem: Error creating list item", e);
                Toast.makeText(SearchActivity.this, "Error creating list item", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showNoResults() {
        Toast.makeText(SearchActivity.this, "No results found", Toast.LENGTH_SHORT).show();
        stopLoading();
        new Handler(Looper.getMainLooper()).postDelayed(this::finish, 2000);
    }

    private void showError(String message) {
        Toast.makeText(SearchActivity.this, message, Toast.LENGTH_SHORT).show();
        stopLoading();
        setResult(RESULT_CANCELED);
        new Handler(Looper.getMainLooper()).postDelayed(this::finish, 2000);
    }

    private void startLoading() {
        isLoading = true;
        runOnUiThread(() -> {
            binding.searchActivityWaitImg.setVisibility(View.VISIBLE);
            binding.searchActivityScroll.setVisibility(View.GONE);
        });
    }

    private void stopLoading() {
        isLoading = false;
        runOnUiThread(() -> {
            binding.searchActivityWaitImg.setVisibility(View.GONE);
            binding.searchActivityScroll.setVisibility(View.VISIBLE);
        });
    }

}
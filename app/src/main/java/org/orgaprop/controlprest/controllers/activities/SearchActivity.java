package org.orgaprop.controlprest.controllers.activities;

import static org.orgaprop.controlprest.controllers.activities.SelectActivity.SELECT_ACTIVITY_RESULT;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.orgaprop.controlprest.BuildConfig;
import org.orgaprop.controlprest.R;
import org.orgaprop.controlprest.databinding.ActivitySearchBinding;
import org.orgaprop.controlprest.models.ListResidModel;
import org.orgaprop.controlprest.services.HttpTask;
import org.orgaprop.controlprest.utils.ToastManager;



public class SearchActivity extends AppCompatActivity {

    private static final String TAG = "SearchActivity";

    private final AtomicBoolean isLoading = new AtomicBoolean(false);

    private FirebaseCrashlytics crashlytics;
    private FirebaseAnalytics analytics;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final List<ListResidModel> searchResults = new ArrayList<>();
    private SearchResultAdapter resultAdapter;

//********* STATIC VARIABLES

    private static final int FINISH_DELAY_MS = 2500;

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

        try {
            initializeServices();
            logInfo("SearchActivity started", "activity_start");

            binding = ActivitySearchBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            setupRecyclerView();
            processIntent();
        } catch (Exception e) {
            logException(e, "init_error", "Error during activity initialization");
            showErrorAndFinish(getString(R.string.une_erreur_est_survenue_lors_de_l_initialisation));
        }
    }

//********* SURCHARGES

    @Override
    protected void onDestroy() {
        try {
            logInfo("onDestroy called", "activity_lifecycle");
            // Clean up resources if needed
            searchResults.clear();
            binding = null;
        } catch (Exception e) {
            logException(e, "destroy_error", "Error in onDestroy");
        } finally {
            super.onDestroy();
        }
    }

//********* PUBLIC FUNCTIONS

    public void searchActivityActions(View v) {
        try {
            if (v == null || v.getTag() == null) {
                logWarning("searchActivityActions: View or tag is null", "null_view_or_tag");
                return;
            }

            String viewTag = v.getTag().toString();
            logInfo("Action requested: " + viewTag, "user_action");

            if (viewTag.equals("cancel")) {
                finishActivity();
            } else {
                logWarning("Unknown tag: " + viewTag, "unknown_tag");
                ToastManager.showError(getString(R.string.erreur_lors_du_traitement_de_l_action));
            }
        } catch (Exception e) {
            logException(e, "action_error", "Error processing action");
            ToastManager.showError(getString(R.string.erreur_lors_du_traitement_de_l_action));
        }
    }

//********* PRIVATE FUNCTIONS

    private void initializeServices() {
        crashlytics = FirebaseCrashlytics.getInstance();
        crashlytics.setCustomKey("deviceModel", Build.MODEL);
        crashlytics.setCustomKey("deviceManufacturer", Build.MANUFACTURER);
        crashlytics.setCustomKey("appVersion", BuildConfig.VERSION_NAME);

        analytics = FirebaseAnalytics.getInstance(this);

        Bundle screenViewParams = new Bundle();
        screenViewParams.putString(FirebaseAnalytics.Param.SCREEN_NAME, "SearchActivity");
        screenViewParams.putString(FirebaseAnalytics.Param.SCREEN_CLASS, "SearchActivity");
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, screenViewParams);
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = new RecyclerView(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        resultAdapter = new SearchResultAdapter(searchResults);
        recyclerView.setAdapter(resultAdapter);

        // Replace ScrollView with RecyclerView
        LinearLayout container = binding.searchActivityLyt;
        container.removeAllViews();
        container.addView(recyclerView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void processIntent() {
        Intent intent = getIntent();
        if (intent == null) {
            logError("Intent is null", "null_intent");
            showErrorAndFinish(getString(R.string.invalid_search_request));
            return;
        }

        String searchValue = intent.getStringExtra(SELECT_SEARCH_ACTIVITY_STR);
        if (TextUtils.isEmpty(searchValue)) {
            logError("Search value is empty or null", "empty_search_value");
            showErrorAndFinish(getString(R.string.search_term_is_empty));
            return;
        }

        logInfo("Searching for: " + searchValue, "search_term");
        crashlytics.setCustomKey("searchValue", searchValue);

        startLoading();
        performSearch(searchValue);
    }

    private void performSearch(String searchValue) {
        if (!isLoading.get()) {
            logWarning("Search attempted while not in loading state", "invalid_state");
            return;
        }

        try {
            if (TextUtils.isEmpty(LoginActivity.idMbr)) {
                logError("Member ID is null or empty", "null_member_id");
                showErrorAndFinish(getString(R.string.login_information_is_missing));
                return;
            }

            String encodedSearchValue = URLEncoder.encode(searchValue, "UTF-8");
            String postParams = "mbr=" + LoginActivity.idMbr + "&val=" + encodedSearchValue;

            logInfo("Search parameters prepared", "search_params");
            crashlytics.setCustomKey("searchParams", postParams);

            HttpTask task = new HttpTask(SearchActivity.this);
            CompletableFuture<String> futureResult = task.executeHttpTask(
                    HttpTask.HTTP_TASK_ACT_SEARCH,
                    "rsd",
                    "",
                    postParams
            );

            futureResult.thenAccept(this::processSearchResult).exceptionally(ex -> {
                logException((Exception) ex, "http_exception", "Exception in search request");
                mainHandler.post(() -> {
                    showError(getString(R.string.mess_timeout));
                    setResult(RESULT_CANCELED);
                    finishActivityDelayed();
                });
                return null;
            });
        } catch (UnsupportedEncodingException e) {
            logException(e, "encoding_error", "Error encoding search parameters");
            showErrorAndFinish(getString(R.string.error_encoding_search_term));
        } catch (Exception e) {
            logException(e, "unexpected_error", "Unexpected error in performSearch");
            showErrorAndFinish(getString(R.string.an_unexpected_error_occurred));
        }
    }

    private void processSearchResult(String result) {
        if (result == null) {
            logError("Server returned null response", "null_result");
            mainHandler.post(() -> showErrorAndFinish(getString(R.string.server_returned_null_response)));
            return;
        }

        logInfo("Response received: " + (result.startsWith("1") ? "Success" : "Error"), "search_result");

        if (result.startsWith("1")) {
            try {
                String data = result.substring(1);
                if (TextUtils.isEmpty(data)) {
                    logWarning("Empty result data", "empty_result");
                    mainHandler.post(this::showNoResults);
                } else {
                    logInfo("Processing search data", "processing_data");
                    parseSearchResults(data);
                }
            } catch (Exception e) {
                logException(e, "parsing_error", "Error parsing search result");
                mainHandler.post(() -> showErrorAndFinish(getString(R.string.error_parsing_server_response)));
            }
        } else {
            String errorMessage = result.length() > 1 ? result.substring(1) : "Unknown error";
            logError("Server error: " + errorMessage, "server_error");

            mainHandler.post(() -> {
                showError(errorMessage);
                setResult(RESULT_CANCELED);
                finishActivityDelayed();
            });
        }
    }

    private void parseSearchResults(String data) {
        if (TextUtils.isEmpty(data)) {
            mainHandler.post(this::showNoResults);
            return;
        }

        List<ListResidModel> parsedResults = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(data, "£");
        int itemCount = tokenizer.countTokens();

        logInfo("Found " + itemCount + " results", "results_count");
        crashlytics.setCustomKey("resultCount", itemCount);

        if (itemCount == 0) {
            mainHandler.post(this::showNoResults);
            return;
        }

        try {
            while (tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken();
                if (TextUtils.isEmpty(token)) {
                    continue;
                }

                ListResidModel model = parseResidenceItem(token);
                if (model != null) {
                    parsedResults.add(model);
                }
            }

            searchResults.clear();
            searchResults.addAll(parsedResults);

            mainHandler.post(() -> {
                if (searchResults.isEmpty()) {
                    showNoResults();
                } else {
                    resultAdapter.notifyDataSetChanged();
                    stopLoading();
                }
            });
        } catch (Exception e) {
            logException(e, "parsing_error", "Error parsing search results");
            mainHandler.post(() -> showErrorAndFinish(getString(R.string.error_processing_search_results)));
        }
    }

    private ListResidModel parseResidenceItem(String token) {
        try {
            StringTokenizer item = new StringTokenizer(token, "§");
            if (item.countTokens() < 10) {
                logWarning("Insufficient tokens in item data: " + item.countTokens(), "insufficient_tokens");
                return null;
            }

            ListResidModel fiche = new ListResidModel();

            fiche.setAgc(item.nextToken());
            fiche.setGrp(item.nextToken());
            fiche.setId(Integer.parseInt(item.nextToken()));
            fiche.setVisited(item.nextToken().equals("1"));
            fiche.setRef(item.nextToken());
            fiche.setName(item.nextToken());
            fiche.setEntry(item.nextToken());
            fiche.setAdr(item.nextToken());

            String cp = item.hasMoreTokens() ? item.nextToken() : "";
            String city = item.hasMoreTokens() ? item.nextToken() : "";
            fiche.setCity(cp, city);

            if (item.hasMoreTokens()) {
                fiche.setLast(item.nextToken());
            }

            return fiche;
        } catch (NumberFormatException e) {
            logException(e, "numeric_conversion_error", "Error parsing numeric field");
            return null;
        } catch (Exception e) {
            logException(e, "item_parsing_error", "Error parsing residence item");
            return null;
        }
    }

    private void showNoResults() {
        logInfo("No results found", "no_results");
        ToastManager.showShort("Aucun résultat");
        stopLoading();
        finishActivityDelayed();
    }

    private void showError(String message) {
        logError("Error: " + message, "displayed_error");
        ToastManager.showError(message);
        stopLoading();
    }

    private void showErrorAndFinish(String message) {
        showError(message);
        finishActivityDelayed();
    }

    private void finishActivity() {
        logInfo("Finishing activity", "finish_activity");
        setResult(RESULT_CANCELED);
        finish();
    }

    private void finishActivityDelayed() {
        mainHandler.postDelayed(this::finish, FINISH_DELAY_MS);
    }

    private void startLoading() {
        isLoading.set(true);
        mainHandler.post(() -> {
            binding.searchActivityWaitImg.setVisibility(View.VISIBLE);
            binding.searchActivityScroll.setVisibility(View.GONE);
        });
    }

    private void stopLoading() {
        isLoading.set(false);
        mainHandler.post(() -> {
            binding.searchActivityWaitImg.setVisibility(View.GONE);
            binding.searchActivityScroll.setVisibility(View.VISIBLE);
        });
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

//********* INNER CLASSES

    private class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {
        private final List<ListResidModel> residenceList;

        public SearchResultAdapter(List<ListResidModel> residenceList) {
            this.residenceList = residenceList;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.resid_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ListResidModel model = residenceList.get(position);
            holder.bind(model);
        }

        @Override
        public int getItemCount() {
            return residenceList.size();
        }

        private class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            private final TextView textViewRef;
            private final TextView textViewName;
            private final TextView textViewEntry;
            private final TextView textViewAdr;
            private final TextView textViewCity;
            private final TextView textViewLast;
            private ListResidModel currentModel;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                textViewRef = itemView.findViewById(R.id.fiche_search_resid_ref);
                textViewName = itemView.findViewById(R.id.fiche_search_resid_name);
                textViewEntry = itemView.findViewById(R.id.fiche_search_resid_entry);
                textViewAdr = itemView.findViewById(R.id.fiche_search_resid_adr);
                textViewCity = itemView.findViewById(R.id.fiche_search_resid_city);
                textViewLast = itemView.findViewById(R.id.fiche_search_resid_last);

                itemView.setOnClickListener(this);
            }

            public void bind(ListResidModel model) {
                this.currentModel = model;

                textViewRef.setText(model.getRef());
                textViewName.setText(model.getName());
                textViewEntry.setText(model.getEntry());
                textViewAdr.setText(model.getAdr());
                textViewCity.setText(model.getCity());
                textViewLast.setText(model.getLast());

                // Set background based on visited state
                if (model.isVisited()) {
                    itemView.setBackground(AppCompatResources.getDrawable(
                            SearchActivity.this, R.drawable.button_enabled));
                } else {
                    itemView.setBackground(AppCompatResources.getDrawable(
                            SearchActivity.this, R.drawable.button_standard));
                }
            }

            @Override
            public void onClick(View v) {
                if (isLoading.get() || currentModel == null) {
                    return;
                }

                try {
                    logInfo("Residence selected: " + currentModel.getName() +
                            " (ID: " + currentModel.getId() + ")", "residence_selected");

                    crashlytics.setCustomKey("selectedResidenceId", currentModel.getId());
                    crashlytics.setCustomKey("selectedResidenceRef", currentModel.getRef());

                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(SELECT_ACTIVITY_RESULT, SELECT_SEARCH_ACTIVITY_REQUEST);
                    resultIntent.putExtra(SELECT_SEARCH_ACTIVITY_RESULT_AGC, currentModel.getAgc());
                    resultIntent.putExtra(SELECT_SEARCH_ACTIVITY_RESULT_GRP, currentModel.getGrp());
                    resultIntent.putExtra(SELECT_SEARCH_ACTIVITY_RESULT_RSD, Integer.toString(currentModel.getId()));

                    setResult(RESULT_OK, resultIntent);
                    finish();
                } catch (Exception e) {
                    logException(e, "selection_error", "Error selecting residence");
                    ToastManager.showError(getString(R.string.erreur_lors_de_la_s_lection));
                }
            }
        }
    }

}
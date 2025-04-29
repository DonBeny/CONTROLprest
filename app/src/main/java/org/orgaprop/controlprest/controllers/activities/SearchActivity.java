package org.orgaprop.controlprest.controllers.activities;

import static org.orgaprop.controlprest.controllers.activities.SelectActivity.SELECT_ACTIVITY_RESULT;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.orgaprop.controlprest.BuildConfig;
import org.orgaprop.controlprest.R;
import org.orgaprop.controlprest.databinding.ActivitySearchBinding;
import org.orgaprop.controlprest.models.ListResidModel;
import org.orgaprop.controlprest.services.HttpTask;
import org.orgaprop.controlprest.utils.ToastManager;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.StringTokenizer;
import java.util.concurrent.CompletableFuture;

public class SearchActivity extends AppCompatActivity {

    private static final String TAG = "SearchActivity";
    private boolean isLoading = false;
    private FirebaseCrashlytics crashlytics;
    private FirebaseAnalytics analytics;

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

        try {
            crashlytics = FirebaseCrashlytics.getInstance();
            crashlytics.setCustomKey("deviceModel", Build.MODEL);
            crashlytics.setCustomKey("deviceManufacturer", Build.MANUFACTURER);
            crashlytics.setCustomKey("appVersion", BuildConfig.VERSION_NAME);
            crashlytics.log("SearchActivity démarrée");

            analytics = FirebaseAnalytics.getInstance(this);

            Bundle screenViewParams = new Bundle();
            screenViewParams.putString(FirebaseAnalytics.Param.SCREEN_NAME, "SearchActivity");
            screenViewParams.putString(FirebaseAnalytics.Param.SCREEN_CLASS, "SearchActivity");
            analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, screenViewParams);

            binding = ActivitySearchBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            Intent intent = getIntent();
            if (intent == null) {
                crashlytics.log("Intent reçu est null");
                Log.e(TAG, "onCreate: Intent is null");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "null_intent");
                errorParams.putString("class", "SearchActivity");
                analytics.logEvent("onCreate_intent", errorParams);

                showError(getString(R.string.invalid_search_request));
                return;
            }

            String searchValue = intent.getStringExtra(SELECT_SEARCH_ACTIVITY_STR);
            if (TextUtils.isEmpty(searchValue)) {
                crashlytics.log("La valeur de recherche est vide ou nulle");
                Log.e(TAG, "onCreate: Search value is empty or null");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "empty_search_value");
                errorParams.putString("class", "SearchActivity");
                analytics.logEvent("onCreate_search_value", errorParams);

                showError(getString(R.string.search_term_is_empty));
                return;
            }

            crashlytics.setCustomKey("searchValue", searchValue);
            crashlytics.log("Recherche de: " + searchValue);

            startLoading();
            performSearch(searchValue);
        } catch (Exception e) {
            if (crashlytics != null) {
                crashlytics.recordException(e);
            } else {
                FirebaseCrashlytics.getInstance().recordException(e);
            }
            Log.e(TAG, "Erreur lors de l'initialisation de SearchActivity", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "init_error");
            errorParams.putString("class", "SearchActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("onCreate_init_error", errorParams);

            showError(getString(R.string.une_erreur_est_survenue_lors_de_l_initialisation));
            finish();
        }
    }

//********* SURCHARGES

    @Override
    protected void onDestroy() {
        try {
            crashlytics.log("onDestroy called");
            // Clean up resources if needed
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur dans onDestroy", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "destroy_error");
            errorParams.putString("class", "SearchActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("onDestroy_error", errorParams);
        } finally {
            super.onDestroy();
        }
    }

//********* PUBLIC FUNCTIONS

    public void searchActivityActions(View v) {
        try {
            crashlytics.log("searchActivityActions called");

            if (v == null || v.getTag() == null) {
                crashlytics.log("searchActivityActions: View or tag is null");
                Log.e(TAG, "searchActivityActions: View or tag is null");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "null_view_or_tag");
                errorParams.putString("class", "SearchActivity");
                analytics.logEvent("searchActivityActions_null_view_or_tag", errorParams);

                return;
            }

            String viewTag = v.getTag().toString();
            crashlytics.setCustomKey("viewTag", viewTag);
            crashlytics.log("Action: " + viewTag);

            if (viewTag.equals("cancel")) {
                crashlytics.log("Action d'annulation");
                Log.d(TAG, "Action d'annulation");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "cancel_action");
                errorParams.putString("class", "SearchActivity");
                analytics.logEvent("searchActivityActions_cancel", errorParams);

                finishActivity();
            } else {
                crashlytics.log("searchActivityActions: Unknown tag: " + viewTag);
                Log.w(TAG, "searchActivityActions: Unknown tag: " + viewTag);

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "unknown_tag");
                errorParams.putString("class", "SearchActivity");
                analytics.logEvent("searchActivityActions_unknown_tag", errorParams);

                showError("Unknown tag: " + viewTag);
            }
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur lors du traitement de l'action", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "action_error");
            errorParams.putString("class", "SearchActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("searchActivityActions_app_error", errorParams);

            showError(getString(R.string.erreur_lors_du_traitement_de_l_action));
        }
    }

//********* PRIVATE FUNCTIONS

    private void performSearch(String searchValue) {
        try {
            crashlytics.log("performSearch called");

            String strSearch = URLEncoder.encode(searchValue, "UTF-8");
            if (TextUtils.isEmpty(LoginActivity.idMbr)) {
                crashlytics.log("Member ID is null or empty");
                Log.e(TAG, "performSearch: Member ID is null or empty");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "null_member_id");
                errorParams.putString("class", "SearchActivity");
                analytics.logEvent("performSearch_null_member_id", errorParams);

                showError(getString(R.string.login_information_is_missing));
                return;
            }

            String stringPost = "mbr=" + LoginActivity.idMbr + "&val=" + strSearch;
            crashlytics.setCustomKey("stringPost", stringPost);
            crashlytics.log("Paramètres de recherche préparés");

            HttpTask task = new HttpTask(SearchActivity.this);
            crashlytics.log("Exécution de la requête HTTP");

            CompletableFuture<String> futureResult = task.executeHttpTask(
                    HttpTask.HTTP_TASK_ACT_SEARCH,
                    "rsd",
                    "",
                    stringPost
            );

            futureResult.thenAccept(result -> {
                try {
                    if (result == null) {
                        crashlytics.log("Server returned null response");
                        Log.e(TAG, "performSearch: Server returned null response");

                        Bundle errorParams = new Bundle();
                        errorParams.putString("error_type", "null_result");
                        errorParams.putString("class", "SearchActivity");
                        analytics.logEvent("performSearch_null_result", errorParams);

                        runOnUiThread(() -> {
                            showError(getString(R.string.server_returned_null_response));
                        });
                        return;
                    }

                    crashlytics.log("Réponse reçue: " + (result.startsWith("1") ? "Succès" : "Échec"));

                    if (result.startsWith("1")) {
                        try {
                            String data = result.substring(1);
                            if (TextUtils.isEmpty(data)) {
                                crashlytics.log("Données vides reçues du serveur");
                                Log.e(TAG, "performSearch: Empty result");

                                Bundle errorParams = new Bundle();
                                errorParams.putString("error_type", "empty_result");
                                errorParams.putString("class", "SearchActivity");
                                analytics.logEvent("performSearch_empty_result", errorParams);

                                runOnUiThread(this::showNoResults);
                            } else {
                                crashlytics.log("Traitement des données de recherche");
                                makeView(data);
                            }
                        } catch (IndexOutOfBoundsException e) {
                            crashlytics.recordException(e);
                            crashlytics.log("Erreur lors de l'analyse du résultat");
                            Log.e(TAG, "performSearch: Error parsing result", e);

                            Bundle errorParams = new Bundle();
                            errorParams.putString("error_type", "parsing_error");
                            errorParams.putString("class", "SearchActivity");
                            analytics.logEvent("performSearch_parsing_error", errorParams);

                            runOnUiThread(() -> showError(getString(R.string.error_parsing_server_response)));
                        }
                    } else {
                        String errorMessage = result.length() > 1 ? result.substring(1) : "Unknown error";
                        crashlytics.log("Erreur serveur: " + errorMessage);
                        Log.e(TAG, "performSearch: Server returned error: " + errorMessage);

                        Bundle errorParams = new Bundle();
                        errorParams.putString("error_type", "server_error");
                        errorParams.putString("class", "SearchActivity");
                        errorParams.putString("error_message", errorMessage);
                        analytics.logEvent("performSearch_server_error", errorParams);

                        runOnUiThread(() -> {
                            showError(errorMessage);
                            setResult(RESULT_CANCELED);
                            new Handler(Looper.getMainLooper()).postDelayed(this::finish, 3000);
                        });
                    }
                } catch (Exception e) {
                    crashlytics.recordException(e);
                    Log.e(TAG, "performSearch: Exception during result processing", e);

                    Bundle errorParams = new Bundle();
                    errorParams.putString("error_type", "result_processing_error");
                    errorParams.putString("class", "SearchActivity");
                    errorParams.putString("error_message", e.getMessage());
                    analytics.logEvent("performSearch_result_processing_error", errorParams);

                    runOnUiThread(() -> {
                        showError(getString(R.string.erreur_lors_du_traitement_de_la_r_ponse));
                        setResult(RESULT_CANCELED);
                        new Handler(Looper.getMainLooper()).postDelayed(this::finish, 3000);
                    });
                }
            }).exceptionally(ex -> {
                crashlytics.recordException(ex);
                crashlytics.log("Exception dans la requête HTTP: " + ex.getMessage());
                Log.e(TAG, "performSearch: Exception occurred", ex);

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "http_exception");
                errorParams.putString("class", "SearchActivity");
                errorParams.putString("error_message", ex.getMessage());
                analytics.logEvent("performSearch_http_exception", errorParams);

                runOnUiThread(() -> {
                    showError(getString(R.string.mess_timeout));
                    setResult(RESULT_CANCELED);
                    new Handler(Looper.getMainLooper()).postDelayed(this::finish, 3000);
                });
                return null;
            });
        } catch (UnsupportedEncodingException e) {
            crashlytics.recordException(e);
            crashlytics.log("Erreur d'encodage");
            Log.e(TAG, "performSearch: Encoding error", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "encoding_error");
            errorParams.putString("class", "SearchActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("performSearch_encoding_error", errorParams);

            showError(getString(R.string.error_encoding_search_term));
        } catch (Exception e) {
            crashlytics.recordException(e);
            crashlytics.log("Erreur inattendue: " + e.getMessage());
            Log.e(TAG, "performSearch: Unexpected error", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "unexpected_error");
            errorParams.putString("class", "SearchActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("performSearch_app_error", errorParams);

            showError(getString(R.string.an_unexpected_error_occurred));
        }
    }

    private void finishActivity() {
        try {
            crashlytics.log("finishActivity called");

            if (isLoading) {
                crashlytics.log("Tentative de fermeture pendant le chargement, ignorée");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "closing_during_loading");
                errorParams.putString("class", "SearchActivity");
                analytics.logEvent("finishActivity_closing_during_loading", errorParams);

                return;
            }

            setResult(RESULT_CANCELED);
            finish();
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur lors de la fermeture de l'activité", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "closing_error");
            errorParams.putString("class", "SearchActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("finishActivity_app_error", errorParams);

            finish();
        }
    }

    private void makeView(String string) {
        try {
            crashlytics.log("makeView called");

            if (TextUtils.isEmpty(string)) {
                crashlytics.log("Chaîne de données vide");
                Log.e(TAG, "makeView: Empty string");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "empty_string");
                errorParams.putString("class", "SearchActivity");
                analytics.logEvent("makeView_empty_string", errorParams);

                runOnUiThread(this::showNoResults);
                return;
            }

            LinearLayout mLayout = binding.searchActivityLyt;

            StringTokenizer tokenizer = new StringTokenizer(string, "£");
            int itemCount = tokenizer.countTokens();
            crashlytics.setCustomKey("itemCount", itemCount);
            crashlytics.log("Nombre d'éléments à traiter: " + itemCount);

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
                            crashlytics.log("Jetons insuffisants dans les données: " + item.countTokens());
                            Log.w(TAG, "makeView: Insufficient tokens in item data");

                            Bundle errorParams = new Bundle();
                            errorParams.putString("error_type", "insufficient_tokens");
                            errorParams.putString("class", "SearchActivity");
                            analytics.logEvent("makeView_insufficient_tokens", errorParams);

                            showError(getString(R.string.insufficient_tokens_in_item_data));
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

                            crashlytics.log("Élément traité: " + fiche.getName());
                        } catch (NumberFormatException e) {
                            crashlytics.recordException(e);
                            crashlytics.log("Erreur de conversion numérique");
                            Log.e(TAG, "makeView: Error parsing numeric field", e);

                            Bundle errorParams = new Bundle();
                            errorParams.putString("error_type", "numeric_conversion_error");
                            errorParams.putString("class", "SearchActivity");
                            errorParams.putString("error_message", e.getMessage());
                            analytics.logEvent("makeView_numeric_conversion_error", errorParams);

                            showError(getString(R.string.error_parsing_numeric_field));
                            continue;
                        } catch (Exception e) {
                            crashlytics.recordException(e);
                            crashlytics.log("Erreur lors de la configuration des données");
                            Log.e(TAG, "makeView: Error setting fiche data", e);

                            Bundle errorParams = new Bundle();
                            errorParams.putString("error_type", "setting_fiche_data_error");
                            errorParams.putString("class", "SearchActivity");
                            errorParams.putString("error_message", e.getMessage());
                            analytics.logEvent("makeView_setting_fiche_data_error", errorParams);

                            showError(getString(R.string.error_setting_fiche_data));
                            continue;
                        }

                        createListItem(mLayout, fiche);
                        processedItems++;
                    } catch (Exception e) {
                        crashlytics.recordException(e);
                        crashlytics.log("Erreur lors du traitement du jeton");
                        Log.e(TAG, "makeView: Error processing token", e);

                        Bundle errorParams = new Bundle();
                        errorParams.putString("error_type", "processing_token_error");
                        errorParams.putString("class", "SearchActivity");
                        errorParams.putString("error_message", e.getMessage());
                        analytics.logEvent("makeView_processing_token_error", errorParams);

                        showError(getString(R.string.error_processing_token));
                    }
                }

                final int finalProcessedItems = processedItems;
                crashlytics.setCustomKey("processedItems", finalProcessedItems);
                crashlytics.log("Éléments traités avec succès: " + finalProcessedItems);

                runOnUiThread(() -> {
                    if (finalProcessedItems == 0) {
                        showNoResults();
                    } else {
                        stopLoading();
                    }
                });
            } else {
                crashlytics.log("Aucun élément à traiter");
                Log.e(TAG, "makeView: No items to process");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "no_items_to_process");
                errorParams.putString("class", "SearchActivity");
                analytics.logEvent("makeView_no_items_to_process", errorParams);

                runOnUiThread(this::showNoResults);
            }
        } catch (Exception e) {
            crashlytics.recordException(e);
            crashlytics.log("Erreur inattendue dans makeView");
            Log.e(TAG, "makeView: Unexpected error", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "unexpected_error");
            errorParams.putString("class", "SearchActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("makeView_app_error", errorParams);

            runOnUiThread(() -> showError(getString(R.string.error_processing_search_results)));
        }
    }

    private void createListItem(LinearLayout layout, ListResidModel fiche) {
        runOnUiThread(() -> {
            try {
                crashlytics.log("createListItem: " + fiche.getName());

                View viewElement = LayoutInflater.from(SearchActivity.this).inflate(R.layout.resid_item, null);
                if (viewElement == null) {
                    crashlytics.log("Échec de l'inflation de la vue");
                    Log.e(TAG, "createListItem: Failed to inflate view");

                    Bundle errorParams = new Bundle();
                    errorParams.putString("error_type", "view_inflation_error");
                    errorParams.putString("class", "SearchActivity");
                    analytics.logEvent("createListItem_view_inflation_error", errorParams);

                    showError(getString(R.string.failed_to_inflate_view));
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
                    try {
                        if (isLoading) return;

                        crashlytics.log("Résidence sélectionnée: " + fiche.getName() + " (ID: " + fiche.getId() + ")");
                        crashlytics.setCustomKey("selectedResidenceId", fiche.getId());
                        crashlytics.setCustomKey("selectedResidenceRef", fiche.getRef());

                        Bundle errorParams = new Bundle();
                        errorParams.putString("class", "SearchActivity");
                        errorParams.putString("residence_name", fiche.getName());
                        errorParams.putString("residence_id", Integer.toString(fiche.getId()));
                        analytics.logEvent("createListItem_selected_residence", errorParams);

                        Intent intent = new Intent();
                        intent.putExtra(SELECT_ACTIVITY_RESULT, SELECT_SEARCH_ACTIVITY_REQUEST);
                        intent.putExtra(SELECT_SEARCH_ACTIVITY_RESULT_AGC, fiche.getAgc());
                        intent.putExtra(SELECT_SEARCH_ACTIVITY_RESULT_GRP, fiche.getGrp());
                        intent.putExtra(SELECT_SEARCH_ACTIVITY_RESULT_RSD, Integer.toString(fiche.getId()));

                        setResult(RESULT_OK, intent);
                        finish();
                    } catch (Exception e) {
                        crashlytics.recordException(e);
                        Log.e(TAG, "Erreur lors de la sélection d'un élément", e);

                        Bundle errorParams = new Bundle();
                        errorParams.putString("error_type", "selection_error");
                        errorParams.putString("class", "SearchActivity");
                        errorParams.putString("error_message", e.getMessage());
                        analytics.logEvent("createListItem_selection_error", errorParams);

                        showError(getString(R.string.erreur_lors_de_la_s_lection));
                    }
                });

                layout.addView(viewElement, layoutParams);
            } catch (Exception e) {
                crashlytics.recordException(e);
                Log.e(TAG, "createListItem: Error creating list item", e);

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "list_item_creation_error");
                errorParams.putString("class", "SearchActivity");
                errorParams.putString("error_message", e.getMessage());
                analytics.logEvent("createListItem_list_item_creation_error", errorParams);

                showError(getString(R.string.error_creating_list_item));
            }
        });
    }

    private void showNoResults() {
        try {
            crashlytics.log("showNoResults called");
            ToastManager.showShort("Aucun résultat");
            stopLoading();
            new Handler(Looper.getMainLooper()).postDelayed(this::finish, 2000);
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur dans showNoResults", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "no_results_error");
            errorParams.putString("class", "SearchActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("showNoResults_app_error", errorParams);

            finish();
        }
    }

    private void showError(String message) {
        try {
            crashlytics.log("showError: " + message);
            Log.e(TAG, "showError: " + message);

            Bundle errorParams = new Bundle();
            errorParams.putString("class", "SearchActivity");
            errorParams.putString("error_message", message);
            analytics.logEvent("showError", errorParams);

            ToastManager.showError(message);
            stopLoading();
            setResult(RESULT_CANCELED);
            new Handler(Looper.getMainLooper()).postDelayed(this::finish, 2000);
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur dans showError", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "error_showing_error");
            errorParams.putString("class", "SearchActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("showError_app_error", errorParams);

            finish();
        }
    }

    private void startLoading() {
        try {
            crashlytics.log("startLoading called");
            isLoading = true;
            runOnUiThread(() -> {
                binding.searchActivityWaitImg.setVisibility(View.VISIBLE);
                binding.searchActivityScroll.setVisibility(View.GONE);
            });
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur dans startLoading", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "start_loading_error");
            errorParams.putString("class", "SearchActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("startLoading_app_error", errorParams);
        }
    }

    private void stopLoading() {
        try {
            crashlytics.log("stopLoading called");
            isLoading = false;
            runOnUiThread(() -> {
                binding.searchActivityWaitImg.setVisibility(View.GONE);
                binding.searchActivityScroll.setVisibility(View.VISIBLE);
            });
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur dans stopLoading", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "stop_loading_error");
            errorParams.putString("class", "SearchActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("stopLoading_app_error", errorParams);
        }
    }

}
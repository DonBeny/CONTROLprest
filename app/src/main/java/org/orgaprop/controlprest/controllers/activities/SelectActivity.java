package org.orgaprop.controlprest.controllers.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.orgaprop.controlprest.BuildConfig;
import org.orgaprop.controlprest.R;
import org.orgaprop.controlprest.databinding.ActivitySelectBinding;
import org.orgaprop.controlprest.models.ListResidModel;
import org.orgaprop.controlprest.services.HttpTask;
import org.orgaprop.controlprest.services.PreferencesManager;
import org.orgaprop.controlprest.utils.ToastManager;

import static org.orgaprop.controlprest.controllers.activities.MakeSelectActivity.MAKE_SELECT_ACTIVITY_REQUEST;
import static org.orgaprop.controlprest.controllers.activities.MakeSelectActivity.MAKE_SELECT_ACTIVITY_RESULT;
import static org.orgaprop.controlprest.controllers.activities.MakeSelectActivity.MAKE_SELECT_ACTIVITY_TYPE;
import static org.orgaprop.controlprest.controllers.activities.SearchActivity.SELECT_SEARCH_ACTIVITY_REQUEST;
import static org.orgaprop.controlprest.utils.ToastManager.showError;


public class SelectActivity extends AppCompatActivity {

    private static final String TAG = "SelectActivity";

//********* PRIVATE VARIABLES

    private PreferencesManager preferencesManager;
    private FirebaseCrashlytics crashlytics;
    private FirebaseAnalytics analytics;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final AtomicBoolean isStarted = new AtomicBoolean(false);
    private final AtomicBoolean canCheck = new AtomicBoolean(true);
    private final AtomicBoolean waitDownload = new AtomicBoolean(false);

    private Integer agc = -1;
    private Integer nbAgcs = 0;
    private Integer grp = -1;
    private Integer nbGrps = 0;
    private Integer rsd = -1;
    private Integer nbRsds = 0;

    private String typeSelect = "";
    private String agcSelected = "";
    private String grpSelected = "";
    private String rsdSelected = "";

//********* SYNCHRONIZED DATA COLLECTIONS

    static final ReentrantReadWriteLock dataLock = new ReentrantReadWriteLock();
    static final List<String> idAgcs = Collections.synchronizedList(new ArrayList<>());
    static final List<String> idGrps = Collections.synchronizedList(new ArrayList<>());
    static final List<String> idRsds = Collections.synchronizedList(new ArrayList<>());

    static final List<String> nameAgcs = Collections.synchronizedList(new ArrayList<>());
    static final List<String> nameGrps = Collections.synchronizedList(new ArrayList<>());
    static final List<ListResidModel> nameRsds = Collections.synchronizedList(new ArrayList<>());
    private static final List<JSONObject> dataGrps = Collections.synchronizedList(new ArrayList<>());

//********* PUBLIC CONSTANTS

    public static final String SELECT_ACTIVITY_EXTRA = "agcs";
    public static final String SELECT_ACTIVITY_RSD = "rsd";

    public static final String SELECT_ACTIVITY_RESULT = "typeSelect";

//********* WIDGETS

    private ActivitySelectBinding binding;
    private ActivityResultLauncher<Intent> makeSelectActivityLauncher;

//********* CONSTRUCTOR

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            // Initialiser Crashlytics
            crashlytics = FirebaseCrashlytics.getInstance();
            crashlytics.setCustomKey("deviceModel", Build.MODEL);
            crashlytics.setCustomKey("deviceManufacturer", Build.MANUFACTURER);
            crashlytics.setCustomKey("appVersion", BuildConfig.VERSION_NAME);
            crashlytics.log("SelectActivity démarrée");

            Log.i(TAG, "SelectActivity démarrée");

            analytics = FirebaseAnalytics.getInstance(this);

            logInfo("SelectActivity started", "activity_lifecycle");

            Bundle screenViewParams = new Bundle();
            screenViewParams.putString(FirebaseAnalytics.Param.SCREEN_NAME, "SelectActivity");
            screenViewParams.putString(FirebaseAnalytics.Param.SCREEN_CLASS, "SelectActivity");
            analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, screenViewParams);

            binding = ActivitySelectBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            EditText mSearchInput = binding.selectActivitySearchInput;

            makeSelectActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> handleActivityResult(result.getResultCode(), result.getData())
            );

            preferencesManager = PreferencesManager.getInstance(this);

            try {
                if (mSearchInput != null) {
                    mSearchInput.setOnEditorActionListener((textView, actionId, keyEvent) -> {
                        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                            startActivitySearch();

                            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

                            if (imm != null) {
                                imm.hideSoftInputFromWindow(textView.getWindowToken(), 0);
                                return true;
                            }
                        }
                        return false;
                    });
                    mSearchInput.setOnFocusChangeListener((v, hasFocus) -> {
                        final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                        }
                    });
                }

                logInfo("Initializing data", "init_data");

                // Clear existing data before loading
                clearAllData();

                // Initialize UI and load data
                chargAgcs();

                final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                }
            } catch (Exception e) {
                logException(e, "init_error", "Error initializing UI components");

                showError(getString(R.string.error_initializing) + e.getMessage());
            }
        } catch (Exception e) {
            if (crashlytics != null) {
                crashlytics.recordException(e);
            } else {
                FirebaseCrashlytics.getInstance().recordException(e);
            }
            Log.e(TAG, "Erreur fatale dans onCreate", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "fatal_error");
            errorParams.putString("class", "SelectActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("onCreate_app_error", errorParams);

            showError(getString(R.string.une_erreur_est_survenue_lors_de_l_initialisation));
        }
    }

//********* SURCHARGE

    @Override
    protected void onResume() {
        super.onResume();

        try {
            logInfo("onResume called", "activity_lifecycle");

            if (canCheck.get()) {
                isStarted.set(false);
            }

            canCheck.set(true);
            showWait(false);
        } catch (Exception e) {
            logException(e, "resume_error", "Error in onResume");
            showError(getString(R.string.erreur_lors_du_d_marrage_de_l_application));
        }
    }

    @Override
    protected void onDestroy() {
        try {
            logInfo("onDestroy called", "activity_lifecycle");

            // Clean up resources
            clearAllData();

            super.onDestroy();
        } catch (Exception e) {
            logException(e, "destroy_error", "Error in onDestroy");
            showError(getString(R.string.error_cleaning_up) + e.getMessage());
        }
    }

//********* PUBLIC FUNCTIONS

    public void selectActivityActions(View v) {
        try {
            logInfo("selectActivityActions called", "user_action");

            if (v == null || v.getTag() == null) {
                logError("View or view tag is null", "null_view_or_tag");

                showError(getString(R.string.error_processing_action));

                return;
            }

            String viewTag = v.getTag().toString();

            crashlytics.setCustomKey("viewTag", viewTag);
            logInfo("Action: " + viewTag, "user_action_type");

            switch (viewTag) {
                case "search": startActivitySearch(); break;
                case "go": startActivityStartCtrl(); break;
                case "off": deconnectMbr(); break;
                case "cancel": finishActivity(); break;
                case "agc":
                case "grp":
                case "rsd":
                    typeSelect = viewTag;
                    startActivitySelect();
                    break;
                default:
                    logWarning("Unknown view tag: " + viewTag, "unknown_view_tag");

                    showError(getString(R.string.error_processing_action));

                    break;
            }
        } catch (Exception e) {
            logException(e, "action_error", "Error in selectActivityActions");

            showError(getString(R.string.error_processing_action));
        }
    }

//********* PRIVATE FUNCTIONS

    /**
     * Clear all data collections safely
     */
    private void clearAllData() {
        dataLock.writeLock().lock();
        try {
            idAgcs.clear();
            idGrps.clear();
            idRsds.clear();
            nameAgcs.clear();
            nameGrps.clear();
            nameRsds.clear();
            dataGrps.clear();
            logInfo("All data collections cleared", "data_clear");
        } finally {
            dataLock.writeLock().unlock();
        }
    }

    /**
     * Handle activity result with safety checks
     */
    private void handleActivityResult(int resultCode, Intent data) {
        try {
            logInfo("handleActivityResult: " + resultCode, "activity_result");

            TextView mSpinnerAgc = binding.selectActivityAgcSpinner;
            TextView mSpinnerGrp = binding.selectActivityGrpSpinner;
            TextView mSpinnerRsd = binding.selectActivityRsdSpinner;

            if (resultCode != RESULT_OK || data == null) {
                logWarning("Result not OK or data is null", "invalid_result");
                return;
            }

            String typeResult = data.getStringExtra(SELECT_ACTIVITY_RESULT);
            if (typeResult == null) {
                logWarning("typeResult is null", "null_type_result");
                return;
            }

            crashlytics.setCustomKey("typeResult", typeResult);

            if (Objects.equals(typeResult, SELECT_SEARCH_ACTIVITY_REQUEST)) {
                handleSearchActivityResult(data, mSpinnerAgc, mSpinnerGrp);
            } else if (Objects.equals(typeResult, MAKE_SELECT_ACTIVITY_REQUEST)) {
                handleMakeSelectActivityResult(data, mSpinnerAgc, mSpinnerGrp, mSpinnerRsd);
            }
        } catch (Exception e) {
            logException(e, "result_error", "Error in handleActivityResult");

            showError("processing result: " + e.getMessage());
        }
    }

    /**
     * Handle search activity result
     */
    private void handleSearchActivityResult(Intent data, TextView mSpinnerAgc, TextView mSpinnerGrp) {
        boolean needsGroupUpdate = false;

        logInfo("Handling search activity result", "search_result");

        String newAgcSelected = data.getStringExtra(SearchActivity.SELECT_SEARCH_ACTIVITY_RESULT_AGC);
        String newGrpSelected = data.getStringExtra(SearchActivity.SELECT_SEARCH_ACTIVITY_RESULT_GRP);
        String newRsdSelected = data.getStringExtra(SearchActivity.SELECT_SEARCH_ACTIVITY_RESULT_RSD);

        // Log and track selected data
        logSearchSelection(newAgcSelected, newGrpSelected, newRsdSelected);

        dataLock.readLock().lock();
        try {
            // Process agency selection
            if (newAgcSelected != null && !newAgcSelected.isEmpty()) {
                int newAgcIndex = getIndexInList(idAgcs, newAgcSelected);

                if (isValidIndex(newAgcIndex, nameAgcs)) {
                    if (agc != newAgcIndex) {
                        logInfo("New agency selected, clearing groups", "selection_change");
                        needsGroupUpdate = true;
                    }

                    agc = newAgcIndex;
                    agcSelected = newAgcSelected;

                    runOnUiThread(() -> {
                        if (mSpinnerAgc != null && isValidIndex(agc, nameAgcs)) {
                            mSpinnerAgc.setText(nameAgcs.get(agc));
                        }
                    });

                    // Process group selection
                    if (newGrpSelected != null && !newGrpSelected.isEmpty()) {
                        int newGrp = getIndexInList(idGrps, newGrpSelected);

                        if (needsGroupUpdate) {
                            // We need to load groups first
                            chargGrps();
                        } else if (grp != newGrp) {
                            // Group has changed, we need to update residences
                            logInfo("New group selected, updating residences", "selection_change");
                            if (isValidIndex(newGrp, nameGrps)) {
                                grp = newGrp;
                                grpSelected = newGrpSelected;

                                runOnUiThread(() -> {
                                    if (mSpinnerGrp != null && isValidIndex(grp, nameGrps)) {
                                        mSpinnerGrp.setText(nameGrps.get(grp));
                                    }
                                });

                                makeRsds();
                            }
                        }
                    }
                } else {
                    logInvalidIndex("agency", newAgcIndex);
                }
            }
        } finally {
            dataLock.readLock().unlock();
        }
    }

    /**
     * Handle make select activity result
     */
    private void handleMakeSelectActivityResult(Intent data, TextView mSpinnerAgc, TextView mSpinnerGrp, TextView mSpinnerRsd) {
        int selectType = data.getIntExtra(MAKE_SELECT_ACTIVITY_TYPE, 0);
        crashlytics.setCustomKey("selectType", selectType);
        logInfo("Handling make select result, type: " + selectType, "make_select_result");

        switch (selectType) {
            case MakeSelectActivity.MAKE_SELECT_ACTIVITY_AGC:
                handleAgencySelection(data, mSpinnerAgc);
                break;
            case MakeSelectActivity.MAKE_SELECT_ACTIVITY_GRP:
                handleGroupSelection(data, mSpinnerGrp);
                break;
            case MakeSelectActivity.MAKE_SELECT_ACTIVITY_RSD:
                handleResidenceSelection(data, mSpinnerRsd);
                break;
        }
    }

    /**
     * Handle agency selection from MakeSelectActivity
     */
    private void handleAgencySelection(Intent data, TextView mSpinnerAgc) {
        agcSelected = data.getStringExtra(MAKE_SELECT_ACTIVITY_RESULT);
        crashlytics.setCustomKey("agcSelected", agcSelected != null ? agcSelected : "null");

        dataLock.readLock().lock();
        try {
            if (agcSelected != null && !agcSelected.isEmpty()) {
                int newAgc = getIndexInList(idAgcs, agcSelected);

                if (isValidIndex(newAgc, nameAgcs)) {
                    agc = newAgc;

                    logInfo("New agency selected: " + nameAgcs.get(agc), "agency_selected");

                    // Reset group and residence data
                    clearGroupAndResidenceData();

                    runOnUiThread(() -> {
                        if (mSpinnerAgc != null && isValidIndex(agc, nameAgcs)) {
                            mSpinnerAgc.setText(nameAgcs.get(agc));
                        }
                    });

                    chargGrps();
                } else {
                    logInvalidIndex("agency", newAgc);
                }
            }
        } finally {
            dataLock.readLock().unlock();
        }
    }

    /**
     * Handle group selection from MakeSelectActivity
     */
    private void handleGroupSelection(Intent data, TextView mSpinnerGrp) {
        grpSelected = data.getStringExtra(MAKE_SELECT_ACTIVITY_RESULT);
        crashlytics.setCustomKey("grpSelected", grpSelected != null ? grpSelected : "null");

        dataLock.readLock().lock();
        try {
            if (grpSelected != null && !grpSelected.isEmpty()) {
                int newGrp = getIndexInList(idGrps, grpSelected);

                if (isValidIndex(newGrp, nameGrps) && !grp.equals(newGrp)) {
                    grp = newGrp;

                    logInfo("New group selected: " + nameGrps.get(grp), "group_selected");

                    // Reset residence data
                    clearResidenceData();

                    runOnUiThread(() -> {
                        if (mSpinnerGrp != null && isValidIndex(grp, nameGrps)) {
                            mSpinnerGrp.setText(nameGrps.get(grp));
                        }
                    });

                    makeRsds();
                } else if (!isValidIndex(newGrp, nameGrps)) {
                    logInvalidIndex("group", newGrp);
                }
            }
        } finally {
            dataLock.readLock().unlock();
        }
    }

    /**
     * Handle residence selection from MakeSelectActivity
     */
    private void handleResidenceSelection(Intent data, TextView mSpinnerRsd) {
        rsdSelected = data.getStringExtra(MAKE_SELECT_ACTIVITY_RESULT);
        crashlytics.setCustomKey("rsdSelected", rsdSelected != null ? rsdSelected : "null");

        dataLock.readLock().lock();
        try {
            if (rsdSelected != null && !rsdSelected.isEmpty()) {
                int newRsd = getIndexInList(idRsds, rsdSelected);

                if (isValidIndex(newRsd, nameRsds)) {
                    rsd = newRsd;

                    logInfo("New residence selected: " + nameRsds.get(rsd).getName(), "residence_selected");
                    crashlytics.setCustomKey("selectedResidenceRef", nameRsds.get(rsd).getRef());

                    final String residenceName = nameRsds.get(rsd).getName() + " " + nameRsds.get(rsd).getAdr();
                    runOnUiThread(() -> {
                        if (mSpinnerRsd != null) {
                            mSpinnerRsd.setText(residenceName);
                        }
                    });
                } else {
                    logInvalidIndex("residence", newRsd);
                }
            }
        } finally {
            dataLock.readLock().unlock();
        }
    }

    /**
     * Clear group and residence data
     */
    private void clearGroupAndResidenceData() {
        dataLock.writeLock().lock();
        try {
            idGrps.clear();
            nameGrps.clear();
            dataGrps.clear();
            grp = -1;
            grpSelected = "";
            nbGrps = 0;

            clearResidenceData();
            logInfo("Group and residence data cleared", "data_clear");
        } finally {
            dataLock.writeLock().unlock();
        }
    }

    /**
     * Clear residence data
     */
    private void clearResidenceData() {
        dataLock.writeLock().lock();
        try {
            idRsds.clear();
            nameRsds.clear();
            rsd = -1;
            rsdSelected = "";
            nbRsds = 0;
            logInfo("Residence data cleared", "data_clear");
        } finally {
            dataLock.writeLock().unlock();
        }
    }

    /**
     * Start search activity with safety checks
     */
    private void startActivitySearch() {
        try {
            logInfo("startActivitySearch called", "activity_navigation");

            EditText mSearchInput = binding.selectActivitySearchInput;

            if (mSearchInput == null) {
                logError("Search input is null", "null_search_input");

                ToastManager.showShort(getString(R.string.search_input_is_null));

                return;
            }

            String searchText = mSearchInput.getText().toString().trim();
            crashlytics.setCustomKey("searchText", searchText);

            if (!searchText.isEmpty() && !waitDownload.get()) {
                logInfo("Starting search for: " + searchText, "search_start");

                Intent intent = new Intent(SelectActivity.this, SearchActivity.class);

                canCheck.set(false);
                showWait(true);

                intent.putExtra(SearchActivity.SELECT_SEARCH_ACTIVITY_STR, searchText);

                makeSelectActivityLauncher.launch(intent);
            } else if (searchText.isEmpty()) {
                logWarning("Empty search term", "empty_search");
                showError(getString(R.string.please_enter_a_search_term));
            }
        } catch (Exception e) {
            logException(e, "search_error", "Error in startActivitySearch");

            showError(getString(R.string.starting_search) + e.getMessage());
            showWait(false);
        }
    }

    /**
     * Start selection activity with safety checks
     */
    private void startActivitySelect() {
        try {
            logInfo("startActivitySelect called: " + typeSelect, "activity_navigation");

            switch (typeSelect) {
                case "agc":
                    if (!waitDownload.get() && nbAgcs > 0) {
                        logInfo("Starting agency selection", "select_agency");
                        showWait(true);
                        canCheck.set(false);

                        Intent intent = new Intent(SelectActivity.this, MakeSelectActivity.class);
                        intent.putExtra(MAKE_SELECT_ACTIVITY_TYPE, MakeSelectActivity.MAKE_SELECT_ACTIVITY_AGC);

                        makeSelectActivityLauncher.launch(intent);
                    } else {
                        logWarning("No agencies available", "no_agencies");
                        showError(getString(R.string.no_agencies_available));
                    }
                    break;
                case "grp":
                    if (!waitDownload.get() && agc >= 0 && nbGrps > 0) {
                        logInfo("Starting group selection", "select_group");
                        showWait(true);
                        canCheck.set(false);

                        Intent intent = new Intent(SelectActivity.this, MakeSelectActivity.class);
                        intent.putExtra(MAKE_SELECT_ACTIVITY_TYPE, MakeSelectActivity.MAKE_SELECT_ACTIVITY_GRP);

                        makeSelectActivityLauncher.launch(intent);
                    } else {
                        if (agc < 0) {
                            showAgencyRequiredError();
                        } else if (nbGrps <= 0) {
                            showNoGroupsError();
                        }
                    }
                    break;
                case "rsd":
                    if (!waitDownload.get() && grp >= 0 && nbRsds > 0) {
                        logInfo("Starting residence selection", "select_residence");
                        showWait(true);
                        canCheck.set(false);

                        Intent intent = new Intent(SelectActivity.this, MakeSelectActivity.class);
                        intent.putExtra(MAKE_SELECT_ACTIVITY_TYPE, MakeSelectActivity.MAKE_SELECT_ACTIVITY_RSD);

                        makeSelectActivityLauncher.launch(intent);
                    } else {
                        if (agc < 0) {
                            showAgencyRequiredError();
                        } else if (grp < 0) {
                            showGroupRequiredError();
                        } else if (nbRsds <= 0) {
                            showNoResidencesError();
                        }
                    }
                    break;
                default:
                    logWarning("Unknown selection type: " + typeSelect, "unknown_type");

                    showError(getString(R.string.unknown_type_select) + typeSelect);
                    break;
            }
        } catch (Exception e) {
            logException(e, "select_error", "Error in startActivitySelect");

            showError(getString(R.string.starting_select) + e.getMessage());
            showWait(false);
        }
    }

    private void startActivityStartCtrl() {
        try {
            logInfo("startActivityStartCtrl called", "activity_navigation");

            if (!waitDownload.get() && !isStarted.get() && rsd >= 0) {
                isStarted.set(true);
                canCheck.set(false);

                preferencesManager.setAgency(agcSelected);
                preferencesManager.setGroup(grpSelected);
                preferencesManager.setResidence(rsdSelected);

                logInfo("Starting control for residence: " + rsdSelected, "control_start");
                crashlytics.setCustomKey("controlResidenceId", rsdSelected);

                Intent intent = new Intent(SelectActivity.this, NfsActivity.class);
                intent.putExtra(SELECT_ACTIVITY_RSD, rsdSelected);

                startActivity(intent);
            } else if (rsd < 0) {
                logWarning("No residence selected", "no_residence_selected");
                showError(getString(R.string.please_select_a_residence_first));
            }
        } catch (Exception e) {
            logException(e, "start_control_error", "Error starting control activity");
            showError(getString(R.string.starting_control) + e.getMessage());
        }
    }

    private void deconnectMbr() {
        try {
            logInfo("deconnectMbr called", "user_action");
            showWait(true);

            HttpTask task = new HttpTask(SelectActivity.this);

            logInfo("Sending disconnect request", "network_request");

            CompletableFuture<String> futureResult = task.executeHttpTask(
                    HttpTask.HTTP_TASK_ACT_CONEX,
                    HttpTask.HTTP_TASK_CBL_NO,
                    "",
                    "mbr=" + LoginActivity.idMbr
            );

            futureResult.thenAccept(result -> {
                try {
                    if (result.startsWith("1")) {
                        logInfo("Disconnection successful", "network_success");

                        resetPreferences();
                        finishLoginAndSelf();
                    } else {
                        showWait(false);

                        String errorMessage = result.length() > 1 ?
                                result.substring(1) : "Unknown error";

                        logError("Disconnection error: " + errorMessage, "network_error");

                        runOnUiThread(() -> showError(errorMessage));

                        // Even if there was an error, try to finish activities
                        resetPreferences();
                        finishLoginAndSelf();
                    }
                } catch (Exception e) {
                    logException(e, "disconnect_result_error", "Error processing disconnection result");
                    showWait(false);
                    resetPreferences();
                    finish();
                }
            }).exceptionally(ex -> {
                String errorMessage = ex instanceof CancellationException ?
                        "Request cancelled" :
                        "Network connection error";

                logException((Exception) ex, "network_error", "Network error during disconnection: " + errorMessage);
                showWait(false);

                runOnUiThread(() -> showError(errorMessage));

                resetPreferences();
                finishLoginAndSelf();
                return null;
            });
        } catch (Exception e) {
            logException(e, "disconnect_error", "Error in deconnectMbr");
            showError(getString(R.string.disconnection_error) + e.getMessage());
            showWait(false);
            resetPreferences();
            finish();
        }
    }

    /**
     * Reset all user preferences
     */
    private void resetPreferences() {
        try {
            preferencesManager.setMbrId("new");
            preferencesManager.setAgency("");
            preferencesManager.setGroup("");
            preferencesManager.setResidence("");
            logInfo("User preferences reset", "preferences_reset");
        } catch (Exception e) {
            logException(e, "preferences_error", "Error resetting preferences");
        }
    }

    /**
     * Finish login activity and self safely
     */
    private void finishLoginAndSelf() {
        try {
            LoginActivity loginActivity = LoginActivity.getInstance();
            if (loginActivity != null) {
                loginActivity.finish();
                logInfo("LoginActivity finished", "activity_finish");
            }
        } catch (Exception e) {
            logException(e, "login_finish_error", "Error finishing LoginActivity");

            runOnUiThread(() ->
                    showError(getString(R.string.finishing_loginactivity) + e.getMessage())
            );
        } finally {
            finish();
        }
    }

    /**
     * Finish activity safely
     */
    private void finishActivity() {
        try {
            logInfo("finishActivity called", "activity_lifecycle");
            runOnUiThread(this::finish);
        } catch (Exception e) {
            logException(e, "finish_error", "Error finishing activity");
            finish();
        }
    }

    /**
     * Load agencies data with proper error handling
     */
    private void chargAgcs() {
        try {
            logInfo("chargAgcs called", "data_loading");

            TextView mSpinnerAgc = binding.selectActivityAgcSpinner;
            TextView mSpinnerGrp = binding.selectActivityGrpSpinner;
            TextView mSpinnerRsd = binding.selectActivityRsdSpinner;

            Intent intent = getIntent();
            String agenciesData = intent.getStringExtra(SELECT_ACTIVITY_EXTRA);

            if (agenciesData == null || agenciesData.isEmpty()) {
                logError("Agencies data is null or empty", "empty_data");

                showError(getString(R.string.no_agencies_data_available));
                return;
            }

            StringTokenizer tokenizer = new StringTokenizer(agenciesData, "§");
            String defaultMessage = getString(R.string.select_an_agency);

            waitDownload.set(true);

            dataLock.writeLock().lock();
            try {
                idAgcs.clear();
                nameAgcs.clear();
                nbAgcs = 0;

                runOnUiThread(() -> {
                    mSpinnerGrp.setText(defaultMessage);
                    mSpinnerRsd.setText(defaultMessage);
                });

                if (tokenizer.countTokens() > 0) {
                    processAgenciesData(tokenizer);
                }

                crashlytics.setCustomKey("nbAgcs", nbAgcs);
                logInfo("Agencies loaded: " + nbAgcs, "data_loaded");

                String displayMessage = defaultMessage;
                if (!agcSelected.isEmpty()) {
                    agc = getIndexInList(idAgcs, agcSelected);
                    if (isValidIndex(agc, nameAgcs)) {
                        displayMessage = nameAgcs.get(agc);
                        chargGrps();
                    }
                }

                final String finalMessage = displayMessage;
                runOnUiThread(() -> mSpinnerAgc.setText(finalMessage));
            } finally {
                dataLock.writeLock().unlock();
                waitDownload.set(false);
            }
        } catch (Exception e) {
            logException(e, "agencies_load_error", "Error loading agencies");
            showError(getString(R.string.loading_agencies) + e.getMessage());
            waitDownload.set(false);
        }
    }

    /**
     * Process agencies data from tokenizer
     */
    private void processAgenciesData(StringTokenizer tokenizer) {
        int count = tokenizer.countTokens();
        logInfo("Processing " + count + " agencies", "data_processing");

        while (tokenizer.hasMoreTokens()) {
            String item = tokenizer.nextToken();
            int separatorIndex = item.indexOf("£");

            if (separatorIndex > 0 && separatorIndex < item.length() - 1) {
                String id = item.substring(0, separatorIndex);
                String name = item.substring(separatorIndex + 1);

                idAgcs.add(id);
                nameAgcs.add(name);
                nbAgcs++;

                // If this is the only agency, select it automatically
                if (count == 1) {
                    logInfo("Auto-selecting the only available agency: " + name, "auto_selection");
                    agcSelected = id;
                    agc = 0; // First index
                }
            } else {
                logWarning("Invalid agency format: " + item, "invalid_format");
                showError(getString(R.string.invalid_agency_format) + item);
            }
        }
    }

    /**
     * Load groups data with proper error handling and background processing
     */
    private void chargGrps() {
        try {
            logInfo("chargGrps called", "data_loading");

            if (agc < 0 || !isValidIndex(agc, idAgcs)) {
                logError("Invalid agency index: " + agc, "invalid_index");
                showError(getString(R.string.invalid_agency_index) + ": " + agc);
                return;
            }

            TextView mSpinnerGrp = binding.selectActivityGrpSpinner;
            TextView mSpinnerRsd = binding.selectActivityRsdSpinner;
            pl.droidsonroids.gif.GifImageView mWaitGrpImg = binding.selectActivityWaitGrpImg;

            waitDownload.set(true);

            dataLock.writeLock().lock();
            try {
                idGrps.clear();
                nameGrps.clear();
                dataGrps.clear();
                nbGrps = 0;
            } finally {
                dataLock.writeLock().unlock();
            }

            runOnUiThread(() -> mWaitGrpImg.setVisibility(View.VISIBLE));

            HttpTask task = new HttpTask(SelectActivity.this);

            logInfo("Sending HTTP request for groups of agency: " + agcSelected, "network_request");

            CompletableFuture<String> futureResult = task.executeHttpTask(
                    HttpTask.HTTP_TASK_ACT_LIST,
                    HttpTask.HTTP_TASK_CBL_GRPS,
                    "val=" + agcSelected,
                    "mbr=" + LoginActivity.idMbr
            );

            futureResult.thenAccept(result -> {
                try {
                    String displayMessage = getString(R.string.select_a_group);

                    if (result.startsWith("1")) {
                        logInfo("Groups data received", "network_success");

                        try {
                            String jsonData = result.substring(1);
                            if (jsonData.isEmpty()) {
                                logWarning("Empty JSON data received", "empty_json");
                                runOnUiThread(() -> showError(getString(R.string.no_group_data_received)));
                                return;
                            }

                            JSONObject obj = new JSONObject(jsonData);

                            if (obj.has("grps")) {
                                JSONObject grpsObj = obj.getJSONObject("grps");
                                Iterator<String> keys_grps = grpsObj.keys();

                                dataLock.writeLock().lock();
                                try {
                                    while (keys_grps.hasNext()) {
                                        String kg = keys_grps.next();
                                        JSONObject obj_grp = grpsObj.getJSONObject(kg);

                                        idGrps.add(obj_grp.getString("id"));
                                        nameGrps.add(obj_grp.getString("name"));
                                        dataGrps.add(obj_grp.getJSONObject("rsds"));

                                        nbGrps++;
                                    }
                                } finally {
                                    dataLock.writeLock().unlock();
                                }

                                crashlytics.setCustomKey("nbGrps", nbGrps);
                                logInfo("Groups loaded: " + nbGrps, "data_loaded");
                            } else {
                                logWarning("No 'grps' key in JSON data", "missing_key");
                                runOnUiThread(() -> showError(getString(R.string.erreur_lors_du_traitement_de_la_r_ponse)));
                                return;
                            }

                            dataLock.readLock().lock();
                            try {
                                if (!grpSelected.isEmpty() && idGrps.contains(grpSelected)) {
                                    grp = getIndexInList(idGrps, grpSelected);

                                    if (isValidIndex(grp, nameGrps)) {
                                        logInfo("Group selected: " + grp, "group_selected");
                                        displayMessage = nameGrps.get(grp);
                                        makeRsds();
                                    } else {
                                        logWarning("Group not selected: " + grp, "invalid_group");
                                        final String finalDisplayMessage = displayMessage;
                                        runOnUiThread(() -> mSpinnerRsd.setText(finalDisplayMessage));
                                    }
                                } else {
                                    logInfo("No group selected", "no_selection");
                                    final String finalDisplayMessage = displayMessage;
                                    runOnUiThread(() -> mSpinnerRsd.setText(finalDisplayMessage));
                                }
                            } finally {
                                dataLock.readLock().unlock();
                            }
                        } catch (JSONException e) {
                            logException(e, "json_parsing_error", "Error parsing JSON data");
                            runOnUiThread(() -> showError(getString(R.string.error_parsing_groups_data)));
                        }
                    } else {
                        final String errorMessage = result.length() > 1 ? result.substring(1) : "Unknown error";

                        logError("Error loading groups: " + errorMessage, "server_error");
                        runOnUiThread(() -> showError(errorMessage));
                    }

                    final String finalMessage = displayMessage;
                    runOnUiThread(() -> mSpinnerGrp.setText(finalMessage));
                } finally {
                    runOnUiThread(() -> {
                        mWaitGrpImg.setVisibility(View.INVISIBLE);
                        waitDownload.set(false);
                    });
                }
            }).exceptionally(ex -> {
                logException((Exception) ex, "network_error", "Exception during groups loading");

                runOnUiThread(() -> {
                    mWaitGrpImg.setVisibility(View.INVISIBLE);
                    waitDownload.set(false);
                    showError(getString(R.string.loading_groups));
                });

                return null;
            });
        } catch (Exception e) {
            logException(e, "groups_load_error", "Error loading groups");

            runOnUiThread(() -> {
                if (binding != null) {
                    binding.selectActivityWaitGrpImg.setVisibility(View.INVISIBLE);
                }
                waitDownload.set(false);
                showError(getString(R.string.loading_groups));
            });
        }
    }

    /**
     * Load residences data with proper error handling
     */
    private void makeRsds() {
        try {
            logInfo("makeRsds called", "data_loading");

            TextView mSpinnerRsd = binding.selectActivityRsdSpinner;

            dataLock.readLock().lock();
            try {
                if (grp < 0 || !isValidIndex(grp, dataGrps)) {
                    logError("Invalid group index: " + grp, "invalid_index");
                    runOnUiThread(() -> ToastManager.showError(getString(R.string.invalid_group_index) + grp));
                    return;
                }

                JSONObject grpData = dataGrps.get(grp);
                if (grpData == null) {
                    logError("Group data is null", "null_data");
                    runOnUiThread(() -> ToastManager.showError(getString(R.string.no_data_available_for_this_group)));
                    return;
                }
            } finally {
                dataLock.readLock().unlock();
            }

            dataLock.writeLock().lock();
            try {
                idRsds.clear();
                nameRsds.clear();
                nbRsds = 0;

                JSONObject grpData = dataGrps.get(grp);
                Iterator<String> keys_rsds = grpData.keys();

                while (keys_rsds.hasNext()) {
                    try {
                        String kr = keys_rsds.next();
                        if (!grpData.has(kr)) continue;

                        JSONObject obj = grpData.getJSONObject(kr);

                        // Safely extract data with proper validation
                        String id = obj.optString("id", "");
                        if (id.isEmpty()) {
                            logWarning("Residence with empty ID found, skipping", "empty_id");
                            continue;
                        }

                        ListResidModel fiche = new ListResidModel();

                        try {
                            // Use optString to safely get JSON values with defaults
                            fiche.setId(Integer.parseInt(id));
                            fiche.setAgc(agcSelected);
                            fiche.setGrp(grpSelected);
                            fiche.setRef(obj.optString("ref", ""));
                            fiche.setName(obj.optString("name", ""));
                            fiche.setEntry(obj.optString("entry", ""));
                            fiche.setAdr(obj.optString("adr", ""));
                            fiche.setCity(obj.optString("cp", ""), obj.optString("city", ""));

                            // Set last control date if available
                            if (obj.has("last")) {
                                fiche.setLast(obj.optString("last", ""));
                            }

                            idRsds.add(String.valueOf(fiche.getId()));
                            nameRsds.add(fiche);
                            nbRsds++;
                        } catch (NumberFormatException e) {
                            logException(e, "parsing_error", "Error parsing residence ID");
                        }
                    } catch (JSONException e) {
                        logException(e, "json_error", "Error processing residence data");
                    }
                }

                crashlytics.setCustomKey("nbRsds", nbRsds);
                logInfo("Residences loaded: " + nbRsds, "data_loaded");

                // Set default message
                String displayMessage = getString(R.string.lbl_select_entry);

                // Check if we have a previously selected residence
                if (!rsdSelected.isEmpty()) {
                    int selectedIndex = getIndexInList(idRsds, rsdSelected);
                    if (isValidIndex(selectedIndex, nameRsds)) {
                        rsd = selectedIndex;
                        ListResidModel selectedResid = nameRsds.get(rsd);
                        displayMessage = selectedResid.getName();
                        if (!selectedResid.getAdr().isEmpty()) {
                            displayMessage += " " + selectedResid.getAdr();
                        }
                    } else {
                        logWarning("Previously selected residence not found: " + rsdSelected, "not_found");
                        runOnUiThread(() -> ToastManager.showError(getString(R.string.selected_residence_no_longer_available)));
                        rsdSelected = "";
                        rsd = -1;
                    }
                }

                // Update UI on the main thread with a final reference to displayMessage
                final String finalMessage = displayMessage;
                runOnUiThread(() -> {
                    if (mSpinnerRsd != null) {
                        mSpinnerRsd.setText(finalMessage);
                    }
                });

                if (nbRsds == 0) {
                    runOnUiThread(() -> ToastManager.showError(getString(R.string.no_residences_available_for_this_group)));
                }
            } finally {
                dataLock.writeLock().unlock();
            }
        } catch (Exception e) {
            logException(e, "residences_load_error", "Error loading residences");
            runOnUiThread(() -> ToastManager.showError(getString(R.string.loading_residences)));
        }
    }

    /**
     * Helper method to get index in a list safely
     * @param list The list to search in
     * @param value The value to search for
     * @return The index of the value in the list, or -1 if not found
     */
    private <T> int getIndexInList(List<T> list, T value) {
        if (list == null || value == null) return -1;

        dataLock.readLock().lock();
        try {
            return list.indexOf(value);
        } finally {
            dataLock.readLock().unlock();
        }
    }

    /**
     * Helper method to check if an index is valid for a list
     * @param index The index to check
     * @param list The list to check against
     * @return true if the index is valid, false otherwise
     */
    private <T> boolean isValidIndex(int index, List<T> list) {
        if (list == null) return false;

        dataLock.readLock().lock();
        try {
            return index >= 0 && index < list.size();
        } finally {
            dataLock.readLock().unlock();
        }
    }

    /**
     * Shows or hides the wait indicator safely
     * @param show True to show the indicator, false to hide it
     */
    private void showWait(boolean show) {
        try {
            logInfo("showWait: " + show, "ui_update");

            pl.droidsonroids.gif.GifImageView mImgWait = binding.selectActivityWaitImg;
            if (mImgWait != null) {
                runOnUiThread(() -> mImgWait.setVisibility(show ? View.VISIBLE : View.INVISIBLE));
            }
        } catch (Exception e) {
            logException(e, "wait_indicator_error", "Error showing/hiding wait indicator");
        }
    }

    /**
     * Show error for missing agency selection
     */
    private void showAgencyRequiredError() {
        logWarning("No agency selected", "no_agency_selected");
        showError(getString(R.string.please_select_an_agency_first));
    }

    /**
     * Show error for missing group selection
     */
    private void showGroupRequiredError() {
        logWarning("No group selected", "no_group_selected");
        showError(getString(R.string.please_select_a_group_first));
    }

    /**
     * Show error for no residences available
     */
    private void showNoResidencesError() {
        logWarning("No residences available", "no_residences");
        showError(getString(R.string.no_residences_available));
    }

    /**
     * Show error for no groups available
     */
    private void showNoGroupsError() {
        logWarning("No groups available", "no_groups");
        showError(getString(R.string.no_groups_available));
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

    /**
     * Log search selection details
     */
    private void logSearchSelection(String newAgcSelected, String newGrpSelected, String newRsdSelected) {
        crashlytics.setCustomKey("agcSelected", newAgcSelected != null ? newAgcSelected : "null");
        crashlytics.setCustomKey("grpSelected", newGrpSelected != null ? newGrpSelected : "null");
        crashlytics.setCustomKey("rsdSelected", newRsdSelected != null ? newRsdSelected : "null");

        logInfo("Search selection - Agency: " + newAgcSelected +
                        ", Group: " + newGrpSelected +
                        ", Residence: " + newRsdSelected,
                "search_selection");
    }

    /**
     * Log invalid index error
     */
    private void logInvalidIndex(String type, int index) {
        logWarning("Invalid " + type + " index: " + index, "invalid_index");
        showError(getString(R.string.invalid_agency_selection));
    }

}

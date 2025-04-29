package org.orgaprop.controlprest.controllers.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
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
import java.util.Iterator;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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


public class SelectActivity extends AppCompatActivity {

    private static final String TAG = "SelectActivity";

//********* PRIVATE VARIABLES

    private PreferencesManager preferencesManager;
    private FirebaseCrashlytics crashlytics;
    private FirebaseAnalytics analytics;

    private boolean isStarted;
    private boolean canCheck;
    private boolean waitDownload;

    private Integer agc;
    private Integer nbAgcs = 0;
    private Integer grp;
    private Integer nbGrps = 0;
    private ArrayList<JSONObject> dataGrps = new ArrayList<>();
    private Integer rsd;
    private Integer nbRsds = 0;

    private String typeSelect;
    private String agcSelected;
    private String grpSelected;
    private String rsdSelected;

//********* PUBLIC VARIABLES

    public static final ArrayList<String> idAgcs = new ArrayList<>();
    public static final ArrayList<String> idGrps = new ArrayList<>();
    public static final ArrayList<String> idRsds = new ArrayList<>();

    public static final ArrayList<String> nameAgcs = new ArrayList<>();
    public static final ArrayList<String> nameGrps = new ArrayList<>();
    public static final ArrayList<ListResidModel> nameRsds = new ArrayList<>();

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

            analytics = FirebaseAnalytics.getInstance(this);

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

            isStarted = false;
            canCheck = true;
            waitDownload = false;

            agc = -1;
            grp = -1;
            rsd = -1;

            agcSelected = "";
            grpSelected = "";
            rsdSelected = "";

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

                crashlytics.log("Initialisation des données");

                // Initialize UI and load data
                chargAgcs();

                final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                }
            } catch (Exception e) {
                crashlytics.recordException(e);
                Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "init_error");
                errorParams.putString("class", "SelectActivity");
                errorParams.putString("error_message", e.getMessage());
                analytics.logEvent("onCreate_init_error", errorParams);

                ToastManager.showError(getString(R.string.error_initializing) + e.getMessage());
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

            ToastManager.showError(getString(R.string.une_erreur_est_survenue_lors_de_l_initialisation));
        }
    }

//********* SURCHARGE

    @Override
    protected void onResume() {
        super.onResume();

        try {
            crashlytics.log("onResume called");

            if (canCheck) {
                isStarted = false;
            }

            canCheck = true;
            showWait(false);
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur dans onResume", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "resume_error");
            errorParams.putString("class", "SelectActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("onResume_app_error", errorParams);

            ToastManager.showError(getString(R.string.erreur_lors_du_d_marrage_de_l_application));
        }
    }

    @Override
    protected void onDestroy() {
        try {
            crashlytics.log("onDestroy called");

            // Clean up resources
            idAgcs.clear();
            idGrps.clear();
            idRsds.clear();
            nameAgcs.clear();
            nameGrps.clear();
            nameRsds.clear();
            dataGrps.clear();

            super.onDestroy();
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Error in onDestroy: " + e.getMessage(), e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "destroy_error");
            errorParams.putString("class", "SelectActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("onDestroy_app_error", errorParams);

            ToastManager.showError(getString(R.string.error_cleaning_up) + e.getMessage());
        }
    }

//********* PUBLIC FUNCTIONS

    public void selectActivityActions(View v) {
        try {
            crashlytics.log("selectActivityActions called");

            if (v == null || v.getTag() == null) {
                crashlytics.log("View or view tag is null");
                Log.e(TAG, "View or view tag is null");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "null_view_or_tag");
                errorParams.putString("class", "SelectActivity");
                analytics.logEvent("selectActivityActions_null_view_or_tag", errorParams);

                ToastManager.showError(getString(R.string.error_processing_action));
                return;
            }

            String viewTag = v.getTag().toString();

            crashlytics.setCustomKey("viewTag", viewTag);
            crashlytics.log("Action: " + viewTag);

            Bundle errorParams = new Bundle();
            errorParams.putString("class", "SelectActivity");
            errorParams.putString("tag_value", viewTag);
            analytics.logEvent("selectActivityActions", errorParams);

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
                    crashlytics.log("Unknown view tag: " + viewTag);
                    Log.w(TAG, "Unknown view tag: " + viewTag);

                    errorParams = new Bundle();
                    errorParams.putString("error_type", "unknown_view_tag");
                    errorParams.putString("class", "SelectActivity");
                    analytics.logEvent("selectActivityActions_unknown_view_tag", errorParams);

                    ToastManager.showError(getString(R.string.error_processing_action));
                    break;
            }
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Error in selectActivityActions: " + e.getMessage(), e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "action_error");
            errorParams.putString("class", "SelectActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("selectActivityActions_app_error", errorParams);

            ToastManager.showError(getString(R.string.error_processing_action));
        }
    }

//********* PRIVATE FUNCTIONS

    private void handleActivityResult(int resultCode, Intent data) {
        try {
            crashlytics.log("handleActivityResult: " + resultCode);

            TextView mSpinnerAgc = binding.selectActivityAgcSpinner;
            TextView mSpinnerGrp = binding.selectActivityGrpSpinner;
            TextView mSpinnerRsd = binding.selectActivityRsdSpinner;

            if (resultCode == RESULT_OK && data != null) {
                String typeResult = data.getStringExtra(SELECT_ACTIVITY_RESULT);
                crashlytics.setCustomKey("typeResult", typeResult != null ? typeResult : "null");

                if (Objects.equals(typeResult, SELECT_SEARCH_ACTIVITY_REQUEST)) {
                    boolean b_grps = false;
                    boolean b_rsds = false;

                    crashlytics.log("Retour de recherche");
                    Log.d(TAG, "handleActivityResult => search return");

                    agcSelected = data.getStringExtra(SearchActivity.SELECT_SEARCH_ACTIVITY_RESULT_AGC);
                    grpSelected = data.getStringExtra(SearchActivity.SELECT_SEARCH_ACTIVITY_RESULT_GRP);
                    rsdSelected = data.getStringExtra(SearchActivity.SELECT_SEARCH_ACTIVITY_RESULT_RSD);

                    crashlytics.setCustomKey("agcSelected", agcSelected != null ? agcSelected : "null");
                    crashlytics.setCustomKey("grpSelected", grpSelected != null ? grpSelected : "null");
                    crashlytics.setCustomKey("rsdSelected", rsdSelected != null ? rsdSelected : "null");

                    Log.d(TAG, "handleActivityResult::agcSelected => " + agcSelected);
                    Log.d(TAG, "handleActivityResult::grpSelected => " + grpSelected);
                    Log.d(TAG, "handleActivityResult::rsdSelected => " + rsdSelected);

                    Bundle selectParams = new Bundle();
                    selectParams.putString("class", "SelectActivity");
                    selectParams.putString("agcSelected", agcSelected != null ? agcSelected : "null");
                    selectParams.putString("grpSelected", grpSelected != null ? grpSelected : "null");
                    selectParams.putString("rsdSelected", rsdSelected != null ? rsdSelected : "null");
                    analytics.logEvent("handleActivityResult_search_return", selectParams);

                    // Check for null or invalid indices to prevent ArrayIndexOutOfBoundsException
                    if (agcSelected != null && !agcSelected.isEmpty()) {
                        int newAgc = idAgcs.indexOf(agcSelected);

                        if (newAgc >= 0 && newAgc < nameAgcs.size()) {
                            if (!agc.equals(newAgc)) {
                                crashlytics.log("Nouvelle agence sélectionnée, nettoyage des groupes");

                                idGrps.clear();
                                nameGrps.clear();
                                dataGrps.clear();
                                grp = -1;
                                nbGrps = 0;
                                b_grps = true;
                            }

                            agc = newAgc;
                            mSpinnerAgc.setText(nameAgcs.get(agc));

                            if (grpSelected != null && !grpSelected.isEmpty()) {
                                int newGrp = idGrps.indexOf(grpSelected);

                                if (b_grps) {
                                    chargGrps();
                                } else if (!grp.equals(newGrp)) {
                                    crashlytics.log("Nouveau groupe sélectionné, nettoyage des résidences");
                                    idRsds.clear();
                                    nameRsds.clear();
                                    rsd = -1;
                                    nbRsds = 0;
                                    b_rsds = true;

                                    if (newGrp >= 0 && newGrp < nameGrps.size()) {
                                        grp = newGrp;
                                        mSpinnerGrp.setText(nameGrps.get(grp));
                                        makeRsds();
                                    }
                                }
                            }
                        } else {
                            crashlytics.log("Index d'agence invalide: " + newAgc);
                            Log.e(TAG, "Invalid agency index: " + newAgc);

                            Bundle errorParams = new Bundle();
                            errorParams.putString("error_type", "invalid_agency_index");
                            errorParams.putString("class", "SelectActivity");
                            analytics.logEvent("handleActivityResult_agency", errorParams);

                            ToastManager.showError(getString(R.string.invalid_agency_selection));
                        }
                    }
                } else if (Objects.equals(typeResult, MAKE_SELECT_ACTIVITY_REQUEST)) {
                    int selectType = data.getIntExtra(MAKE_SELECT_ACTIVITY_TYPE, 0);
                    crashlytics.setCustomKey("selectType", selectType);
                    crashlytics.log("Retour de sélection, type: " + selectType);

                    if (selectType == MakeSelectActivity.MAKE_SELECT_ACTIVITY_AGC) {
                        agcSelected = data.getStringExtra(MAKE_SELECT_ACTIVITY_RESULT);
                        crashlytics.setCustomKey("agcSelected", agcSelected != null ? agcSelected : "null");

                        if (agcSelected != null && !agcSelected.isEmpty()) {
                            int newAgc = idAgcs.indexOf(agcSelected);

                            if (newAgc >= 0 && newAgc < nameAgcs.size()) {
                                agc = newAgc;

                                crashlytics.log("Nouvelle agence sélectionnée: " + nameAgcs.get(agc));

                                idGrps.clear();
                                nameGrps.clear();
                                dataGrps.clear();
                                grp = -1;
                                grpSelected = "";
                                nbGrps = 0;

                                idRsds.clear();
                                nameRsds.clear();
                                rsd = -1;
                                rsdSelected = "";
                                nbRsds = 0;

                                runOnUiThread(() -> mSpinnerAgc.setText(nameAgcs.get(agc)));
                                chargGrps();
                            } else {
                                crashlytics.log("Index d'agence invalide: " + newAgc);
                                Log.e(TAG, "Invalid agency index: " + newAgc);

                                Bundle errorParams = new Bundle();
                                errorParams.putString("error_type", "invalid_agency_index");
                                errorParams.putString("class", "SelectActivity");
                                analytics.logEvent("handleActivityResult_agency", errorParams);

                                ToastManager.showError(getString(R.string.invalid_agency_selection));
                            }
                        }
                    } else if (selectType == MakeSelectActivity.MAKE_SELECT_ACTIVITY_GRP) {
                        grpSelected = data.getStringExtra(MAKE_SELECT_ACTIVITY_RESULT);
                        crashlytics.setCustomKey("grpSelected", grpSelected != null ? grpSelected : "null");

                        if (grpSelected != null && !grpSelected.isEmpty()) {
                            int newGrp = idGrps.indexOf(grpSelected);

                            if (newGrp >= 0 && newGrp < nameGrps.size() && !grp.equals(newGrp)) {
                                grp = newGrp;

                                crashlytics.log("Nouveau groupe sélectionné: " + nameGrps.get(grp));

                                idRsds.clear();
                                nameRsds.clear();

                                rsd = -1;
                                rsdSelected = "";
                                nbRsds = 0;

                                runOnUiThread(() -> mSpinnerGrp.setText(nameGrps.get(grp)));
                                makeRsds();
                            } else if (newGrp < 0 || newGrp >= nameGrps.size()) {
                                crashlytics.log("Index de groupe invalide: " + newGrp);
                                Log.e(TAG, "Invalid group index: " + newGrp);

                                Bundle errorParams = new Bundle();
                                errorParams.putString("error_type", "invalid_group_index");
                                errorParams.putString("class", "SelectActivity");
                                analytics.logEvent("handleActivityResult_group", errorParams);

                                ToastManager.showError(getString(R.string.invalid_group_selection));
                            }
                        }
                    } else if (selectType == MakeSelectActivity.MAKE_SELECT_ACTIVITY_RSD) {
                        rsdSelected = data.getStringExtra(MAKE_SELECT_ACTIVITY_RESULT);
                        crashlytics.setCustomKey("rsdSelected", rsdSelected != null ? rsdSelected : "null");

                        if (rsdSelected != null && !rsdSelected.isEmpty()) {
                            int newRsd = idRsds.indexOf(rsdSelected);

                            if (newRsd >= 0 && newRsd < nameRsds.size()) {
                                rsd = newRsd;

                                crashlytics.log("Nouvelle résidence sélectionnée: " + nameRsds.get(rsd).getName());
                                crashlytics.setCustomKey("selectedResidenceRef", nameRsds.get(rsd).getRef());

                                final String residenceName = nameRsds.get(rsd).getName() + " " + nameRsds.get(rsd).getAdress();
                                runOnUiThread(() -> mSpinnerRsd.setText(residenceName));
                            } else {
                                crashlytics.log("Index de résidence invalide: " + newRsd);
                                Log.e(TAG, "Invalid residence index: " + newRsd);

                                Bundle errorParams = new Bundle();
                                errorParams.putString("error_type", "invalid_residence_index");
                                errorParams.putString("class", "SelectActivity");
                                analytics.logEvent("handleActivityResult_residence", errorParams);

                                ToastManager.showError(getString(R.string.invalid_residence_selection));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Error in handleActivityResult: " + e.getMessage(), e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "result_error");
            errorParams.putString("class", "SelectActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("handleActivityResult_app_error", errorParams);

            ToastManager.showError("processing result: " + e.getMessage());
        }
    }

    private void startActivitySearch() {
        try {
            crashlytics.log("startActivitySearch called");

            EditText mSearchInput = binding.selectActivitySearchInput;

            if (mSearchInput == null) {
                crashlytics.log("Search input is null");
                Log.e(TAG, "Search input is null");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "null_search_input");
                errorParams.putString("class", "SelectActivity");
                analytics.logEvent("startActivitySearch_null_search_input", errorParams);

                ToastManager.showShort(getString(R.string.search_input_is_null));
                return;
            }

            String searchText = mSearchInput.getText().toString().trim();
            crashlytics.setCustomKey("searchText", searchText);

            if (!searchText.isEmpty() && !waitDownload) {
                crashlytics.log("Lancement de la recherche: " + searchText);

                Intent intent = new Intent(SelectActivity.this, SearchActivity.class);

                canCheck = false;
                showWait(true);

                intent.putExtra(SearchActivity.SELECT_SEARCH_ACTIVITY_STR, searchText);

                makeSelectActivityLauncher.launch(intent);
            } else if (searchText.isEmpty()) {
                crashlytics.log("Terme de recherche vide");
                Log.e(TAG, "Please enter a search term");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "empty_search_term");
                errorParams.putString("class", "SelectActivity");
                analytics.logEvent("startActivitySearch_empty_search_term", errorParams);

                ToastManager.showError(getString(R.string.please_enter_a_search_term));
            }
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Error in startActivitySearch: " + e.getMessage(), e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "search_error");
            errorParams.putString("class", "SelectActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("startActivitySearch_app_error", errorParams);

            ToastManager.showError(getString(R.string.starting_search) + e.getMessage());
            showWait(false);
        }
    }

    private void startActivitySelect() {
        try {
            crashlytics.log("startActivitySelect called: " + typeSelect);

            switch (typeSelect) {
                case "agc":
                    if (!waitDownload && nbAgcs > 0) {
                        crashlytics.log("Lancement de la sélection d'agence");
                        showWait(true);
                        canCheck = false;

                        Intent intent = new Intent(SelectActivity.this, MakeSelectActivity.class);
                        intent.putExtra(MAKE_SELECT_ACTIVITY_TYPE, MakeSelectActivity.MAKE_SELECT_ACTIVITY_AGC);

                        makeSelectActivityLauncher.launch(intent);
                    } else {
                        crashlytics.log("Aucune agence disponible");
                        Log.e(TAG, "No agencies available");

                        Bundle errorParams = new Bundle();
                        errorParams.putString("error_type", "no_agencies_available");
                        errorParams.putString("class", "SelectActivity");
                        analytics.logEvent("startActivitySelect_no_agencies", errorParams);

                        ToastManager.showError(getString(R.string.no_agencies_available));
                    }
                    break;
                case "grp":
                    if (!waitDownload && agc >= 0 && nbGrps > 0) {
                        crashlytics.log("Lancement de la sélection de groupe");
                        showWait(true);
                        canCheck = false;

                        Intent intent = new Intent(SelectActivity.this, MakeSelectActivity.class);
                        intent.putExtra(MAKE_SELECT_ACTIVITY_TYPE, MakeSelectActivity.MAKE_SELECT_ACTIVITY_GRP);

                        makeSelectActivityLauncher.launch(intent);
                    } else {
                        if (agc < 0) {
                            crashlytics.log("Aucune agence sélectionnée");
                            Log.e(TAG, "Please select an agency first");

                            Bundle errorParams = new Bundle();
                            errorParams.putString("error_type", "no_agency_selected");
                            errorParams.putString("class", "SelectActivity");
                            analytics.logEvent("startActivitySelect_no_agency_selected", errorParams);

                            ToastManager.showError(getString(R.string.please_select_an_agency_first));
                        } else if (nbGrps <= 0) {
                            crashlytics.log("Aucun groupe disponible");
                            Log.e(TAG, "No groups available");

                            Bundle errorParams = new Bundle();
                            errorParams.putString("error_type", "no_groups_available");
                            errorParams.putString("class", "SelectActivity");
                            analytics.logEvent("startActivitySelect_no_groups", errorParams);

                            ToastManager.showError(getString(R.string.no_groups_available));
                        }
                    }
                    break;
                case "rsd":
                    if (!waitDownload && grp >= 0 && nbRsds > 0) {
                        crashlytics.log("Lancement de la sélection de résidence");
                        showWait(true);
                        canCheck = false;

                        Intent intent = new Intent(SelectActivity.this, MakeSelectActivity.class);
                        intent.putExtra(MAKE_SELECT_ACTIVITY_TYPE, MakeSelectActivity.MAKE_SELECT_ACTIVITY_RSD);

                        makeSelectActivityLauncher.launch(intent);
                    } else {
                        if (agc < 0) {
                            crashlytics.log("Aucune agence sélectionnée");
                            Log.e(TAG, "Please select an agency first");

                            Bundle errorParams = new Bundle();
                            errorParams.putString("error_type", "no_agency_selected");
                            errorParams.putString("class", "SelectActivity");
                            analytics.logEvent("startActivitySelect_no_agency_selected", errorParams);

                            ToastManager.showError(getString(R.string.please_select_an_agency_first));
                        } else if (grp < 0) {
                            crashlytics.log("Aucun groupe sélectionné");
                            Log.e(TAG, "Please select a group first");

                            Bundle errorParams = new Bundle();
                            errorParams.putString("error_type", "no_group_selected");
                            errorParams.putString("class", "SelectActivity");
                            analytics.logEvent("startActivitySelect_no_group_selected", errorParams);

                            ToastManager.showError(getString(R.string.please_select_a_group_first));
                        } else if (nbRsds <= 0) {
                            crashlytics.log("Aucune résidence disponible");
                            Log.e(TAG, "No residences available");

                            Bundle errorParams = new Bundle();
                            errorParams.putString("error_type", "no_residences_available");
                            errorParams.putString("class", "SelectActivity");
                            analytics.logEvent("startActivitySelect_no_residences", errorParams);

                            ToastManager.showError(getString(R.string.no_residences_available));
                        }
                    }
                    break;
                default:
                    crashlytics.log("Type de sélection inconnu: " + typeSelect);
                    Log.w(TAG, "Unknown type select: " + typeSelect);

                    Bundle errorParams = new Bundle();
                    errorParams.putString("error_type", "unknown_type_select");
                    errorParams.putString("class", "SelectActivity");
                    analytics.logEvent("startActivitySelect_unknown_type", errorParams);

                    ToastManager.showError(getString(R.string.unknown_type_select) + typeSelect);
                    break;
            }
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Error in startActivitySelect: " + e.getMessage(), e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "select_error");
            errorParams.putString("class", "SelectActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("startActivitySelect_app_error", errorParams);

            ToastManager.showError(getString(R.string.starting_select) + e.getMessage());
            showWait(false);
        }
    }

    private void startActivityStartCtrl() {
        try {
            crashlytics.log("startActivityStartCtrl called");

            if (!waitDownload && !isStarted && rsd >= 0) {
                isStarted = true;
                canCheck = false;

                preferencesManager.setAgency(agcSelected);
                preferencesManager.setGroup(grpSelected);
                preferencesManager.setResidence(rsdSelected);

                crashlytics.log("Démarrage du contrôle, résidence: " + rsdSelected);
                crashlytics.setCustomKey("controlResidenceId", rsdSelected);

                Bundle logParams = new Bundle();
                logParams.putString("agency", agcSelected);
                logParams.putString("group", grpSelected);
                logParams.putString("residence", rsdSelected);
                analytics.logEvent("startActivityStartCtrl_params", logParams);

                Intent intent = new Intent(SelectActivity.this, NfsActivity.class);
                intent.putExtra(SELECT_ACTIVITY_RSD, rsdSelected);

                startActivity(intent);
            } else if (rsd < 0) {
                crashlytics.log("Aucune résidence sélectionnée");
                Log.e(TAG, "Please select a residence first");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "no_residence_selected");
                errorParams.putString("class", "SelectActivity");
                analytics.logEvent("startActivityStartCtrl_no_resid_selected", errorParams);

                ToastManager.showError(getString(R.string.please_select_a_residence_first));
            }
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Error in startActivityStartCtrl: " + e.getMessage(), e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "start_error");
            errorParams.putString("class", "SelectActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("startActivityStartCtrl_app_error", errorParams);

            ToastManager.showError(getString(R.string.starting_control) + e.getMessage());
        }
    }

    private void deconnectMbr() {
        try {
            crashlytics.log("deconnectMbr called");
            showWait(true);

            HttpTask task = new HttpTask(SelectActivity.this);

            crashlytics.log("Envoi de la requête de déconnexion");

            CompletableFuture<String> futureResult = task.executeHttpTask(
                    HttpTask.HTTP_TASK_ACT_CONEX,
                    HttpTask.HTTP_TASK_CBL_NO,
                    "",
                    "mbr=" + LoginActivity.idMbr
            );

            futureResult.thenAccept(result -> {
                try {
                    if (result.startsWith("1")) {
                        crashlytics.log("Déconnexion réussie");

                        preferencesManager.setMbrId("new");
                        preferencesManager.setAgency("");
                        preferencesManager.setGroup("");
                        preferencesManager.setResidence("");

                        try {
                            LoginActivity loginActivity = LoginActivity.getInstance();
                            if (loginActivity != null) {
                                loginActivity.finish();
                            }
                        } catch (Exception e) {
                            crashlytics.recordException(e);
                            Log.e(TAG, "Error finishing LoginActivity: " + e.getMessage(), e);

                            Bundle errorParams = new Bundle();
                            errorParams.putString("error_type", "finishing_login_error");
                            errorParams.putString("class", "SelectActivity");
                            errorParams.putString("error_message", e.getMessage());
                            analytics.logEvent("deconnectMbr_finishing_login_error", errorParams);

                            runOnUiThread(() ->
                                ToastManager.showError(getString(R.string.finishing_loginactivity) + e.getMessage())
                            );
                        }

                        finish();
                    } else {
                        showWait(false);

                        String errorMessage = result.substring(1);

                        crashlytics.log("Erreur lors de la déconnexion: " + errorMessage);
                        Log.e(TAG, "Error in deconnectMbr: " + errorMessage);

                        Bundle errorParams = new Bundle();
                        errorParams.putString("error_type", "deconnection_error");
                        errorParams.putString("class", "SelectActivity");
                        errorParams.putString("error_message", errorMessage);
                        analytics.logEvent("deconnectMbr_error", errorParams);

                        runOnUiThread(() ->
                            ToastManager.showError(errorMessage)
                        );

                        try {
                            LoginActivity loginActivity = LoginActivity.getInstance();
                            if (loginActivity != null) {
                                loginActivity.finish();
                            }
                        } catch (Exception e) {
                            crashlytics.recordException(e);
                            Log.e(TAG, "Error finishing LoginActivity: " + e.getMessage(), e);

                            Bundle errorParams2 = new Bundle();
                            errorParams2.putString("error_type", "finishing_login_error");
                            errorParams2.putString("class", "SelectActivity");
                            errorParams2.putString("error_message", e.getMessage());
                            analytics.logEvent("deconnectMbr_finishing_login_error", errorParams2);

                            runOnUiThread(() ->
                                ToastManager.showError(getString(R.string.finishing_loginactivity)+ e.getMessage())
                            );
                        }

                        finish();
                    }
                } catch (Exception e) {
                    crashlytics.recordException(e);
                    Log.e(TAG, "Error processing deconnection result: " + e.getMessage(), e);

                    Bundle errorParams = new Bundle();
                    errorParams.putString("error_type", "deconnection_result_error");
                    errorParams.putString("class", "SelectActivity");
                    errorParams.putString("error_message", e.getMessage());
                    analytics.logEvent("deconnectMbr_result_error", errorParams);

                    showWait(false);
                    finish();
                }
            }).exceptionally(ex -> {
                crashlytics.recordException(ex);
                showWait(false);
                String errorMessage = ex instanceof CancellationException ?
                        "Request cancelled" :
                        "Network connection error";

                crashlytics.log("Erreur exceptionnelle lors de la déconnexion: " + errorMessage);
                Log.e(TAG, "Error in deconnectMbr: " + ex.getMessage(), ex);

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "deconnection_error");
                errorParams.putString("class", "SelectActivity");
                errorParams.putString("error_message", errorMessage);
                analytics.logEvent("deconnectMbr_error", errorParams);

                runOnUiThread(() -> ToastManager.showError(errorMessage));

                try {
                    LoginActivity loginActivity = LoginActivity.getInstance();
                    if (loginActivity != null) {
                        loginActivity.finish();
                    }
                } catch (Exception e) {
                    crashlytics.recordException(e);
                    Log.e(TAG, "Error finishing LoginActivity: " + e.getMessage(), e);

                    Bundle errorParams2 = new Bundle();
                    errorParams2.putString("error_type", "finishing_login_error");
                    errorParams2.putString("class", "SelectActivity");
                    errorParams2.putString("error_message", e.getMessage());
                    analytics.logEvent("deconnectMbr_finishing_login_error", errorParams2);

                    runOnUiThread(() ->
                        ToastManager.showError(getString(R.string.finishing_loginactivity) + e.getMessage())
                    );
                }

                finish();
                return null;
            });
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Error in deconnectMbr: " + e.getMessage(), e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "deconnection_error");
            errorParams.putString("class", "SelectActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("deconnectMbr_error", errorParams);

            ToastManager.showError(getString(R.string.disconnection_error) + e.getMessage());
            showWait(false);
        }
    }

    private void finishActivity() {
        try {
            crashlytics.log("finishActivity called");
            runOnUiThread(this::finish);
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur lors de la fermeture de l'activité", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "finish_error");
            errorParams.putString("class", "SelectActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("finishActivity_error", errorParams);

            finish();
        }
    }

    private void chargAgcs() {
        try {
            crashlytics.log("chargAgcs called");

            TextView mSpinnerAgc = binding.selectActivityAgcSpinner;
            TextView mSpinnerGrp = binding.selectActivityGrpSpinner;
            TextView mSpinnerRsd = binding.selectActivityRsdSpinner;

            Intent intent = getIntent();
            String agenciesData = intent.getStringExtra(SELECT_ACTIVITY_EXTRA);

            if (agenciesData == null || agenciesData.isEmpty()) {
                crashlytics.log("Agencies data is null or empty");
                Log.e(TAG, "Agencies data is null or empty");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "no_agencies_data");
                errorParams.putString("class", "SelectActivity");
                analytics.logEvent("chargAgcs_no_agencies_data", errorParams);

                ToastManager.showError(getString(R.string.no_agencies_data_available));
                return;
            }

            StringTokenizer tokenizer = new StringTokenizer(agenciesData, "§");
            String defaultMessage = getString(R.string.select_an_agency);

            waitDownload = true;

            idAgcs.clear();
            nameAgcs.clear();
            nbAgcs = 0;

            mSpinnerGrp.setText(defaultMessage);
            mSpinnerRsd.setText(defaultMessage);

            if (tokenizer.countTokens() > 1) {
                crashlytics.log("Plusieurs agences disponibles: " + tokenizer.countTokens());

                while (tokenizer.hasMoreTokens()) {
                    String item = tokenizer.nextToken();
                    int separatorIndex = item.indexOf("£");

                    if (separatorIndex > 0 && separatorIndex < item.length() - 1) {
                        idAgcs.add(item.substring(0, separatorIndex));
                        nameAgcs.add(item.substring(separatorIndex + 1));
                        nbAgcs++;
                    } else {
                        crashlytics.log("Format d'agence invalide: " + item);
                        Log.w(TAG, "Invalid agency format: " + item);

                        Bundle errorParams = new Bundle();
                        errorParams.putString("error_type", "invalid_agency_format");
                        errorParams.putString("class", "SelectActivity");
                        analytics.logEvent("chargAgcs_invalid_agency_format", errorParams);

                        ToastManager.showError(getString(R.string.invalid_agency_format) + item);
                    }
                }
            } else if (tokenizer.countTokens() == 1) {
                crashlytics.log("Une seule agence disponible");

                String item = tokenizer.nextToken();
                int separatorIndex = item.indexOf("£");

                if (separatorIndex > 0 && separatorIndex < item.length() - 1) {
                    idAgcs.add(item.substring(0, separatorIndex));
                    nameAgcs.add(item.substring(separatorIndex + 1));
                    agcSelected = idAgcs.get(0);
                    nbAgcs = 1;
                } else {
                    crashlytics.log("Format d'agence invalide: " + item);
                    Log.w(TAG, "Invalid agency format: " + item);

                    Bundle errorParams = new Bundle();
                    errorParams.putString("error_type", "invalid_agency_format");
                    errorParams.putString("class", "SelectActivity");
                    analytics.logEvent("chargAgcs_invalid_agency_format", errorParams);

                    ToastManager.showError(getString(R.string.invalid_agency_format) + item);
                }
            }

            crashlytics.setCustomKey("nbAgcs", nbAgcs);
            crashlytics.log("Nombre d'agences chargées: " + nbAgcs);

            String displayMessage = defaultMessage;
            if (!agcSelected.isEmpty()) {
                agc = idAgcs.indexOf(agcSelected);
                if (agc >= 0 && agc < nameAgcs.size()) {
                    displayMessage = nameAgcs.get(agc);
                    chargGrps();
                }
            }

            final String finalMessage = displayMessage;
            mSpinnerAgc.setText(finalMessage);

            waitDownload = false;
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Error in chargAgcs: " + e.getMessage(), e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "charg_agcs_error");
            errorParams.putString("class", "SelectActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("chargAgcs_error", errorParams);

            ToastManager.showError(getString(R.string.loading_agencies) + e.getMessage());
            waitDownload = false;
        }
    }

    private void chargGrps() {
        try {
            crashlytics.log("chargGrps called");
            Log.d(TAG, "chargGrps called");

            TextView mSpinnerGrp = binding.selectActivityGrpSpinner;
            TextView mSpinnerRsd = binding.selectActivityRsdSpinner;
            pl.droidsonroids.gif.GifImageView mWaitGrpImg = binding.selectActivityWaitGrpImg;

            waitDownload = true;
            idGrps.clear();
            nameGrps.clear();
            dataGrps.clear();
            nbGrps = 0;

            mWaitGrpImg.setVisibility(View.VISIBLE);

            HttpTask task = new HttpTask(SelectActivity.this);

            crashlytics.log("Requête HTTP pour les groupes de l'agence: " + agcSelected);
            Log.d(TAG, "HTTP request for groups of agency: " + agcSelected);

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
                        Log.d(TAG, "Données JSON reçues: " + result.substring(1));

                        try {
                            String jsonData = result.substring(1);
                            if (jsonData.isEmpty()) {
                                crashlytics.log("Données JSON vides reçues");
                                Log.w(TAG, "Empty JSON data received");

                                Bundle errorParams = new Bundle();
                                errorParams.putString("error_type", "empty_json_data");
                                errorParams.putString("class", "SelectActivity");
                                analytics.logEvent("chargGrps_empty_json_data", errorParams);

                                runOnUiThread(() -> ToastManager.showError(getString(R.string.no_group_data_received)));
                                return;
                            }

                            JSONObject obj = new JSONObject(jsonData);

                            Log.d(TAG, "Données JSON analysées: " + obj);

                            if (obj.has("grps")) {
                                JSONObject grpsObj = obj.getJSONObject("grps");
                                Iterator<String> keys_grps = grpsObj.keys();

                                while (keys_grps.hasNext()) {
                                    String kg = keys_grps.next();
                                    JSONObject obj_grp = grpsObj.getJSONObject(kg);

                                    idGrps.add(obj_grp.getString("id"));
                                    nameGrps.add(obj_grp.getString("name"));
                                    dataGrps.add(obj_grp.getJSONObject("rsds"));

                                    nbGrps++;
                                }

                                crashlytics.setCustomKey("nbGrps", nbGrps);
                                crashlytics.log("Nombre de groupes chargés: " + nbGrps);
                            } else {
                                crashlytics.log("Clé 'grps' absente des données JSON");
                                Log.w(TAG, "No 'grps' key in JSON data");

                                Bundle errorParams = new Bundle();
                                errorParams.putString("error_type", "no_grps_key_in_json_data");
                                errorParams.putString("class", "SelectActivity");
                                analytics.logEvent("chargGrps_no_grps_key_in_json_data", errorParams);

                                runOnUiThread(() -> ToastManager.showError(getString(R.string.erreur_lors_du_traitement_de_la_r_ponse)));
                                return;
                            }

                            Log.d(TAG, "grpSelected: " + grpSelected);

                            if (!grpSelected.isEmpty() && idGrps.contains(grpSelected)) {
                                grp = idGrps.indexOf(grpSelected);

                                Log.d(TAG, "grp: " + grp);

                                if (grp >= 0 && grp < nameGrps.size()) {
                                    Log.d(TAG, "Group selected: " + grp);

                                    displayMessage = nameGrps.get(grp);
                                    makeRsds();
                                } else {
                                    Log.w(TAG, "Group not selected: " + grp);
                                    final String finalDisplayMessage = displayMessage;
                                    runOnUiThread(() -> mSpinnerRsd.setText(finalDisplayMessage));
                                }
                            } else {
                                Log.w(TAG, "Group not selected: " + grp);
                                final String finalDisplayMessage = displayMessage;
                                runOnUiThread(() -> mSpinnerRsd.setText(finalDisplayMessage));
                            }
                        } catch (JSONException e) {
                            crashlytics.recordException(e);
                            crashlytics.log("Erreur d'analyse JSON: " + e.getMessage());
                            Log.e(TAG, "JSON parsing error: " + e.getMessage(), e);

                            Bundle errorParams = new Bundle();
                            errorParams.putString("error_type", "json_parsing_error");
                            errorParams.putString("class", "SelectActivity");
                            errorParams.putString("error_message", e.getMessage());
                            analytics.logEvent("chargGrps_json_parsing_error", errorParams);

                            runOnUiThread(() -> ToastManager.showError(getString(R.string.error_parsing_groups_data)));
                        }
                    } else {
                        final String errorMessage = result.substring(1);

                        crashlytics.log("Erreur lors du chargement des groupes: " + errorMessage);
                        Log.e(TAG, "Error in chargGrps: " + errorMessage);

                        Bundle errorParams = new Bundle();
                        errorParams.putString("error_type", "error_loading_groups");
                        errorParams.putString("class", "SelectActivity");
                        errorParams.putString("error_message", errorMessage);
                        analytics.logEvent("chargGrps_error_loading_groups", errorParams);

                        runOnUiThread(() -> ToastManager.showError(errorMessage));
                    }

                    final String finalMessage = displayMessage;
                    runOnUiThread(() -> mSpinnerGrp.setText(finalMessage));
                } finally {
                    runOnUiThread(() -> {
                        mWaitGrpImg.setVisibility(View.INVISIBLE);
                        waitDownload = false;
                    });
                }
            }).exceptionally(ex -> {
                crashlytics.recordException(ex);
                crashlytics.log("Exception lors du chargement des groupes: " + ex.getMessage());

                Log.e(TAG, "Error in chargGrps: " + (ex instanceof ExecutionException ?
                        Objects.requireNonNull(ex.getCause()).getMessage() : ex.getMessage()), ex);

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "error_loading_groups");
                errorParams.putString("class", "SelectActivity");
                errorParams.putString("error_message", ex.getMessage());
                analytics.logEvent("chargGrps_error_loading_groups", errorParams);

                runOnUiThread(() -> {
                    mWaitGrpImg.setVisibility(View.INVISIBLE);
                    waitDownload = false;
                    ToastManager.showError(getString(R.string.loading_groups));
                });

                return null;
            });
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Error in chargGrps: " + e.getMessage(), e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "error_loading_groups");
            errorParams.putString("class", "SelectActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("chargGrps_error_loading_groups", errorParams);

            runOnUiThread(() -> {
                if (binding != null) {
                    binding.selectActivityWaitGrpImg.setVisibility(View.INVISIBLE);
                }
                waitDownload = false;
                ToastManager.showError(getString(R.string.loading_groups));
            });
        }
    }

    private void makeRsds() {
        try {
            crashlytics.log("makeRsds called");

            TextView mSpinnerRsd = binding.selectActivityRsdSpinner;

            if (grp < 0 || grp >= dataGrps.size()) {
                crashlytics.log("Index de groupe invalide: " + grp);
                Log.e(TAG, "Invalid group index: " + grp);

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "invalid_group_index");
                errorParams.putString("class", "SelectActivity");
                analytics.logEvent("makeRsds_invalid_group_index", errorParams);

                runOnUiThread(() -> ToastManager.showError(getString(R.string.invalid_group_index) + grp));
                return;
            }

            idRsds.clear();
            nameRsds.clear();
            nbRsds = 0;

            JSONObject grpData = dataGrps.get(grp);
            if (grpData == null) {
                crashlytics.log("Données de groupe nulles");
                Log.e(TAG, "Group data is null");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "null_group_data");
                errorParams.putString("class", "SelectActivity");
                analytics.logEvent("makeRsds_null_group_data", errorParams);

                runOnUiThread(() -> ToastManager.showError(getString(R.string.no_data_available_for_this_group)));
                return;
            }

            Iterator<String> keys_rsds;
            try {
                keys_rsds = grpData.keys();
            } catch (Exception e) {
                crashlytics.recordException(e);
                crashlytics.log("Erreur lors de la récupération des clés: " + e.getMessage());
                Log.e(TAG, "Error getting keys from group data: " + e.getMessage(), e);

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "error_getting_keys");
                errorParams.putString("class", "SelectActivity");
                errorParams.putString("error_message", e.getMessage());
                analytics.logEvent("makeRsds_error_getting_keys", errorParams);

                runOnUiThread(() -> ToastManager.showError(getString(R.string.processing_residence_data)));
                return;
            }

            while (keys_rsds.hasNext()) {
                try {
                    String kr = keys_rsds.next();
                    JSONObject obj = grpData.getJSONObject(kr);

                    // Safely extract data with proper validation
                    String id = obj.optString("id", "");
                    if (id.isEmpty()) {
                        crashlytics.log("Résidence avec ID vide trouvée, ignorée");
                        Log.w(TAG, "Residence with empty ID found, skipping");

                        Bundle errorParams = new Bundle();
                        errorParams.putString("error_type", "empty_residence_id");
                        errorParams.putString("class", "SelectActivity");
                        analytics.logEvent("makeRsds_empty_residence_id", errorParams);

                        runOnUiThread(() -> ToastManager.showError(getString(R.string.residence_with_empty_id_found_skipping)));
                        continue;
                    }

                    ListResidModel fiche = new ListResidModel();

                    // Use optString to safely get JSON values with defaults
                    fiche.setId(Integer.parseInt(id));
                    fiche.setAgc(agcSelected);
                    fiche.setGrp(grpSelected);
                    fiche.setRef(obj.optString("ref", ""));
                    fiche.setName(obj.optString("name", ""));
                    fiche.setEntry(obj.optString("entry", ""));
                    fiche.setAdresse(obj.optString("adr", ""));
                    fiche.setCity(obj.optString("cp", ""), obj.optString("city", ""));
                    // Set last control date if available
                    if (obj.has("last")) {
                        fiche.setLast(obj.optString("last", ""));
                    }

                    idRsds.add(String.valueOf(fiche.getId()));
                    nameRsds.add(fiche);
                    nbRsds++;
                } catch (JSONException e) {
                    crashlytics.recordException(e);
                    crashlytics.log("Erreur lors du traitement des données de résidence: " + e.getMessage());
                    Log.e(TAG, "Error processing residence data: " + e.getMessage(), e);

                    Bundle errorParams = new Bundle();
                    errorParams.putString("error_type", "error_processing_residence_data");
                    errorParams.putString("class", "SelectActivity");
                    errorParams.putString("error_message", e.getMessage());
                    analytics.logEvent("makeRsds_error_processing_residence_data", errorParams);

                    runOnUiThread(() -> ToastManager.showError(getString(R.string.processing_residence_data)));
                } catch (NumberFormatException e) {
                    crashlytics.recordException(e);
                    crashlytics.log("Erreur lors de la conversion de l'ID en nombre: " + e.getMessage());
                    Log.e(TAG, "Error parsing residence ID: " + e.getMessage(), e);

                    Bundle errorParams = new Bundle();
                    errorParams.putString("error_type", "parsing_residence_id");
                    errorParams.putString("class", "SelectActivity");
                    errorParams.putString("error_message", e.getMessage());
                    analytics.logEvent("makeRsds_parsing_residence_id", errorParams);

                    runOnUiThread(() -> ToastManager.showError(getString(R.string.parsing_residence_id)));
                }
            }

            crashlytics.setCustomKey("nbRsds", nbRsds);
            crashlytics.log("Nombre de résidences chargées: " + nbRsds);

            // Set default message
            String displayMessage = getString(R.string.lbl_select_entry);

            // Check if we have a previously selected residence
            if (!rsdSelected.isEmpty()) {
                int selectedIndex = idRsds.indexOf(rsdSelected);
                if (selectedIndex >= 0 && selectedIndex < nameRsds.size()) {
                    rsd = selectedIndex;
                    ListResidModel selectedResid = nameRsds.get(rsd);
                    displayMessage = selectedResid.getName();
                    if (!selectedResid.getAdress().isEmpty()) {
                        displayMessage += " " + selectedResid.getAdress();
                    }
                } else {
                    crashlytics.log("Résidence précédemment sélectionnée non trouvée: " + rsdSelected);
                    Log.w(TAG, "Previously selected residence not found: " + rsdSelected);

                    Bundle errorParams = new Bundle();
                    errorParams.putString("error_type", "previously_selected_residence_not_found");
                    errorParams.putString("class", "SelectActivity");
                    analytics.logEvent("makeRsds_prev_selected_resid_not_found", errorParams);

                    runOnUiThread(() -> ToastManager.showError(getString(R.string.selected_residence_no_longer_available)));
                    rsdSelected = "";
                    rsd = -1;
                }
            }

            // Update UI on the main thread
            final String finalMessage = displayMessage;
            runOnUiThread(() -> mSpinnerRsd.setText(finalMessage));

            // Log residence count
            Log.d(TAG, "Loaded " + nbRsds + " residences");

            if (nbRsds == 0) {
                runOnUiThread(() -> ToastManager.showError(getString(R.string.no_residences_available_for_this_group)));
            }
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Error in makeRsds: " + e.getMessage(), e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "error_making_rsds");
            errorParams.putString("class", "SelectActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("makeRsds_error_making_rsds", errorParams);

            runOnUiThread(() -> ToastManager.showError(getString(R.string.loading_residences)));
        }
    }

    /**
     * Shows or hides the wait indicator.
     *
     * @param show True to show the indicator, false to hide it
     */
    private void showWait(boolean show) {
        try {
            crashlytics.log("showWait: " + show);

            pl.droidsonroids.gif.GifImageView mImgWait = binding.selectActivityWaitImg;

            runOnUiThread(() -> mImgWait.setVisibility(show ? View.VISIBLE : View.INVISIBLE));
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Error in showWait: " + e.getMessage(), e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "error_showing_wait_indicator");
            errorParams.putString("class", "SelectActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("showWait_error_showing_wait_indicator", errorParams);
        }
    }

}

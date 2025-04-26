package org.orgaprop.controlprest.controllers.activities;

import static org.orgaprop.controlprest.controllers.activities.MakeSelectActivity.MAKE_SELECT_ACTIVITY_REQUEST;
import static org.orgaprop.controlprest.controllers.activities.MakeSelectActivity.MAKE_SELECT_ACTIVITY_REQUEST_OK;
import static org.orgaprop.controlprest.controllers.activities.MakeSelectActivity.MAKE_SELECT_ACTIVITY_RESULT;
import static org.orgaprop.controlprest.controllers.activities.MakeSelectActivity.MAKE_SELECT_ACTIVITY_TYPE;
import static org.orgaprop.controlprest.controllers.activities.SearchActivity.SELECT_SEARCH_ACTIVITY_REQUEST;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.orgaprop.controlprest.R;
import org.orgaprop.controlprest.databinding.ActivitySelectBinding;
import org.orgaprop.controlprest.models.ListResidModel;
import org.orgaprop.controlprest.services.HttpTask;
import org.orgaprop.controlprest.services.PreferencesManager;
import org.orgaprop.controlprest.utils.ToastManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class SelectActivity extends AppCompatActivity {

    private static final String TAG = "SelectActivity";

//********* PRIVATE VARIABLES

    private PreferencesManager preferencesManager;

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

            // Initialize UI and load data
            chargAgcs();

            final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            ToastManager.showError("Error initializing: " + e.getMessage());
        }
    }

//********* SURCHARGE

    @Override
    protected void onResume() {
        super.onResume();

        if (canCheck) {
            isStarted = false;
        }

        canCheck = true;
        showWait(false);
    }

    @Override
    protected void onDestroy() {
        try {
            super.onDestroy();

            // Clean up resources
            idAgcs.clear();
            idGrps.clear();
            idRsds.clear();
            nameAgcs.clear();
            nameGrps.clear();
            nameRsds.clear();
            dataGrps.clear();
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy: " + e.getMessage(), e);
            ToastManager.showError("Error cleaning up: " + e.getMessage());
        }
    }

//********* PUBLIC FUNCTIONS

    public void selectActivityActions(View v) {
        try {
            if (v == null || v.getTag() == null) {
                Log.e(TAG, "View or view tag is null");
                ToastManager.showError("View or view tag is null");
                return;
            }

            String viewTag = v.getTag().toString();

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
                    Log.w(TAG, "Unknown view tag: " + viewTag);
                    ToastManager.showError("Unknown view tag: " + viewTag);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in selectActivityActions: " + e.getMessage(), e);
            ToastManager.showError("Error processing action: " + e.getMessage());
        }
    }

//********* PRIVATE FUNCTIONS

    private void handleActivityResult(int resultCode, Intent data) {
        try {
            TextView mSpinnerAgc = binding.selectActivityAgcSpinner;
            TextView mSpinnerGrp = binding.selectActivityGrpSpinner;
            TextView mSpinnerRsd = binding.selectActivityRsdSpinner;

            if (resultCode == RESULT_OK && data != null) {
                String typeResult = data.getStringExtra(SELECT_ACTIVITY_RESULT);

                if (Objects.equals(typeResult, SELECT_SEARCH_ACTIVITY_REQUEST)) {
                    boolean b_grps = false;
                    boolean b_rsds = false;

                    Log.d(TAG, "handleActivityResult => search return");
                    ToastManager.showShort("handleActivityResult => search return");

                    agcSelected = data.getStringExtra(SearchActivity.SELECT_SEARCH_ACTIVITY_RESULT_AGC);
                    grpSelected = data.getStringExtra(SearchActivity.SELECT_SEARCH_ACTIVITY_RESULT_GRP);
                    rsdSelected = data.getStringExtra(SearchActivity.SELECT_SEARCH_ACTIVITY_RESULT_RSD);

                    Log.d(TAG, "handleActivityResult::agcSelected => " + agcSelected);
                    Log.d(TAG, "handleActivityResult::grpSelected => " + grpSelected);
                    Log.d(TAG, "handleActivityResult::rsdSelected => " + rsdSelected);

                    // Check for null or invalid indices to prevent ArrayIndexOutOfBoundsException
                    if (agcSelected != null && !agcSelected.isEmpty()) {
                        int newAgc = idAgcs.indexOf(agcSelected);

                        if (newAgc >= 0 && newAgc < nameAgcs.size()) {
                            if (!agc.equals(newAgc)) {
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
                            Log.e(TAG, "Invalid agency index: " + newAgc);
                            ToastManager.showError("Invalid agency selection");
                        }
                    }
                } else if (Objects.equals(typeResult, MAKE_SELECT_ACTIVITY_REQUEST)) {
                    int selectType = data.getIntExtra(MAKE_SELECT_ACTIVITY_TYPE, 0);

                    if (selectType == MakeSelectActivity.MAKE_SELECT_ACTIVITY_AGC) {
                        agcSelected = data.getStringExtra(MAKE_SELECT_ACTIVITY_RESULT);

                        if (agcSelected != null && !agcSelected.isEmpty()) {
                            int newAgc = idAgcs.indexOf(agcSelected);

                            if (newAgc >= 0 && newAgc < nameAgcs.size()) {
                                agc = newAgc;

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
                                Log.e(TAG, "Invalid agency index: " + newAgc);
                                ToastManager.showError("Invalid agency selection");
                            }
                        }
                    } else if (selectType == MakeSelectActivity.MAKE_SELECT_ACTIVITY_GRP) {
                        grpSelected = data.getStringExtra(MAKE_SELECT_ACTIVITY_RESULT);

                        if (grpSelected != null && !grpSelected.isEmpty()) {
                            int newGrp = idGrps.indexOf(grpSelected);

                            if (newGrp >= 0 && newGrp < nameGrps.size() && !grp.equals(newGrp)) {
                                grp = newGrp;

                                idRsds.clear();
                                nameRsds.clear();

                                rsd = -1;
                                rsdSelected = "";
                                nbRsds = 0;

                                runOnUiThread(() -> mSpinnerGrp.setText(nameGrps.get(grp)));
                                makeRsds();
                            } else if (newGrp < 0 || newGrp >= nameGrps.size()) {
                                Log.e(TAG, "Invalid group index: " + newGrp);
                                ToastManager.showError("Invalid group selection");
                            }
                        }
                    } else if (selectType == MakeSelectActivity.MAKE_SELECT_ACTIVITY_RSD) {
                        rsdSelected = data.getStringExtra(MAKE_SELECT_ACTIVITY_RESULT);

                        if (rsdSelected != null && !rsdSelected.isEmpty()) {
                            int newRsd = idRsds.indexOf(rsdSelected);

                            if (newRsd >= 0 && newRsd < nameRsds.size()) {
                                rsd = newRsd;

                                final String residenceName = nameRsds.get(rsd).getName() + " " + nameRsds.get(rsd).getAdress();
                                runOnUiThread(() -> mSpinnerRsd.setText(residenceName));
                            } else {
                                Log.e(TAG, "Invalid residence index: " + newRsd);
                                ToastManager.showError("Invalid residence selection");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in handleActivityResult: " + e.getMessage(), e);
            ToastManager.showError("processing result: " + e.getMessage());
        }
    }

    private void startActivitySearch() {
        try {
            EditText mSearchInput = binding.selectActivitySearchInput;

            if (mSearchInput == null) {
                Log.e(TAG, "Search input is null");
                ToastManager.showShort("Search input is null");
                return;
            }

            String searchText = mSearchInput.getText().toString().trim();
            if (!searchText.isEmpty() && !waitDownload) {
                Intent intent = new Intent(SelectActivity.this, SearchActivity.class);

                canCheck = false;
                showWait(true);

                intent.putExtra(SearchActivity.SELECT_SEARCH_ACTIVITY_STR, searchText);

                makeSelectActivityLauncher.launch(intent);
            } else if (searchText.isEmpty()) {
                Log.e(TAG, "Please enter a search term");
                ToastManager.showError("Please enter a search term");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in startActivitySearch: " + e.getMessage(), e);
            ToastManager.showError("starting search: " + e.getMessage());
            showWait(false);
        }
    }

    private void startActivitySelect() {
        try {
            switch (typeSelect) {
                case "agc":
                    if (!waitDownload && nbAgcs > 0) {
                        showWait(true);
                        canCheck = false;

                        Intent intent = new Intent(SelectActivity.this, MakeSelectActivity.class);
                        intent.putExtra(MAKE_SELECT_ACTIVITY_TYPE, MakeSelectActivity.MAKE_SELECT_ACTIVITY_AGC);

                        makeSelectActivityLauncher.launch(intent);
                    } else {
                        Log.e(TAG, "No agencies available");
                        ToastManager.showError("No agencies available");
                    }
                    break;
                case "grp":
                    if (!waitDownload && agc >= 0 && nbGrps > 0) {
                        showWait(true);
                        canCheck = false;

                        Intent intent = new Intent(SelectActivity.this, MakeSelectActivity.class);
                        intent.putExtra(MAKE_SELECT_ACTIVITY_TYPE, MakeSelectActivity.MAKE_SELECT_ACTIVITY_GRP);

                        makeSelectActivityLauncher.launch(intent);
                    } else {
                        if (agc < 0) {
                            Log.e(TAG, "Please select an agency first");
                            ToastManager.showError("Please select an agency first");
                        } else if (nbGrps <= 0) {
                            Log.e(TAG, "No groups available");
                            ToastManager.showError("No groups available");
                        }
                    }
                    break;
                case "rsd":
                    if (!waitDownload && grp >= 0 && nbRsds > 0) {
                        showWait(true);
                        canCheck = false;

                        Intent intent = new Intent(SelectActivity.this, MakeSelectActivity.class);
                        intent.putExtra(MAKE_SELECT_ACTIVITY_TYPE, MakeSelectActivity.MAKE_SELECT_ACTIVITY_RSD);

                        makeSelectActivityLauncher.launch(intent);
                    } else {
                        if (agc < 0) {
                            Log.e(TAG, "Please select an agency first");
                            ToastManager.showError("Please select an agency first");
                        } else if (grp < 0) {
                            Log.e(TAG, "Please select a group first");
                            ToastManager.showError("Please select a group first");
                        } else if (nbRsds <= 0) {
                            Log.e(TAG, "No residences available");
                            ToastManager.showError("No residences available");
                        }
                    }
                    break;
                default:
                    Log.w(TAG, "Unknown type select: " + typeSelect);
                    ToastManager.showError("Unknown type select: " + typeSelect);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in startActivitySelect: " + e.getMessage(), e);
            ToastManager.showError("starting select: " + e.getMessage());
            showWait(false);
        }
    }

    private void startActivityStartCtrl() {
        try {
            if (!waitDownload && !isStarted && rsd >= 0) {
                isStarted = true;
                canCheck = false;

                preferencesManager.setAgency(agcSelected);
                preferencesManager.setGroup(grpSelected);
                preferencesManager.setResidence(rsdSelected);

                Intent intent = new Intent(SelectActivity.this, NfsActivity.class);
                intent.putExtra(SELECT_ACTIVITY_RSD, rsdSelected);

                startActivity(intent);
            } else if (rsd < 0) {
                Log.e(TAG, "Please select a residence first");
                ToastManager.showError("Please select a residence first");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in startActivityStartCtrl: " + e.getMessage(), e);
            ToastManager.showError("starting control: " + e.getMessage());
        }
    }

    private void deconnectMbr() {
        try {
            showWait(true);

            HttpTask task = new HttpTask(SelectActivity.this);
            CompletableFuture<String> futureResult = task.executeHttpTask(
                    HttpTask.HTTP_TASK_ACT_CONEX,
                    HttpTask.HTTP_TASK_CBL_NO,
                    "",
                    "mbr=" + LoginActivity.idMbr
            );

            futureResult.thenAccept(result -> {
                if (result.startsWith("1")) {
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
                        Log.e(TAG, "Error finishing LoginActivity: " + e.getMessage(), e);
                        runOnUiThread(() ->
                            ToastManager.showError("finishing LoginActivity: " + e.getMessage())
                        );
                    }

                    finish();
                } else {
                    showWait(false);

                    Log.e(TAG, "Error in deconnectMbr: " + result.substring(1));
                    runOnUiThread(() ->
                        ToastManager.showError(result.substring(1))
                    );

                    try {
                        LoginActivity loginActivity = LoginActivity.getInstance();
                        if (loginActivity != null) {
                            loginActivity.finish();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error finishing LoginActivity: " + e.getMessage(), e);
                        runOnUiThread(() ->
                            ToastManager.showError("finishing LoginActivity: " + e.getMessage())
                        );
                    }

                    finish();
                }
            }).exceptionally(ex -> {
                showWait(false);
                String errorMessage = ex instanceof CancellationException ?
                        "Request cancelled" :
                        "Network connection error";

                Log.e(TAG, "Error in deconnectMbr: " + ex.getMessage(), ex);
                runOnUiThread(() -> ToastManager.showError(errorMessage));

                try {
                    LoginActivity loginActivity = LoginActivity.getInstance();
                    if (loginActivity != null) {
                        loginActivity.finish();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error finishing LoginActivity: " + e.getMessage(), e);
                    runOnUiThread(() ->
                        ToastManager.showError("finishing LoginActivity: " + e.getMessage())
                    );
                }

                finish();
                return null;
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in deconnectMbr: " + e.getMessage(), e);
            ToastManager.showError("Disconnection error: " + e.getMessage());
            showWait(false);
        }
    }

    private void finishActivity() {
        runOnUiThread(this::finish);
    }

    private void chargAgcs() {
        try {
            TextView mSpinnerAgc = binding.selectActivityAgcSpinner;
            TextView mSpinnerGrp = binding.selectActivityGrpSpinner;
            TextView mSpinnerRsd = binding.selectActivityRsdSpinner;

            Intent intent = getIntent();
            String agenciesData = intent.getStringExtra(SELECT_ACTIVITY_EXTRA);

            if (agenciesData == null || agenciesData.isEmpty()) {
                Log.e(TAG, "Agencies data is null or empty");
                ToastManager.showError("No agencies data available");
                return;
            }

            StringTokenizer tokenizer = new StringTokenizer(agenciesData, "§");
            String defaultMessage = "Select an agency";

            waitDownload = true;

            idAgcs.clear();
            nameAgcs.clear();
            nbAgcs = 0;

            mSpinnerGrp.setText(defaultMessage);
            mSpinnerRsd.setText(defaultMessage);

            if (tokenizer.countTokens() > 1) {
                while (tokenizer.hasMoreTokens()) {
                    String item = tokenizer.nextToken();
                    int separatorIndex = item.indexOf("£");

                    if (separatorIndex > 0 && separatorIndex < item.length() - 1) {
                        idAgcs.add(item.substring(0, separatorIndex));
                        nameAgcs.add(item.substring(separatorIndex + 1));
                        nbAgcs++;
                    } else {
                        Log.w(TAG, "Invalid agency format: " + item);
                        ToastManager.showError("Invalid agency format: " + item);
                    }
                }
            } else if (tokenizer.countTokens() == 1) {
                String item = tokenizer.nextToken();
                int separatorIndex = item.indexOf("£");

                if (separatorIndex > 0 && separatorIndex < item.length() - 1) {
                    idAgcs.add(item.substring(0, separatorIndex));
                    nameAgcs.add(item.substring(separatorIndex + 1));
                    agcSelected = idAgcs.get(0);
                    nbAgcs = 1;
                } else {
                    Log.w(TAG, "Invalid agency format: " + item);
                    ToastManager.showError("Invalid agency format: " + item);
                }
            }

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
            Log.e(TAG, "Error in chargAgcs: " + e.getMessage(), e);
            ToastManager.showError("loading agencies: " + e.getMessage());
            waitDownload = false;
        }
    }

    private void chargGrps() {
        try {
            TextView mSpinnerGrp = binding.selectActivityGrpSpinner;
            pl.droidsonroids.gif.GifImageView mWaitGrpImg = binding.selectActivityWaitGrpImg;

            waitDownload = true;
            idGrps.clear();
            nameGrps.clear();
            dataGrps.clear();
            nbGrps = 0;

            mWaitGrpImg.setVisibility(View.VISIBLE);

            HttpTask task = new HttpTask(SelectActivity.this);
            CompletableFuture<String> futureResult = task.executeHttpTask(
                    HttpTask.HTTP_TASK_ACT_LIST,
                    HttpTask.HTTP_TASK_CBL_GRPS,
                    "val=" + agcSelected,
                    "mbr=" + LoginActivity.idMbr
            );

            futureResult.thenAccept(result -> {
                try {
                    String displayMessage = "Select a group";

                    if (result.startsWith("1")) {
                        try {
                            String jsonData = result.substring(1);
                            if (jsonData.isEmpty()) {
                                Log.w(TAG, "Empty JSON data received");
                                runOnUiThread(() -> ToastManager.showError("No group data received"));
                                return;
                            }

                            JSONObject obj = new JSONObject(jsonData);

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
                            } else {
                                Log.w(TAG, "No 'grps' key in JSON data");
                                runOnUiThread(() -> ToastManager.showError("No 'grps' key in JSON data"));
                                return;
                            }

                            if (!grpSelected.isEmpty() && idGrps.contains(grpSelected)) {
                                grp = idGrps.indexOf(grpSelected);
                                if (grp >= 0 && grp < nameGrps.size()) {
                                    displayMessage = nameGrps.get(grp);
                                    makeRsds();
                                }
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON parsing error: " + e.getMessage(), e);
                            runOnUiThread(() -> ToastManager.showError("Error parsing groups data: " + e.getMessage()));
                        }
                    } else {
                        final String errorMessage = result.substring(1);
                        Log.e(TAG, "Error in chargGrps: " + errorMessage);
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
                Log.e(TAG, "Error in chargGrps: " + (ex instanceof ExecutionException ?
                        Objects.requireNonNull(ex.getCause()).getMessage() : ex.getMessage()), ex);

                runOnUiThread(() -> {
                    mWaitGrpImg.setVisibility(View.INVISIBLE);
                    waitDownload = false;
                    ToastManager.showError("loading groups: " + ex.getMessage());
                });

                return null;
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in chargGrps: " + e.getMessage(), e);

            runOnUiThread(() -> {
                if (binding != null) {
                    binding.selectActivityWaitGrpImg.setVisibility(View.INVISIBLE);
                }
                waitDownload = false;
                ToastManager.showError("loading groups: " + e.getMessage());
            });
        }
    }

    private void makeRsds() {
        try {
            TextView mSpinnerRsd = binding.selectActivityRsdSpinner;

            if (grp < 0 || grp >= dataGrps.size()) {
                Log.e(TAG, "Invalid group index: " + grp);
                runOnUiThread(() -> ToastManager.showError("Invalid group index: " + grp));
                return;
            }

            idRsds.clear();
            nameRsds.clear();
            nbRsds = 0;

            JSONObject grpData = dataGrps.get(grp);
            if (grpData == null) {
                Log.e(TAG, "Group data is null");
                runOnUiThread(() -> ToastManager.showError("No data available for this group"));
                return;
            }

            Iterator<String> keys_rsds;
            try {
                keys_rsds = grpData.keys();
            } catch (Exception e) {
                Log.e(TAG, "Error getting keys from group data: " + e.getMessage(), e);
                runOnUiThread(() -> ToastManager.showError("processing residence data"));
                return;
            }

            while (keys_rsds.hasNext()) {
                try {
                    String kr = keys_rsds.next();
                    JSONObject obj = grpData.getJSONObject(kr);

                    // Safely extract data with proper validation
                    String id = obj.optString("id", "");
                    if (id.isEmpty()) {
                        Log.w(TAG, "Residence with empty ID found, skipping");
                        runOnUiThread(() -> ToastManager.showError("Residence with empty ID found, skipping"));
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
                    Log.e(TAG, "Error processing residence data: " + e.getMessage(), e);
                    runOnUiThread(() -> ToastManager.showError("processing residence data"));
                    continue;
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing residence ID: " + e.getMessage(), e);
                    runOnUiThread(() -> ToastManager.showError("parsing residence ID"));
                    continue;
                }
            }

            // Set default message
            String displayMessage = "Select a residence";

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
                    Log.w(TAG, "Previously selected residence not found: " + rsdSelected);
                    runOnUiThread(() -> ToastManager.showError("Selected residence no longer available"));
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
                runOnUiThread(() -> ToastManager.showError("No residences available for this group"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in makeRsds: " + e.getMessage(), e);
            runOnUiThread(() -> ToastManager.showError("loading residences: " + e.getMessage()));
        }
    }

    /**
     * Shows or hides the wait indicator.
     *
     * @param show True to show the indicator, false to hide it
     */
    private void showWait(boolean show) {
        try {
            pl.droidsonroids.gif.GifImageView mImgWait = binding.selectActivityWaitImg;

            runOnUiThread(() -> mImgWait.setVisibility(show ? View.VISIBLE : View.INVISIBLE));
        } catch (Exception e) {
            Log.e(TAG, "Error in showWait: " + e.getMessage(), e);
            runOnUiThread(() -> ToastManager.showShort("showing wait indicator: " + e.getMessage()));
        }
    }

}

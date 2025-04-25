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
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.orgaprop.controlprest.R;
import org.orgaprop.controlprest.databinding.ActivitySelectBinding;
import org.orgaprop.controlprest.models.ListResidModel;
import org.orgaprop.controlprest.services.HttpTask;
import org.orgaprop.controlprest.services.Prefs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.concurrent.CompletableFuture;

public class SelectActivity extends AppCompatActivity {

//********* PRIVATE VARIABLES

    private Prefs prefs;

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

        prefs = new Prefs(this);

        isStarted = false;
        canCheck = true;
        waitDownload = false;

        agc = -1;
        grp = -1;
        rsd = -1;

        agcSelected = "";
        grpSelected = "";
        rsdSelected = "";

        assert mSearchInput != null;
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

        chargAgcs();

        final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }
    }
    @Override
    protected void onResume() {
        super.onResume();

        if (canCheck) {
            isStarted = false;
        }

        canCheck = true;
        showWait(false);
    }

//********* SURCHARGE



//********* PUBLIC FUNCTIONS

    public void selectActivityActions(View v) {
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
        }
    }

//********* PRIVATE FUNCTIONS

    private void handleActivityResult(int resultCode, Intent data) {
        TextView mSpinnerAgc = binding.selectActivityAgcSpinner;
        TextView mSpinnerGrp = binding.selectActivityGrpSpinner;
        TextView mSpinnerRsd = binding.selectActivityRsdSpinner;

        if( resultCode == RESULT_OK && data != null ) {
            String typeResult = data.getStringExtra(SELECT_ACTIVITY_RESULT);

            if( Objects.equals(typeResult, SELECT_SEARCH_ACTIVITY_REQUEST) ) {
                boolean b_grps = false;
                boolean b_rsds = false;

                Log.e("SelectActivity", "handleActivityResult => search return");

                agcSelected = data.getStringExtra(SearchActivity.SELECT_SEARCH_ACTIVITY_RESULT_AGC);
                grpSelected = data.getStringExtra(SearchActivity.SELECT_SEARCH_ACTIVITY_RESULT_GRP);
                rsdSelected = data.getStringExtra(SearchActivity.SELECT_SEARCH_ACTIVITY_RESULT_RSD);

                Log.e("SelectActivity", "handleActivityResult::agcSelected => "+agcSelected);
                Log.e("SelectActivity", "handleActivityResult::grpSelected => "+grpSelected);
                Log.e("SelectActivity", "handleActivityResult::rsdSelected => "+rsdSelected);

                if( !agc.equals(idAgcs.indexOf(agcSelected)) ) {
                    idGrps.clear();
                    nameGrps.clear();
                    dataGrps.clear();
                    grp = -1;
                    nbGrps = 0;
                    b_grps = true;
                }
                if( !grp.equals(idGrps.indexOf(grpSelected)) ) {
                    idRsds.clear();
                    nameRsds.clear();
                    rsd = -1;
                    nbRsds = 0;
                    b_rsds = true;
                }

                if( agcSelected != null && !agcSelected.isEmpty() ) {
                    agc = idAgcs.indexOf(agcSelected);
                    mSpinnerAgc.setText(nameAgcs.get(agc));

                    if (b_grps) {
                        chargGrps();
                    } else if (b_rsds) {
                        grp = idGrps.indexOf(grpSelected);
                        mSpinnerGrp.setText(nameGrps.get(grp));
                        makeRsds();
                    }
                }
            }
            if( Objects.equals(typeResult, MAKE_SELECT_ACTIVITY_REQUEST) ) {
                if( data.getIntExtra(MAKE_SELECT_ACTIVITY_TYPE, 0) == MakeSelectActivity.MAKE_SELECT_ACTIVITY_AGC ) {
                    agcSelected = data.getStringExtra(MAKE_SELECT_ACTIVITY_RESULT);

                    if( agcSelected != null && !agcSelected.isEmpty() ) {
                        agc = idAgcs.indexOf(agcSelected);

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
                    }
                }
                if( data.getIntExtra(MAKE_SELECT_ACTIVITY_TYPE, 0) == MakeSelectActivity.MAKE_SELECT_ACTIVITY_GRP ) {
                    grpSelected = data.getStringExtra(MAKE_SELECT_ACTIVITY_RESULT);

                    if( grpSelected != null && !grpSelected.isEmpty() && !grp.equals(idGrps.indexOf(grpSelected)) ) {
                        grp = idGrps.indexOf(grpSelected);

                        idRsds.clear();
                        nameRsds.clear();

                        rsd = -1;
                        rsdSelected = "";
                        nbRsds = 0;

                        runOnUiThread(() -> mSpinnerGrp.setText(nameGrps.get(grp)));
                        makeRsds();
                    }
                }
                if( data.getIntExtra(MAKE_SELECT_ACTIVITY_TYPE, 0) == MakeSelectActivity.MAKE_SELECT_ACTIVITY_RSD ) {
                    rsdSelected = data.getStringExtra(MAKE_SELECT_ACTIVITY_RESULT);

                    if( rsdSelected != null && !rsdSelected.isEmpty() ) {
                        rsd = idRsds.indexOf(rsdSelected);

                        String m = nameRsds.get(rsd).getName() + " " + nameRsds.get(rsd).getAdress();
                        runOnUiThread(() -> mSpinnerRsd.setText(m));
                    }
                }
            }
        }
    }

    private void startActivitySearch() {
        EditText mSearchInput = binding.selectActivitySearchInput;

        assert mSearchInput != null;
        if( !mSearchInput.getText().toString().isEmpty() && !waitDownload ) {
            Intent intent = new Intent(SelectActivity.this, SearchActivity.class);

            canCheck = false;
            showWait(true);

            intent.putExtra(SearchActivity.SELECT_SEARCH_ACTIVITY_STR, mSearchInput.getText().toString().trim());

            makeSelectActivityLauncher.launch(intent);
        }
    }
    private void startActivitySelect() {
        switch( typeSelect ) {
            case "agc":
                if( !waitDownload && nbAgcs > 0 ) {
                    showWait(true);
                    canCheck = false;

                    Intent intent = new Intent(SelectActivity.this, MakeSelectActivity.class);
                    intent.putExtra(MAKE_SELECT_ACTIVITY_TYPE, MakeSelectActivity.MAKE_SELECT_ACTIVITY_AGC);

                    makeSelectActivityLauncher.launch(intent);
                }
                break;
            case "grp":
                if( !waitDownload && agc >= 0 && nbGrps > 0 ) {
                    showWait(true);
                    canCheck = false;

                    Intent intent = new Intent(SelectActivity.this, MakeSelectActivity.class);
                    intent.putExtra(MAKE_SELECT_ACTIVITY_TYPE, MakeSelectActivity.MAKE_SELECT_ACTIVITY_GRP);

                    makeSelectActivityLauncher.launch(intent);
                } else {
                    if( agc < 0 ) {
                        Toast.makeText(SelectActivity.this, "Choisissez un agence", Toast.LENGTH_LONG).show();
                    }
                }
                break;
            case "rsd":
                if( !waitDownload && grp >= 0 && nbRsds > 0 ) {
                    showWait(true);
                    canCheck = false;

                    Intent intent = new Intent(SelectActivity.this, MakeSelectActivity.class);
                    intent.putExtra(MAKE_SELECT_ACTIVITY_TYPE, MakeSelectActivity.MAKE_SELECT_ACTIVITY_RSD);

                    makeSelectActivityLauncher.launch(intent);
                } else {
                    if( agc < 0 ) {
                        Toast.makeText(SelectActivity.this, "Choisissez un agence", Toast.LENGTH_LONG).show();
                    } else if( grp < 0 ) {
                        Toast.makeText(SelectActivity.this, "Choisissez un groupement", Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }
    private void startActivityStartCtrl() {
        if( !waitDownload && !isStarted && rsd >= 0 ) {
            isStarted = true;
            canCheck = false;

            prefs.setAgency(agcSelected);
            prefs.setGroup(grpSelected);
            prefs.setResidence(rsdSelected);

            Intent intent = new Intent(SelectActivity.this, NfsActivity.class);

            intent.putExtra(SELECT_ACTIVITY_RSD, rsdSelected);

            startActivity(intent);
        }
    }
    private void deconnectMbr() {
        HttpTask task = new HttpTask(SelectActivity.this);
        CompletableFuture<String> futureResult = task.executeHttpTask(HttpTask.HTTP_TASK_ACT_CONEX, HttpTask.HTTP_TASK_CBL_NO, "", "mbr="+LoginActivity.idMbr);

        futureResult.thenAccept(result -> {
            if( result.startsWith("1") ) {
                prefs.setMbr("new");
                prefs.setAgency("");
                prefs.setGroup("");
                prefs.setResidence("");

                LoginActivity.getInstance().finish();
                finish();
            } else {
                Toast.makeText(SelectActivity.this, result.substring(1), Toast.LENGTH_SHORT).show();

                LoginActivity.getInstance().finish();
                finish();
            }
        }).exceptionally(ex -> {
            Toast.makeText(SelectActivity.this, getResources().getString(R.string.mess_timeout), Toast.LENGTH_SHORT).show();

            LoginActivity.getInstance().finish();
            finish();

            return null;
        });
    }
    private void finishActivity() {
        runOnUiThread(this::finish);
    }

    private void chargAgcs() {
        TextView mSpinnerAgc = binding.selectActivityAgcSpinner;
        TextView mSpinnerGrp = binding.selectActivityGrpSpinner;
        TextView mSpinnerRsd = binding.selectActivityRsdSpinner;

        Intent intent = getIntent();
        StringTokenizer tokenizer = new StringTokenizer(intent.getStringExtra(SELECT_ACTIVITY_EXTRA), "§");
        String m = "Sélectionner une agence";

        waitDownload = true;

        idAgcs.clear();
        nameAgcs.clear();

        mSpinnerGrp.setText(m);
        mSpinnerRsd.setText(m);

        if( tokenizer.countTokens() > 1 ) {
            while( tokenizer.hasMoreTokens() ) {
                String item = tokenizer.nextToken();

                idAgcs.add(item.substring(0, item.indexOf("£")));
                nameAgcs.add(item.substring(item.indexOf("£") + 1));
                nbAgcs++;
            }
        } else {
            String item = tokenizer.nextToken();

            idAgcs.add(item.substring(0, item.indexOf("£")));
            nameAgcs.add(item.substring(item.indexOf("£") + 1));
            agcSelected = idAgcs.get(0);
        }

        if( !agcSelected.isEmpty() ) {
            agc = idAgcs.indexOf(agcSelected);
            m = nameAgcs.get(agc);
            chargGrps();
        }

        mSpinnerAgc.setText(m);

        waitDownload = false;
    }
    private void chargGrps() {
        TextView mSpinnerGrp = binding.selectActivityGrpSpinner;
        pl.droidsonroids.gif.GifImageView mWaitGrpImg = binding.selectActivityWaitGrpImg;

        waitDownload = true;

        mWaitGrpImg.setVisibility(View.VISIBLE);

        HttpTask task = new HttpTask(SelectActivity.this);
        CompletableFuture<String> futureResult = task.executeHttpTask(HttpTask.HTTP_TASK_ACT_LIST, HttpTask.HTTP_TASK_CBL_GRPS, "val="+agcSelected, "mbr="+LoginActivity.idMbr);

        futureResult.thenAccept(result -> {
            if( result.startsWith("1") ) {
                String mess = "Sélectionner un groupement";

                try {
                    JSONObject obj = new JSONObject(result.substring(1));

                    if (obj.has("grps")) {
                        Iterator<String> keys_grps = obj.getJSONObject("grps").keys();

                        while (keys_grps.hasNext()) {
                            String kg = keys_grps.next();
                            JSONObject obj_grp = obj.getJSONObject("grps").getJSONObject(kg);

                            idGrps.add(obj_grp.getString("id"));
                            nameGrps.add(obj_grp.getString("name"));
                            dataGrps.add(obj_grp.getJSONObject("rsds"));

                            nbGrps++;
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if( !grpSelected.isEmpty() && idGrps.contains(grpSelected) ) {
                    grp = idGrps.indexOf(grpSelected);
                    mess = nameGrps.get(grp);

                    makeRsds();
                }

                String finalMess = mess;
                SelectActivity.this.runOnUiThread(() -> mSpinnerGrp.setText(finalMess));
            } else {
                Toast.makeText(SelectActivity.this, result.substring(1), Toast.LENGTH_SHORT).show();
            }
        }).exceptionally(ex -> {
            Toast.makeText(SelectActivity.this, getResources().getString(R.string.mess_timeout), Toast.LENGTH_SHORT).show();

            return null;
        });

        SelectActivity.this.runOnUiThread(() -> {
            mWaitGrpImg.setVisibility(View.INVISIBLE);
        });

        waitDownload = false;
    }
    private void makeRsds() {
        TextView mSpinnerRsd = binding.selectActivityRsdSpinner;

        Iterator<String> keys_rsds = dataGrps.get(grp).keys();

        while( keys_rsds.hasNext() ) {
            try{
                String kr = keys_rsds.next();
                ListResidModel fiche = new ListResidModel();
                JSONObject obj = dataGrps.get(grp).getJSONObject(kr);

                fiche.setId(Integer.parseInt(obj.getString("id")));
                fiche.setAgc(agcSelected);
                fiche.setGrp(grpSelected);
                fiche.setRef(obj.getString("ref"));
                fiche.setName(obj.getString("name"));
                fiche.setEntry(obj.getString("entry"));
                fiche.setAdresse(obj.getString("adr"));
                fiche.setCity(obj.getString("cp"), obj.getString("city"));

                idRsds.add(String.valueOf(fiche.getId()));
                nameRsds.add(fiche);
                nbRsds++;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        SelectActivity.this.runOnUiThread(() -> mSpinnerRsd.setText("Sélectionner une résidence"));

        if( !rsdSelected.isEmpty() ) {
            if( idRsds.contains(rsdSelected) ) {
                rsd = idRsds.indexOf(rsdSelected);
                String m = nameRsds.get(rsd).getName() + " " + nameRsds.get(rsd).getAdress();

                SelectActivity.this.runOnUiThread(() -> mSpinnerRsd.setText(m));
            } else {
                Toast.makeText(SelectActivity.this, "Résidence inconnue", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showWait(Boolean b) {
        pl.droidsonroids.gif.GifImageView mImgWait = binding.selectActivityWaitImg;

        runOnUiThread(() -> {
            if( b ) {
                mImgWait.setVisibility(View.VISIBLE);
            } else {
                mImgWait.setVisibility(View.INVISIBLE);
            }
        });
    }

}
package org.orgaprop.controlprest.controllers.activities;

import static org.orgaprop.controlprest.controllers.activities.SelectActivity.SELECT_ACTIVITY_RESULT;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import java.util.ArrayList;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.orgaprop.controlprest.BuildConfig;
import org.orgaprop.controlprest.R;
import org.orgaprop.controlprest.databinding.ActivityMakeSelectBinding;
import org.orgaprop.controlprest.models.ListResidModel;
import org.orgaprop.controlprest.utils.ToastManager;

public class MakeSelectActivity extends AppCompatActivity {

    private static final String TAG = "MakeSelectActivity";
    private FirebaseCrashlytics crashlytics;
    private FirebaseAnalytics analytics;

//********** STATIC VARIABLES

    public static final String MAKE_SELECT_ACTIVITY_TYPE = "typeResult";
    public static final String MAKE_SELECT_ACTIVITY_RESULT = "result";

    public static final int MAKE_SELECT_ACTIVITY_AGC = 1;
    public static final int MAKE_SELECT_ACTIVITY_GRP = 2;
    public static final int MAKE_SELECT_ACTIVITY_RSD = 3;

    public static final String MAKE_SELECT_ACTIVITY_REQUEST = "make";
    public static final int MAKE_SELECT_ACTIVITY_REQUEST_OK = 1;
    public static final int MAKE_SELECT_ACTIVITY_REQUEST_CANCEL = 0;

//********** WIDGETS

    private ActivityMakeSelectBinding binding;

//********** CONSTRUCTORS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            crashlytics = FirebaseCrashlytics.getInstance();
            crashlytics.setCustomKey("deviceModel", Build.MODEL);
            crashlytics.setCustomKey("deviceManufacturer", Build.MANUFACTURER);
            crashlytics.setCustomKey("appVersion", BuildConfig.VERSION_NAME);
            crashlytics.log("MakeSelectActivity démarrée");

            analytics = FirebaseAnalytics.getInstance(this);

            Bundle screenViewParams = new Bundle();
            screenViewParams.putString(FirebaseAnalytics.Param.SCREEN_NAME, "MakeSelect");
            screenViewParams.putString(FirebaseAnalytics.Param.SCREEN_CLASS, "MakeSelectActivity");
            analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, screenViewParams);

            binding = ActivityMakeSelectBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            Intent intent = getIntent();

            if (intent == null) {
                crashlytics.log("Intent reçu est null");
                Log.e(TAG, "Intent reçu est null");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "null_intent");
                errorParams.putString("class", "MakeSelectActivity");
                analytics.logEvent("onCreate_intent", errorParams);

                showErrorAndFinish(getString(R.string.donn_es_d_entr_e_invalides));
                return;
            }

            int typeSelect = intent.getIntExtra(MAKE_SELECT_ACTIVITY_TYPE, 0);

            if (typeSelect == 0) {
                crashlytics.log("Type de sélection invalide (0)");
                Log.e(TAG, "Type de sélection invalide (0)");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "invalid_type");
                errorParams.putString("class", "MakeSelectActivity");
                errorParams.putString("typeSelect", String.valueOf(typeSelect));
                analytics.logEvent("onCreate_type_select", errorParams);

                showErrorAndFinish(getString(R.string.type_de_s_lection_invalide));
                return;
            }

            crashlytics.setCustomKey("selectionType", typeSelect);
            crashlytics.log("Type de sélection: " + typeSelect);

            Bundle typeParams = new Bundle();
            typeParams.putString("class", "MakeSelectActivity");
            typeParams.putString("selection_type", String.valueOf(typeSelect));
            analytics.logEvent("onCreate_selection_type", typeParams);

            // Charger les données selon le type de sélection
            loadDataBasedOnType(typeSelect);

        } catch (Exception e) {
            if (crashlytics != null) {
                crashlytics.recordException(e);
            }
            Log.e(TAG, "Erreur lors de l'initialisation de MakeSelectActivity", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "init_error");
            errorParams.putString("class", "MakeSelectActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("onCreate_init_error", errorParams);

            showErrorAndFinish(getString(R.string.erreur_lors_de_l_initialisation));
        }
    }

//********* PUBLIC FUNCTIONS

    public void makeSelectActivityActions(View v) {
        try {
            if (v == null || v.getTag() == null) {
                crashlytics.log("Vue ou tag null dans makeSelectActivityActions");
                Log.e(TAG, "Vue ou tag null dans makeSelectActivityActions");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "null_view");
                errorParams.putString("class", "MakeSelectActivity");
                analytics.logEvent("makeSelectActivityActions_null_view", errorParams);

                return;
            }

            String viewTag = v.getTag().toString();
            crashlytics.setCustomKey("action", viewTag);

            if (viewTag.equals("cancel")) {
                crashlytics.log("Action annulée par l'utilisateur");
                finishActivity();
            } else {
                crashlytics.log("Action non reconnue: " + viewTag);
                Log.w(TAG, "Tag non reconnu: " + viewTag);

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "unknown_action");
                errorParams.putString("class", "MakeSelectActivity");
                errorParams.putString("action", viewTag);
                analytics.logEvent("makeSelectActivityActions_unknown_action", errorParams);
            }
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur lors du traitement de l'action", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "action_error");
            errorParams.putString("class", "MakeSelectActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("makeSelectActivityActions_app_error", errorParams);

            ToastManager.showError(getString(R.string.erreur_lors_du_traitement_de_l_action));
        }
    }

//********** PRIVATE FUNCTIONS

    /**
     * Charge les données en fonction du type de sélection
     */
    private void loadDataBasedOnType(int typeSelect) {
        try {
            crashlytics.log("loadDataBasedOnType called");

            switch (typeSelect) {
                case MAKE_SELECT_ACTIVITY_AGC:
                    loadAgencies();
                    break;
                case MAKE_SELECT_ACTIVITY_GRP:
                    loadGroups();
                    break;
                case MAKE_SELECT_ACTIVITY_RSD:
                    loadResidences();
                    break;
                default:
                    crashlytics.log("Type de sélection inconnu: " + typeSelect);
                    Log.e(TAG, "Type de sélection inconnu: " + typeSelect);

                    Bundle errorParams = new Bundle();
                    errorParams.putString("error_type", "unknown_type");
                    errorParams.putString("class", "MakeSelectActivity");
                    errorParams.putString("typeSelect", String.valueOf(typeSelect));
                    analytics.logEvent("loadDataBasedOnType_unknown_type", errorParams);

                    showErrorAndFinish(getString(R.string.type_de_s_lection_inconnu));
                    break;
            }
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur lors du chargement des données", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "load_data_error");
            errorParams.putString("class", "MakeSelectActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("loadDataBasedOnType_app_error", errorParams);

            showErrorAndFinish("Erreur lors du chargement des données");
        }
    }

    /**
     * Charge la liste des agences
     */
    private void loadAgencies() {
        try {
            crashlytics.log("loadAgencies called");

            TextView mTitle = binding.makeSelectActivityTitle;
            LinearLayout mLayout = binding.makeSelectActivityLyt;

            ArrayList<String> nameAgcs = SelectActivity.nameAgcs;
            ArrayList<String> idAgcs = SelectActivity.idAgcs;

            if (nameAgcs.isEmpty() || idAgcs.isEmpty()) {
                crashlytics.log("Listes d'agences vides");
                Log.w(TAG, "Listes d'agences vides");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "empty_list");
                errorParams.putString("class", "MakeSelectActivity");
                analytics.logEvent("loadAgencies_empty_list", errorParams);

                ToastManager.showShort(getString(R.string.aucune_agence_disponible));
                finishActivity();
                return;
            }

            crashlytics.setCustomKey("agenciesCount", nameAgcs.size());

            if (nameAgcs.size() != idAgcs.size()) {
                crashlytics.log("Incohérence entre les tailles des listes d'agences: " +
                        nameAgcs.size() + " vs " + idAgcs.size());
                Log.e(TAG, "Incohérence entre les tailles des listes d'agences: " +
                        nameAgcs.size() + " vs " + idAgcs.size());

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "exec_error");
                errorParams.putString("class", "MakeSelectActivity");
                errorParams.putString("error_message", nameAgcs.size() + " vs " + idAgcs.size());
                analytics.logEvent("loadAgencies_agencies_error", errorParams);

                showErrorAndFinish(getString(R.string.incoh_rence_dans_les_donn_es_d_agences));
                return;
            }

            mTitle.setText(R.string.lbl_agc);
            mLayout.removeAllViews(); // S'assurer que la liste est vide

            for (int i = 0; i < nameAgcs.size(); i++) {

                try {
                    // Créer la vue d'élément
                    View viewElement = LayoutInflater.from(this).inflate(R.layout.agence_item, null);

                    if (viewElement == null) {
                        crashlytics.log("Échec de l'inflation de la vue d'agence");
                        Log.e(TAG, "Échec de l'inflation de la vue d'agence");

                        Bundle errorParams = new Bundle();
                        errorParams.putString("error_type", "inflate_error");
                        errorParams.putString("class", "MakeSelectActivity");
                        analytics.logEvent("loadAgencies_inflate_error", errorParams);

                        ToastManager.showError(getString(R.string.chec_de_l_inflation_de_la_vue_d_agence));
                        continue;
                    }

                    TextView textView = viewElement.findViewById(R.id.agence_item_name);

                    if (textView == null) {
                        crashlytics.log("TextView non trouvé dans la vue d'agence");
                        Log.e(TAG, "TextView non trouvé dans la vue d'agence");

                        Bundle errorParams = new Bundle();
                        errorParams.putString("error_type", "textview_not_found");
                        errorParams.putString("class", "MakeSelectActivity");
                        analytics.logEvent("loadAgencies_textview_not_found", errorParams);

                        ToastManager.showError(getString(R.string.textview_non_trouv_dans_la_vue_d_agence));
                        continue;
                    }

                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);

                    textView.setText(nameAgcs.get(i));
                    textView.setTag(idAgcs.get(i));

                    layoutParams.setMargins(5, 5, 5, 5);

                    // Configurer le gestionnaire de clics
                    textView.setOnClickListener(view -> {
                        try {
                            if (view.getTag() == null) {
                                crashlytics.log("Tag null lors du clic sur une agence");
                                Log.e(TAG, "Tag null lors du clic sur une agence");

                                Bundle errorParams = new Bundle();
                                errorParams.putString("error_type", "tag_null");
                                errorParams.putString("class", "MakeSelectActivity");
                                analytics.logEvent("loadAgencies_click_agency", errorParams);

                                ToastManager.showError(getString(R.string.tag_null_lors_du_clic_sur_une_agence));
                                return;
                            }

                            String agencyId = view.getTag().toString();

                            crashlytics.log("Agence sélectionnée: " + agencyId);

                            Bundle errorParams = new Bundle();
                            errorParams.putString("class", "MakeSelectActivity");
                            errorParams.putString("agencyId", agencyId);
                            analytics.logEvent("loadAgencies_click_agency", errorParams);

                            Intent resultIntent = new Intent();
                            resultIntent.putExtra(SELECT_ACTIVITY_RESULT, MAKE_SELECT_ACTIVITY_REQUEST);
                            resultIntent.putExtra(MAKE_SELECT_ACTIVITY_TYPE, MAKE_SELECT_ACTIVITY_AGC);
                            resultIntent.putExtra(MAKE_SELECT_ACTIVITY_RESULT, view.getTag().toString());

                            setResult(RESULT_OK, resultIntent);
                            finish();
                        } catch (Exception e) {
                            crashlytics.recordException(e);
                            Log.e(TAG, "Erreur lors du traitement du clic sur l'agence", e);

                            Bundle errorParams = new Bundle();
                            errorParams.putString("error_type", "click_error");
                            errorParams.putString("class", "MakeSelectActivity");
                            errorParams.putString("error_message", e.getMessage());
                            analytics.logEvent("loadAgencies_click_error", errorParams);

                            ToastManager.showError(getString(R.string.erreur_lors_de_la_s_lection));
                        }
                    });

                    runOnUiThread(() -> {
                        try {
                            mLayout.addView(viewElement, layoutParams);
                        } catch (Exception e) {
                            crashlytics.recordException(e);
                            Log.e(TAG, "Erreur lors de l'ajout de la vue d'agence au layout", e);

                            Bundle errorParams = new Bundle();
                            errorParams.putString("error_type", "add_view_error");
                            errorParams.putString("class", "MakeSelectActivity");
                            errorParams.putString("error_message", e.getMessage());
                            analytics.logEvent("loadAgencies_add_view_error", errorParams);

                            ToastManager.showError(getString(R.string.erreur_lors_de_l_ajout_de_la_vue_d_agence_au_layout));
                        }
                    });
                } catch (Exception e) {
                    crashlytics.recordException(e);
                    Log.e(TAG, "Erreur lors de la création de l'élément d'agence à la position " + i, e);

                    Bundle errorParams = new Bundle();
                    errorParams.putString("error_type", "create_view_error");
                    errorParams.putString("class", "MakeSelectActivity");
                    errorParams.putString("error_message", e.getMessage());
                    analytics.logEvent("loadAgencies_create_view_error", errorParams);

                    ToastManager.showError(getString(R.string.erreur_lors_de_la_cr_ation_de_la_vue));
                }
            }
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur générale lors du chargement des agences", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "load_agencies_error");
            errorParams.putString("class", "MakeSelectActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("loadAgencies_app_error", errorParams);

            showErrorAndFinish(getString(R.string.erreur_lors_du_chargement_des_agences));
        }
    }

    /**
     * Charge la liste des groupements
     */
    private void loadGroups() {
        try {
            crashlytics.log("loadGroups called");

            TextView mTitle = binding.makeSelectActivityTitle;
            LinearLayout mLayout = binding.makeSelectActivityLyt;

            ArrayList<String> nameGrps = SelectActivity.nameGrps;
            ArrayList<String> idGrps = SelectActivity.idGrps;

            if (nameGrps.isEmpty() || idGrps.isEmpty()) {
                crashlytics.log("Listes de groupements vides");
                Log.w(TAG, "Listes de groupements vides");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "empty_list");
                errorParams.putString("class", "MakeSelectActivity");
                analytics.logEvent("loadGroups_empty_list", errorParams);

                ToastManager.showShort(getString(R.string.aucun_groupement_disponible));
                finishActivity();
                return;
            }

            crashlytics.setCustomKey("groupsCount", nameGrps.size());

            if (nameGrps.size() != idGrps.size()) {
                crashlytics.log("Incohérence entre les tailles des listes de groupements: " +
                        nameGrps.size() + " vs " + idGrps.size());
                Log.e(TAG, "Incohérence entre les tailles des listes de groupements: " +
                        nameGrps.size() + " vs " + idGrps.size());

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "exec_error");
                errorParams.putString("class", "MakeSelectActivity");
                errorParams.putString("error_message", nameGrps.size() + " vs " + idGrps.size());
                analytics.logEvent("loadGroups_groups_error", errorParams);

                showErrorAndFinish(getString(R.string.incoh_rence_dans_les_donn_es_de_groupements));
                return;
            }

            mTitle.setText(R.string.lbl_grp);
            mLayout.removeAllViews(); // S'assurer que la liste est vide

            for (int i = 0; i < nameGrps.size(); i++) {

                try {
                    // Créer la vue d'élément
                    View viewElement = LayoutInflater.from(this).inflate(R.layout.group_item, null);

                    if (viewElement == null) {
                        crashlytics.log("Incohérence entre les tailles des listes de groupements");
                        Log.e(TAG, "Échec de l'inflation de la vue de groupement");

                        Bundle errorParams = new Bundle();
                        errorParams.putString("error_type", "inflate_error");
                        errorParams.putString("class", "MakeSelectActivity");
                        analytics.logEvent("loadGroups_inflate_error", errorParams);

                        ToastManager.showError(getString(R.string.chec_de_la_construction_de_la_vue_de_groupement));
                        continue;
                    }

                    TextView textView = viewElement.findViewById(R.id.group_item_nom);

                    if (textView == null) {
                        crashlytics.log("TextView non trouvé dans la vue de groupement");
                        Log.e(TAG, "TextView non trouvé dans la vue de groupement");

                        Bundle errorParams = new Bundle();
                        errorParams.putString("error_type", "textview_not_found");
                        errorParams.putString("class", "MakeSelectActivity");
                        analytics.logEvent("loadGroups_textview_not_found", errorParams);

                        ToastManager.showError(getString(R.string.chec_de_la_construction_de_la_vue_de_groupement));
                        continue;
                    }

                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);

                    textView.setText(nameGrps.get(i));
                    textView.setTag(idGrps.get(i));

                    layoutParams.setMargins(5, 5, 5, 5);

                    // Configurer le gestionnaire de clics
                    textView.setOnClickListener(view -> {
                        try {
                            if (view.getTag() == null) {
                                crashlytics.log("Tag null lors du clic sur un groupement");
                                Log.e(TAG, "Tag null lors du clic sur un groupement");
                                ToastManager.showError(getString(R.string.tag_null_lors_du_clic_sur_un_groupement));
                                return;
                            }

                            String groupId = view.getTag().toString();
                            crashlytics.log("Groupement sélectionné: " + groupId);

                            Bundle errorParams = new Bundle();
                            errorParams.putString("class", "MakeSelectActivity");
                            errorParams.putString("groupId", groupId);
                            analytics.logEvent("loadGroups_click_group", errorParams);

                            Intent resultIntent = new Intent();
                            resultIntent.putExtra(SELECT_ACTIVITY_RESULT, MAKE_SELECT_ACTIVITY_REQUEST);
                            resultIntent.putExtra(MAKE_SELECT_ACTIVITY_TYPE, MAKE_SELECT_ACTIVITY_GRP);
                            resultIntent.putExtra(MAKE_SELECT_ACTIVITY_RESULT, view.getTag().toString());

                            setResult(RESULT_OK, resultIntent);
                            finish();
                        } catch (Exception e) {
                            crashlytics.recordException(e);
                            Log.e(TAG, "Erreur lors du traitement du clic sur le groupement", e);

                            Bundle errorParams = new Bundle();
                            errorParams.putString("error_type", "click_error");
                            errorParams.putString("class", "MakeSelectActivity");
                            errorParams.putString("error_message", e.getMessage());
                            analytics.logEvent("loadGroups_click_error", errorParams);

                            ToastManager.showError(getString(R.string.erreur_lors_de_la_s_lection));
                        }
                    });

                    runOnUiThread(() -> {
                        try {
                            mLayout.addView(viewElement, layoutParams);
                        } catch (Exception e) {
                            crashlytics.recordException(e);
                            Log.e(TAG, "Erreur lors de l'ajout de la vue de groupement au layout", e);

                            Bundle errorParams = new Bundle();
                            errorParams.putString("error_type", "add_view_error");
                            errorParams.putString("class", "MakeSelectActivity");
                            errorParams.putString("error_message", e.getMessage());
                            analytics.logEvent("loadGroups_add_view_error", errorParams);

                            ToastManager.showError(getString(R.string.chec_de_la_construction_de_la_vue_de_groupement));
                        }
                    });
                } catch (Exception e) {
                    crashlytics.recordException(e);
                    Log.e(TAG, "Erreur lors de la création de l'élément de groupement à la position " + i, e);

                    Bundle errorParams = new Bundle();
                    errorParams.putString("error_type", "create_view_error");
                    errorParams.putString("class", "MakeSelectActivity");
                    errorParams.putString("error_message", e.getMessage());
                    analytics.logEvent("loadGroups_create_view_error", errorParams);

                    ToastManager.showError(getString(R.string.chec_de_la_construction_de_la_vue_de_groupement));
                }
            }
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur générale lors du chargement des groupements", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "load_groups_error");
            errorParams.putString("class", "MakeSelectActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("loadGroups_app_error", errorParams);

            showErrorAndFinish(getString(R.string.erreur_lors_du_chargement_des_groupements));
        }
    }

    /**
     * Charge la liste des résidences
     */
    private void loadResidences() {
        try {
            crashlytics.log("loadResidences called");

            TextView mTitle = binding.makeSelectActivityTitle;
            LinearLayout mLayout = binding.makeSelectActivityLyt;

            ArrayList<ListResidModel> nameRsds = SelectActivity.nameRsds;
            ArrayList<String> idRsds = SelectActivity.idRsds;

            if (nameRsds.isEmpty() || idRsds.isEmpty()) {
                crashlytics.log("Listes de résidences vides");
                Log.w(TAG, "Listes de résidences vides");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "empty_list");
                errorParams.putString("class", "MakeSelectActivity");
                analytics.logEvent("loadResidences_empty_list", errorParams);

                ToastManager.showError(getString(R.string.aucune_r_sidence_disponible));
                finishActivity();
                return;
            }

            if (nameRsds.size() != idRsds.size()) {
                crashlytics.log("Incohérence entre les tailles des listes de résidences: " +
                        nameRsds.size() + " vs " + idRsds.size());
                Log.e(TAG, "Incohérence entre les tailles des listes de résidences: " +
                        nameRsds.size() + " vs " + idRsds.size());

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "exec_error");
                errorParams.putString("class", "MakeSelectActivity");
                errorParams.putString("error_message", nameRsds.size() + " vs " + idRsds.size());
                analytics.logEvent("loadResidences_resid_error", errorParams);

                showErrorAndFinish(getString(R.string.incoh_rence_dans_les_donn_es_de_r_sidences));
                return;
            }

            mTitle.setText(R.string.selectionner_une_entree);
            mLayout.removeAllViews(); // S'assurer que la liste est vide

            for (int i = 0; i < nameRsds.size(); i++) {

                try {
                    // Créer la vue d'élément
                    View viewElement = LayoutInflater.from(this).inflate(R.layout.resid_item, null);

                    if (viewElement == null) {
                        crashlytics.log("Échec de l'inflation de la vue de résidence");
                        Log.e(TAG, "Échec de l'inflation de la vue de résidence");

                        Bundle errorParams = new Bundle();
                        errorParams.putString("error_type", "inflate_error");
                        errorParams.putString("class", "MakeSelectActivity");
                        analytics.logEvent("loadResidences_inflate_error", errorParams);

                        continue;
                    }

                    viewElement.setTag(idRsds.get(i));

                    // Récupérer tous les TextView nécessaires
                    TextView textViewRef = viewElement.findViewById(R.id.fiche_search_resid_ref);
                    TextView textViewName = viewElement.findViewById(R.id.fiche_search_resid_name);
                    TextView textViewEntry = viewElement.findViewById(R.id.fiche_search_resid_entry);
                    TextView textViewAdr = viewElement.findViewById(R.id.fiche_search_resid_adr);
                    TextView textViewCity = viewElement.findViewById(R.id.fiche_search_resid_city);
                    TextView textViewLast = viewElement.findViewById(R.id.fiche_search_resid_last);

                    // Vérifier que tous les TextView sont valides
                    if (textViewRef == null || textViewName == null || textViewEntry == null ||
                            textViewAdr == null || textViewCity == null || textViewLast == null) {
                        crashlytics.log("Un ou plusieurs TextView manquants dans la vue de résidence");
                        Log.e(TAG, "Un ou plusieurs TextView manquants dans la vue de résidence");

                        Bundle errorParams = new Bundle();
                        errorParams.putString("error_type", "textview_missing");
                        errorParams.putString("class", "MakeSelectActivity");
                        analytics.logEvent("loadResidences_textview_missing", errorParams);

                        ToastManager.showError(getString(R.string.un_ou_plusieurs_textview_manquants_dans_la_vue_de_r_sidence));
                        continue;
                    }

                    // Obtenir les données de la résidence
                    ListResidModel residModel = nameRsds.get(i);

                    // Définir les valeurs des TextView
                    textViewRef.setText(residModel.getRef());
                    textViewName.setText(residModel.getName());
                    textViewEntry.setText(residModel.getEntry());
                    textViewAdr.setText(residModel.getAdress());
                    textViewCity.setText(residModel.getCity());
                    textViewLast.setText(residModel.getLast() != null ? residModel.getLast() : "");

                    // Définir l'arrière-plan en fonction de l'état de visite
                    if (residModel.isVisited()) {
                        viewElement.setBackground(AppCompatResources.getDrawable(this, R.drawable.button_enabled));
                    } else {
                        viewElement.setBackground(AppCompatResources.getDrawable(this, R.drawable.button_standard));
                    }

                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    layoutParams.setMargins(5, 5, 5, 5);

                    // Configurer le gestionnaire de clics
                    viewElement.setOnClickListener(view -> {
                        try {
                            if (view.getTag() == null) {
                                crashlytics.log("Tag null lors du clic sur une résidence");
                                Log.e(TAG, "Tag null lors du clic sur une résidence");
                                ToastManager.showError(getString(R.string.tag_null_lors_du_clic_sur_une_r_sidence));
                                return;
                            }

                            String residenceId = view.getTag().toString();
                            crashlytics.log("Résidence sélectionnée: " + residenceId);

                            Bundle errorParams = new Bundle();
                            errorParams.putString("class", "MakeSelectActivity");
                            errorParams.putString("residenceId", residenceId);
                            analytics.logEvent("loadResidences_click_residence", errorParams);

                            Intent resultIntent = new Intent();
                            resultIntent.putExtra(SELECT_ACTIVITY_RESULT, MAKE_SELECT_ACTIVITY_REQUEST);
                            resultIntent.putExtra(MAKE_SELECT_ACTIVITY_TYPE, MAKE_SELECT_ACTIVITY_RSD);
                            resultIntent.putExtra(MAKE_SELECT_ACTIVITY_RESULT, view.getTag().toString());

                            setResult(RESULT_OK, resultIntent);
                            finish();
                        } catch (Exception e) {
                            crashlytics.recordException(e);
                            Log.e(TAG, "Erreur lors du traitement du clic sur la résidence", e);

                            Bundle errorParams = new Bundle();
                            errorParams.putString("error_type", "click_error");
                            errorParams.putString("class", "MakeSelectActivity");
                            errorParams.putString("error_message", e.getMessage());
                            analytics.logEvent("loadResidences_click_error", errorParams);

                            ToastManager.showError(getString(R.string.erreur_lors_de_la_s_lection));
                        }
                    });

                    runOnUiThread(() -> {
                        try {
                            mLayout.addView(viewElement, layoutParams);
                        } catch (Exception e) {
                            crashlytics.recordException(e);
                            Log.e(TAG, "Erreur lors de l'ajout de la vue de résidence au layout", e);

                            Bundle errorParams = new Bundle();
                            errorParams.putString("error_type", "add_view_error");
                            errorParams.putString("class", "MakeSelectActivity");
                            errorParams.putString("error_message", e.getMessage());
                            analytics.logEvent("loadResidences_add_view_error", errorParams);

                            ToastManager.showError(getString(R.string.erreur_lors_de_l_ajout_de_la_vue_de_r_sidence_au_layout));
                        }
                    });
                } catch (Exception e) {
                    crashlytics.recordException(e);
                    Log.e(TAG, "Erreur lors de la création de l'élément de résidence à la position " + i, e);

                    Bundle errorParams = new Bundle();
                    errorParams.putString("error_type", "create_view_error");
                    errorParams.putString("class", "MakeSelectActivity");
                    errorParams.putString("error_message", e.getMessage());
                    analytics.logEvent("loadResidences_create_view_error", errorParams);

                    ToastManager.showError(getString(R.string.erreur_lors_de_la_cr_ation_de_l_l_ment_de_r_sidence));
                }
            }
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur générale lors du chargement des résidences", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "load_residences_error");
            errorParams.putString("class", "MakeSelectActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("loadResidences_app_error", errorParams);

            showErrorAndFinish(getString(R.string.erreur_lors_du_chargement_des_r_sidences));
        }
    }

    /**
     * Termine l'activité avec un résultat d'annulation
     */
    private void finishActivity() {
        try {
            crashlytics.log("finishActivity called");

            setResult(MAKE_SELECT_ACTIVITY_REQUEST_CANCEL);
            finish();
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur lors de la fermeture de l'activité", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "finish_error");
            errorParams.putString("class", "MakeSelectActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("finishActivity_error", errorParams);

            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::finish, 3000);
        }
    }

    /**
     * Affiche une erreur et termine l'activité
     */
    private void showErrorAndFinish(String message) {
        try {
            crashlytics.log("showErrorAndFinish called");

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "show_error");
            errorParams.putString("class", "MakeSelectActivity");
            errorParams.putString("error_message", message);
            analytics.logEvent("showErrorAndFinish_error", errorParams);

            ToastManager.showError(message);
            Log.e(TAG, "Erreur fatale: " + message);
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::finishActivity, 3000);
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur lors de l'affichage de l'erreur", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "show_error");
            errorParams.putString("class", "MakeSelectActivity");
            errorParams.putString("error_message", message);
            analytics.logEvent("showErrorAndFinish_error", errorParams);

            ToastManager.showError(message);
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::finish, 3000);
        }
    }

}
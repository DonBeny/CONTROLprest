package org.orgaprop.controlprest.controllers.activities;

import static org.orgaprop.controlprest.controllers.activities.SelectActivity.SELECT_ACTIVITY_RESULT;
import static org.orgaprop.controlprest.controllers.activities.SelectActivity.dataLock;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.orgaprop.controlprest.BuildConfig;
import org.orgaprop.controlprest.R;
import org.orgaprop.controlprest.controllers.adapters.AgencyAdapter;
import org.orgaprop.controlprest.controllers.adapters.GroupAdapter;
import org.orgaprop.controlprest.controllers.adapters.ResidenceAdapter;
import org.orgaprop.controlprest.databinding.ActivityMakeSelectBinding;
import org.orgaprop.controlprest.models.ListResidModel;
import org.orgaprop.controlprest.utils.ToastManager;



public class MakeSelectActivity extends AppCompatActivity {

    private static final String TAG = "MakeSelectActivity";

    private FirebaseCrashlytics crashlytics;
    private FirebaseAnalytics analytics;
    private Handler uiHandler;
    private boolean isLoading = false;

    private final List<String> localIdList = new ArrayList<>();
    private final List<String> localNameList = new ArrayList<>();
    private final List<ListResidModel> localResidList = new ArrayList<>();

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
    private ProgressBar progressBar;

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

            Log.i(TAG, "MakeSelectActivity démarrée");

            analytics = FirebaseAnalytics.getInstance(this);

            Bundle screenViewParams = new Bundle();
            screenViewParams.putString(FirebaseAnalytics.Param.SCREEN_NAME, "MakeSelect");
            screenViewParams.putString(FirebaseAnalytics.Param.SCREEN_CLASS, "MakeSelectActivity");
            analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, screenViewParams);

            binding = ActivityMakeSelectBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            uiHandler = new Handler(Looper.getMainLooper());
            progressBar = binding.makeSelectActivityProgressBar;

            binding.makeSelectActivityRecyclerView.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            binding.makeSelectActivityLoadingText.setVisibility(View.VISIBLE);

            Intent intent = getIntent();
            if (intent == null) {
                logError("Intent reçu est null", "null_intent");
                showErrorAndFinish(getString(R.string.donn_es_d_entr_e_invalides));
                return;
            }

            int typeSelect = intent.getIntExtra(MAKE_SELECT_ACTIVITY_TYPE, 0);
            if (typeSelect == 0) {
                logError("Type de sélection invalide (0)", "invalid_type");
                showErrorAndFinish(getString(R.string.type_de_s_lection_invalide));
                return;
            }

            crashlytics.setCustomKey("selectionType", typeSelect);
            logInfo("Type de sélection: " + typeSelect, "selection_type");

            // Charger les données selon le type de sélection
            uiHandler.postDelayed(() -> loadDataBasedOnType(typeSelect), 100);
        } catch (Exception e) {
            logException(e, "init_error", "Error during initialization");
            showErrorAndFinish(getString(R.string.erreur_lors_de_l_initialisation));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Nettoyage des données locales
        localIdList.clear();
        localNameList.clear();
        localResidList.clear();

        // Supprimer tous les callbacks en attente pour éviter les fuites de mémoire
        if (uiHandler != null) {
            uiHandler.removeCallbacksAndMessages(null);
        }
    }

//********* PUBLIC FUNCTIONS

    public void makeSelectActivityActions(View v) {
        try {
            if (v == null || v.getTag() == null) {
                logWarning("Vue ou tag null dans makeSelectActivityActions", "null_view");
                return;
            }

            String viewTag = v.getTag().toString();
            crashlytics.setCustomKey("action", viewTag);

            if (viewTag.equals("cancel")) {
                logInfo("Action annulée par l'utilisateur", "user_cancel");
                finishActivity();
            } else {
                logWarning("Action non reconnue: " + viewTag, "unknown_action");
                ToastManager.showError(getString(R.string.erreur_lors_du_traitement_de_l_action));
            }
        } catch (Exception e) {
            logException(e, "action_error", "Error while processing action");
            ToastManager.showError(getString(R.string.erreur_lors_du_traitement_de_l_action));
        }
    }

//********** PRIVATE FUNCTIONS

    /**
     * Charge les données en fonction du type de sélection
     */
    private void loadDataBasedOnType(int typeSelect) {
        showLoading(true);

        try {
            logInfo("loadDataBasedOnType called", "load_data");

            Thread dataLoadingThread = new Thread(() -> {
                try {
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
                            logError("Type de sélection inconnu: " + typeSelect, "unknown_type");
                            uiHandler.post(() -> showErrorAndFinish(getString(R.string.type_de_s_lection_inconnu)));
                            break;
                    }
                } catch (Exception e) {
                    logException(e, "load_data_error", "Error loading data");
                    uiHandler.post(() -> showErrorAndFinish("Erreur lors du chargement des données"));
                }
            }, "DataLoaderThread");

            dataLoadingThread.setPriority(Thread.MIN_PRIORITY);
            dataLoadingThread.start();
        } catch (Exception e) {
            logException(e, "load_data_error", "Error loading data");
            showErrorAndFinish("Erreur lors du chargement des données");
        }
    }

    /**
     * Charge la liste des agences
     */
    private void loadAgencies() {
        try {
            logInfo("loadAgencies called", "load_agencies");

            final List<String> threadSafeIdList;
            final List<String> threadSafeNameList;

            dataLock.readLock().lock();
            try {
                if (SelectActivity.nameAgcs.isEmpty() || SelectActivity.idAgcs.isEmpty()) {
                    logWarning("Listes d'agences vides", "empty_list");
                    uiHandler.post(() -> {
                        ToastManager.showShort(getString(R.string.aucune_agence_disponible));
                        finishActivity();
                    });
                    return;
                }

                crashlytics.setCustomKey("agenciesCount", SelectActivity.nameAgcs.size());

                if (SelectActivity.nameAgcs.size() != SelectActivity.idAgcs.size()) {
                    logError("Incohérence entre les tailles des listes d'agences: " +
                            SelectActivity.nameAgcs.size() + " vs " + SelectActivity.idAgcs.size(), "exec_error");
                    uiHandler.post(() -> showErrorAndFinish(getString(R.string.incoh_rence_dans_les_donn_es_d_agences)));
                    return;
                }

                threadSafeIdList = new ArrayList<>(SelectActivity.idAgcs);
                threadSafeNameList = new ArrayList<>(SelectActivity.nameAgcs);
            } finally {
                dataLock.readLock().unlock();
            }

            uiHandler.post(() -> {
                try {
                    localIdList.clear();
                    localNameList.clear();
                    localIdList.addAll(threadSafeIdList);
                    localNameList.addAll(threadSafeNameList);

                    TextView mTitle = binding.makeSelectActivityTitle;
                    RecyclerView recyclerView = binding.makeSelectActivityRecyclerView;

                    mTitle.setText(R.string.lbl_agc);

                    recyclerView.setLayoutManager(new LinearLayoutManager(this));
                    recyclerView.setVisibility(View.VISIBLE);
                    AgencyAdapter adapter = new AgencyAdapter(localNameList, localIdList, this::onAgencySelected);
                    recyclerView.setAdapter(adapter);

                    showLoading(false);
                } catch (Exception e) {
                    logException(e, "ui_update_error", "Error updating UI in loadAgencies");
                    showErrorAndFinish(getString(R.string.erreur_lors_du_chargement_des_agences));
                }
            });
        } catch (Exception e) {
            logException(e, "load_agencies_error", "Error loading agencies");
            uiHandler.post(() -> showErrorAndFinish(getString(R.string.erreur_lors_du_chargement_des_agences)));
        }
    }

    /**
     * Charge la liste des groupements
     */
    private void loadGroups() {
        try {
            logInfo("loadGroups called", "load_groups");

            final List<String> threadSafeIdList = new ArrayList<>();
            final List<String> threadSafeNameList = new ArrayList<>();

            dataLock.readLock().lock();
            try {
                if (SelectActivity.nameGrps.isEmpty() || SelectActivity.idGrps.isEmpty()) {
                    logWarning("Listes de groupements vides", "empty_list");
                    uiHandler.post(() -> {
                        ToastManager.showShort(getString(R.string.aucun_groupement_disponible));
                        finishActivity();
                    });
                    return;
                }

                crashlytics.setCustomKey("groupsCount", SelectActivity.nameGrps.size());
                logInfo("Nombre de groupes: " + SelectActivity.nameGrps.size(), "group_count");

                if (SelectActivity.nameGrps.size() != SelectActivity.idGrps.size()) {
                    logError("Incohérence entre les tailles des listes de groupements: " +
                            SelectActivity.nameGrps.size() + " vs " + SelectActivity.idGrps.size(), "exec_error");
                    uiHandler.post(() -> showErrorAndFinish(getString(R.string.incoh_rence_dans_les_donn_es_de_groupements)));
                    return;
                }

                // Copier les données dans des listes locales thread-safe
                threadSafeIdList.addAll(SelectActivity.idGrps);
                threadSafeNameList.addAll(SelectActivity.nameGrps);

                // Log des groupes récupérés pour débogage
                for (int i = 0; i < threadSafeNameList.size(); i++) {
                    logInfo("Groupe " + i + ": " + threadSafeNameList.get(i) + " (ID: " + threadSafeIdList.get(i) + ")", "group_data");
                }
            } finally {
                dataLock.readLock().unlock();
            }

            uiHandler.post(() -> {
                try {
                    // Copier les données depuis les listes thread-safe vers les listes locales
                    // sur le thread UI
                    localIdList.clear();
                    localNameList.clear();
                    localIdList.addAll(threadSafeIdList);
                    localNameList.addAll(threadSafeNameList);

                    TextView mTitle = binding.makeSelectActivityTitle;
                    RecyclerView recyclerView = binding.makeSelectActivityRecyclerView;

                    mTitle.setText(R.string.lbl_grp);

                    // Configurer le RecyclerView avec un adapter
                    recyclerView.setLayoutManager(new LinearLayoutManager(this));
                    recyclerView.setVisibility(View.VISIBLE);
                    GroupAdapter adapter = new GroupAdapter(localNameList, localIdList, this::onGroupSelected);
                    recyclerView.setAdapter(adapter);

                    showLoading(false);
                } catch (Exception e) {
                    logException(e, "ui_update_error", "Error updating UI in loadGroups");
                    showErrorAndFinish(getString(R.string.erreur_lors_du_chargement_des_groupements));
                }
            });
        } catch (Exception e) {
            logException(e, "load_groups_error", "Error loading groups");
            uiHandler.post(() -> showErrorAndFinish(getString(R.string.erreur_lors_du_chargement_des_groupements)));
        }
    }

    /**
     * Charge la liste des résidences
     */
    private void loadResidences() {
        try {
            logInfo("loadResidences called", "load_residences");

            final List<String> threadSafeIdList;
            final List<ListResidModel> threadSafeResidList;

            dataLock.readLock().lock();
            try {
                if (SelectActivity.nameRsds.isEmpty() || SelectActivity.idRsds.isEmpty()) {
                    logWarning("Listes de résidences vides", "empty_list");
                    uiHandler.post(() -> {
                        ToastManager.showError(getString(R.string.aucune_r_sidence_disponible));
                        finishActivity();
                    });
                    return;
                }

                crashlytics.setCustomKey("residencesCount", SelectActivity.nameRsds.size());
                logInfo("Nombre de résidences: " + SelectActivity.nameRsds.size(), "residence_count");

                if (SelectActivity.nameRsds.size() != SelectActivity.idRsds.size()) {
                    logError("Incohérence entre les tailles des listes de résidences: " +
                            SelectActivity.nameRsds.size() + " vs " + SelectActivity.idRsds.size(), "exec_error");
                    uiHandler.post(() -> showErrorAndFinish(getString(R.string.incoh_rence_dans_les_donn_es_de_r_sidences)));
                    return;
                }

                threadSafeIdList = new ArrayList<>(SelectActivity.idRsds);
                threadSafeResidList = new ArrayList<>(SelectActivity.nameRsds);
            } finally {
                dataLock.readLock().unlock();
            }

            uiHandler.post(() -> {
                try {
                    localIdList.clear();
                    localResidList.clear();
                    localIdList.addAll(threadSafeIdList);
                    localResidList.addAll(threadSafeResidList);

                    TextView mTitle = binding.makeSelectActivityTitle;
                    RecyclerView recyclerView = binding.makeSelectActivityRecyclerView;

                    mTitle.setText(R.string.selectionner_une_entree);

                    recyclerView.setLayoutManager(new LinearLayoutManager(this));
                    recyclerView.setVisibility(View.VISIBLE);
                    ResidenceAdapter adapter = new ResidenceAdapter(localResidList, localIdList, this::onResidenceSelected);
                    recyclerView.setAdapter(adapter);

                    showLoading(false);
                } catch (Exception e) {
                    logException(e, "ui_update_error", "Error updating UI in loadResidences");
                    showErrorAndFinish(getString(R.string.erreur_lors_du_chargement_des_r_sidences));
                }
            });
        } catch (Exception e) {
            logException(e, "load_residences_error", "Error loading residences");
            showErrorAndFinish(getString(R.string.erreur_lors_du_chargement_des_r_sidences));
        }
    }

    /**
     * Gère la sélection d'une agence
     */
    private void onAgencySelected(String agencyId) {
        try {
            if (agencyId == null) {
                logWarning("ID d'agence null", "null_agency_id");
                ToastManager.showError(getString(R.string.tag_null_lors_du_clic_sur_une_agence));
                return;
            }

            logInfo("Agence sélectionnée: " + agencyId, "agency_selected");

            Intent resultIntent = new Intent();
            resultIntent.putExtra(SELECT_ACTIVITY_RESULT, MAKE_SELECT_ACTIVITY_REQUEST);
            resultIntent.putExtra(MAKE_SELECT_ACTIVITY_TYPE, MAKE_SELECT_ACTIVITY_AGC);
            resultIntent.putExtra(MAKE_SELECT_ACTIVITY_RESULT, agencyId);

            setResult(RESULT_OK, resultIntent);
            finish();
        } catch (Exception e) {
            logException(e, "selection_error", "Error selecting agency");
            ToastManager.showError(getString(R.string.erreur_lors_de_la_s_lection));
        }
    }

    /**
     * Gère la sélection d'un groupe
     */
    private void onGroupSelected(String groupId) {
        try {
            if (groupId == null) {
                logWarning("ID de groupe null", "null_group_id");
                ToastManager.showError(getString(R.string.tag_null_lors_du_clic_sur_un_groupement));
                return;
            }

            logInfo("Groupe sélectionné: " + groupId, "group_selected");

            Intent resultIntent = new Intent();
            resultIntent.putExtra(SELECT_ACTIVITY_RESULT, MAKE_SELECT_ACTIVITY_REQUEST);
            resultIntent.putExtra(MAKE_SELECT_ACTIVITY_TYPE, MAKE_SELECT_ACTIVITY_GRP);
            resultIntent.putExtra(MAKE_SELECT_ACTIVITY_RESULT, groupId);

            setResult(RESULT_OK, resultIntent);
            finish();
        } catch (Exception e) {
            logException(e, "selection_error", "Error selecting group");
            ToastManager.showError(getString(R.string.erreur_lors_de_la_s_lection));
        }
    }

    /**
     * Gère la sélection d'une résidence
     */
    private void onResidenceSelected(String residenceId) {
        try {
            if (residenceId == null) {
                logWarning("ID de résidence null", "null_residence_id");
                ToastManager.showError(getString(R.string.tag_null_lors_du_clic_sur_une_r_sidence));
                return;
            }

            logInfo("Résidence sélectionnée: " + residenceId, "residence_selected");

            Intent resultIntent = new Intent();
            resultIntent.putExtra(SELECT_ACTIVITY_RESULT, MAKE_SELECT_ACTIVITY_REQUEST);
            resultIntent.putExtra(MAKE_SELECT_ACTIVITY_TYPE, MAKE_SELECT_ACTIVITY_RSD);
            resultIntent.putExtra(MAKE_SELECT_ACTIVITY_RESULT, residenceId);

            setResult(RESULT_OK, resultIntent);
            finish();
        } catch (Exception e) {
            logException(e, "selection_error", "Error selecting residence");
            ToastManager.showError(getString(R.string.erreur_lors_de_la_s_lection));
        }
    }

    /**
     * Termine l'activité avec un résultat d'annulation
     */
    private void finishActivity() {
        try {
            logInfo("finishActivity called", "finish");
            setResult(MAKE_SELECT_ACTIVITY_REQUEST_CANCEL);
            finish();
        } catch (Exception e) {
            logException(e, "finish_error", "Error finishing activity");
            finish();
        }
    }

    /**
     * Affiche une erreur et termine l'activité
     */
    private void showErrorAndFinish(String message) {
        try {
            logError("showErrorAndFinish: " + message, "error_and_finish");

            // Assurer que nous sommes sur le thread UI
            if (Looper.myLooper() == Looper.getMainLooper()) {
                ToastManager.showError(message);
                setResult(RESULT_CANCELED);
                // Utiliser un délai avant de finir l'activité
                uiHandler.postDelayed(this::finish, 3000);
            } else {
                uiHandler.post(() -> {
                    ToastManager.showError(message);
                    setResult(RESULT_CANCELED);
                    uiHandler.postDelayed(this::finish, 3000);
                });
            }
        } catch (Exception e) {
            logException(e, "show_error_finish", "Error showing error");
            finish();
        }
    }

    /**
     * Affiche ou masque l'indicateur de chargement
     */
    private void showLoading(boolean show) {
        try {
            isLoading = show;
            uiHandler.post(() -> {
                if (progressBar != null) {
                    progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
                }

                TextView loadingText = binding.makeSelectActivityLoadingText;
                loadingText.setVisibility(show ? View.VISIBLE : View.GONE);

                RecyclerView recyclerView = binding.makeSelectActivityRecyclerView;
                recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);

                logInfo("Indicateur de chargement " + (show ? "affiché" : "masqué"), "loading_state");
            });
        } catch (Exception e) {
            Log.e(TAG, "Error showing loading indicator: " + e.getMessage());
        }
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

}
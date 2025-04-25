package org.orgaprop.controlprest.controllers.activities;

import static org.orgaprop.controlprest.controllers.activities.SelectActivity.SELECT_ACTIVITY_RESULT;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.orgaprop.controlprest.R;
import org.orgaprop.controlprest.databinding.ActivityMakeSelectBinding;
import org.orgaprop.controlprest.models.ListResidModel;

import java.util.ArrayList;

public class MakeSelectActivity extends AppCompatActivity {

    private static final String TAG = "MakeSelectActivity";

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
            binding = ActivityMakeSelectBinding.inflate(getLayoutInflater());

            setContentView(binding.getRoot());

            Intent intent = getIntent();

            if (intent == null) {
                Log.e(TAG, "Intent reçu est null");
                showErrorAndFinish("Données d'entrée invalides");
                return;
            }

            int typeSelect = intent.getIntExtra(MAKE_SELECT_ACTIVITY_TYPE, 0);

            if (typeSelect == 0) {
                Log.e(TAG, "Type de sélection invalide (0)");
                showErrorAndFinish("Type de sélection invalide");
                return;
            }

            // Charger les données selon le type de sélection
            loadDataBasedOnType(typeSelect);

        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'initialisation de MakeSelectActivity", e);
            showErrorAndFinish("Erreur lors de l'initialisation");
        }
    }

//********* PUBLIC FUNCTIONS

    public void makeSelectActivityActions(View v) {
        try {
            if (v == null || v.getTag() == null) {
                Log.e(TAG, "Vue ou tag null dans makeSelectActivityActions");
                return;
            }

            String viewTag = v.getTag().toString();

            if (viewTag.equals("cancel")) {
                finishActivity();
            } else {
                Log.w(TAG, "Tag non reconnu: " + viewTag);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du traitement de l'action", e);
            Toast.makeText(this, "Erreur lors du traitement de l'action", Toast.LENGTH_SHORT).show();
        }
    }

//********** PRIVATE FUNCTIONS

    /**
     * Charge les données en fonction du type de sélection
     */
    private void loadDataBasedOnType(int typeSelect) {
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
                    Log.e(TAG, "Type de sélection inconnu: " + typeSelect);
                    showErrorAndFinish("Type de sélection inconnu");
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du chargement des données", e);
            showErrorAndFinish("Erreur lors du chargement des données");
        }
    }

    /**
     * Charge la liste des agences
     */
    private void loadAgencies() {
        try {
            TextView mTitle = binding.makeSelectActivityTitle;
            LinearLayout mLayout = binding.makeSelectActivityLyt;

            ArrayList<String> nameAgcs = SelectActivity.nameAgcs;
            ArrayList<String> idAgcs = SelectActivity.idAgcs;

            if (nameAgcs.isEmpty() || idAgcs.isEmpty()) {
                Log.w(TAG, "Listes d'agences vides");
                Toast.makeText(this, "Aucune agence disponible", Toast.LENGTH_SHORT).show();
                finishActivity();
                return;
            }

            if (nameAgcs.size() != idAgcs.size()) {
                Log.e(TAG, "Incohérence entre les tailles des listes d'agences: " +
                        nameAgcs.size() + " vs " + idAgcs.size());
                showErrorAndFinish("Incohérence dans les données d'agences");
                return;
            }

            mTitle.setText(R.string.lbl_agc);
            mLayout.removeAllViews(); // S'assurer que la liste est vide

            for (int i = 0; i < nameAgcs.size(); i++) {

                try {
                    // Créer la vue d'élément
                    View viewElement = LayoutInflater.from(this).inflate(R.layout.agence_item, null);

                    if (viewElement == null) {
                        Log.e(TAG, "Échec de l'inflation de la vue d'agence");
                        Toast.makeText(this, "Échec de l'inflation de la vue d'agence", Toast.LENGTH_SHORT).show();
                        continue;
                    }

                    TextView textView = viewElement.findViewById(R.id.agence_item_name);

                    if (textView == null) {
                        Log.e(TAG, "TextView non trouvé dans la vue d'agence");
                        Toast.makeText(this, "TextView non trouvé dans la vue d'agence", Toast.LENGTH_SHORT).show();
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
                                Log.e(TAG, "Tag null lors du clic sur une agence");
                                Toast.makeText(this, "Tag null lors du clic sur une agence", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            Intent resultIntent = new Intent();
                            resultIntent.putExtra(SELECT_ACTIVITY_RESULT, MAKE_SELECT_ACTIVITY_REQUEST);
                            resultIntent.putExtra(MAKE_SELECT_ACTIVITY_TYPE, MAKE_SELECT_ACTIVITY_AGC);
                            resultIntent.putExtra(MAKE_SELECT_ACTIVITY_RESULT, view.getTag().toString());

                            setResult(RESULT_OK, resultIntent);
                            finish();
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur lors du traitement du clic sur l'agence", e);
                            Toast.makeText(MakeSelectActivity.this, "Erreur lors de la sélection", Toast.LENGTH_SHORT).show();
                        }
                    });

                    runOnUiThread(() -> {
                        try {
                            mLayout.addView(viewElement, layoutParams);
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur lors de l'ajout de la vue d'agence au layout", e);
                            Toast.makeText(MakeSelectActivity.this, "Erreur lors de l'ajout de la vue d'agence au layout", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Erreur lors de la création de l'élément d'agence à la position " + i, e);
                    Toast.makeText(MakeSelectActivity.this, "Erreur lors de la création de l'élément d'agence", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur générale lors du chargement des agences", e);
            Toast.makeText(MakeSelectActivity.this, "Erreur générale lors du chargement des agences", Toast.LENGTH_SHORT).show();
            showErrorAndFinish("Erreur lors du chargement des agences");
        }
    }

    /**
     * Charge la liste des groupements
     */
    private void loadGroups() {
        try {
            TextView mTitle = binding.makeSelectActivityTitle;
            LinearLayout mLayout = binding.makeSelectActivityLyt;

            ArrayList<String> nameGrps = SelectActivity.nameGrps;
            ArrayList<String> idGrps = SelectActivity.idGrps;

            if (nameGrps.isEmpty() || idGrps.isEmpty()) {
                Log.w(TAG, "Listes de groupements vides");
                Toast.makeText(this, "Aucun groupement disponible", Toast.LENGTH_SHORT).show();
                finishActivity();
                return;
            }

            if (nameGrps.size() != idGrps.size()) {
                Log.e(TAG, "Incohérence entre les tailles des listes de groupements: " +
                        nameGrps.size() + " vs " + idGrps.size());
                showErrorAndFinish("Incohérence dans les données de groupements");
                return;
            }

            mTitle.setText(R.string.lbl_grp);
            mLayout.removeAllViews(); // S'assurer que la liste est vide

            for (int i = 0; i < nameGrps.size(); i++) {

                try {
                    // Créer la vue d'élément
                    View viewElement = LayoutInflater.from(this).inflate(R.layout.group_item, null);

                    if (viewElement == null) {
                        Log.e(TAG, "Échec de l'inflation de la vue de groupement");
                        Toast.makeText(this, "Échec de l'inflation de la vue de groupement", Toast.LENGTH_SHORT).show();
                        continue;
                    }

                    TextView textView = viewElement.findViewById(R.id.group_item_nom);

                    if (textView == null) {
                        Log.e(TAG, "TextView non trouvé dans la vue de groupement");
                        Toast.makeText(this, "TextView non trouvé dans la vue de groupement", Toast.LENGTH_SHORT).show();
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
                                Log.e(TAG, "Tag null lors du clic sur un groupement");
                                Toast.makeText(this, "Tag null lors du clic sur un groupement", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            Intent resultIntent = new Intent();
                            resultIntent.putExtra(SELECT_ACTIVITY_RESULT, MAKE_SELECT_ACTIVITY_REQUEST);
                            resultIntent.putExtra(MAKE_SELECT_ACTIVITY_TYPE, MAKE_SELECT_ACTIVITY_GRP);
                            resultIntent.putExtra(MAKE_SELECT_ACTIVITY_RESULT, view.getTag().toString());

                            setResult(RESULT_OK, resultIntent);
                            finish();
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur lors du traitement du clic sur le groupement", e);
                            Toast.makeText(MakeSelectActivity.this, "Erreur lors de la sélection", Toast.LENGTH_SHORT).show();
                        }
                    });

                    runOnUiThread(() -> {
                        try {
                            mLayout.addView(viewElement, layoutParams);
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur lors de l'ajout de la vue de groupement au layout", e);
                            Toast.makeText(MakeSelectActivity.this, "Erreur lors de l'ajout de la vue de groupement au layout", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Erreur lors de la création de l'élément de groupement à la position " + i, e);
                    Toast.makeText(MakeSelectActivity.this, "Erreur lors de la création de l'élément de groupement", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur générale lors du chargement des groupements", e);
            showErrorAndFinish("Erreur lors du chargement des groupements");
        }
    }

    /**
     * Charge la liste des résidences
     */
    private void loadResidences() {
        try {
            TextView mTitle = binding.makeSelectActivityTitle;
            LinearLayout mLayout = binding.makeSelectActivityLyt;

            ArrayList<ListResidModel> nameRsds = SelectActivity.nameRsds;
            ArrayList<String> idRsds = SelectActivity.idRsds;

            if (nameRsds.isEmpty() || idRsds.isEmpty()) {
                Log.w(TAG, "Listes de résidences vides");
                Toast.makeText(this, "Aucune résidence disponible", Toast.LENGTH_SHORT).show();
                finishActivity();
                return;
            }

            if (nameRsds.size() != idRsds.size()) {
                Log.e(TAG, "Incohérence entre les tailles des listes de résidences: " +
                        nameRsds.size() + " vs " + idRsds.size());
                showErrorAndFinish("Incohérence dans les données de résidences");
                return;
            }

            mTitle.setText(R.string.selectionner_une_entree);
            mLayout.removeAllViews(); // S'assurer que la liste est vide

            for (int i = 0; i < nameRsds.size(); i++) {

                try {
                    // Créer la vue d'élément
                    View viewElement = LayoutInflater.from(this).inflate(R.layout.resid_item, null);

                    if (viewElement == null) {
                        Log.e(TAG, "Échec de l'inflation de la vue de résidence");
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
                        Log.e(TAG, "Un ou plusieurs TextView manquants dans la vue de résidence");
                        Toast.makeText(this, "Un ou plusieurs TextView manquants dans la vue de résidence", Toast.LENGTH_SHORT).show();
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
                                Log.e(TAG, "Tag null lors du clic sur une résidence");
                                Toast.makeText(this, "Tag null lors du clic sur une résidence", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            Intent resultIntent = new Intent();
                            resultIntent.putExtra(SELECT_ACTIVITY_RESULT, MAKE_SELECT_ACTIVITY_REQUEST);
                            resultIntent.putExtra(MAKE_SELECT_ACTIVITY_TYPE, MAKE_SELECT_ACTIVITY_RSD);
                            resultIntent.putExtra(MAKE_SELECT_ACTIVITY_RESULT, view.getTag().toString());

                            setResult(RESULT_OK, resultIntent);
                            finish();
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur lors du traitement du clic sur la résidence", e);
                            Toast.makeText(MakeSelectActivity.this, "Erreur lors de la sélection", Toast.LENGTH_SHORT).show();
                        }
                    });

                    runOnUiThread(() -> {
                        try {
                            mLayout.addView(viewElement, layoutParams);
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur lors de l'ajout de la vue de résidence au layout", e);
                            Toast.makeText(MakeSelectActivity.this, "Erreur lors de l'ajout de la vue de résidence au layout", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Erreur lors de la création de l'élément de résidence à la position " + i, e);
                    Toast.makeText(MakeSelectActivity.this, "Erreur lors de la création de l'élément de résidence", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur générale lors du chargement des résidences", e);
            showErrorAndFinish("Erreur lors du chargement des résidences");
        }
    }

    /**
     * Termine l'activité avec un résultat d'annulation
     */
    private void finishActivity() {
        try {
            setResult(MAKE_SELECT_ACTIVITY_REQUEST_CANCEL);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la fermeture de l'activité", e);
            Toast.makeText(MakeSelectActivity.this, "Erreur lors de la fermeture de l'activité", Toast.LENGTH_SHORT).show();
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::finish, 3000);
        }
    }

    /**
     * Affiche une erreur et termine l'activité
     */
    private void showErrorAndFinish(String message) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            Log.e(TAG, "Erreur fatale: " + message);
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::finishActivity, 3000);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'affichage de l'erreur", e);
            Toast.makeText(MakeSelectActivity.this, "Erreur lors de l'affichage de l'erreur", Toast.LENGTH_SHORT).show();
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::finish, 3000);
        }
    }

}
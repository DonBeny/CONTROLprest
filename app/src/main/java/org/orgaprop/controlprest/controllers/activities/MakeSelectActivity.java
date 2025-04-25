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

import org.orgaprop.controlprest.R;
import org.orgaprop.controlprest.databinding.ActivityMakeSelectBinding;
import org.orgaprop.controlprest.models.ListResidModel;

import java.util.ArrayList;

public class MakeSelectActivity extends AppCompatActivity {

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

        binding = ActivityMakeSelectBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Intent intent = getIntent();

        int typeSelect = intent.getIntExtra(MAKE_SELECT_ACTIVITY_TYPE, 0);

        switch( typeSelect ) {
            case MAKE_SELECT_ACTIVITY_AGC: chargAgcs(); break;
            case MAKE_SELECT_ACTIVITY_GRP: chargGrps(); break;
            case MAKE_SELECT_ACTIVITY_RSD: chargRsds(); break;
        }
    }

//********* PUBLIC FUNCTIONS

    public void makeSelectActivityActions(View v) {
        String viewTag = v.getTag().toString();

        switch( viewTag ) {
            case "cancel": finishActivity(); break;
        }
    }

//********** PRIVATE FUNCTIONS

    private void chargAgcs() {
        TextView mTitle = binding.makeSelectActivityTitle;
        LinearLayout mLayout = binding.makeSelectActivityLyt;

        ArrayList<String> nameAgcs = SelectActivity.nameAgcs;
        ArrayList<String> idAgcs = SelectActivity.idAgcs;

        mTitle.setText("SELECTIONNER UNE AGENCE");

        for( int i = 0; i < nameAgcs.size(); i++ ) {
            View viewElement = LayoutInflater.from(MakeSelectActivity.this).inflate(R.layout.agence_item, null);
            TextView textView = viewElement.findViewById(R.id.agence_item_name);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

            textView.setText(nameAgcs.get(i));
            textView.setTag(idAgcs.get(i));

            layoutParams.setMargins(5,5,5,5);

            textView.setOnClickListener(view -> {
                Intent intent = new Intent();

                intent.putExtra(SELECT_ACTIVITY_RESULT, MAKE_SELECT_ACTIVITY_REQUEST);
                intent.putExtra(MAKE_SELECT_ACTIVITY_TYPE, MAKE_SELECT_ACTIVITY_AGC);
                intent.putExtra(MAKE_SELECT_ACTIVITY_RESULT, view.getTag().toString());

                setResult(RESULT_OK, intent);
                finish();
            });

            runOnUiThread(() -> mLayout.addView(viewElement, layoutParams));
        }
    }
    private void chargGrps() {
        TextView mTitle = binding.makeSelectActivityTitle;
        LinearLayout mLayout = binding.makeSelectActivityLyt;

        ArrayList<String> nameGrps = SelectActivity.nameGrps;
        ArrayList<String> idGrps = SelectActivity.idGrps;

        mTitle.setText("SELECTIONNER UN GROUPEMENT");

        for( int i = 0; i < nameGrps.size(); i++ ) {
            View viewElement = LayoutInflater.from(MakeSelectActivity.this).inflate(R.layout.group_item, null);
            TextView textView = viewElement.findViewById(R.id.group_item_nom);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

            textView.setText(nameGrps.get(i));
            textView.setTag(idGrps.get(i));

            layoutParams.setMargins(5,5,5,5);

            textView.setOnClickListener(view -> {
                Intent intent = new Intent();

                intent.putExtra(SELECT_ACTIVITY_RESULT, MAKE_SELECT_ACTIVITY_REQUEST);
                intent.putExtra(MAKE_SELECT_ACTIVITY_TYPE, MAKE_SELECT_ACTIVITY_GRP);
                intent.putExtra(MAKE_SELECT_ACTIVITY_RESULT, view.getTag().toString());

                setResult(RESULT_OK, intent);
                finish();
            });

            runOnUiThread(() -> mLayout.addView(viewElement, layoutParams));
        }
    }
    private void chargRsds() {
        TextView mTitle = binding.makeSelectActivityTitle;
        LinearLayout mLayout = binding.makeSelectActivityLyt;

        ArrayList<ListResidModel> nameRsds = SelectActivity.nameRsds;
        ArrayList<String> idRsds = SelectActivity.idRsds;

        mTitle.setText("SELECTIONNER UNE ENTREE");

        for( int i = 0; i < nameRsds.size(); i++ ) {
            View viewElement = LayoutInflater.from(MakeSelectActivity.this).inflate(R.layout.resid_item, null);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

            viewElement.setTag(idRsds.get(i));

            TextView textViewRef = viewElement.findViewById(R.id.fiche_search_resid_ref);
            textViewRef.setText(nameRsds.get(i).getRef());
            TextView textViewName = viewElement.findViewById(R.id.fiche_search_resid_name);
            textViewName.setText(nameRsds.get(i).getName());
            TextView textViewEntry = viewElement.findViewById(R.id.fiche_search_resid_entry);
            textViewEntry.setText(nameRsds.get(i).getEntry());
            TextView textViewAdr = viewElement.findViewById(R.id.fiche_search_resid_adr);
            textViewAdr.setText(nameRsds.get(i).getAdress());
            TextView textViewCity = viewElement.findViewById(R.id.fiche_search_resid_city);
            textViewCity.setText(nameRsds.get(i).getCity());
            TextView textViewLast = viewElement.findViewById(R.id.fiche_search_resid_last);
            textViewLast.setText(nameRsds.get(i).getLast());

            if( nameRsds.get(i).isVisited() ) {
                viewElement.setBackground(AppCompatResources.getDrawable(this, R.drawable.button_enabled));
            } else {
                viewElement.setBackground(AppCompatResources.getDrawable(this, R.drawable.button_standard));
            }

            layoutParams.setMargins(5,5,5,5);

            viewElement.setOnClickListener(view -> {
                Intent intent = new Intent();

                intent.putExtra(SELECT_ACTIVITY_RESULT, MAKE_SELECT_ACTIVITY_REQUEST);
                intent.putExtra(MAKE_SELECT_ACTIVITY_TYPE, MAKE_SELECT_ACTIVITY_RSD);
                intent.putExtra(MAKE_SELECT_ACTIVITY_RESULT, view.getTag().toString());

                setResult(RESULT_OK, intent);
                finish();
            });

            runOnUiThread(() -> mLayout.addView(viewElement, layoutParams));
        }
    }

    private void finishActivity() {
        setResult(MAKE_SELECT_ACTIVITY_REQUEST_CANCEL);
        finish();
    }

}
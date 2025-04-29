package org.orgaprop.controlprest.controllers.activities;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.orgaprop.controlprest.BuildConfig;
import org.orgaprop.controlprest.R;
import org.orgaprop.controlprest.databinding.ActivityNfsBinding;
import org.orgaprop.controlprest.services.NFCManager;
import org.orgaprop.controlprest.utils.ToastManager;

public class NfsActivity extends AppCompatActivity {

//********* PRIVATE VARIABLES

    private static final String TAG = "NfsActivity";
    private String rsdSelected;
    private String txtWrite;

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private IntentFilter[] intentFilters;
    private String[][] techLists;
    private boolean isTagWritten = false;
    private NFCManager nfcManager;
    private FirebaseCrashlytics crashlytics;
    private FirebaseAnalytics analytics;

//********* STATIC VARIABLES

    public static final String NFS_ACTIVITY_ADR_HTML = "www.orgaprop.org/cs/ControlPrest/index.php?ctrl_prest=";

    public static final String NFS_ACTIVITY_EXTRA = "rsd";

//********* WIDGETS

    private ActivityNfsBinding binding;

//********* CONSTRUCTORS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            crashlytics = FirebaseCrashlytics.getInstance();
            crashlytics.setCustomKey("deviceModel", Build.MODEL);
            crashlytics.setCustomKey("deviceManufacturer", Build.MANUFACTURER);
            crashlytics.setCustomKey("appVersion", BuildConfig.VERSION_NAME);
            crashlytics.log("NfsActivity démarrée");

            analytics = FirebaseAnalytics.getInstance(this);

            Bundle screenViewParams = new Bundle();
            screenViewParams.putString(FirebaseAnalytics.Param.SCREEN_NAME, "NfsActivity");
            screenViewParams.putString(FirebaseAnalytics.Param.SCREEN_CLASS, "NfsActivity");
            analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, screenViewParams);

            binding = ActivityNfsBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            Intent intent = getIntent();
            if (intent == null) {
                crashlytics.log("Intent reçu est null");
                Log.e(TAG, "Intent reçu est null");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "null_intent");
                errorParams.putString("class", "NfsActivity");
                analytics.logEvent("onCreate_intent", errorParams);

                showError(getString(R.string.erreur_d_initialisation));
                finish();
                return;
            }

            rsdSelected = intent.getStringExtra(NFS_ACTIVITY_EXTRA);
            if (rsdSelected == null || rsdSelected.isEmpty()) {
                crashlytics.log("La résidence sélectionnée est null ou vide");
                Log.e(TAG, "La résidence sélectionnée est null ou vide");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "null_rsd");
                errorParams.putString("class", "NfsActivity");
                analytics.logEvent("onCreate_rsd", errorParams);

                showError(getString(R.string.aucune_r_sidence_s_lectionn_e));
                finish();
                return;
            }

            txtWrite = NfsActivity.NFS_ACTIVITY_ADR_HTML + rsdSelected + "&clt=" + LoginActivity.id_client;

            crashlytics.setCustomKey("rsdSelected", rsdSelected);
            crashlytics.setCustomKey("txtWrite", txtWrite);

            if (LoginActivity.id_client != null && !LoginActivity.id_client.isEmpty()) {
                txtWrite += "&clt=" + LoginActivity.id_client;

                setupNfc();
                setupBackButton();
            } else {
                crashlytics.log("L'ID client est null ou vide");
                Log.w(TAG, "L'ID client est null ou vide");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "null_id_client");
                errorParams.putString("class", "NfsActivity");
                analytics.logEvent("onCreate_id_client", errorParams);

                showError(getString(R.string.l_id_client_est_null_ou_vide));
            }
        } catch (Exception e) {
            if (crashlytics != null) {
                crashlytics.recordException(e);
            }

            Log.e(TAG, "Erreur lors de l'initialisation de l'activité: " + e.getMessage(), e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "init_error");
            errorParams.putString("class", "NfsActivity");
            analytics.logEvent("onCreate_app_error", errorParams);

            showError(getString(R.string.erreur_d_initialisation));
            finish();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();

        try {
            crashlytics.log("onResume called");

            enableForegroundDispatch();
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur lors de l'activation du dispatch NFC: " + e.getMessage(), e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "resume_error");
            errorParams.putString("class", "NfsActivity");
            analytics.logEvent("onResume_app_error", errorParams);

            showError(getString(R.string.erreur_d_activation_nfc));
        }
    }
    @Override
    protected void onPause() {
        try {
            crashlytics.log("onPause called");

            disableForegroundDispatch();
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur lors de la désactivation du dispatch NFC: " + e.getMessage(), e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "pause_error");
            errorParams.putString("class", "NfsActivity");
            analytics.logEvent("onPause_app_error", errorParams);

            showError(getString(R.string.erreur_d_activation_nfc));
        } finally {
            super.onPause();
        }
    }

//********* SURCHARGE

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        try {
            crashlytics.log("onNewIntent called");

            handleIntent(intent);
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur lors du traitement de l'intent NFC: " + e.getMessage(), e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "new_intent_error");
            errorParams.putString("class", "NfsActivity");
            analytics.logEvent("onNewIntent_app_error", errorParams);

            showError(getString(R.string.erreur_lors_de_la_lecture_du_tag_nfc));
        }
    }

//********* PUBLIC FUNCTIONS

    public void nfsActivityActions(View v) {
        try {
            crashlytics.log("Action vue NFC");

            String viewTag = v.getTag() != null ? v.getTag().toString() : "";

            if (viewTag.equals("close")) {
                closeNfsActivity();
            } else {
                crashlytics.log("Tag d'action inconnu: " + viewTag);
                Log.w(TAG, "Tag d'action inconnu: " + viewTag);

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "unknown_action");
                errorParams.putString("class", "NfsActivity");
                analytics.logEvent("nfsActivityActions_unknown_action", errorParams);

                showError(getString(R.string.tag_d_action_inconnu));
            }
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur lors du traitement de l'action: " + e.getMessage(), e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "action_error");
            errorParams.putString("class", "NfsActivity");
            analytics.logEvent("nfsActivityActions_app_error", errorParams);

            showError(getString(R.string.erreur_lors_du_traitement_de_l_action));
        }
    }

//********* PRIVATE FUNCTIONS

    private void setupNfc() {
        try {
            crashlytics.log("setupNfc called");

            nfcAdapter = NfcAdapter.getDefaultAdapter(this);

            if (nfcAdapter == null) {
                crashlytics.log("L'adaptateur NFC est null");
                Log.e(TAG, "L'adaptateur NFC est null");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "null_nfc_adapter");
                errorParams.putString("class", "NfsActivity");
                analytics.logEvent("setupNfc_null_adapter", errorParams);

                showError(getString(R.string.mess_bad_nfc));
                finish();
                return;
            }

            if (!nfcAdapter.isEnabled()) {
                crashlytics.log("L'adaptateur NFC n'est pas activé");
                Log.e(TAG, "L'adaptateur NFC n'est pas activé");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "nfc_disabled");
                errorParams.putString("class", "NfsActivity");
                analytics.logEvent("setupNfc_nfc_disabled", errorParams);

                showError(getString(R.string.mess_bad_nfc));
                Intent nfcSettings = new Intent(android.provider.Settings.ACTION_NFC_SETTINGS);
                startActivity(nfcSettings);
                return;
            }

            nfcManager = new NFCManager(this);

            Intent nfcIntent = new Intent(this, getClass());
            nfcIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    ? PendingIntent.FLAG_MUTABLE
                    : PendingIntent.FLAG_IMMUTABLE;

            pendingIntent = PendingIntent.getActivity(this, 0, nfcIntent, flags);
            intentFilters = new IntentFilter[]{};
            techLists = new String[][]{{Ndef.class.getName()}, {NdefFormatable.class.getName()}};
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur lors de la configuration NFC: " + e.getMessage(), e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "config_error");
            errorParams.putString("class", "NfsActivity");
            analytics.logEvent("setupNfc_app_error", errorParams);

            showError(getString(R.string.erreur_lors_de_la_configuration_nfc));
            finish();
        }
    }

    private void setupBackButton() {
        OnBackPressedDispatcher dispatcher = getOnBackPressedDispatcher();
        dispatcher.addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                closeNfsActivity();
            }
        });
    }

    private void handleIntent(Intent intent) {
        crashlytics.log("Traitement d'un nouvel intent NFC");
        Log.d(TAG, "Traitement d'un nouvel intent NFC");

        LinearLayout mStartLayout = binding.nfsActivityStartLyt;
        LinearLayout mEndLayout = binding.nfsActivityEndLyt;
        TextView endText = binding.nfsActivityEndTxt;

        String action = intent.getAction();
        if (action == null) {
            crashlytics.log("L'action de l'intent est null");
            Log.w(TAG, "L'action de l'intent est null");

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "null_action");
            errorParams.putString("class", "NfsActivity");
            analytics.logEvent("handleIntent_null_action", errorParams);

            return;
        }

        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {

            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag == null) {
                crashlytics.log("Tag NFC non détecté");
                Log.e(TAG, "Tag NFC non détecté");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "null_tag");
                errorParams.putString("class", "NfsActivity");
                analytics.logEvent("handleIntent_null_tag", errorParams);

                showError("Tag NFC non détecté");
                return;
            }

            try {
                boolean success = writeTagData(tag);
                if (success) {
                    isTagWritten = true;

                    // Mettre à jour l'UI
                    runOnUiThread(() -> {
                        try {
                            mStartLayout.setVisibility(View.INVISIBLE);
                            endText.setText(R.string.txt_close_tag);
                            mEndLayout.setVisibility(View.VISIBLE);
                        } catch (Exception e) {
                            crashlytics.recordException(e);
                            Log.e(TAG, "Erreur lors de la mise à jour de l'UI après écriture: " + e.getMessage(), e);

                            Bundle errorParams = new Bundle();
                            errorParams.putString("error_type", "ui_update_error");
                            errorParams.putString("class", "NfsActivity");
                            analytics.logEvent("handleIntent_ui_update_error", errorParams);
                        }
                    });

                    ToastManager.showLong("Tag programmé avec succès");
                } else {
                    crashlytics.log("Erreur lors de l'écriture du tag");
                    Log.e(TAG, "Erreur lors de l'écriture du tag");

                    Bundle errorParams = new Bundle();
                    errorParams.putString("error_type", "write_error");
                    errorParams.putString("class", "NfsActivity");
                    analytics.logEvent("handleIntent_write_error", errorParams);

                    showError(getString(R.string.chec_de_l_criture_du_tag));
                }
            } catch (NFCManager.NFCTagReadOnly e) {
                crashlytics.log("Le tag est en lecture seule");
                Log.e(TAG, "Le tag est en lecture seule", e);

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "read_only_tag");
                errorParams.putString("class", "NfsActivity");
                analytics.logEvent("handleIntent_read_only_tag", errorParams);

                showError(getString(R.string.ce_tag_est_en_lecture_seule_et_ne_peut_pas_tre_modifi));
            } catch (NFCManager.NFCTagCapacityExceeded e) {
                crashlytics.log("La capacité du tag est dépassée");
                Log.e(TAG, "La capacité du tag est dépassée", e);

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "capacity_exceeded");
                errorParams.putString("class", "NfsActivity");
                errorParams.putString("error_message", e.getMessage());
                analytics.logEvent("handleIntent_capacity_exceeded", errorParams);

                showError(getString(R.string.ce_tag_est_trop_petit_pour_contenir_les_donn_es));
            } catch (IOException e) {
                crashlytics.log("Erreur d'E/S lors de l'écriture du tag");
                Log.e(TAG, "Erreur d'E/S lors de l'écriture du tag", e);

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "io_error");
                errorParams.putString("class", "NfsActivity");
                errorParams.putString("error_message", e.getMessage());
                analytics.logEvent("handleIntent_io_error", errorParams);

                showError(getString(R.string.erreur_lors_de_l_criture_du_tag_veuillez_r_essayer));
            } catch (FormatException e) {
                crashlytics.log("Erreur de format lors de l'écriture du tag");
                Log.e(TAG, "Erreur de format lors de l'écriture du tag", e);

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "format_error");
                errorParams.putString("class", "NfsActivity");
                errorParams.putString("error_message", e.getMessage());
                analytics.logEvent("handleIntent_format_error", errorParams);

                showError(getString(R.string.format_de_tag_non_compatible));
            } catch (Exception e) {
                crashlytics.log("Erreur inattendue lors de l'écriture du tag: " + e.getMessage());
                Log.e(TAG, "Erreur inattendue lors de l'écriture du tag: " + e.getMessage(), e);

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "unexpected_write_error");
                errorParams.putString("class", "NfsActivity");
                errorParams.putString("error_message", e.getMessage());
                analytics.logEvent("handleIntent_unexpected_write_error", errorParams);

                showError(getString(R.string.erreur_inattendue_lors_de_l_criture_du_tag));
            }
        } else {
            crashlytics.log("Action non reconnue: " + action);
            Log.w(TAG, "Action non reconnue: " + action);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "unknown_action");
            errorParams.putString("class", "NfsActivity");
            analytics.logEvent("handleIntent_unknown_action", errorParams);
        }
    }
    /**
     * Écrit les données sur le tag NFC
     * @param tag Tag NFC à écrire
     * @return true si l'écriture a réussi
     */
    private boolean writeTagData(Tag tag) throws IOException, FormatException,
            NFCManager.NFCTagReadOnly, NFCManager.NFCTagCapacityExceeded {
        crashlytics.log("writeTagData called");

        if (tag == null) {
            crashlytics.log("Le tag est null");
            Log.e(TAG, "Le tag est null");

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "null_tag");
            errorParams.putString("class", "NfsActivity");
            analytics.logEvent("writeTagData_null_tag", errorParams);

            return false;
        }

        if (txtWrite == null || txtWrite.isEmpty()) {
            crashlytics.log("Rien à écrire sur le tag");
            Log.e(TAG, "Rien à écrire sur le tag");

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "empty_txt");
            errorParams.putString("class", "NfsActivity");
            analytics.logEvent("writeTagData_empty_txt", errorParams);

            return false;
        }

        // Créer l'URI Record
        NdefRecord uriRecord = createUriRecord(txtWrite);
        NdefMessage ndefMessage = new NdefMessage(new NdefRecord[] { uriRecord });

        // Utiliser NFCManager pour l'écriture
        if (nfcManager != null) {
            return nfcManager.writeTag(tag, ndefMessage, false);
        } else {
            // Fallback si NFCManager n'est pas disponible
            crashlytics.log("NFCManager non disponible, utilisation de writeNdefMessage");
            Log.d(TAG, "NFCManager non disponible, utilisation de writeNdefMessage");

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "nfc_manager_not_available");
            errorParams.putString("class", "NfsActivity");
            analytics.logEvent("writeTagData_nfc_manager_not_available", errorParams);

            return writeNdefMessage(tag, ndefMessage);
        }
    }
    /**
     * Crée un enregistrement URI pour le NFC
     */
    private NdefRecord createUriRecord(String message) {
        byte[] uriBytes = message.getBytes(StandardCharsets.UTF_8);
        byte[] recordPayload = new byte[uriBytes.length + 1];
        recordPayload[0] = 0x04; // Préfixe -- 0x04 = "https://" -- 0x02 = "https://www." -- 0x01 = "http://www."
        System.arraycopy(uriBytes, 0, recordPayload, 1, uriBytes.length);

        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_URI, new byte[0], recordPayload);
    }
    /**
     * Méthode de secours pour écrire le message NDEF
     */
    private boolean writeNdefMessage(Tag tag, NdefMessage ndefMessage)
            throws IOException, FormatException, NFCManager.NFCTagReadOnly {

        Ndef ndef = Ndef.get(tag);

        try {
            if (ndef != null) {
                ndef.connect();

                if (!ndef.isWritable()) {
                    crashlytics.log("Le tag est en lecture seule");
                    Log.e(TAG, "Le tag est en lecture seule");

                    Bundle errorParams = new Bundle();
                    errorParams.putString("error_type", "read_only_tag");
                    errorParams.putString("class", "NfsActivity");
                    analytics.logEvent("writeNdefMessage_read_only_tag", errorParams);

                    throw new NFCManager.NFCTagReadOnly();
                }

                if (ndef.getMaxSize() < ndefMessage.getByteArrayLength()) {
                    crashlytics.log("La capacité du tag est dépassée");
                    Log.e(TAG, "La capacité du tag est dépassée");

                    Bundle errorParams = new Bundle();
                    errorParams.putString("error_type", "capacity_exceeded");
                    errorParams.putString("class", "NfsActivity");
                    analytics.logEvent("writeNdefMessage_capacity_exceeded", errorParams);

                    throw new NFCManager.NFCTagCapacityExceeded();
                }

                ndef.writeNdefMessage(ndefMessage);
                return true;
            } else {
                NdefFormatable ndefFormatable = NdefFormatable.get(tag);
                if (ndefFormatable != null) {
                    try {
                        ndefFormatable.connect();
                        ndefFormatable.format(ndefMessage);
                        return true;
                    } finally {
                        ndefFormatable.close();
                    }
                }
            }
        } catch (NFCManager.NFCTagCapacityExceeded e) {
            crashlytics.log("La capacité du tag est dépassée");
            Log.e(TAG, "La capacité du tag est dépassée", e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "capacity_exceeded");
            errorParams.putString("class", "NfsActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("writeNdefMessage_capacity_exceeded", errorParams);

            throw new RuntimeException(e);
        } finally {
            if (ndef != null && ndef.isConnected()) {
                ndef.close();
            }
        }

        return false;
    }
    /**
     * Active l'interception des tags NFC
     */
    private void enableForegroundDispatch() {
        try {
            crashlytics.log("enableForegroundDispatch called");

            if (nfcAdapter != null && pendingIntent != null) {
                nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, techLists);
                crashlytics.log("NFC Foreground Dispatch activé");
                Log.d(TAG, "NFC Foreground Dispatch activé");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "success");
                errorParams.putString("class", "NfsActivity");
                analytics.logEvent("enableForegroundDispatch_success", errorParams);
            }
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur lors de l'activation du Foreground Dispatch: " + e.getMessage(), e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "enable_error");
            errorParams.putString("class", "NfsActivity");
            analytics.logEvent("enableForegroundDispatch_app_error", errorParams);
        }
    }
    /**
     * Désactive l'interception des tags NFC
     */
    private void disableForegroundDispatch() {
        try {
            crashlytics.log("disableForegroundDispatch called");

            if (nfcAdapter != null) {
                nfcAdapter.disableForegroundDispatch(this);
                crashlytics.log("NFC Foreground Dispatch désactivé");
                Log.d(TAG, "NFC Foreground Dispatch désactivé");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "success");
                errorParams.putString("class", "NfsActivity");
                analytics.logEvent("disableForegroundDispatch_success", errorParams);
            }
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur lors de la désactivation du Foreground Dispatch: " + e.getMessage(), e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "disable_error");
            errorParams.putString("class", "NfsActivity");
            analytics.logEvent("disableForegroundDispatch_app_error", errorParams);
        }
    }
    /**
     * Ferme l'activité en nettoyant les ressources
     */
    private void closeNfsActivity() {
        try {
            crashlytics.log("closeNfsActivity called");

            disableForegroundDispatch();
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Erreur lors de la fermeture de l'activité: " + e.getMessage(), e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "close_error");
            errorParams.putString("class", "NfsActivity");
            analytics.logEvent("closeNfsActivity_app_error", errorParams);
        } finally {
            finish();
        }
    }

    /**
     * Affiche un message d'erreur à l'utilisateur
     */
    private void showError(String message) {
        crashlytics.log("showError called");

        if (message == null || message.isEmpty()) {
            message = "Une erreur est survenue";
        }

        try {
            crashlytics.log("Erreur affichée: " + message);
            Log.e(TAG, "Erreur affichée: " + message);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "show_error");
            errorParams.putString("class", "NfsActivity");
            errorParams.putString("error_message", message);
            analytics.logEvent("showError_app_error", errorParams);

            ToastManager.showError(message);
        } catch (Exception e) {
            crashlytics.recordException(e);
            Log.e(TAG, "Impossible d'afficher le message d'erreur: " + e.getMessage(), e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "show_error_error");
            errorParams.putString("class", "NfsActivity");
            errorParams.putString("error_message", e.getMessage());
            analytics.logEvent("showError_app_error", errorParams);
        }
    }

}

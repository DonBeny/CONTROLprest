package org.orgaprop.controlprest.controllers.activities;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.appcompat.app.AppCompatActivity;

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
import android.widget.Toast;

import org.orgaprop.controlprest.R;
import org.orgaprop.controlprest.databinding.ActivityNfsBinding;
import org.orgaprop.controlprest.services.NFCManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

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
            binding = ActivityNfsBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            Intent intent = getIntent();
            if (intent == null) {
                Log.e(TAG, "Intent reçu est null");
                showError("Erreur d'initialisation");
                finish();
                return;
            }

            rsdSelected = intent.getStringExtra(NFS_ACTIVITY_EXTRA);
            if (rsdSelected == null || rsdSelected.isEmpty()) {
                Log.e(TAG, "La résidence sélectionnée est null ou vide");
                showError("Aucune résidence sélectionnée");
                finish();
                return;
            }

            txtWrite = NfsActivity.NFS_ACTIVITY_ADR_HTML + rsdSelected + "&clt=" + LoginActivity.id_client;

            if (LoginActivity.id_client != null && !LoginActivity.id_client.isEmpty()) {
                txtWrite += "&clt=" + LoginActivity.id_client;

                setupNfc();
                setupBackButton();
            } else {
                Log.w(TAG, "L'ID client est null ou vide");
                showError("L'ID client est null ou vide");
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'initialisation de l'activité: " + e.getMessage(), e);
            showError("Erreur d'initialisation");
            finish();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();

        try {
            enableForegroundDispatch();
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'activation du dispatch NFC: " + e.getMessage(), e);
            showError("Erreur d'activation NFC");
        }
    }
    @Override
    protected void onPause() {
        try {
            disableForegroundDispatch();
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la désactivation du dispatch NFC: " + e.getMessage(), e);
            showError("Erreur de désactivation NFC");
        } finally {
            super.onPause();
        }
    }

//********* SURCHARGE

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        try {
            handleIntent(intent);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du traitement de l'intent NFC: " + e.getMessage(), e);
            showError("Erreur lors de la lecture du tag NFC");
        }
    }

//********* PUBLIC FUNCTIONS

    public void nfsActivityActions(View v) {
        try {
            String viewTag = v.getTag() != null ? v.getTag().toString() : "";

            if (viewTag.equals("close")) {
                closeNfsActivity();
            } else {
                Log.w(TAG, "Tag d'action inconnu: " + viewTag);
                showError("Tag d'action inconnu");
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du traitement de l'action: " + e.getMessage(), e);
            showError("Erreur lors du traitement de l'action");
        }
    }

//********* PRIVATE FUNCTIONS

    private void setupNfc() {
        try {
            nfcAdapter = NfcAdapter.getDefaultAdapter(this);

            if (nfcAdapter == null) {
                Log.e(TAG, "L'adaptateur NFC est null");
                showError(getString(R.string.mess_bad_nfc));
                finish();
                return;
            }

            if (!nfcAdapter.isEnabled()) {
                Log.e(TAG, "L'adaptateur NFC n'est pas activé");
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
            Log.e(TAG, "Erreur lors de la configuration NFC: " + e.getMessage(), e);
            showError("Erreur lors de la configuration NFC");
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
        Log.d(TAG, "Traitement d'un nouvel intent NFC");

        LinearLayout mStartLayout = binding.nfsActivityStartLyt;
        LinearLayout mEndLayout = binding.nfsActivityEndLyt;
        TextView endText = binding.nfsActivityEndTxt;

        String action = intent.getAction();
        if (action == null) {
            Log.w(TAG, "L'action de l'intent est null");
            return;
        }

        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {

            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag == null) {
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
                            Log.e(TAG, "Erreur lors de la mise à jour de l'UI après écriture: " + e.getMessage(), e);
                        }
                    });

                    Toast.makeText(this, "Tag programmé avec succès", Toast.LENGTH_SHORT).show();
                } else {
                    showError("Échec de l'écriture du tag");
                }
            } catch (NFCManager.NFCTagReadOnly e) {
                Log.e(TAG, "Le tag est en lecture seule", e);
                showError("Ce tag est en lecture seule et ne peut pas être modifié");
            } catch (NFCManager.NFCTagCapacityExceeded e) {
                Log.e(TAG, "La capacité du tag est dépassée", e);
                showError("Ce tag est trop petit pour contenir les données");
            } catch (IOException e) {
                Log.e(TAG, "Erreur d'E/S lors de l'écriture du tag", e);
                showError("Erreur lors de l'écriture du tag. Veuillez réessayer.");
            } catch (FormatException e) {
                Log.e(TAG, "Erreur de format lors de l'écriture du tag", e);
                showError("Format de tag non compatible");
            } catch (Exception e) {
                Log.e(TAG, "Erreur inattendue lors de l'écriture du tag: " + e.getMessage(), e);
                showError("Erreur inattendue lors de l'écriture du tag");
            }
        } else {
            Log.w(TAG, "Action non reconnue: " + action);
        }
    }
    /**
     * Écrit les données sur le tag NFC
     * @param tag Tag NFC à écrire
     * @return true si l'écriture a réussi
     */
    private boolean writeTagData(Tag tag) throws IOException, FormatException,
            NFCManager.NFCTagReadOnly, NFCManager.NFCTagCapacityExceeded {

        if (tag == null) {
            Log.e(TAG, "Le tag est null");
            return false;
        }

        if (txtWrite == null || txtWrite.isEmpty()) {
            Log.e(TAG, "Rien à écrire sur le tag");
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
                    throw new NFCManager.NFCTagReadOnly();
                }

                if (ndef.getMaxSize() < ndefMessage.getByteArrayLength()) {
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
            if (nfcAdapter != null && pendingIntent != null) {
                nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, techLists);
                Log.d(TAG, "NFC Foreground Dispatch activé");
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'activation du Foreground Dispatch: " + e.getMessage(), e);
        }
    }
    /**
     * Désactive l'interception des tags NFC
     */
    private void disableForegroundDispatch() {
        try {
            if (nfcAdapter != null) {
                nfcAdapter.disableForegroundDispatch(this);
                Log.d(TAG, "NFC Foreground Dispatch désactivé");
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la désactivation du Foreground Dispatch: " + e.getMessage(), e);
        }
    }
    /**
     * Ferme l'activité en nettoyant les ressources
     */
    private void closeNfsActivity() {
        try {
            disableForegroundDispatch();
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la fermeture de l'activité: " + e.getMessage(), e);
        } finally {
            finish();
        }
    }

    /**
     * Affiche un message d'erreur à l'utilisateur
     */
    private void showError(String message) {
        if (message == null || message.isEmpty()) {
            message = "Une erreur est survenue";
        }

        try {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            Log.e(TAG, "Erreur affichée: " + message);
        } catch (Exception e) {
            Log.e(TAG, "Impossible d'afficher le message d'erreur: " + e.getMessage(), e);
        }
    }

}
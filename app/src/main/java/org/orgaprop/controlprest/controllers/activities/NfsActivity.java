package org.orgaprop.controlprest.controllers.activities;

import android.app.AlertDialog;
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
    private boolean isWritingTag = false;
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

            analytics = FirebaseAnalytics.getInstance(this);
            logInfo("NfsActivity started", "activity_start");

            Bundle screenViewParams = new Bundle();
            screenViewParams.putString(FirebaseAnalytics.Param.SCREEN_NAME, "NfsActivity");
            screenViewParams.putString(FirebaseAnalytics.Param.SCREEN_CLASS, "NfsActivity");
            analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, screenViewParams);

            binding = ActivityNfsBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            validateInputParameters();
        } catch (Exception e) {
            logException(e, "init_error", "Error during initialization");
            showErrorAndFinish(getString(R.string.erreur_d_initialisation));
        }
    }
    @Override
    protected void onResume() {
        super.onResume();

        try {
            logInfo("onResume called", "lifecycle");
            enableForegroundDispatch();
        } catch (Exception e) {
            logException(e, "resume_error", "Error in onResume");
            showError(getString(R.string.erreur_d_activation_nfc));
        }
    }
    @Override
    protected void onPause() {
        try {
            logInfo("onPause called", "lifecycle");
            disableForegroundDispatch();
        } catch (Exception e) {
            logException(e, "pause_error", "Error in onPause");
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
            logInfo("onNewIntent called", "new_intent");
            if (isWritingTag) {
                logWarning("Tag writing already in progress", "writing_in_progress");
                ToastManager.showShort("Tag writing in progress, please wait");
                return;
            }
            handleIntent(intent);
        } catch (Exception e) {
            logException(e, "new_intent_error", "Error handling NFC intent");
            showError(getString(R.string.erreur_lors_de_la_lecture_du_tag_nfc));
        }
    }

//********* PUBLIC FUNCTIONS

    public void nfsActivityActions(View v) {
        try {
            logInfo("NFC activity action triggered", "ui_action");

            if (v == null || v.getTag() == null) {
                logWarning("View or tag is null", "null_view_tag");
                showError(getString(R.string.erreur_lors_du_traitement_de_l_action));
                return;
            }

            String viewTag = v.getTag().toString();

            if (viewTag.equals("close")) {
                if (isWritingTag) {
                    confirmCloseWhileWriting();
                } else {
                    closeNfsActivity();
                }
            } else {
                logWarning("Unknown action tag: " + viewTag, "unknown_action");
                showError(getString(R.string.tag_d_action_inconnu));
            }
        } catch (Exception e) {
            logException(e, "action_error", "Error processing tag action");
            showError(getString(R.string.erreur_lors_du_traitement_de_l_action));
        }
    }

//********* PRIVATE FUNCTIONS

    /**
     * Validates the input parameters and initializes the NFC components
     */
    private void validateInputParameters() {
        Intent intent = getIntent();
        if (intent == null) {
            logError("Intent is null", "null_intent");
            showErrorAndFinish(getString(R.string.erreur_d_initialisation));
            return;
        }

        rsdSelected = intent.getStringExtra(NFS_ACTIVITY_EXTRA);
        if (rsdSelected == null || rsdSelected.isEmpty()) {
            logError("Selected residence is null or empty", "null_rsd");
            showErrorAndFinish(getString(R.string.aucune_r_sidence_s_lectionn_e));
            return;
        }

        // Build the URL string properly without duplication
        StringBuilder urlBuilder = new StringBuilder(NFS_ACTIVITY_ADR_HTML);
        urlBuilder.append(rsdSelected);

        if (LoginActivity.id_client != null && !LoginActivity.id_client.isEmpty()) {
            urlBuilder.append("&clt=").append(LoginActivity.id_client);
            txtWrite = urlBuilder.toString();

            logInfo("URL to write: " + txtWrite, "url_created");
            crashlytics.setCustomKey("rsdSelected", rsdSelected);
            crashlytics.setCustomKey("txtWrite", txtWrite);

            setupNfc();
            setupBackButton();
        } else {
            logError("Client ID is null or empty", "null_client_id");
            showErrorAndFinish(getString(R.string.l_id_client_est_null_ou_vide));
        }
    }

    /**
     * Sets up the NFC adapter and related components
     */
    private void setupNfc() {
        try {
            logInfo("Setting up NFC", "nfc_setup");

            nfcAdapter = NfcAdapter.getDefaultAdapter(this);

            if (nfcAdapter == null) {
                logError("NFC adapter is null", "null_nfc_adapter");
                showErrorAndFinish(getString(R.string.mess_bad_nfc));
                return;
            }

            if (!nfcAdapter.isEnabled()) {
                logError("NFC adapter is not enabled", "nfc_disabled");

                // Show a dialog asking the user to enable NFC instead of immediately finishing
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("NFC Disabled")
                        .setMessage(getString(R.string.mess_bad_nfc))
                        .setCancelable(false)
                        .setPositiveButton("Enable NFC", (dialog, which) -> {
                            try {
                                Intent nfcSettings = new Intent(android.provider.Settings.ACTION_NFC_SETTINGS);
                                startActivity(nfcSettings);
                            } catch (Exception e) {
                                logException(e, "nfc_settings_error", "Error opening NFC settings");
                                showErrorAndFinish("Unable to open NFC settings");
                            }
                        })
                        .setNegativeButton("Close", (dialog, which) -> finish());

                builder.create().show();
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

            logInfo("NFC setup completed successfully", "nfc_setup_success");
        } catch (Exception e) {
            logException(e, "nfc_setup_error", "Error setting up NFC");
            showErrorAndFinish(getString(R.string.erreur_lors_de_la_configuration_nfc));
        }
    }

    /**
     * Sets up the back button behavior with confirmation
     */
    private void setupBackButton() {
        OnBackPressedDispatcher dispatcher = getOnBackPressedDispatcher();
        dispatcher.addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isWritingTag) {
                    confirmCloseWhileWriting();
                } else if (isTagWritten) {
                    closeNfsActivity();
                } else {
                    confirmClose();
                }
            }
        });
    }

    /**
     * Show confirmation dialog before closing if writing is in progress
     */
    private void confirmCloseWhileWriting() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Writing in Progress")
                .setMessage("Writing to NFC tag is in progress. Are you sure you want to cancel?")
                .setPositiveButton("Yes", (dialog, which) -> closeNfsActivity())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    /**
     * Show confirmation dialog before closing if no tag has been written
     */
    private void confirmClose() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Close")
                .setMessage("No tag has been written yet. Are you sure you want to exit?")
                .setPositiveButton("Yes", (dialog, which) -> closeNfsActivity())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    /**
     * Handles incoming NFC intents
     */
    private void handleIntent(Intent intent) {
        logInfo("Processing new NFC intent", "handle_intent");

        LinearLayout mStartLayout = binding.nfsActivityStartLyt;
        LinearLayout mEndLayout = binding.nfsActivityEndLyt;
        TextView endText = binding.nfsActivityEndTxt;

        String action = intent.getAction();
        if (action == null) {
            logWarning("Intent action is null", "null_action");
            return;
        }

        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {

            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag == null) {
                logError("NFC tag not detected", "null_tag");
                showError("NFC tag not detected");
                return;
            }

            // Check if txtWrite is valid before proceeding
            if (txtWrite == null || txtWrite.isEmpty()) {
                logError("Nothing to write to tag", "empty_content");
                showError("Nothing to write to tag");
                return;
            }

            isWritingTag = true;

            try {
                // Create the URI Record first and then write it
                NdefRecord uriRecord = createUriRecord(txtWrite);
                NdefMessage ndefMessage = new NdefMessage(new NdefRecord[] { uriRecord });

                boolean success = writeTag(tag, ndefMessage);

                if (success) {
                    isTagWritten = true;
                    isWritingTag = false;

                    // Update UI
                    runOnUiThread(() -> {
                        try {
                            mStartLayout.setVisibility(View.INVISIBLE);
                            endText.setText(R.string.txt_close_tag);
                            mEndLayout.setVisibility(View.VISIBLE);
                        } catch (Exception e) {
                            logException(e, "ui_update_error", "Error updating UI after writing");
                        }
                    });

                    ToastManager.showLong("Tag programmed successfully");
                } else {
                    isWritingTag = false;
                    logError("Failed to write tag", "write_error");
                    showError(getString(R.string.chec_de_l_criture_du_tag));
                }
            } catch (NFCManager.NFCTagReadOnly e) {
                isWritingTag = false;
                logError("Tag is read-only", "read_only_tag");
                showError(getString(R.string.ce_tag_est_en_lecture_seule_et_ne_peut_pas_tre_modifi));
            } catch (NFCManager.NFCTagCapacityExceeded e) {
                isWritingTag = false;
                logError("Tag capacity exceeded", "capacity_exceeded");
                showError(getString(R.string.ce_tag_est_trop_petit_pour_contenir_les_donn_es));
            } catch (NFCManager.NFCOperationTimeout e) {
                isWritingTag = false;
                logError("NFC operation timed out", "nfc_timeout");
                showError("The NFC operation timed out. Please try again.");
            } catch (IOException e) {
                isWritingTag = false;
                logException(e, "io_error", "I/O error writing tag");
                showError(getString(R.string.erreur_lors_de_l_criture_du_tag_veuillez_r_essayer));
            } catch (FormatException e) {
                isWritingTag = false;
                logException(e, "format_error", "Format error writing tag");
                showError(getString(R.string.format_de_tag_non_compatible));
            } catch (Exception e) {
                isWritingTag = false;
                logException(e, "unexpected_write_error", "Unexpected error writing tag");
                showError(getString(R.string.erreur_inattendue_lors_de_l_criture_du_tag));
            }
        } else {
            logWarning("Unrecognized action: " + action, "unknown_action");
        }
    }

    /**
     * Write data to the NFC tag
     * Unified method that handles both Ndef and NdefFormatable tags
     */
    private boolean writeTag(Tag tag, NdefMessage ndefMessage)
            throws IOException, FormatException, NFCManager.NFCTagReadOnly,
            NFCManager.NFCTagCapacityExceeded, NFCManager.NFCOperationTimeout {

        if (tag == null) {
            logError("Tag is null", "null_tag");
            return false;
        }

        if (ndefMessage == null) {
            logError("Nothing to write", "null_message");
            return false;
        }

        // Use NFCManager if available (preferred approach)
        if (nfcManager != null) {
            logInfo("Using NFCManager to write tag", "using_nfc_manager");
            return nfcManager.writeTag(tag, ndefMessage, false);
        } else {
            // This is a fallback but shouldn't normally be reached
            logWarning("NFCManager not available, using direct write method", "nfc_manager_unavailable");

            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                return writeNdefTag(ndef, ndefMessage);
            } else {
                return formatAndWriteTag(tag, ndefMessage);
            }
        }
    }

    /**
     * Write to an already NDEF formatted tag
     */
    private boolean writeNdefTag(Ndef ndef, NdefMessage ndefMessage)
            throws IOException, FormatException, NFCManager.NFCTagReadOnly, NFCManager.NFCTagCapacityExceeded {

        try {
            ndef.connect();

            if (!ndef.isWritable()) {
                logError("Tag is read-only", "read_only_tag");
                throw new NFCManager.NFCTagReadOnly();
            }

            if (ndef.getMaxSize() < ndefMessage.getByteArrayLength()) {
                logError("Tag capacity exceeded", "capacity_exceeded");
                throw new NFCManager.NFCTagCapacityExceeded();
            }

            ndef.writeNdefMessage(ndefMessage);
            logInfo("Successfully wrote to NDEF tag", "ndef_write_success");
            return true;
        } finally {
            try {
                if (ndef.isConnected()) {
                    ndef.close();
                }
            } catch (Exception e) {
                logException(e, "close_error", "Error closing NDEF connection");
            }
        }
    }

    /**
     * Format and write to an unformatted tag
     */
    private boolean formatAndWriteTag(Tag tag, NdefMessage ndefMessage)
            throws IOException, FormatException {

        NdefFormatable format = NdefFormatable.get(tag);
        if (format == null) {
            logError("Tag cannot be formatted to NDEF", "not_formattable");
            return false;
        }

        try {
            format.connect();
            format.format(ndefMessage);
            logInfo("Successfully formatted and wrote to tag", "format_write_success");
            return true;
        } finally {
            try {
                format.close();
            } catch (Exception e) {
                logException(e, "close_error", "Error closing NdefFormatable connection");
            }
        }
    }

    /**
     * Crée un enregistrement URI pour le NFC
     */
    private NdefRecord createUriRecord(String message) {
        byte[] uriBytes = message.getBytes(StandardCharsets.UTF_8);
        byte[] recordPayload = new byte[uriBytes.length + 1];
        recordPayload[0] = 0x04; // Prefix -- 0x04 = "https://" -- 0x02 = "https://www." -- 0x01 = "http://www."
        System.arraycopy(uriBytes, 0, recordPayload, 1, uriBytes.length);

        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_URI, new byte[0], recordPayload);
    }

    /**
     * Active l'interception des tags NFC
     */
    private void enableForegroundDispatch() {
        try {
            logInfo("Enabling foreground dispatch", "enable_dispatch");

            if (nfcAdapter != null && pendingIntent != null) {
                nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, techLists);
                logInfo("NFC Foreground Dispatch enabled", "dispatch_enabled");
            }
        } catch (Exception e) {
            logException(e, "enable_dispatch_error", "Error enabling foreground dispatch");
        }
    }

    /**
     * Désactive l'interception des tags NFC
     */
    private void disableForegroundDispatch() {
        try {
            logInfo("Disabling foreground dispatch", "disable_dispatch");

            if (nfcAdapter != null) {
                nfcAdapter.disableForegroundDispatch(this);
                logInfo("NFC Foreground Dispatch disabled", "dispatch_disabled");
            }
        } catch (Exception e) {
            logException(e, "disable_dispatch_error", "Error disabling foreground dispatch");
        }
    }

    /**
     * Ferme l'activité en nettoyant les ressources
     */
    private void closeNfsActivity() {
        try {
            logInfo("Closing NfsActivity", "close_activity");
            disableForegroundDispatch();
        } catch (Exception e) {
            logException(e, "close_error", "Error closing activity");
        } finally {
            finish();
        }
    }

    /**
     * Show error and finish activity
     */
    private void showErrorAndFinish(String message) {
        showError(message);
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::finish, 2000);
    }

    /**
     * Display an error message to the user
     */
    private void showError(String message) {
        if (message == null || message.isEmpty()) {
            message = "An error occurred";
        }

        try {
            logError("Error displayed: " + message, "show_error");
            ToastManager.showError(message);
        } catch (Exception e) {
            logException(e, "show_error_error", "Error displaying error message");
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

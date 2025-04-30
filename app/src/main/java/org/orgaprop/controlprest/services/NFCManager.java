package org.orgaprop.controlprest.services;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.orgaprop.controlprest.R;



public class NFCManager {

//********* PRIVATE VARIABLES

    private static final String TAG = "NFCManager";
    private final Activity activity;
    private NfcAdapter nfcAdpt;
    private FirebaseCrashlytics crashlytics;
    private FirebaseAnalytics analytics;

    private static final int NFC_OPERATION_TIMEOUT = 10000;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public interface NFCCallback {
        void onSuccess(String message);
        void onError(String errorMessage);
    }

    private final NFCCallback defaultCallback = new NFCCallback() {
        @Override
        public void onSuccess(String message) {
            logInfo(message, "nfc_success");
        }

        @Override
        public void onError(String errorMessage) {
            logError(errorMessage, "nfc_error");
        }
    };

//********* PUBLIC CLASSES

    public static class NFCNotSupported extends Exception {
        public NFCNotSupported() {
            super();
        }
    }
    public static class NFCNotEnabled extends Exception {
        public NFCNotEnabled() {
            super();
        }
    }
    public static class NFCTagReadOnly extends Exception {
        public NFCTagReadOnly() {
            super();
        }
    }
    public static class NFCTagCapacityExceeded extends Exception {
        public NFCTagCapacityExceeded() {
            super();
        }
    }
    public static class NFCOperationTimeout extends Exception {
        public NFCOperationTimeout() {
            super();
        }
    }

//********* CONSTRUCTORS

    public NFCManager(Activity activity) {
        if (activity == null) {
            throw new IllegalArgumentException("Activity cannot be null");
        }

        this.activity = activity;

        try {
            // Initialiser Crashlytics et Analytics une seule fois
            this.crashlytics = FirebaseCrashlytics.getInstance();
            this.analytics = FirebaseAnalytics.getInstance(activity);

            logInfo("NFCManager initialized", "init");

            // Ajouter des données de contexte utiles
            crashlytics.setCustomKey("deviceModel", Build.MODEL);
            crashlytics.setCustomKey("deviceManufacturer", Build.MANUFACTURER);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing NFCManager", e);
            FirebaseCrashlytics.getInstance().recordException(e);
            throw new RuntimeException("Failed to initialize NFCManager", e);
        }
    }

//********* PUBLIC FUNCTIONS

    /**
     * Vérifie si NFC est supporté et activé sur le périphérique
     * @throws NFCNotSupported si NFC n'est pas supporté
     * @throws NFCNotEnabled si NFC est supporté mais non activé
     */
    public void verifyNFC() throws NFCNotSupported, NFCNotEnabled {
        logInfo("Verifying NFC availability", "nfc_verify");

        nfcAdpt = NfcAdapter.getDefaultAdapter(activity);

        if (nfcAdpt == null) {
            logError("NFC not supported on this device", "nfc_not_supported");
            throw new NFCNotSupported();
        }

        if (!nfcAdpt.isEnabled()) {
            logError("NFC is disabled on this device", "nfc_not_enabled");
            throw new NFCNotEnabled();
        }

        logInfo("NFC verified successfully", "nfc_verify_success");
    }

    /**
     * Active l'expédition NFC en premier plan pour l'activité
     */
    public void enableDispatch(NFCCallback callback) {
        NFCCallback effectiveCallback = callback != null ? callback : defaultCallback;

        logInfo("Enabling NFC foreground dispatch", "nfc_enable_dispatch");

        if (nfcAdpt == null) {
            try {
                nfcAdpt = NfcAdapter.getDefaultAdapter(activity);
                if (nfcAdpt == null) {
                    String errorMsg = activity.getString(R.string.mess_bad_nfc);
                    logError("NFC adapter is null when enabling dispatch", "null_adapter");
                    effectiveCallback.onError(errorMsg);
                    return;
                }
            } catch (Exception e) {
                logException(e, "nfc_init_error", "Error initializing NFC adapter");
                effectiveCallback.onError(activity.getString(R.string.erreur_d_activation_nfc));
                return;
            }
        }

        try {
            Intent nfcIntent = new Intent(activity, activity.getClass());
            nfcIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    ? PendingIntent.FLAG_MUTABLE
                    : PendingIntent.FLAG_UPDATE_CURRENT;

            PendingIntent pendingIntent = PendingIntent.getActivity(activity, 0, nfcIntent, flags);
            IntentFilter[] intentFiltersArray = new IntentFilter[] {};
            String[][] techList = new String[][] {
                    { android.nfc.tech.Ndef.class.getName() },
                    { android.nfc.tech.NdefFormatable.class.getName() }
            };

            nfcAdpt.enableForegroundDispatch(activity, pendingIntent, intentFiltersArray, techList);
            logInfo("NFC Foreground Dispatch enabled", "nfc_dispatch_enabled");
            effectiveCallback.onSuccess("NFC ready");
        } catch (Exception e) {
            logException(e, "enable_dispatch_error", "Error enabling NFC dispatch");
            effectiveCallback.onError(activity.getString(R.string.erreur_d_activation_nfc));
        }
    }

    /**
     * Version simplifiée de enableDispatch sans callback
     */
    public void enableDispatch() {
        enableDispatch(null);
    }

    /**
     * Désactive l'expédition NFC en premier plan
     */
    public void disableDispatch() {
        logInfo("Disabling NFC foreground dispatch", "nfc_disable_dispatch");

        if (nfcAdpt != null) {
            try {
                nfcAdpt.disableForegroundDispatch(activity);
                logInfo("NFC Foreground Dispatch disabled", "nfc_dispatch_disabled");
            } catch (Exception e) {
                logException(e, "disable_dispatch_error", "Error disabling NFC dispatch");
                // Ne pas lancer d'exception car cette méthode est souvent appelée dans onPause
            }
        } else {
            logWarning("Cannot disable NFC Dispatch: adapter is null", "null_adapter");
        }
    }

    /**
     * Écrit un message NDEF sur un tag NFC avec timeout et gestion d'erreurs améliorée
     * @param tag Tag NFC à écrire
     * @param message Message NDEF à écrire
     * @param makeReadOnly Rendre le tag en lecture seule après l'écriture
     * @param callback Callback pour les résultats de l'opération
     * @return true si l'écriture a réussi
     * @throws IOException si une erreur d'E/S se produit
     * @throws FormatException si le message est mal formaté
     * @throws NFCTagReadOnly si le tag est en lecture seule
     * @throws NFCTagCapacityExceeded si le message dépasse la capacité du tag
     * @throws NFCOperationTimeout si l'opération prend trop de temps
     */
    public boolean writeTag(Tag tag, NdefMessage message, boolean makeReadOnly, NFCCallback callback)
            throws IOException, FormatException, NFCTagReadOnly, NFCTagCapacityExceeded, NFCOperationTimeout {

        NFCCallback effectiveCallback = callback != null ? callback : defaultCallback;

        // Validation des entrées
        if (tag == null) {
            logError("Tag is null", "null_tag");
            effectiveCallback.onError(activity.getString(R.string.erreur_lors_de_l_criture_du_tag_veuillez_r_essayer));
            throw new IllegalArgumentException("Tag cannot be null");
        }

        if (message == null) {
            logError("Message is null", "null_message");
            effectiveCallback.onError(activity.getString(R.string.erreur_lors_de_l_criture_du_tag_veuillez_r_essayer));
            throw new IllegalArgumentException("Message cannot be null");
        }

        // Logging et télémétrie
        String tagId = bytesToHex(tag.getId());
        crashlytics.setCustomKey("messageSize", message.getByteArrayLength());
        crashlytics.setCustomKey("makeReadOnly", makeReadOnly);
        crashlytics.setCustomKey("tagId", tagId);

        logInfo("Writing to NFC tag (ID: " + tagId + "), message size: " + message.getByteArrayLength() + " bytes", "nfc_write");

        // Variables pour la gestion du timeout
        final boolean[] operationCompleted = {false};
        final boolean[] operationSuccessful = {false};
        final Exception[] operationException = {null};

        // Configurer le timeout
        Runnable timeoutRunnable = () -> {
            if (!operationCompleted[0]) {
                operationCompleted[0] = true;
                logError("NFC operation timed out after " + NFC_OPERATION_TIMEOUT + "ms", "nfc_timeout");
                effectiveCallback.onError(activity.getString(R.string.erreur_lors_de_l_criture_du_tag_veuillez_r_essayer));
            }
        };
        handler.postDelayed(timeoutRunnable, NFC_OPERATION_TIMEOUT);

        try {
            Ndef ndefTag = Ndef.get(tag);

            if (ndefTag == null) {
                // Le tag n'est pas encore au format NDEF, essayer de le formater
                NdefFormatable nForm = NdefFormatable.get(tag);
                if (nForm == null) {
                    logError("Tag is not NDEF formattable", "not_formatable");
                    effectiveCallback.onError(activity.getString(R.string.format_de_tag_non_compatible));
                    return false;
                }

                try {
                    nForm.connect();
                    nForm.format(message);
                    operationSuccessful[0] = true;

                    logInfo("Tag formatted and written successfully", "nfc_format_success");
                    effectiveCallback.onSuccess(activity.getString(R.string.txt_close_tag));

                    return true;
                } finally {
                    safeClose(nForm);
                }
            } else {
                try {
                    logInfo("NDEF tag detected, connecting...", "nfc_connect");
                    ndefTag.connect();

                    if (!ndefTag.isWritable()) {
                        logError("Tag is read-only", "read_only");
                        effectiveCallback.onError(activity.getString(R.string.ce_tag_est_en_lecture_seule_et_ne_peut_pas_tre_modifi));
                        throw new NFCTagReadOnly();
                    }

                    int maxSize = ndefTag.getMaxSize();
                    int messageSize = message.getByteArrayLength();

                    logInfo("Max tag size: " + maxSize + ", message size: " + messageSize, "nfc_size_check");

                    if (maxSize < messageSize) {
                        logError("Tag capacity exceeded: " + messageSize + " > " + maxSize, "capacity_exceeded");
                        effectiveCallback.onError(activity.getString(R.string.ce_tag_est_trop_petit_pour_contenir_les_donn_es));
                        throw new NFCTagCapacityExceeded();
                    }

                    ndefTag.writeNdefMessage(message);

                    if (makeReadOnly && ndefTag.canMakeReadOnly()) {
                        boolean success = ndefTag.makeReadOnly();
                        if (success) {
                            logInfo("Tag made read-only", "nfc_readonly_success");
                        } else {
                            logWarning("Failed to make tag read-only", "nfc_readonly_failed");
                        }
                    }

                    operationSuccessful[0] = true;
                    logInfo("NDEF message written successfully", "nfc_write_success");
                    effectiveCallback.onSuccess(activity.getString(R.string.txt_close_tag));

                    return true;
                } finally {
                    safeClose(ndefTag);
                }
            }
        } catch (TagLostException e) {
            operationException[0] = e;
            logException(e, "tag_lost", "Tag was lost during writing");
            effectiveCallback.onError(activity.getString(R.string.erreur_lors_de_l_criture_du_tag_veuillez_r_essayer));
            throw new IOException("Tag was removed during writing", e);
        } catch (IOException e) {
            operationException[0] = e;
            logException(e, "io_error", "IO error during tag writing");
            effectiveCallback.onError(activity.getString(R.string.erreur_lors_de_l_criture_du_tag_veuillez_r_essayer));
            throw e;
        } catch (FormatException e) {
            operationException[0] = e;
            logException(e, "format_error", "Format error during tag writing");
            effectiveCallback.onError(activity.getString(R.string.format_de_tag_non_compatible));
            throw e;
        } catch (Exception e) {
            operationException[0] = e;
            logException(e, "unexpected_error", "Unexpected error during tag writing");
            effectiveCallback.onError(activity.getString(R.string.erreur_inattendue_lors_de_l_criture_du_tag));
            throw new IOException("Error writing to NFC tag", e);
        } finally {
            operationCompleted[0] = true;
            handler.removeCallbacks(timeoutRunnable);
        }
    }

    /**
     * Version simplifiée de writeTag sans callback
     */
    public boolean writeTag(Tag tag, NdefMessage message, boolean makeReadOnly)
            throws IOException, FormatException, NFCTagReadOnly, NFCTagCapacityExceeded, NFCOperationTimeout {
        return writeTag(tag, message, makeReadOnly, null);
    }

    /**
     * Ferme en toute sécurité une ressource NFC
     * @param closeable L'objet à fermer (Ndef ou NdefFormatable)
     */
    private void safeClose(Object closeable) {
        if (closeable == null) {
            return;
        }

        try {
            if (closeable instanceof Ndef) {
                Ndef ndef = (Ndef) closeable;
                if (ndef.isConnected()) {
                    ndef.close();
                }
            } else if (closeable instanceof NdefFormatable) {
                NdefFormatable formatable = (NdefFormatable) closeable;
                formatable.close();
            }
        } catch (Exception e) {
            logException(e, "close_error", "Error closing NFC resource");
            // Ne pas lancer d'exception pour les erreurs de fermeture
        }
    }

    /**
     * Crée un message URI NDEF
     * @param content Contenu de l'URI
     * @param type Préfixe de l'URI
     * @return Message NDEF contenant l'URI
     */
    public NdefMessage createUriMessage(String content, String type) {
        if (content == null || type == null) {
            logError("Content or type is null", "null_parameters");
            throw new IllegalArgumentException("Content and type cannot be null");
        }

        try {
            crashlytics.setCustomKey("uriContent", content);
            crashlytics.setCustomKey("uriType", type);

            NdefRecord record = NdefRecord.createUri(type + content);
            logInfo("URI message created successfully", "uri_created");

            return new NdefMessage(new NdefRecord[]{record});
        } catch (Exception e) {
            logException(e, "create_uri_error", "Error creating URI message");
            throw new IllegalArgumentException("Error creating URI message", e);
        }
    }

    /**
     * Crée un message texte NDEF
     * @param content Contenu du texte
     * @return Message NDEF contenant le texte
     */
    public NdefMessage createTextMessage(String content) {
        if (content == null) {
            logError("Content is null", "null_content");
            throw new IllegalArgumentException("Content cannot be null");
        }

        try {
            crashlytics.setCustomKey("textContentLength", content.length());
            logInfo("Creating text message, size: " + content.length() + " chars", "text_message");

            // Obtenir les octets UTF-8
            byte[] lang = Locale.getDefault().getLanguage().getBytes(StandardCharsets.UTF_8);
            byte[] text = content.getBytes(StandardCharsets.UTF_8); // Contenu en UTF-8

            int langSize = lang.length;
            int textLength = text.length;

            ByteArrayOutputStream payload = new ByteArrayOutputStream(1 + langSize + textLength);
            payload.write((byte) (langSize & 0x1F));
            payload.write(lang, 0, langSize);
            payload.write(text, 0, textLength);
            NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload.toByteArray());

            logInfo("Text message created successfully", "text_created");
            return new NdefMessage(new NdefRecord[]{record});
        } catch (Exception e) {
            logException(e, "create_text_error", "Error creating text message");
            throw new IllegalArgumentException("Error creating text message", e);
        }
    }

    /**
     * Crée un message externe NDEF
     * @param content Contenu du message
     * @return Message NDEF externe
     */
    public NdefMessage createExternalMessage(String content) {
        if (content == null) {
            logError("Content is null", "null_content");
            throw new IllegalArgumentException("Content cannot be null");
        }

        try {
            crashlytics.setCustomKey("externalContentLength", content.length());
            logInfo("Creating external message, size: " + content.length() + " chars", "external_message");

            // Utiliser le package de l'application pour éviter les conflits
            String packageName = activity.getPackageName();

            NdefRecord externalRecord = NdefRecord.createExternal(
                    packageName, // Domaine - devrait correspondre au package de l'app
                    "data",      // Type - identifie le format du contenu
                    content.getBytes(StandardCharsets.UTF_8)
            );

            logInfo("External message created successfully", "external_created");
            return new NdefMessage(new NdefRecord[] { externalRecord });
        } catch (Exception e) {
            logException(e, "create_external_error", "Error creating external message");
            throw new IllegalArgumentException("Error creating external message", e);
        }
    }

    /**
     * Convertit un tableau d'octets en chaîne hexadécimale
     * @param bytes Tableau d'octets à convertir
     * @return Chaîne hexadécimale
     */
    private String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

//********* LOGGING METHODS

    private void logInfo(String message, String infoType) {
        try {
            crashlytics.log("INFO: " + message);
            Log.i(TAG, message);

            Bundle params = new Bundle();
            params.putString("info_type", infoType);
            params.putString("class", "NFCManager");
            params.putString("info_message", message);
            if (analytics != null) {
                analytics.logEvent("app_info", params);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in logInfo: " + e.getMessage());
        }
    }

    private void logWarning(String message, String warningType) {
        try {
            crashlytics.log("WARNING: " + message);
            Log.w(TAG, message);

            Bundle params = new Bundle();
            params.putString("warning_type", warningType);
            params.putString("class", "NFCManager");
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
            params.putString("class", "NFCManager");
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
            params.putString("class", "NFCManager");
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

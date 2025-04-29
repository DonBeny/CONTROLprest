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
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.orgaprop.controlprest.utils.ToastManager;



public class NFCManager {

//********* PRIVATE VARIABLES

    private static final String TAG = "NFCManager";
    private final Activity activity;
    private NfcAdapter nfcAdpt;
    private FirebaseCrashlytics crashlytics;
    private FirebaseAnalytics analytics;

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
            super("Le tag NFC est en lecture seule");
        }
    }
    public static class NFCTagCapacityExceeded extends Exception {
        public NFCTagCapacityExceeded() {
            super("La capacité du tag NFC a été dépassée");
        }
    }

//********* CONSTRUCTORS

    public NFCManager(Activity activity) {
        try {
            if (activity == null) {
                throw new IllegalArgumentException("L'activité ne peut pas être nulle");
            }
            this.activity = activity;

            // Initialiser Crashlytics
            this.crashlytics = FirebaseCrashlytics.getInstance();
            crashlytics.log("NFCManager initialisé");
            crashlytics.setCustomKey("deviceModel", Build.MODEL);
            crashlytics.setCustomKey("deviceManufacturer", Build.MANUFACTURER);

            this.analytics = FirebaseAnalytics.getInstance(activity);

            Bundle screenViewParams = new Bundle();
            screenViewParams.putString(FirebaseAnalytics.Param.SCREEN_NAME, "NFCManager");
            screenViewParams.putString(FirebaseAnalytics.Param.SCREEN_CLASS, "NFCManager");
            analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, screenViewParams);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'initialisation de NFCManager", e);
            FirebaseCrashlytics.getInstance();
            FirebaseCrashlytics.getInstance().recordException(e);

            if (activity != null) {
                try {
                    this.analytics = FirebaseAnalytics.getInstance(activity);

                    Bundle screenViewParams = new Bundle();
                    screenViewParams.putString(FirebaseAnalytics.Param.SCREEN_NAME, "NFCManager");
                    screenViewParams.putString(FirebaseAnalytics.Param.SCREEN_CLASS, "NFCManager");
                    analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, screenViewParams);

                    Bundle errorParams = new Bundle();
                    errorParams.putString("error_type", "app_error");
                    errorParams.putString("class", "NFCManager");
                    errorParams.putString("message", e.getMessage());
                    analytics.logEvent("NFCManager_app_error", errorParams);
                } catch (Exception analyticsEx) {
                    Log.e(TAG, "Exception dans onCreate analytics: " + analyticsEx.getMessage(), analyticsEx);
                }
            }

            ToastManager.showError("Erreur lors de l'initialisation de NFCManager");

            throw e;
        }
    }

//********* PUBLIC FUNCTIONS

    /**
     * Vérifie si NFC est supporté et activé sur le périphérique
     * @throws NFCNotSupported si NFC n'est pas supporté
     * @throws NFCNotEnabled si NFC est supporté mais non activé
     */
    public void verifyNFC() throws NFCNotSupported, NFCNotEnabled {
        try {
            crashlytics.log("verifyNFC called");

            nfcAdpt = NfcAdapter.getDefaultAdapter(activity);

            if (nfcAdpt == null) {
                crashlytics.log("NFC non supporté sur cet appareil");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "not_supported");
                errorParams.putString("class", "NFCManager");
                analytics.logEvent("verifyNFC_not_supported", errorParams);

                ToastManager.showError("NFC non supporté sur cet appareil");

                throw new NFCNotSupported();
            }
            if (!nfcAdpt.isEnabled()) {
                crashlytics.log("NFC désactivé sur cet appareil");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "not_enabled");
                errorParams.putString("class", "NFCManager");
                analytics.logEvent("verifyNFC_not_enabled", errorParams);

                ToastManager.showError("NFC désactivé sur cet appareil");

                throw new NFCNotEnabled();
            }

            crashlytics.log("NFC vérifié avec succès");
        } catch (Exception e) {
            if (e instanceof NFCNotSupported || e instanceof NFCNotEnabled) {
                crashlytics.log("Exception NFC spécifique: " + e.getClass().getSimpleName());

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", e.getClass().getSimpleName());
                errorParams.putString("class", "NFCManager");
                errorParams.putString("message", e.getMessage());
                analytics.logEvent("verifyNFC_specific_exception", errorParams);

                throw e;
            }
            crashlytics.recordException(e);
            crashlytics.log("Erreur lors de la vérification NFC: " + e.getMessage());
            Log.e(TAG, "Erreur lors de la vérification NFC: " + e.getMessage(), e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "generic_exception");
            errorParams.putString("class", "NFCManager");
            errorParams.putString("message", e.getMessage());
            analytics.logEvent("verifyNFC_generic_exception", errorParams);

            ToastManager.showError("Erreur lors de la vérification NFC");
            throw new RuntimeException("Erreur lors de la vérification NFC", e);
        }
    }

    /**
     * Active l'expédition NFC en premier plan pour l'activité
     */
    public void enableDispatch() {
        try {
            crashlytics.log("enableDispatch called");

            if (nfcAdpt == null) {
                crashlytics.log("L'adaptateur NFC est null lors de l'activation de l'expédition");
                Log.e(TAG, "L'adaptateur NFC est null lors de l'activation de l'expédition");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "null_adapter");
                errorParams.putString("class", "NFCManager");
                analytics.logEvent("enableDispatch_null_adapter", errorParams);

                ToastManager.showError("Erreur lors de l'activation de l'expédition NFC");
                return;
            }

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
            crashlytics.log("NFC Foreground Dispatch activé");
        } catch (Exception e) {
            crashlytics.recordException(e);
            crashlytics.log("Erreur lors de l'activation de l'expédition NFC: " + e.getMessage());
            Log.e(TAG, "Erreur lors de l'activation de l'expédition NFC: " + e.getMessage(), e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "generic_exception");
            errorParams.putString("class", "NFCManager");
            errorParams.putString("message", e.getMessage());
            analytics.logEvent("enableDispatch_app_error", errorParams);

            ToastManager.showError("Erreur lors de l'activation de l'expédition NFC");
        }
    }

    /**
     * Désactive l'expédition NFC en premier plan
     */
    public void disableDispatch() {
        try {
            crashlytics.log("disableDispatch called");

            if (nfcAdpt != null) {
                nfcAdpt.disableForegroundDispatch(activity);
                crashlytics.log("NFC Foreground Dispatch désactivé");
            } else {
                crashlytics.log("Impossible de désactiver NFC Dispatch: adaptateur null");
                Log.e(TAG, "Impossible de désactiver NFC Dispatch: adaptateur null");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "null_adapter");
                errorParams.putString("class", "NFCManager");
                analytics.logEvent("disableDispatch_null_adapter", errorParams);
            }
        } catch (Exception e) {
            crashlytics.recordException(e);
            crashlytics.log("Erreur lors de la désactivation de l'expédition NFC: " + e.getMessage());
            Log.e(TAG, "Erreur lors de la désactivation de l'expédition NFC: " + e.getMessage(), e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "generic_exception");
            errorParams.putString("class", "NFCManager");
            errorParams.putString("message", e.getMessage());
            analytics.logEvent("disableDispatch_app_error", errorParams);

            ToastManager.showError("Erreur lors de la désactivation de l'expédition NFC");
        }
    }

    /**
     * Écrit un message NDEF sur un tag NFC
     * @param tag Tag NFC à écrire
     * @param message Message NDEF à écrire
     * @param makeReadOnly Rendre le tag en lecture seule après l'écriture
     * @return true si l'écriture a réussi
     * @throws IOException si une erreur d'E/S se produit
     * @throws FormatException si le message est mal formaté
     * @throws NFCTagReadOnly si le tag est en lecture seule
     * @throws NFCTagCapacityExceeded si le message dépasse la capacité du tag
     */
    public boolean writeTag(Tag tag, NdefMessage message, boolean makeReadOnly)
            throws IOException, FormatException, NFCTagReadOnly, NFCTagCapacityExceeded {
        try {
            crashlytics.log("writeTag called");

            if (tag == null) {
                crashlytics.log("Le tag est null");
                Log.e(TAG, "Le tag est null");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "null_tag");
                errorParams.putString("class", "NFCManager");
                analytics.logEvent("writeTag_null_tag", errorParams);

                ToastManager.showError("Le tag est null");
                return false;
            }

            if (message == null) {
                crashlytics.log("Le message est null");
                Log.e(TAG, "Le message est null");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "null_message");
                errorParams.putString("class", "NFCManager");
                analytics.logEvent("writeTag_null_message", errorParams);

                ToastManager.showError("Le message est null");
                return false;
            }

            crashlytics.setCustomKey("messageSize", message.getByteArrayLength());
            crashlytics.setCustomKey("makeReadOnly", makeReadOnly);

            String tagId = bytesToHex(tag.getId());
            crashlytics.setCustomKey("tagId", tagId);
            crashlytics.log("Écriture d'un tag (ID: " + tagId + ")");

            Bundle tagParams = new Bundle();
            tagParams.putString("tagId", tagId);
            analytics.logEvent("writeTag_tag", tagParams);

            Ndef ndefTag = Ndef.get(tag);

            try {
                if (ndefTag == null) {
                    // Le tag n'est pas encore au format NDEF
                    crashlytics.log("Tag non formaté en NDEF, tentative de formatage");

                    NdefFormatable nForm = NdefFormatable.get(tag);
                    if (nForm == null) {
                        crashlytics.log("Le tag n'est pas formatable en NDEF");
                        Log.e(TAG, "Le tag n'est pas formatable en NDEF");

                        Bundle errorParams = new Bundle();
                        errorParams.putString("error_type", "not_formatable");
                        errorParams.putString("class", "NFCManager");
                        analytics.logEvent("writeTag_not_formatable", errorParams);

                        ToastManager.showError("Le tag n'est pas formatable en NDEF");
                        return false;
                    }

                    try {
                        nForm.connect();
                        nForm.format(message);
                        crashlytics.log("Tag formaté et écrit avec succès");
                        Log.d(TAG, "Tag formaté et écrit avec succès");

                        Bundle tagParams2 = new Bundle();
                        tagParams2.putString("tagId", tagId);
                        analytics.logEvent("writeTag_tag", tagParams2);

                        ToastManager.showShort("Tag formaté et écrit avec succès");
                        return true;
                    } finally {
                        try {
                            nForm.close();
                        } catch (Exception e) {
                            crashlytics.recordException(e);
                            Log.e(TAG, "Erreur lors de la fermeture du formateur NDEF: " + e.getMessage(), e);

                            Bundle errorParams = new Bundle();
                            errorParams.putString("error_type", "close_form_exception");
                            errorParams.putString("class", "NFCManager");
                            errorParams.putString("message", e.getMessage());
                            analytics.logEvent("writeTag_close_form_exception", errorParams);

                            ToastManager.showError("Erreur lors de la fermeture du formateur NDEF");
                        }
                    }
                } else {
                    try {
                        crashlytics.log("Tag NDEF détecté, connexion...");
                        ndefTag.connect();

                        if (!ndefTag.isWritable()) {
                            crashlytics.log("Tag en lecture seule");
                            Log.e(TAG, "Le tag est en lecture seule");

                            Bundle errorParams = new Bundle();
                            errorParams.putString("error_type", "read_only");
                            errorParams.putString("class", "NFCManager");
                            analytics.logEvent("writeTag_read_only", errorParams);

                            ToastManager.showError("Le tag est en lecture seule");

                            throw new NFCTagReadOnly();
                        }

                        int maxSize = ndefTag.getMaxSize();
                        int messageSize = message.getByteArrayLength();
                        crashlytics.log("Taille maximale du tag: " + maxSize + ", taille du message: " + messageSize);

                        if (maxSize < messageSize) {
                            crashlytics.log("Capacité du tag dépassée");
                            Log.e(TAG, "La capacité du tag a été dépassée");

                            Bundle errorParams = new Bundle();
                            errorParams.putString("error_type", "capacity_exceeded");
                            errorParams.putString("class", "NFCManager");
                            analytics.logEvent("writeTag_capacity_exceeded", errorParams);

                            ToastManager.showError("La capacité du tag a été dépassée");

                            throw new NFCTagCapacityExceeded();
                        }

                        ndefTag.writeNdefMessage(message);
                        crashlytics.log("Message NDEF écrit avec succès");
                        Log.d(TAG, "Tag écrit avec succès");

                        if (makeReadOnly) {
                            if (ndefTag.canMakeReadOnly()) {
                                ndefTag.makeReadOnly();
                                crashlytics.log("Tag mis en lecture seule");
                            } else {
                                crashlytics.log("Ce tag ne supporte pas le mode lecture seule");
                                Log.w(TAG, "Ce tag ne supporte pas le mode lecture seule");

                                Bundle errorParams = new Bundle();
                                errorParams.putString("error_type", "read_only_not_supported");
                                errorParams.putString("class", "NFCManager");
                                analytics.logEvent("writeTag_read_only_not_supported", errorParams);

                                ToastManager.showShort("Ce tag ne supporte pas le mode lecture seule");
                            }
                        }

                        ToastManager.showShort("Tag écrit avec succès");
                        return true;
                    } finally {
                        try {
                            ndefTag.close();
                        } catch (Exception e) {
                            crashlytics.recordException(e);
                            Log.e(TAG, "Erreur lors de la fermeture du tag NDEF: " + e.getMessage(), e);

                            Bundle errorParams = new Bundle();
                            errorParams.putString("error_type", "close_tag_exception");
                            errorParams.putString("class", "NFCManager");
                            errorParams.putString("message", e.getMessage());
                            analytics.logEvent("writeTag_close_tag_exception", errorParams);

                            ToastManager.showError("Erreur lors de la fermeture du tag NDEF");
                        }
                    }
                }
            } catch (TagLostException e) {
                crashlytics.recordException(e);
                crashlytics.log("Le tag a été perdu pendant l'écriture");
                Log.e(TAG, "Le tag a été perdu pendant l'écriture", e);

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "tag_lost");
                errorParams.putString("class", "NFCManager");
                errorParams.putString("message", e.getMessage());
                analytics.logEvent("writeTag_tag_lost", errorParams);

                ToastManager.showError("Le tag a été retiré pendant l'écriture");
                throw new IOException("Le tag a été retiré pendant l'écriture", e);
            } catch (IOException | FormatException e) {
                crashlytics.recordException(e);
                crashlytics.log("Erreur lors de l'écriture: " + e.getClass().getSimpleName());

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", e.getClass().getSimpleName());
                errorParams.putString("class", "NFCManager");
                errorParams.putString("message", e.getMessage());
                analytics.logEvent("writeTag_error", errorParams);

                ToastManager.showError("Erreur lors de l'écriture");

                throw e;
            } catch (NFCTagReadOnly e) {
                crashlytics.log("Exception NFCTagReadOnly: " + e.getMessage());
                throw e;
            } catch (NFCTagCapacityExceeded e) {
                crashlytics.log("Exception NFCTagCapacityExceeded: " + e.getMessage());
                throw e;
            } catch (Exception e) {
                crashlytics.recordException(e);
                crashlytics.log("Erreur inattendue lors de l'écriture du tag: " + e.getMessage());
                Log.e(TAG, "Erreur inattendue lors de l'écriture du tag: " + e.getMessage(), e);

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "unexpected_exception");
                errorParams.putString("class", "NFCManager");
                errorParams.putString("message", e.getMessage());
                analytics.logEvent("writeTag_unexpected_exception", errorParams);

                ToastManager.showError("Erreur lors de l'écriture du tag");

                throw new IOException("Erreur lors de l'écriture du tag", e);
            }
        } catch (Exception e) {
            if (!(e instanceof IOException || e instanceof FormatException ||
                    e instanceof NFCTagReadOnly || e instanceof NFCTagCapacityExceeded)) {
                crashlytics.recordException(e);
                crashlytics.log("Exception non gérée dans writeTag: " + e.getMessage());

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "unexpected_exception");
                errorParams.putString("class", "NFCManager");
                errorParams.putString("message", e.getMessage());
                analytics.logEvent("writeTag_app_error", errorParams);

                ToastManager.showError("Erreur lors de l'écriture du tag");
            }
            throw e;
        }
    }

    /**
     * Crée un message URI NDEF
     * @param content Contenu de l'URI
     * @param type Préfixe de l'URI
     * @return Message NDEF contenant l'URI
     */
    public NdefMessage createUriMessage(String content, String type) {
        try {
            crashlytics.log("createUriMessage called");

            if (content == null || type == null) {
                crashlytics.log("Le contenu ou le type est null");
                Log.e(TAG, "Le contenu et le type ne peuvent pas être nuls");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "null_content_or_type");
                errorParams.putString("class", "NFCManager");
                analytics.logEvent("createUriMessage_null_content_or_type", errorParams);

                ToastManager.showError("Le contenu et le type ne peuvent pas être nuls");

                throw new IllegalArgumentException("Le contenu et le type ne peuvent pas être nuls");
            }

            crashlytics.setCustomKey("uriContent", content);
            crashlytics.setCustomKey("uriType", type);

            NdefRecord record = NdefRecord.createUri(type + content);
            crashlytics.log("Message URI créé avec succès");

            return new NdefMessage(new NdefRecord[]{record});
        } catch (Exception e) {
            crashlytics.recordException(e);
            crashlytics.log("Erreur lors de la création du message URI: " + e.getMessage());
            Log.e(TAG, "Erreur lors de la création du message URI: " + e.getMessage(), e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "create_uri_exception");
            errorParams.putString("class", "NFCManager");
            errorParams.putString("message", e.getMessage());
            analytics.logEvent("createUriMessage_app_error", errorParams);

            ToastManager.showError("Erreur lors de la création du message URI");

            throw new RuntimeException("Erreur lors de la création du message URI", e);
        }
    }

    /**
     * Crée un message texte NDEF
     * @param content Contenu du texte
     * @return Message NDEF contenant le texte
     */
    public NdefMessage createTextMessage(String content) {
        try {
            crashlytics.log("createTextMessage called");

            if (content == null) {
                crashlytics.log("Le contenu est null");
                Log.e(TAG, "Le contenu ne peut pas être nul");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "null_content");
                errorParams.putString("class", "NFCManager");
                analytics.logEvent("createTextMessage_null_content", errorParams);

                ToastManager.showError("Le contenu ne peut pas être nul");
                throw new IllegalArgumentException("Le contenu ne peut pas être nul");
            }

            crashlytics.setCustomKey("textContentLength", content.length());

            // Get UTF-8 byte
            byte[] lang = Locale.getDefault().getLanguage().getBytes(StandardCharsets.UTF_8);
            byte[] text = content.getBytes(StandardCharsets.UTF_8); // Content in UTF-8

            int langSize = lang.length;
            int textLength = text.length;

            crashlytics.log("Création d'un message texte, taille: " + textLength + " octets");

            ByteArrayOutputStream payload = new ByteArrayOutputStream(1 + langSize + textLength);
            payload.write((byte) (langSize & 0x1F));
            payload.write(lang, 0, langSize);
            payload.write(text, 0, textLength);
            NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload.toByteArray());

            crashlytics.log("Message texte créé avec succès");

            return new NdefMessage(new NdefRecord[]{record});
        } catch (Exception e) {
            crashlytics.recordException(e);
            crashlytics.log("Erreur inattendue lors de la création du message texte: " + e.getMessage());
            Log.e(TAG, "Erreur inattendue lors de la création du message texte: " + e.getMessage(), e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "unexpected_exception");
            errorParams.putString("class", "NFCManager");
            errorParams.putString("message", e.getMessage());
            analytics.logEvent("createTextMessage_app_error", errorParams);

            ToastManager.showError("Erreur lors de la création du message texte");

            throw new RuntimeException("Erreur lors de la création du message texte", e);
        }
    }

    /**
     * Crée un message externe NDEF
     * @param content Contenu du message
     * @return Message NDEF externe
     */
    public NdefMessage createExternalMessage(String content) {
        try {
            crashlytics.log("createExternalMessage called");

            if (content == null) {
                crashlytics.log("Le contenu est null");
                Log.e(TAG, "Le contenu ne peut pas être nul");

                Bundle errorParams = new Bundle();
                errorParams.putString("error_type", "null_content");
                errorParams.putString("class", "NFCManager");
                analytics.logEvent("createExternalMessage_null_content", errorParams);

                ToastManager.showError("Le contenu ne peut pas être nul");

                throw new IllegalArgumentException("Le contenu ne peut pas être nul");
            }

            crashlytics.setCustomKey("externalContentLength", content.length());
            crashlytics.log("Création d'un message externe, taille: " + content.length() + " caractères");

            NdefRecord externalRecord = NdefRecord.createExternal("fr.benysoftware", "data", content.getBytes(StandardCharsets.UTF_8));
            crashlytics.log("Message externe créé avec succès");
            return new NdefMessage(new NdefRecord[] { externalRecord });
        } catch (Exception e) {
            crashlytics.recordException(e);
            crashlytics.log("Erreur lors de la création du message externe: " + e.getMessage());

            Log.e(TAG, "Erreur lors de la création du message externe: " + e.getMessage(), e);

            Bundle errorParams = new Bundle();
            errorParams.putString("error_type", "create_external_exception");
            errorParams.putString("class", "NFCManager");
            errorParams.putString("message", e.getMessage());
            analytics.logEvent("createExternalMessage_app_error", errorParams);

            ToastManager.showError("Erreur lors de la création du message externe");

            throw new RuntimeException("Erreur lors de la création du message externe", e);
        }
    }

    /**
     * Convertit un tableau d'octets en chaîne hexadécimale
     * @param bytes Tableau d'octets à convertir
     * @return Chaîne hexadécimale
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

}

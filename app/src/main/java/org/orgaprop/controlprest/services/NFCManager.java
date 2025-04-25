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
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class NFCManager {

//********* PRIVATE VARIABLES

    private static final String TAG = "NFCManager";
    private final Activity activity;
    private NfcAdapter nfcAdpt;

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
        if (activity == null) {
            throw new IllegalArgumentException("L'activité ne peut pas être nulle");
        }
        this.activity = activity;
    }

//********* PUBLIC FUNCTIONS

    /**
     * Vérifie si NFC est supporté et activé sur le périphérique
     * @throws NFCNotSupported si NFC n'est pas supporté
     * @throws NFCNotEnabled si NFC est supporté mais non activé
     */
    public void verifyNFC() throws NFCNotSupported, NFCNotEnabled {
        try {
            nfcAdpt = NfcAdapter.getDefaultAdapter(activity);

            if (nfcAdpt == null) throw new NFCNotSupported();
            if (!nfcAdpt.isEnabled()) throw new NFCNotEnabled();
        } catch (Exception e) {
            if (e instanceof NFCNotSupported || e instanceof NFCNotEnabled) {
                throw e;
            }
            Log.e(TAG, "Erreur lors de la vérification NFC: " + e.getMessage(), e);
            Toast.makeText(activity, "Erreur lors de la vérification NFC", Toast.LENGTH_SHORT).show();
            throw new RuntimeException("Erreur lors de la vérification NFC", e);
        }
    }
    /**
     * Active l'expédition NFC en premier plan pour l'activité
     */
    public void enableDispatch() {
        try {
            if (nfcAdpt == null) {
                Log.e(TAG, "L'adaptateur NFC est null lors de l'activation de l'expédition");
                Toast.makeText(activity, "Erreur lors de l'activation de l'expédition NFC", Toast.LENGTH_SHORT).show();
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
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'activation de l'expédition NFC: " + e.getMessage(), e);
            Toast.makeText(activity, "Erreur lors de l'activation de l'expédition NFC", Toast.LENGTH_SHORT).show();
        }
    }
    /**
     * Désactive l'expédition NFC en premier plan
     */
    public void disableDispatch() {
        try {
            if (nfcAdpt != null) {
                nfcAdpt.disableForegroundDispatch(activity);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la désactivation de l'expédition NFC: " + e.getMessage(), e);
            Toast.makeText(activity, "Erreur lors de la désactivation de l'expédition NFC", Toast.LENGTH_SHORT).show();
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
        if (tag == null) {
            Log.e(TAG, "Le tag est null");
            Toast.makeText(activity, "Le tag est null", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (message == null) {
            Log.e(TAG, "Le message est null");
            Toast.makeText(activity, "Le message est null", Toast.LENGTH_SHORT).show();
            return false;
        }

        Ndef ndefTag = Ndef.get(tag);

        try {
            if (ndefTag == null) {
                // Le tag n'est pas encore au format NDEF
                NdefFormatable nForm = NdefFormatable.get(tag);
                if (nForm == null) {
                    Log.e(TAG, "Le tag n'est pas formatable en NDEF");
                    Toast.makeText(activity, "Le tag n'est pas formatable en NDEF", Toast.LENGTH_SHORT).show();
                    return false;
                }

                try {
                    nForm.connect();
                    nForm.format(message);
                    Log.d(TAG, "Tag formaté et écrit avec succès");
                    Toast.makeText(activity, "Tag formaté et écrit avec succès", Toast.LENGTH_SHORT).show();
                    return true;
                } finally {
                    try {
                        nForm.close();
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur lors de la fermeture du formateur NDEF: " + e.getMessage(), e);
                        Toast.makeText(activity, "Erreur lors de la fermeture du formateur NDEF", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                try {
                    ndefTag.connect();

                    if (!ndefTag.isWritable()) {
                        throw new NFCTagReadOnly();
                    }

                    if (ndefTag.getMaxSize() < message.getByteArrayLength()) {
                        throw new NFCTagCapacityExceeded();
                    }

                    ndefTag.writeNdefMessage(message);

                    if (makeReadOnly) {
                        if (ndefTag.canMakeReadOnly()) {
                            ndefTag.makeReadOnly();
                        } else {
                            Log.w(TAG, "Ce tag ne supporte pas le mode lecture seule");
                            Toast.makeText(activity, "Ce tag ne supporte pas le mode lecture seule", Toast.LENGTH_SHORT).show();
                        }
                    }

                    Log.d(TAG, "Tag écrit avec succès");
                    Toast.makeText(activity, "Tag écrit avec succès", Toast.LENGTH_SHORT).show();
                    return true;
                } finally {
                    try {
                        ndefTag.close();
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur lors de la fermeture du tag NDEF: " + e.getMessage(), e);
                        Toast.makeText(activity, "Erreur lors de la fermeture du tag NDEF", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } catch (TagLostException e) {
            Log.e(TAG, "Le tag a été perdu pendant l'écriture", e);
            Toast.makeText(activity, "Le tag a été retiré pendant l'écriture", Toast.LENGTH_SHORT).show();
            throw new IOException("Le tag a été retiré pendant l'écriture", e);
        } catch (IOException | FormatException | NFCTagReadOnly | NFCTagCapacityExceeded e) {
            throw e;
        } catch (Exception e) {
            Log.e(TAG, "Erreur inattendue lors de l'écriture du tag: " + e.getMessage(), e);
            Toast.makeText(activity, "Erreur lors de l'écriture du tag", Toast.LENGTH_SHORT).show();
            throw new IOException("Erreur lors de l'écriture du tag", e);
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
            if (content == null || type == null) {
                Log.e(TAG, "Le contenu et le type ne peuvent pas être nuls");
                Toast.makeText(activity, "Le contenu et le type ne peuvent pas être nuls", Toast.LENGTH_SHORT).show();
                throw new IllegalArgumentException("Le contenu et le type ne peuvent pas être nuls");
            }

            NdefRecord record = NdefRecord.createUri(type + content);
            return new NdefMessage(new NdefRecord[]{record});
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la création du message URI: " + e.getMessage(), e);
            Toast.makeText(activity, "Erreur lors de la création du message URI", Toast.LENGTH_SHORT).show();
            throw new RuntimeException("Erreur lors de la création du message URI", e);
        }
    }
    /**
     * Crée un message texte NDEF
     * @param content Contenu du texte
     * @return Message NDEF contenant le texte
     */
    public NdefMessage createTextMessage(String content) {
        if (content == null) {
            Log.e(TAG, "Le contenu ne peut pas être nul");
            Toast.makeText(activity, "Le contenu ne peut pas être nul", Toast.LENGTH_SHORT).show();
            throw new IllegalArgumentException("Le contenu ne peut pas être nul");
        }

        try {
            // Get UTF-8 byte
            byte[] lang = Locale.getDefault().getLanguage().getBytes(StandardCharsets.UTF_8);
            byte[] text = content.getBytes(StandardCharsets.UTF_8); // Content in UTF-8

            int langSize = lang.length;
            int textLength = text.length;

            ByteArrayOutputStream payload = new ByteArrayOutputStream(1 + langSize + textLength);
            payload.write((byte) (langSize & 0x1F));
            payload.write(lang, 0, langSize);
            payload.write(text, 0, textLength);
            NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload.toByteArray());
            return new NdefMessage(new NdefRecord[]{record});
        } catch (Exception e) {
            Log.e(TAG, "Erreur inattendue lors de la création du message texte: " + e.getMessage(), e);
            Toast.makeText(activity, "Erreur lors de la création du message texte", Toast.LENGTH_SHORT).show();
            throw new RuntimeException("Erreur lors de la création du message texte", e);
        }
    }
    /**
     * Crée un message externe NDEF
     * @param content Contenu du message
     * @return Message NDEF externe
     */
    public NdefMessage createExternalMessage(String content) {
        if (content == null) {
            Log.e(TAG, "Le contenu ne peut pas être nul");
            Toast.makeText(activity, "Le contenu ne peut pas être nul", Toast.LENGTH_SHORT).show();
            throw new IllegalArgumentException("Le contenu ne peut pas être nul");
        }

        try {
            NdefRecord externalRecord = NdefRecord.createExternal("fr.benysoftware", "data", content.getBytes(StandardCharsets.UTF_8));
            return new NdefMessage(new NdefRecord[] { externalRecord });
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la création du message externe: " + e.getMessage(), e);
            Toast.makeText(activity, "Erreur lors de la création du message externe", Toast.LENGTH_SHORT).show();
            throw new RuntimeException("Erreur lors de la création du message externe", e);
        }
    }

}

package org.orgaprop.controlprest.services;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class NFCManager {

//********* PRIVATE VARIABLES

    private Activity activity;
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

//********* CONSTRUCTORS

    public NFCManager(Activity activity) {
        this.activity = activity;
    }

//********* PUBLIC FUNCTIONS

    public void verifyNFC() throws NFCNotSupported, NFCNotEnabled {
        nfcAdpt = NfcAdapter.getDefaultAdapter(activity);

        if( nfcAdpt == null ) throw new NFCNotSupported();
        if( !nfcAdpt.isEnabled() ) throw new NFCNotEnabled();
    }
    public void enableDispatch() {
        Intent nfcIntent = new Intent(activity, getClass());

        nfcIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(activity, 0, nfcIntent, 0);
        IntentFilter[] intentFiltersArray = new IntentFilter[] {};
        String[][] techList = new String[][] { { android.nfc.tech.Ndef.class.getName() }, { android.nfc.tech.NdefFormatable.class.getName() } };

        nfcAdpt.enableForegroundDispatch(activity, pendingIntent, intentFiltersArray, techList);
    }
    public void disableDispatch() {
        nfcAdpt.disableForegroundDispatch(activity);
    }

    public void writeTag(Tag tag, NdefMessage message, boolean makeReadOnly)  {
        if (tag != null) {
            try {
                Ndef ndefTag = Ndef.get(tag);

                if (ndefTag == null) {
                    // Let's try to format the Tag in NDEF
                    NdefFormatable nForm = NdefFormatable.get(tag);
                    if (nForm != null) {
                        nForm.connect();
                        nForm.format(message);
                        nForm.close();
                    }
                } else {
                    ndefTag.connect();
                    ndefTag.writeNdefMessage(message);
                    if( makeReadOnly ) {
                        ndefTag.makeReadOnly();
                    }
                    ndefTag.close();
                }
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
    public NdefMessage createUriMessage(String content, String type) {
        NdefRecord record = NdefRecord.createUri(type + content);
        NdefMessage msg = new NdefMessage(new NdefRecord[]{record});
        return msg;

    }
    public NdefMessage createTextMessage(String content) {
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
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
    public NdefMessage createExternalMessage(String content) {
        NdefRecord externalRecord = NdefRecord.createExternal("fr.benysoftware", "data", content.getBytes());

        NdefMessage ndefMessage = new NdefMessage(new NdefRecord[] { externalRecord });

        return ndefMessage;
    }

}

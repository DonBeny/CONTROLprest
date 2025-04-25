package org.orgaprop.controlprest.controllers.activities;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.orgaprop.controlprest.databinding.ActivityNfsBinding;

import java.nio.charset.StandardCharsets;

public class NfsActivity extends AppCompatActivity {

//********* PRIVATE VARIABLES

    private String rsdSelected;
    private String txtWrite;

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private IntentFilter[] intentFilters;
    private String[][] techLists;

//********* STATIC VARIABLES

    public static final String NFS_ACTIVITY_ADR_HTML = "www.orgaprop.org/cs/ControlPrest/index.php?ctrl_prest=";

    public static final String NFS_ACTIVITY_EXTRA = "rsd";

//********* WIDGETS

    private ActivityNfsBinding binding;

//********* CONSTRUCTORS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityNfsBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        Intent intent = getIntent();

        rsdSelected = intent.getStringExtra(NfsActivity.NFS_ACTIVITY_EXTRA);
        txtWrite = NfsActivity.NFS_ACTIVITY_ADR_HTML + rsdSelected + "&clt=" + LoginActivity.id_client;

        nfcAdapter = NfcAdapter.getDefaultAdapter(NfsActivity.this);

        Intent nfcIntent = new Intent(NfsActivity.this, getClass());

        nfcIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity(NfsActivity.this, 0, nfcIntent, PendingIntent.FLAG_MUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(NfsActivity.this, 0, nfcIntent, PendingIntent.FLAG_IMMUTABLE);
        }

        intentFilters = new IntentFilter[]{};
        techLists = new String[][]{{Ndef.class.getName()}, {NdefFormatable.class.getName()}};

        OnBackPressedDispatcher dispatcher = getOnBackPressedDispatcher();
        dispatcher.addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                closeNfsActivity();
            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();

        enableForegroundDispatch();
    }
    @Override
    protected void onPause() {
        super.onPause();

        disableForegroundDispatch();
    }

//********* SURCHARGE

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        handleIntent(intent);
    }

//********* PUBLIC FUNCTIONS

    public void nfsActivityActions(View v) {
        String viewTag = v.getTag().toString();

        switch (viewTag) {
            case "close":
                closeNfsActivity();
                break;
        }
    }

//********* PRIVATE FUNCTIONS

    private void handleIntent(Intent intent) {
        //Log.e(TAG, "handleIntent::START");

        LinearLayout mStartLayout = binding.nfsActivityStartLyt;
        LinearLayout mEndLayout = binding.nfsActivityEndLyt;

        String action = intent.getAction();

        //Log.e(TAG, "handleIntent::ACTION_TAG_DISCOVERED => "+NfcAdapter.ACTION_TAG_DISCOVERED);
        //Log.e(TAG, "handleIntent::ACTION_NDEF_DISCOVERED => "+NfcAdapter.ACTION_NDEF_DISCOVERED);
        //Log.e(TAG, "handleIntent::ACTION_TECH_DISCOVERED => "+NfcAdapter.ACTION_TECH_DISCOVERED);
        //Log.e(TAG, "handleIntent::intent action => "+action);

        if( NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action) || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action) ) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            NdefRecord uriRecord = createUriRecord(txtWrite);
            NdefMessage ndefMessage = new NdefMessage(new NdefRecord[] { uriRecord });

            //Log.e(TAG, "handleIntent::ready to tag");

            if( tag != null ) {
                if( writeNdefMessage(tag, ndefMessage) ) {
                    Toast.makeText(NfsActivity.this, "Tag written successfully", Toast.LENGTH_SHORT).show();

                    //Log.e(TAG, "handleIntent::Tag written successfully");

                    mStartLayout.setVisibility(View.INVISIBLE);
                    mEndLayout.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(NfsActivity.this, "Failed to write tag", Toast.LENGTH_SHORT).show();

                    //Log.e(TAG, "handleIntent::Failed to write tag");
                }
            } else {
                Toast.makeText(this, "Tag is null", Toast.LENGTH_SHORT).show();

                //Log.e(TAG, "handleIntent::tag is null");
            }
        } else {
            Toast.makeText(this, "Phone not compatible", Toast.LENGTH_SHORT).show();

            //Log.e(TAG, "handleIntent::No action");
        }
    }
    private NdefRecord createUriRecord(String message) {
        //Log.e(TAG, "createNdefMessage::START");

        byte[] uriBytes = message.getBytes(StandardCharsets.UTF_8);
        byte[] recordPayload = new byte[uriBytes.length + 1];
        recordPayload[0] = 0x04; // Préfixe -- 0x04 = "https://" -- 0x02 = "https://www." -- 0x01 = "http://www."
        System.arraycopy(uriBytes, 0, recordPayload, 1, uriBytes.length);

        //Log.e(TAG, "createNdefMessage::END");

        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_URI, new byte[0], recordPayload);

        /*NdefRecord record = NdefRecord.createUri(message);
        return new NdefMessage(record);*/
    }
    private boolean writeNdefMessage(Tag tag, NdefMessage ndefMessage) {
        Ndef ndef = Ndef.get(tag);

        //Log.e(TAG, "writeNdefMessage::START");

        try {
            if (ndef != null) {
                ndef.connect();

                //Log.e(TAG, "writeNdefMessage::connected");

                if (ndef.isWritable()) {
                    ndef.writeNdefMessage(ndefMessage);

                    //Log.e(TAG, "writeNdefMessage::written");

                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (ndef != null) {
                    ndef.close();

                    //Log.e(TAG, "writeNdefMessage::disconnected");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //Log.e(TAG, "writeNdefMessage::ERROR");

        return false;
    }
    private void enableForegroundDispatch() {
        nfcAdapter.enableForegroundDispatch(NfsActivity.this, pendingIntent, intentFilters, techLists);
    }
    private void disableForegroundDispatch() {
        nfcAdapter.disableForegroundDispatch(NfsActivity.this);
    }
    private void closeNfsActivity() {
        disableForegroundDispatch();

        finish();
    }

}
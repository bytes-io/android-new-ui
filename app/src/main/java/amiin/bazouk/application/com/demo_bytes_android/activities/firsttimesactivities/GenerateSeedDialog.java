package amiin.bazouk.application.com.demo_bytes_android.activities.firsttimesactivities;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.widget.Button;

import amiin.bazouk.application.com.demo_bytes_android.R;

public class GenerateSeedDialog extends DialogFragment {

    private String generatedSeed;

    public GenerateSeedDialog() {
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        generatedSeed = bundle.getString("generatedSeed");

        final AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.generate_seed)
                .setMessage(R.string.message_generate_seed)
                .setCancelable(false)
                .setNegativeButton(R.string.buttons_ok, null)
                .create();

        alertDialog.show();
        return alertDialog;
    }
}

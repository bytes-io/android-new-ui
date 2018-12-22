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

public class CopySeedDialog extends DialogFragment {

    private String generatedSeed;

    public CopySeedDialog() {
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        generatedSeed = bundle.getString("generatedSeed");

        final AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.copy_seed)
                .setMessage(R.string.messages_copy_seed)
                .setCancelable(false)
                .setPositiveButton(R.string.buttons_ok, null)
                .setNegativeButton(R.string.buttons_cancel, null)
                .create();

        alertDialog.setOnShowListener(dialog -> {

            Button button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText(getActivity().getString(R.string.seed), generatedSeed);
                clipboard.setPrimaryClip(clip);
                dialog.dismiss();
            });
        });

        alertDialog.show();
        return alertDialog;
    }
}

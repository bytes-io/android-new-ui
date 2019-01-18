package amiin.bazouk.application.com.demo_bytes_android.activities;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import amiin.bazouk.application.com.demo_bytes_android.R;

public class RootDetectedDialog extends DialogFragment {

    public RootDetectedDialog() {
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        return new AlertDialog.Builder(getActivity(), R.style.AppCompatAlertDialogStyle)
                .setTitle(R.string.title_root_detected)
                .setMessage(R.string.message_root_detected)
                .setCancelable(false)
                .setPositiveButton(R.string.buttons_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        getActivity().finish();
                        System.exit(0);
                    }
                })
                .create();
    }
}

package amiin.bazouk.application.com.demo_bytes_android;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class crypto_fragment extends Fragment {

    String address = "Your current address";//Account.getCurrentAddress(this)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //to move

        View result = inflater.inflate(R.layout.crypto_fragment, container, false);

        ((TextView)result.findViewById(R.id.iota_address_deposit)).setText(address);

        result.findViewById(R.id.copy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("text to copy", ((TextView) container.findViewById(R.id.iota_address_deposit)).getText().toString());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getContext(),"Address Copied",Toast.LENGTH_SHORT).show();
            }
        });

        result.findViewById(R.id.make_withdrawal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder builder;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    builder = new AlertDialog.Builder(getContext(), android.R.style.Theme_Material_Dialog_Alert);
                } else {
                    builder = new AlertDialog.Builder(getContext());
                }
                    builder.setTitle(R.string.withdrawal_confirmed)
                            .setMessage("Hash/link")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }).setIcon(android.R.drawable.ic_dialog_alert).show();
            }
        });
        return result;
    }
}

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
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.simple.parser.ParseException;

import java.io.IOException;

import amiin.bazouk.application.com.demo_bytes_android.iota.Account;
import amiin.bazouk.application.com.demo_bytes_android.iota.AccountException;
import amiin.bazouk.application.com.demo_bytes_android.iota.ResponsePayOut;
import jota.error.ArgumentException;

public class crypto_fragment extends Fragment {

    String address;
    String currentBalance;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View result = inflater.inflate(R.layout.crypto_fragment, container, false);

        Thread getCurrentAddressThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    address = Account.getCurrentAddress(getContext());
                    currentBalance = "Current balance: $"+String.valueOf(Account.getBalance(getContext()).usd);
                } catch (AccountException e) {
                    System.out.println("Failed due to " + e.getMessage());
                    e.printStackTrace();
                }
                if(address == null){
                    address = "You do not have an account yet";
                }
                if(currentBalance == null){
                    currentBalance = "Current balance: $0";
                }
                FragmentActivity fragmentActivity= getActivity();
                if(fragmentActivity!=null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView iotaAddressDepositTextView = result.findViewById(R.id.iota_address_deposit);
                            TextView currentBalanceTextView = result.findViewById(R.id.current_balance);
                            iotaAddressDepositTextView.setTextColor(getResources().getColor(android.R.color.black));
                            currentBalanceTextView.setTextColor(getResources().getColor(android.R.color.black));
                            iotaAddressDepositTextView.setText(address);
                            currentBalanceTextView.setText(currentBalance);
                        }
                    });
                }
            }
        });
        getCurrentAddressThread.start();

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
                final TextView addressEmptyTextView =  result.findViewById(R.id.address_empty);
                addressEmptyTextView.setText("");
                final TextView amountEmptyTextView = result.findViewById(R.id.amount_empty);
                amountEmptyTextView.setText("");
                Thread makeWithdrawalThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final AlertDialog.Builder builder;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            builder = new AlertDialog.Builder(getContext(), android.R.style.Theme_Material_Dialog_Alert);
                        } else {
                            builder = new AlertDialog.Builder(getContext());
                        }
                        String messageBuilder;
                        EditText iotaAddressEditText = result.findViewById(R.id.iota_address_withdraw);
                        EditText amountWithdrawEditText = result.findViewById(R.id.amount_withdraw);
                        String iotaAddress = iotaAddressEditText.getText().toString();
                        String amountWithdraw = amountWithdrawEditText.getText().toString();
                        if(iotaAddress.isEmpty()){
                            FragmentActivity fragmentActivity = getActivity();
                            if(fragmentActivity!=null) {
                                fragmentActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        addressEmptyTextView.setText("Address empty");
                                    }
                                });
                            }
                            return;
                        }
                        else if (amountWithdraw.isEmpty()){
                            FragmentActivity fragmentActivity = getActivity();
                            if(fragmentActivity!=null) {
                                fragmentActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        amountEmptyTextView.setText("Amount empty");
                                    }
                                });
                            }
                            return;
                        }
                        ResponsePayOut responsePayOut = null;
                        try {
                            responsePayOut = Account.payOut(getContext(), iotaAddress, Long.valueOf(amountWithdraw));
                        } catch (AccountException e) {
                            System.out.println("Failed due to " + e.getMessage());
                            e.printStackTrace();
                        }
                        String hash;
                        String link;
                        hash = responsePayOut.hash;
                        link = responsePayOut.link;
                        long time = System.currentTimeMillis();
                        while(true){
                            if (!((hash == null && link == null) || System.currentTimeMillis() < time + 10000))
                                break;
                        }
                        if(hash == null || link == null){
                            messageBuilder = "Problem with withdrawal";
                        }
                        else{
                            messageBuilder = "Hash: "+hash+"\n \n Link: "+link;
                        }
                        FragmentActivity fragmentActivity = getActivity();
                        if(fragmentActivity!=null) {
                            fragmentActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    builder.setTitle(R.string.withdrawal_confirmed)
                                            .setMessage(messageBuilder)
                                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                }
                                            }).setIcon(android.R.drawable.ic_dialog_alert).show();
                                }
                            });
                        }
                    }
                });
                makeWithdrawalThread.start();
            }
        });
        return result;
    }
}

package amiin.bazouk.application.com.demo_bytes_android.fragments;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import amiin.bazouk.application.com.demo_bytes_android.R;
import amiin.bazouk.application.com.demo_bytes_android.iota.Wallet;
import amiin.bazouk.application.com.demo_bytes_android.iota.AccountException;
import amiin.bazouk.application.com.demo_bytes_android.iota.ResponsePayOut;

public class crypto_fragment extends Fragment {

    String address;
    String currentBalance;
    private AlertDialog alertDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View result = inflater.inflate(R.layout.crypto_fragment, container, false);

        Thread getCurrentAddressThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    address = Wallet.getCurrentAddress(getContext());
                } catch (AccountException e) {
                    System.out.println("Failed due to " + e.getMessage());
                    e.printStackTrace();
                }
                try {
                    currentBalance = "$"+String.valueOf(Wallet.getBalance(getContext()).usd);
                } catch (AccountException e) {
                    System.out.println("Failed due to " + e.getMessage());
                    e.printStackTrace();
                }
                if(address == null){
                    address = "Unable to show the your wallet information. Please check your internet connection";
                }
                if(currentBalance == null){
                    currentBalance = "$--";
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

        result.findViewById(R.id.withdrawBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final TextView addressEmptyTextView =  result.findViewById(R.id.address_empty);
                addressEmptyTextView.setText("");
                final TextView amountEmptyTextView = result.findViewById(R.id.amount_empty);
                amountEmptyTextView.setText("");
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext()).setTitle(R.string.withdrawal_confirmation).setPositiveButton("CLOSE", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                Thread makeWithdrawalThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String message;
                        EditText iotaAddressEditText = result.findViewById(R.id.iota_address_withdraw);
                        EditText amountWithdrawEditText = result.findViewById(R.id.amount_withdraw);
                        String iotaAddress = iotaAddressEditText.getText().toString();
                        String amountWithdraw = amountWithdrawEditText.getText().toString();
                        FragmentActivity fragmentActivity;
                        if(iotaAddress.isEmpty()){
                            fragmentActivity = getActivity();
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
                            fragmentActivity = getActivity();
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
                        fragmentActivity = getActivity();
                        if(fragmentActivity!=null) {
                            fragmentActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    alertDialog = builder.setMessage("Loading...").create();
                                    alertDialog.show();
                                }
                            });
                        }
                        ResponsePayOut responsePayOut;
                        try {
                            responsePayOut = Wallet.payOut(getContext(), iotaAddress, Long.valueOf(amountWithdraw));
                        } catch (AccountException e) {
                            System.out.println("Failed due to " + e.getMessage());
                            if(alertDialog!=null && alertDialog.isShowing()) {
                                if(fragmentActivity!=null) {
                                    fragmentActivity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (alertDialog.isShowing()) {
                                                alertDialog.setMessage("Failed due to " + e.getMessage());
                                            }
                                        }
                                    });
                                }
                            }
                            e.printStackTrace();
                            return;
                        }
                        String hash = responsePayOut.hash;
                        String link = responsePayOut.link;
                        long time = System.currentTimeMillis();
                        while(true){
                            if (!((hash == null && link == null) || System.currentTimeMillis() < time + 10000))
                                break;
                        }

                        if(hash == null || link == null){
                            message = "Problem with withdrawal";
                            fragmentActivity = getActivity();
                            if(fragmentActivity!=null) {
                                fragmentActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(alertDialog.isShowing()) {
                                            alertDialog.setMessage(message);
                                        }
                                    }
                                });
                            }
                        }
                        else{
                            message = "<p>"+"Transaction Hash: "+hash+"</p>"+"<br/>"+"<p>"+"<a href=\""+link+"\">View it on explorer</a></p>";
                            Spanned messageWithLink = Html.fromHtml(message);

                            fragmentActivity = getActivity();
                            if(fragmentActivity!=null) {
                                fragmentActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(alertDialog.isShowing()) {
                                            alertDialog.setMessage(messageWithLink);
                                            ((TextView)alertDialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
                                        }
                                    }
                                });
                            }
                        }
                    }
                });
                makeWithdrawalThread.start();
            }
        });
        return result;
    }
}

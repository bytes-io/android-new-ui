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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import amiin.bazouk.application.com.demo_bytes_android.R;
import amiin.bazouk.application.com.demo_bytes_android.iota.ResponseGetBalance;
import amiin.bazouk.application.com.demo_bytes_android.iota.Wallet;
import amiin.bazouk.application.com.demo_bytes_android.iota.AccountException;
import amiin.bazouk.application.com.demo_bytes_android.iota.ResponsePayOut;
import amiin.bazouk.application.com.demo_bytes_android.utils.Round;
import jota.dto.response.GetBalancesResponse;
import jota.utils.IotaUnits;

public class crypto_fragment extends Fragment {

    String address;
    String currentUSDBalance;
    String currentIOTABalance;
    private AlertDialog alertDialog;
    public static double currentBalance = -1;

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
                    ResponseGetBalance responseGetBalance = Wallet.getBalance(getContext());
                    currentBalance = Round.round(responseGetBalance.usd, 2);
                    currentUSDBalance = "$"+currentBalance;
                    currentIOTABalance = "("+responseGetBalance.displayIotaBal+")";
                } catch (AccountException e) {
                    System.out.println("Failed due to " + e.getMessage());
                    e.printStackTrace();
                }
                if(address == null){
                    address = "Unable to show the your wallet information. Please check your internet connection";
                }
                if(currentUSDBalance == null){
                    currentUSDBalance = "$--";
                }
                /*if(Double.parseDouble(currentUSDBalance)>5){
                    result.findViewById(R.id.current_balance);
                }*/
                FragmentActivity fragmentActivity= getActivity();
                if(fragmentActivity!=null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView iotaAddressDepositTextView = result.findViewById(R.id.iota_address_deposit);
                            TextView currentBalanceUSDTextView = result.findViewById(R.id.current_balance);
                            TextView currentBalanceIOTATextView = result.findViewById(R.id.current_balance_iota);
                            iotaAddressDepositTextView.setTextColor(getResources().getColor(android.R.color.black));
                            currentBalanceUSDTextView.setTextColor(getResources().getColor(android.R.color.black));
                            iotaAddressDepositTextView.setText(address);
                            currentBalanceUSDTextView.setText(currentUSDBalance);
                            currentBalanceIOTATextView.setText(currentIOTABalance);
                        }
                    });
                }
            }
        });
        getCurrentAddressThread.start();

        result.findViewById(R.id.copy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    ResponseGetBalance responseGetBalance = Wallet.getBalance(getContext());
                    if(Round.round(responseGetBalance.usd, 2)>3){
                        setAlertDialogBuilder("Limit reached","For your security, we request you to limit your IOTA holdings to $3. Please withdraw excess holdings before trying anything else.");
                        return;
                    }
                } catch (AccountException e) {
                    e.printStackTrace();
                }
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
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                        .setTitle(R.string.withdrawal_confirmation)
                        .setPositiveButton("CLOSE", new DialogInterface.OnClickListener() {
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
                        Spinner unitSpinner = result.findViewById(R.id.new_transfer_units_spinner);
                        String iotaAddress = iotaAddressEditText.getText().toString().trim();
                        String amountWithdraw = amountWithdrawEditText.getText().toString().trim();
                        FragmentActivity fragmentActivity;
                        if(iotaAddress.isEmpty() || !Wallet.isAddressValid(iotaAddress)){
                            fragmentActivity = getActivity();
                            if(fragmentActivity!=null) {
                                fragmentActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        addressEmptyTextView.setText("Invalid Address");
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

                            long amountWithdrawIni = amountInSelectedUnit(amountWithdraw, unitSpinner);
                            responsePayOut = Wallet.payOut(getContext(), iotaAddress, amountWithdrawIni);
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

    private void setAlertDialogBuilder(String title, String message) {
        new AlertDialog.Builder(getContext()).setTitle(title)
                .setMessage(message)
                .setPositiveButton("CLOSE", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Spinner unitsSpinner = getView().findViewById(R.id.new_transfer_units_spinner);
        initUnitsSpinner(unitsSpinner);
    }

    private void initUnitsSpinner(Spinner unitsSpinner) {

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                R.layout.spinner_item, getResources().getStringArray(R.array.listIotaUnits));
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        unitsSpinner.setAdapter(adapter);
        unitsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private Long amountInSelectedUnit(String inputAmount, Spinner unitsSpinner) {
        IotaUnits unit = toIotaUnit(unitsSpinner.getSelectedItemPosition());
        return Long.parseLong(inputAmount) * (long) Math.pow(10, unit.getValue());
    }

    private IotaUnits toIotaUnit(int unitSpinnerItemIndex) {
        IotaUnits iotaUnits;

        switch (unitSpinnerItemIndex) {
            case 0:
                iotaUnits = IotaUnits.IOTA;
                break;
            case 1:
                iotaUnits = IotaUnits.KILO_IOTA;
                break;
            case 2:
                iotaUnits = IotaUnits.MEGA_IOTA;
                break;
            case 3:
                iotaUnits = IotaUnits.GIGA_IOTA;
                break;
            case 4:
                iotaUnits = IotaUnits.TERA_IOTA;
                break;
            case 5:
                iotaUnits = IotaUnits.PETA_IOTA;
                break;
            default:
                iotaUnits = IotaUnits.IOTA;
                break;
        }

        return iotaUnits;
    }

    /*@Override
    public void onResume(){
        super.onResume();
        if(){
            butt
        }
    }*/

}

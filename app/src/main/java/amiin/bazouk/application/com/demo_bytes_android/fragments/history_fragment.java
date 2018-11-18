package amiin.bazouk.application.com.demo_bytes_android.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import amiin.bazouk.application.com.demo_bytes_android.R;
import amiin.bazouk.application.com.demo_bytes_android.iota.Account;
import amiin.bazouk.application.com.demo_bytes_android.iota.AccountException;
import amiin.bazouk.application.com.demo_bytes_android.iota.TxData;

public class history_fragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.history_fragment, container, false);
        final ListView listViewTransactions = result.findViewById(R.id.list_view_transactions);
        ArrayList<Map<String, String>> listMapOfEachTransaction = new ArrayList<>();
        SimpleAdapter adapterTransactions = new SimpleAdapterWithClick(getContext(), listMapOfEachTransaction, R.layout.items_transactions,
                new String[]{"date","value"}, new int[]{R.id.date,R.id.value});
        Thread getTransactionsThread = new Thread(new Runnable() {
            @Override
            public void run() {
                List<TxData> listTransactions = new ArrayList<>();
                try {
                    listTransactions = Account.getTransactionHistory(getContext());
                } catch (AccountException e) {
                    e.printStackTrace();
                }
                for (TxData txData : listTransactions) {
                    HashMap<String, String> mapOfTheNewTransaction = new HashMap<>();
                    mapOfTheNewTransaction.put("value", String.valueOf(txData.value));
                    mapOfTheNewTransaction.put("date", txData.date.toString());
                    listMapOfEachTransaction.add(mapOfTheNewTransaction);
                }
                FragmentActivity activity = getActivity();
                if(activity!=null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            listViewTransactions.setAdapter(adapterTransactions);
                        }
                    });
                }
            }
        });
        getTransactionsThread.start();
        return result;
    }

    private class SimpleAdapterWithClick extends SimpleAdapter {
        private SimpleAdapterWithClick(Context context, List<? extends Map<String, ?>> data, int resource,
                                                   String[] from, int[] to) {
            super(context, data, resource, from, to);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final View convertViewToReturn = super.getView(position, convertView, parent);
            convertViewToReturn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final AlertDialog.Builder builder;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        builder = new AlertDialog.Builder(getContext(), android.R.style.Theme_Material_Dialog_Alert);
                    } else {
                        builder = new AlertDialog.Builder(getContext());
                    }
                    builder.setTitle("Information Transaction")
                            .setMessage("Information about the transaction")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }).setIcon(android.R.drawable.ic_dialog_alert).show();
                }
            });
            return convertViewToReturn;
        }
    }
}

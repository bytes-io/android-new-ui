package amiin.bazouk.application.com.demo_bytes_android.fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
        //View header = getLayoutInflater().inflate(R.layout.header, null);
        //listViewTransactions.addHeaderView(header);
        SimpleAdapter adapterAlarms = new SimpleAdapterWithScrollbarsOnViews(getContext(), listMapOfEachTransaction, R.layout.items_transactions,
                new String[]{"address", "hash", "link","value","date"}, new int[]{R.id.address, R.id.hash, R.id.link,R.id.value,R.id.date});
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
                    mapOfTheNewTransaction.put("address", txData.address);
                    mapOfTheNewTransaction.put("hash", txData.hash);
                    mapOfTheNewTransaction.put("link", txData.link);
                    mapOfTheNewTransaction.put("value", String.valueOf(txData.value));
                    mapOfTheNewTransaction.put("date", txData.date.toString());
                    listMapOfEachTransaction.add(mapOfTheNewTransaction);
                }
                FragmentActivity activity = getActivity();
                if(activity!=null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //((TextView)getLayoutInflater().inflate(R.layout.items_transactions, null).findViewById(R.id.address)).setMovementMethod(new ScrollingMovementMethod());
                            listViewTransactions.setAdapter(adapterAlarms);
                        }
                    });
                }
            }
        });
        getTransactionsThread.start();
        return result;
    }

    private class SimpleAdapterWithScrollbarsOnViews extends SimpleAdapter {
        private SimpleAdapterWithScrollbarsOnViews(Context context, List<? extends Map<String, ?>> data, int resource,
                                                   String[] from, int[] to) {
            super(context, data, resource, from, to);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final View convertViewToReturn = super.getView(position, convertView, parent);
            for (int i = 0; i < 5; i++) {
                ((ViewGroup) convertViewToReturn).getChildAt(i).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((TextView)v).setMovementMethod(new ScrollingMovementMethod());
                    }
                });
            }
            ((ViewGroup) convertViewToReturn).getChildAt(2).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Uri webpage = Uri.parse(((TextView)v).getText().toString());
                    Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
                    if (intent.resolveActivity(getContext().getPackageManager()) != null) {
                        startActivity(intent);
                    }
                }
            });
            return convertViewToReturn;
        }
    }
}

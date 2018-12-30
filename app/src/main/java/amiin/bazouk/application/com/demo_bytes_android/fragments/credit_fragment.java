package amiin.bazouk.application.com.demo_bytes_android.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import amiin.bazouk.application.com.demo_bytes_android.R;
import amiin.bazouk.application.com.demo_bytes_android.activities.AddACreditCardActivity;
import amiin.bazouk.application.com.demo_bytes_android.activities.MainActivity;
import amiin.bazouk.application.com.demo_bytes_android.iota.TxData;

public class credit_fragment extends Fragment {

    private static final String CREDIT_CARD_NUMBER = "credit_card_number";
    private ListView listViewTransactions;
    private ArrayList<Map<String, String>> listMapOfEachCreditCard;
    private SimpleAdapter adapterCreditCards;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.credit_fragment, container, false);
        listViewTransactions = result.findViewById(R.id.list_view_credit_cards);
        //updateListView();
        result.findViewById(R.id.add_credit_card_textview).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                startActivity(new Intent(getContext(), AddACreditCardActivity.class));

                final AlertDialog.Builder builder;
                builder = new AlertDialog.Builder(getContext()).setTitle("Add a new Credit Card").setPositiveButton("CLOSE", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                String message = "Coming soon";

                FragmentActivity fragmentActivity = getActivity();
                if(fragmentActivity!=null) {
                    fragmentActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog alertDialog = builder.setMessage(message).create();
                            alertDialog.show();

                        }
                    });
                }
            }
        });
        return result;
    }

    /*private void updateListView() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        Set<String> setCreditCard = preferences.getStringSet(CREDIT_CARD_NUMBER,new HashSet<>());
        listMapOfEachCreditCard = new ArrayList<>();
        adapterCreditCards = new SimpleAdapter(getContext(), listMapOfEachCreditCard, R.layout.items_credit_cards,
                new String[]{"credit_card","credit_card_number"}, new int[]{R.id.credit_card, R.id.credit_card_number});
        Iterator<String> it = setCreditCard.iterator();
        while (it.hasNext()) {
            HashMap<String, String> mapOfTheNewCreditCard = new HashMap<>();
            mapOfTheNewCreditCard.put("credit_card", "Credit Card");
            StringBuilder creditCardNumber= new StringBuilder();
            int i = 0;
            String creditCardInformation = it.next();
            while(creditCardInformation.charAt(i)!='-'){
                creditCardNumber.append(it.next().charAt(i));
                i++;
            }
            StringBuilder creditCardNumberHidden = new StringBuilder();
            for(int j = 0;j<creditCardNumber.length()-4;j++){
                creditCardNumberHidden.append('*');
            }
            creditCardNumberHidden.append(creditCardNumber.substring(creditCardNumber.length()-4));
            mapOfTheNewCreditCard.put("credit_card_number", creditCardNumberHidden.toString());
            listMapOfEachCreditCard.add(mapOfTheNewCreditCard);
        }
        FragmentActivity activity = getActivity();
        if(activity!=null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    listViewTransactions.setAdapter(adapterCreditCards);
                }
            });
        }
    }*/

    @Override
    public void onResume(){
        super.onResume();
        //updateListView();
    }
}

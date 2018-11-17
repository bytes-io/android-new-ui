package amiin.bazouk.application.com.demo_bytes_android.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.Map;

import amiin.bazouk.application.com.demo_bytes_android.R;
import amiin.bazouk.application.com.demo_bytes_android.activities.AddACreditCardActivity;
import amiin.bazouk.application.com.demo_bytes_android.activities.MainActivity;

public class credit_fragment extends Fragment {

    private ListView listViewTransactions;
    private ArrayList<Map<String, String>> listMapOfEachCreditCard;
    private SimpleAdapter adapterCreditCards;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.credit_fragment, container, false);

        listViewTransactions = result.findViewById(R.id.list_view_credit_cards);
        listMapOfEachCreditCard = new ArrayList<>();
        adapterCreditCards = new SimpleAdapter(getContext(), listMapOfEachCreditCard, R.layout.items_credit_cards,
                new String[]{"credit_card","credit_card_number"}, new int[]{R.id.credit_card, R.id.credit_card_number});

        result.findViewById(R.id.add_credit_card_textview).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), AddACreditCardActivity.class));
            }
        });
        return result;
    }
}

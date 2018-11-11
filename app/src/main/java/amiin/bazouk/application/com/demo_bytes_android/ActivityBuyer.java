package amiin.bazouk.application.com.demo_bytes_android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

import org.json.simple.parser.ParseException;

import java.io.IOException;

import amiin.bazouk.application.com.demo_bytes_android.iota.Account;

public class ActivityBuyer extends AppCompatActivity {
    public static final String PREF_MAX_PRICE = "pref_max_price";

    private double rate = -1;
    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buyer);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        float maxPrice = Float.parseFloat(sharedPref
                .getString(
                    PREF_MAX_PRICE,
                    this.getResources().getString(R.string.default_pref_max_price)
                ));

        System.out.println("onCreate maxPrice: " + maxPrice);
        ((EditText)findViewById(R.id.max_price)).setText(Float.toString(maxPrice));

        Thread conversionThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    rate = Account.getPriceUSD();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        });
        conversionThread.start();

        findViewById(R.id.set_max_price).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent result = new Intent();

                String maxPrice = ((EditText)findViewById(R.id.max_price)).getText().toString();
                System.out.println("Setting new maxPrice: " + maxPrice);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(PREF_MAX_PRICE, maxPrice);
                editor.apply();

                setResult(RESULT_OK, result);
                finish();
            }
        });

        /*
        TextWatcher fieldValidatorTextWatcher = new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if(!s.toString().isEmpty()) {
                    if(rate!=-1) {
                        ((TextView) findViewById(R.id.usd)).setText(String.valueOf(Double.valueOf(s.toString()) * rate));
                    }
                }
                else{
                    ((TextView) findViewById(R.id.usd)).setText("");
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        };
        ((EditText)findViewById(R.id.amount_iota)).addTextChangedListener(fieldValidatorTextWatcher);*/
    }
}

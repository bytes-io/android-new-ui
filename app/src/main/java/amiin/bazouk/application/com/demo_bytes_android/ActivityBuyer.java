package amiin.bazouk.application.com.demo_bytes_android;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

import org.json.simple.parser.ParseException;

import java.io.IOException;

import amiin.bazouk.application.com.demo_bytes_android.iota.Account;

public class ActivityBuyer extends AppCompatActivity {

    private double rate = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buyer);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
                long amount = Long.parseLong(((EditText)findViewById(R.id.amount_iota)).getText().toString());
                result.putExtra(MainActivity.AMOUNT_INTENT, amount);
                setResult(RESULT_OK, result);
                finish();
            }
        });

        /*findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                finish();
            }
        });

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

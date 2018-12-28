package amiin.bazouk.application.com.demo_bytes_android.activities;

import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import amiin.bazouk.application.com.demo_bytes_android.Constants;
import amiin.bazouk.application.com.demo_bytes_android.R;

public class ActivitySeller extends AppCompatActivity {
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        Toolbar toolbar = findViewById(R.id.toolbar);

        if(preferences.getBoolean(Constants.IS_BUYER,false) || preferences.getBoolean(Constants.IS_SELLER,false)){
            findViewById(R.id.appbar).setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.green)));
            toolbar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.green)));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(ContextCompat.getColor(this,R.color.green));
            }
        }
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_back_arrow);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        float maxPrice = Float.parseFloat(preferences.getString(
                Constants.PREF_MAX_PRICE_SELLER,
                this.getResources().getString(R.string.default_pref_max_price)
        ));

        System.out.println("onCreate maxPrice: " + maxPrice);
        ((EditText)findViewById(R.id.max_price)).setText(Float.toString(maxPrice));

        findViewById(R.id.set_max_price).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                String maxPriceText = ((EditText)findViewById(R.id.max_price)).getText().toString();
                System.out.println("Setting new maxPrice: " + maxPriceText);

                // save to pref
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(Constants.PREF_MAX_PRICE_SELLER,  maxPriceText);
                editor.apply();

                finish();
            }
        });

        TextWatcher fieldValidatorTextWatcher = new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String maxPriceText = s.toString();
                System.out.println("maxPriceText TextWatcher" + maxPriceText);

                if(maxPriceText.isEmpty()) {
                    disableSetMaxPrice();
                    return;
                } else {
                    enableSetMaxPrice();
                }

                float maxPrice = Float.parseFloat(maxPriceText);
                if(maxPrice > 100){
                    ((EditText)findViewById(R.id.max_price)).setText("");
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        };
        ((EditText)findViewById(R.id.max_price)).addTextChangedListener(fieldValidatorTextWatcher);
    }

    private void disableSetMaxPrice() {
        findViewById(R.id.set_max_price).setAlpha(.5f);
        findViewById(R.id.set_max_price).setEnabled(false);
    }

    private void enableSetMaxPrice() {
        findViewById(R.id.set_max_price).setAlpha(1);
        findViewById(R.id.set_max_price).setEnabled(true);
    }
}

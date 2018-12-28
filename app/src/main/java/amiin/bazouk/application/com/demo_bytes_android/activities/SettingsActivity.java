package amiin.bazouk.application.com.demo_bytes_android.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import amiin.bazouk.application.com.demo_bytes_android.Constants;
import amiin.bazouk.application.com.demo_bytes_android.R;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        findViewById(R.id.layout_max_price_buyer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this,ActivityBuyer.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.layout_max_price_seller).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this,ActivitySeller.class);
                startActivity(intent);
            }
        });

        float maxPriceBuyer = Float.parseFloat(preferences.getString(
                Constants.PREF_MAX_PRICE_BUYER,
                this.getResources().getString(R.string.default_pref_max_price)
        ));
        ((TextView)findViewById(R.id.textview_max_price_buyer)).setText(Float.toString(maxPriceBuyer));

        float maxPriceSeller = Float.parseFloat(preferences.getString(
                Constants.PREF_MAX_PRICE_SELLER,
                this.getResources().getString(R.string.default_pref_max_price)
        ));
        ((TextView)findViewById(R.id.textview_max_price_seller)).setText(Float.toString(maxPriceSeller));

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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.settings:
                intent = new Intent(SettingsActivity.this, SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.payment:
                intent = new Intent(SettingsActivity.this, Payment.class);
                startActivity(intent);
                break;
            case R.id.history:
                intent = new Intent(SettingsActivity.this, Payment.class);
                intent.putExtra("is_history_intent", true);
                startActivity(intent);
                break;
        }
        return true;
    }

    @Override
    protected void onResume(){
        super.onResume();
        float maxPriceBuyer = Float.parseFloat(PreferenceManager.getDefaultSharedPreferences(this).getString(
                Constants.PREF_MAX_PRICE_BUYER,
                this.getResources().getString(R.string.default_pref_max_price)
        ));
        ((TextView)findViewById(R.id.textview_max_price_buyer)).setText(Float.toString(maxPriceBuyer));
        float maxPriceSeller = Float.parseFloat(PreferenceManager.getDefaultSharedPreferences(this).getString(
                Constants.PREF_MAX_PRICE_SELLER,
                this.getResources().getString(R.string.default_pref_max_price)
        ));
        ((TextView)findViewById(R.id.textview_max_price_seller)).setText(Float.toString(maxPriceSeller));
    }
}

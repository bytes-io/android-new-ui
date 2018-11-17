package amiin.bazouk.application.com.demo_bytes_android.activities;

import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;

import amiin.bazouk.application.com.demo_bytes_android.R;

public class AddACreditCardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_a_credit_card);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        Toolbar toolbar = findViewById(R.id.toolbar);

        if(preferences.getBoolean(MainActivity.IS_BUYER,false) || preferences.getBoolean(MainActivity.IS_SELLER,false)){
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

        findViewById(R.id.ok_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}

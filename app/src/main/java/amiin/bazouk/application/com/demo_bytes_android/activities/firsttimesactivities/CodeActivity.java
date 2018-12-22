package amiin.bazouk.application.com.demo_bytes_android.activities.firsttimesactivities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import amiin.bazouk.application.com.demo_bytes_android.Pinview;
import amiin.bazouk.application.com.demo_bytes_android.R;
import amiin.bazouk.application.com.demo_bytes_android.activities.MainActivity;

import static amiin.bazouk.application.com.demo_bytes_android.activities.firsttimesactivities.JoinActivity.CODE;
import static amiin.bazouk.application.com.demo_bytes_android.activities.firsttimesactivities.JoinActivity.IS_FIRST_TIME;

public class CodeActivity extends AppCompatActivity {

    private String code;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_code);

        code = getIntent().getStringExtra(CODE);
        ((Pinview)findViewById(R.id.pinview)).setPinViewEventListener(new Pinview.PinViewEventListener() {
            @Override
            public void onDataEntered(Pinview pinview, boolean fromUser) {
                if(code.equals(pinview.getValue())) {
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
                    editor.putBoolean(IS_FIRST_TIME,false);
                    editor.apply();
                    startActivity(new Intent(CodeActivity.this,MainActivity.class));
                    //finish();
                }
            }
        });
    }
}

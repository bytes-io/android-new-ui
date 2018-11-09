package amiin.bazouk.application.com.demo_bytes_android;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.RadioButton;

public class SellActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sell_activity);
        setUpToolBar();
    }

    private void setUpToolBar(){

        ((RadioButton)findViewById(R.id.radio_button_2)).setChecked(true);

        findViewById(R.id.radio_button_1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SellActivity.this,MainActivity.class));
            }
        });

        findViewById(R.id.radio_button_3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SellActivity.this,ConnectActivity.class));
            }
        });

        findViewById(R.id.radio_button_4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SellActivity.this,ConnectActivity.class));
            }
        });
    }
}

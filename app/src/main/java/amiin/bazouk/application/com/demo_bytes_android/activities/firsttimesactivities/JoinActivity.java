package amiin.bazouk.application.com.demo_bytes_android.activities.firsttimesactivities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import amiin.bazouk.application.com.demo_bytes_android.R;
import amiin.bazouk.application.com.demo_bytes_android.activities.MainActivity;

public class JoinActivity extends AppCompatActivity {

    static final String IS_FIRST_TIME = "is_first_time";
    static final String CODE = "code";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);
        findViewById(R.id.go_to_code).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String code = "123456";
                sendEmailWithCode(code);
                Intent intent = new Intent(JoinActivity.this, CodeActivity.class);
                intent.putExtra(CODE,code);
                startActivity(intent);
            }
        });

    }

    private void sendEmailWithCode(String code) {
            Thread sendEmailThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String recipient = ((TextView)findViewById(R.id.enter_your_email)).getText().toString();
                        GMailSender sender = new GMailSender(getResources().getString(R.string.username), getResources().getString(R.string.password));
                        sender.sendMail("This is Subject",
                                code,
                                "boukobza.adr@gmail.com",
                                recipient);
                    } catch (Exception e) {
                        Log.e("SendMail", e.getMessage(), e);
                    }
                }
            });
            sendEmailThread.start();
    }

    protected void onStart(){
        super.onStart();
        boolean isFirstTime = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(IS_FIRST_TIME,true);
        if(!PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(IS_FIRST_TIME,true)){
            startActivity(new Intent(this, MainActivity.class));
        }
    }
}

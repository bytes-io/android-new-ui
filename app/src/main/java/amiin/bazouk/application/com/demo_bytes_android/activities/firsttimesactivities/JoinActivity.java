package amiin.bazouk.application.com.demo_bytes_android.activities.firsttimesactivities;

import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import amiin.bazouk.application.com.demo_bytes_android.Constants;
import amiin.bazouk.application.com.demo_bytes_android.R;
import amiin.bazouk.application.com.demo_bytes_android.activities.MainActivity;
import amiin.bazouk.application.com.demo_bytes_android.iota.Seed;
import amiin.bazouk.application.com.demo_bytes_android.iota.SeedValidator;
import jota.utils.SeedRandomGenerator;

public class JoinActivity extends AppCompatActivity {

    private TextInputEditText seedEditText;
    private TextInputLayout seedEditTextLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);
        seedEditText = findViewById(R.id.seed_login_seed_input);
        seedEditTextLayout = findViewById(R.id.seed_login_seed_text_input_layout);

        findViewById(R.id.generate_seed_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String generatedSeed = SeedRandomGenerator.generateNewSeed();
                seedEditText.setText(generatedSeed);
                Bundle bundle = new Bundle();
                bundle.putString("generatedSeed", generatedSeed);
                CopySeedDialog dialog = new CopySeedDialog();
                dialog.setArguments(bundle);
                dialog.show(getFragmentManager(), null);
            }
        });

        findViewById(R.id.go_to_code).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginDialog();
            }
        });
    }

    private void login(){
        String code = "123456";
        sendEmailWithCode(code);
        Intent intent = new Intent(JoinActivity.this, CodeActivity.class);
        intent.putExtra(Constants.CODE,code);
        startActivity(intent);
    }

    private void sendEmailWithCode(String code) {
        final String recipient = ((TextInputEditText)findViewById(R.id.enter_your_email)).getText().toString();
        Thread sendEmailThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    GMailSender sender = new GMailSender(getResources().getString(R.string.username), getResources().getString(R.string.password));
                    sender.sendMail("This is Subject",
                            code,
                            recipient,
                            recipient);
                } catch (Exception e) {
                    Log.e("SendMail", e.getMessage(), e);
                }
            }
        });
        sendEmailThread.start();
    }

    private void loginDialog() {
        String seed = seedEditText.getText().toString();

        if (seed.isEmpty()) {
            seedEditTextLayout.setError(getString(R.string.messages_empty_seed));
            if (seedEditTextLayout.getError() != null)
                return;
        }

        if (SeedValidator.isSeedValid(this, seed) == null) {
            try {
                Seed.saveSeed(getApplicationContext(), seed);
            } catch (Exception e) {
                e.printStackTrace();
            }

            login();

        } else {
            AlertDialog loginDialog = new AlertDialog.Builder(this)
                    .setMessage(SeedValidator.isSeedValid(this, seed))
                    .setCancelable(false)
                    .setPositiveButton(R.string.buttons_ok, null)
                    .setNegativeButton(R.string.buttons_cancel, null)
                    .create();

            loginDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.buttons_login), (dialog, which) -> login());

            loginDialog.show();
        }
    }

    protected void onStart(){
        super.onStart();
        if(!PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(Constants.IS_FIRST_TIME,true)){
            startActivity(new Intent(this, MainActivity.class));
        }
    }
}

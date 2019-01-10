package amiin.bazouk.application.com.demo_bytes_android.activities.firsttimesactivities;

import android.content.Intent;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import amiin.bazouk.application.com.demo_bytes_android.Constants;
import amiin.bazouk.application.com.demo_bytes_android.R;
import amiin.bazouk.application.com.demo_bytes_android.activities.MainActivity;
import amiin.bazouk.application.com.demo_bytes_android.iota.Seed;
import amiin.bazouk.application.com.demo_bytes_android.iota.SeedValidator;
import amiin.bazouk.application.com.demo_bytes_android.utils.EmailValidator;
import jota.utils.SeedRandomGenerator;

public class JoinActivity extends AppCompatActivity {

    private TextInputEditText seedEditText;
    private TextInputLayout seedEditTextLayout;
    private TextInputEditText emailEditText;
    private TextInputLayout emailEditTextLayout;
    private Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);
        loginButton = findViewById(R.id.go_to_code);
        disableLoginButton();

        seedEditText = findViewById(R.id.seed_login_seed_input);
        seedEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.toString().isEmpty()) {
                    disableLoginButton();
                }
                else{
                    enableLoginButton();
                }
            }
        });
        seedEditTextLayout = findViewById(R.id.seed_login_seed_text_input_layout);

        emailEditText = findViewById(R.id.email_input);
        emailEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.toString().isEmpty()) {
                    disableLoginButton();
                }
                else{
                    enableLoginButton();
                }
            }
        });
        emailEditTextLayout = findViewById(R.id.email_login_text_input_layout);


        findViewById(R.id.generate_seed_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String generatedSeed = SeedRandomGenerator.generateNewSeed();
                seedEditText.setText(generatedSeed);
                Bundle bundle = new Bundle();
                bundle.putString("generatedSeed", generatedSeed);
                GenerateSeedDialog dialog = new GenerateSeedDialog();
                dialog.setArguments(bundle);
                dialog.show(getFragmentManager(), null);
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginDialog();
            }
        });
    }

    private void enableLoginButton() {
        loginButton.setEnabled(true);
        loginButton.setBackgroundColor(Color.parseColor("#FFAD6BEF"));
        loginButton.setAlpha(1);
    }

    private void disableLoginButton() {
        loginButton.setEnabled(false);
        loginButton.setBackgroundColor(Color.GRAY);
        loginButton.setAlpha(.5f);
    }

    private void login(){
        String code = "123456";
        sendEmailWithCode(code);
        Intent intent = new Intent(JoinActivity.this, CodeActivity.class);
        intent.putExtra(Constants.CODE,code);
        startActivity(intent);
    }

    private void sendEmailWithCode(String code) {
        final String recipient = ((TextInputEditText)findViewById(R.id.email_input)).getText().toString();
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
        String email = emailEditText.getText().toString();

        if (SeedValidator.isSeedValid(this, seed) == null) {
            seedEditTextLayout.setError(getString(R.string.messages_invalid_seed));
            if (seedEditTextLayout.getError() != null)
                return;
        }

        if(!EmailValidator.isValid(email)) {
            emailEditTextLayout.setError(getString(R.string.messages_invalid_email));
            if (emailEditTextLayout.getError() != null)
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
                    .setNegativeButton(R.string.buttons_ok, null)
                    .create();

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

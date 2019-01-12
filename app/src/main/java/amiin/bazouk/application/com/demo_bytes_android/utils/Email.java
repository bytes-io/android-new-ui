package amiin.bazouk.application.com.demo_bytes_android.utils;

import okhttp3.*;
import java.io.IOException;
import org.json.simple.parser.ParseException;

public class Email {

    private String domainName;
    private String apiKey;

    public Email(String domainName2, String apiKey2) {
        domainName = domainName2;
        apiKey = apiKey2;
    }

    public void sendAuthEmail(String toEmail, String code) throws IOException, ParseException {
        String url = "https://api.mailgun.net/v3/" + domainName + "/messages";

        RequestBody formBody = new FormBody.Builder()
                .add("from", "info@bytes.io")
                .add("to", toEmail)
                .add("subject", "Bytes email verification")
                .add("text", "Your login passcode is " + code)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new BasicAuthInterceptor("api", apiKey))
                .build();
        Response response = client.newCall(request).execute();

        System.out.println(response.body().string());
    }
}

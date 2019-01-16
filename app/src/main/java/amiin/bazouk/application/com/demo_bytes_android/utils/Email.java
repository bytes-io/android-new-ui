package amiin.bazouk.application.com.demo_bytes_android.utils;

import java.io.IOException;

import okhttp3.*;

public class Email {

    private String domainName;
    private String apiKey;

    public Email(String domainName2, String apiKey2) {
        domainName = domainName2;
        apiKey = apiKey2;
    }

    public ResponseBody sendAuthEmail(String toEmail, String code) throws Exception {
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
        Response response;
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
            throw new Exception("Unable to send email.", e);
        }

        return response.body();
    }
}

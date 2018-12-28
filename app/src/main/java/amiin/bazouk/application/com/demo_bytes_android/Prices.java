package amiin.bazouk.application.com.demo_bytes_android;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;

import okhttp3.*;


public class Prices {
    private OkHttpClient client = new OkHttpClient();

    public float get(String ticker) throws IOException, ParseException {
        String url = "https://www.bytes.io/api/prices?tickers[]=" + ticker;
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        String jsonData = response.body().string();

        JSONParser parser = new JSONParser();

        Object obj = parser.parse(jsonData);
        JSONArray jsonArray = (JSONArray) obj;

        JSONObject tickerObj = (JSONObject) jsonArray.get(0);

        String tickerPrice = (String) tickerObj.get("price");
        return Float.parseFloat(tickerPrice);
    }
}


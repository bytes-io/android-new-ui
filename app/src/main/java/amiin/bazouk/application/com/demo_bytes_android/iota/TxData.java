package amiin.bazouk.application.com.demo_bytes_android.iota;

import java.util.Date;

import com.google.gson.Gson;

import jota.model.Transaction;
import jota.utils.IotaUnitConverter;

public class TxData {
    public long timestamp;
    public Date date;
    public String address;
    public String hash;
    public Boolean persistence;
    public long value;
    public String message;
    public String tag;
    public String displayIotaBal;

    public TxData(long timestamp, String address, String hash, Boolean persistence,
                  long value, String message, String tag) {
        this.timestamp = timestamp;
        this.address = address;
        this.hash = hash;
        this.persistence = persistence;
        this.value = value;
        this.message = message;
        this.tag = tag;

        Date date = new Date(timestamp*1000);
        this.date = date;


        this.displayIotaBal = IotaUnitConverter.convertRawIotaAmountToDisplayText(value, false);
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
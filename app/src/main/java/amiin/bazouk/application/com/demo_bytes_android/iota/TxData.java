package amiin.bazouk.application.com.demo_bytes_android.iota;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jota.model.Transaction;

public class TxData {
    public String hash;
    public Date date;
    public Long value;
    public String address;

    public TxData(Transaction tx) {
        this.hash = tx.getHash();

        Date date = new Date((long)tx.getTimestamp()*1000);
        this.date = date;

        this.value = tx.getValue();
        this.address = tx.getAddress();
    }
}
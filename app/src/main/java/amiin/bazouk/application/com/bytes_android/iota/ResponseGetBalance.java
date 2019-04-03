package amiin.bazouk.application.com.bytes_android.iota;

import jota.utils.IotaUnitConverter;

public class ResponseGetBalance {
    public long iota;
    public double usd;
    public String displayIotaBal;

    public ResponseGetBalance(long iota, double usd) {
        this.iota = iota;
        this.usd = usd;
        this.displayIotaBal = IotaUnitConverter.convertRawIotaAmountToDisplayText(iota, false);
    }
}

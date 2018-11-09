package amiin.bazouk.application.com.demo_bytes_android.iota;

class ResponseGetBalance {
    public long miota;
    public double usd;

    public ResponseGetBalance(long miota, double usd) {
        this.miota = miota;
        this.usd = usd;
    }
}

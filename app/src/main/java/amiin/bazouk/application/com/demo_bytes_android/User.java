package amiin.bazouk.application.com.demo_bytes_android;

public class User {

    private long earned;
    private long spent;

    public User(long earned,long spent){
        this.earned = earned;
        this.spent = spent;
    }

    public long getEarned() {
        return earned;
    }

    public long getSpent() {
        return spent;
    }

    public void earn(long valueEarned){
        earned+=valueEarned;
    }

    public void spent(long valueSpent){
        spent+=valueSpent;
    }
}

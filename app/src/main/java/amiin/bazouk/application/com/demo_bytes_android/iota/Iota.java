package amiin.bazouk.application.com.demo_bytes_android.iota;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jota.IotaAPI;
import jota.dto.response.GetNewAddressResponse;
import jota.dto.response.GetNodeInfoResponse;
import jota.error.ArgumentException;
import jota.model.Input;
import jota.model.Transaction;
import jota.model.Transfer;


public class Iota {
    private IotaAPI iotaApi;
    private String seed;

    public int minWeightMagnitude = 14;
    public int depth = 3;
    public int security = 2;

    public Iota(String protocol, String host, String port, String seed)
    {
        iotaApi = new IotaAPI.Builder()
                .protocol(protocol)
                .host(host)
                .port(port)
                .build();
        this.seed = seed;
    }

    public String getLatestMilestone() throws ArgumentException {
        GetNodeInfoResponse nodeInfo = iotaApi.getNodeInfo();
        String latestMilestoneHash = nodeInfo.getLatestMilestone();
        // System.out.println("\n NodeInfo: Latest Milestone Index: " + latestMilestoneHash);

        return latestMilestoneHash;
    }

    public List<String> makeTx(String addressTo, long amountIni) throws ArgumentException {
        boolean validateInputs = true;
        List<Input> inputs = new ArrayList<Input>();
        List<Transaction> tips = new ArrayList<Transaction>();

        List<Transfer> transfers = new ArrayList<Transfer>();
        transfers.add(new Transfer(addressTo, amountIni));

        String remainderAddress = getNewAddress();

        // bundle prep for all transfers
        System.out.println("before prepareTransfers: " + DateFormat.getDateTimeInstance()
                .format(new Date()) );
        List<String> trytesBundle = iotaApi.prepareTransfers(seed, security, transfers, remainderAddress, inputs, tips, validateInputs);
        System.out.println("after prepareTransfers: " + DateFormat.getDateTimeInstance()
                .format(new Date()) );

        String[] trytes = trytesBundle.toArray(new String[0]);
        String reference = getLatestMilestone();

        System.out.println("before sendTrytes: " + DateFormat.getDateTimeInstance()
                .format(new Date()) );
        List<Transaction> transactions = iotaApi.sendTrytes(trytes, depth, minWeightMagnitude, reference);
        System.out.println("after sendTrytes: " + DateFormat.getDateTimeInstance()
                .format(new Date()) );
        System.out.println("\n transactions: " + transactions);


        List<String> tails = new ArrayList<String>();
        for (Transaction t : transactions) {
            tails.add(t.getHash());
        }
        return tails;
    }

    public Boolean verifyTx(List<String> tails) {
        return true;
    }

    public String getNewAddress() throws ArgumentException {
        int index = 0;
        boolean checksum = false;
        int total = 1;
        boolean returnAll = false;

        GetNewAddressResponse getNewAddressResponse = iotaApi.getNewAddress(seed, security, index, checksum, total, returnAll);
        return getNewAddressResponse.getAddresses().get(0);

    }
}
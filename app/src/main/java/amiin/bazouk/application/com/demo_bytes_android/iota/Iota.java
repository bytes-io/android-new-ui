package amiin.bazouk.application.com.demo_bytes_android.iota;

import java.util.ArrayList;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import jota.IotaAPI;
import jota.dto.response.GetBalancesAndFormatResponse;
import jota.dto.response.GetNewAddressResponse;
import jota.dto.response.GetNodeInfoResponse;
import jota.dto.response.GetTransactionsToApproveResponse;
import jota.dto.response.GetTransferResponse;
import jota.dto.response.GetTrytesResponse;
import jota.error.ArgumentException;
import jota.model.Bundle;
import jota.model.Input;
import jota.model.Transaction;
import jota.model.Transfer;
import jota.utils.Checksum;
import jota.utils.IotaAPIUtils;
import jota.utils.StopWatch;

public class Iota {
    private IotaAPI iotaAPI;
    private String seed;
    public String provider;

    public int minWeightMagnitude = 14;
    public int depth = 3;
    public int security = 2;

    public Iota(String provider, String seed) throws MalformedURLException {
        URL aURL = new URL(provider);

        String protocol = aURL.getProtocol();
        String host = aURL.getHost();
        String port = Integer.toString(aURL.getPort());
        iotaAPI = new IotaAPI.Builder().protocol(protocol).host(host).port(port).build();

        this.provider = provider;
        this.seed = seed;
    }

    public boolean isNodeUp() {
        String latestMilestone = null;

        try {
            latestMilestone = this.getLatestMilestone();
            System.out.println("Node conn success:" + this.provider);
            System.out.println("LatestMilestone: " + latestMilestone);

        } catch (IllegalStateException e) {
            System.err.println("\nERROR: host is down: " + e.getMessage());

        } catch (Throwable e) {
            System.err.println("\nERROR: Something went wrong: " + e.getMessage());
            e.printStackTrace();
        }

        return latestMilestone != null;

    }

    public String getLatestMilestone() throws ArgumentException {
        GetNodeInfoResponse nodeInfo = iotaAPI.getNodeInfo();
        String latestMilestoneHash = nodeInfo.getLatestMilestone();
        // System.out.println("\n NodeInfo: Latest Milestone Index: " +
        // latestMilestoneHash);

        return latestMilestoneHash;
    }

    public List<String> makeTx(String addressTo, long amountIni) throws ArgumentException {
        boolean validateInputs = true;
        List<Input> inputs = new ArrayList<Input>();
        List<Transaction> tips = new ArrayList<Transaction>();

        List<Transfer> transfers = new ArrayList<Transfer>();
        transfers.add(new Transfer(addressTo, amountIni));

        String remainderAddress = this.getCurrentAddress();

        // bundle prep for all transfers
        System.out.println("before prepareTransfers: " + DateFormat.getDateTimeInstance().format(new Date()));
        List<String> trytesBundle = iotaAPI.prepareTransfers(seed, security, transfers, remainderAddress, inputs, tips,
                validateInputs);
        System.out.println("after prepareTransfers: " + DateFormat.getDateTimeInstance().format(new Date()));

        String[] trytes = trytesBundle.toArray(new String[0]);
        String reference = getLatestMilestone();

        System.out.println("before sendTrytes: " + DateFormat.getDateTimeInstance().format(new Date()));
        List<Transaction> transactions = iotaAPI.sendTrytes(trytes, depth, minWeightMagnitude, reference);
        System.out.println("after sendTrytes: " + DateFormat.getDateTimeInstance().format(new Date()));
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

    public String getCurrentAddress() throws ArgumentException {
        int index = this.getAvailableAddressIndex(null);
        return this.getAddress(index);
    }

    public long getBalance() throws ArgumentException {

        String currentAddress = this.getCurrentAddress();
        List<String> tips = new ArrayList<String>();
        long threshold = 0;
        int start = 2; // currentAddressIndex
        StopWatch stopWatch = new StopWatch();

        GetBalancesAndFormatResponse res = iotaAPI.getBalanceAndFormat(Arrays.asList(currentAddress), tips, threshold,
                start, stopWatch, security);
        return res.getTotalBalance();
    }

    public List<TxData> getTransactions() throws ArgumentException {
        Integer start = 0;
        Integer end = 10;
        Boolean inclusionStates = true;

        List<String> attachedAddresses = this.getAddresses();
        List<TxData> txDataList = new ArrayList<>();
        List<Address> addresses = new ArrayList<>();

        long totalValue;
        long timestamp = 0;
        String address;
        String hash = null;
        Boolean persistence = null;
        long value;
        String tag = null;
        String message = "";
        String destinationAddress = null;
        long balance;

        GetTransferResponse getTransferResponse = iotaAPI.getTransfers(seed, security, start, end, inclusionStates);
        Bundle[] transferBundle = getTransferResponse.getTransfers();
        System.out.println(transferBundle.length);
        if (transferBundle != null) {
            for (Bundle aTransferBundle : transferBundle) {

                totalValue = 0;

                for (Transaction trx : aTransferBundle.getTransactions()) {

//                	System.out.println(trx.getAddress());
                    address = trx.getAddress();
                    persistence = trx.getPersistence();
                    value = trx.getValue();

                    if (value != 0 && attachedAddresses.contains(Checksum.addChecksum(address)))
                        totalValue += value;

                    if (trx.getCurrentIndex() == 0) {
                        timestamp = trx.getAttachmentTimestamp() / 1000;
                        tag = trx.getTag();
                        destinationAddress = address;
                        hash = trx.getHash();
                    }

                    // check if sent transaction
                    if (attachedAddresses.contains(Checksum.addChecksum(address))) {
                        boolean isRemainder = (trx.getCurrentIndex() == trx.getLastIndex()) && trx.getLastIndex() != 0;
                        if (value < 0 && !isRemainder) {

                            if (addresses.contains(new Address(Checksum.addChecksum(address), false)))
                                addresses.remove(new Address(Checksum.addChecksum(address), false));

                            if (!addresses.contains(new Address(Checksum.addChecksum(address), true)))
                                addresses.add(new Address(Checksum.addChecksum(address), true));
                        } else {
                            if (!addresses.contains(new Address(Checksum.addChecksum(address), true)) &&
                                    !addresses.contains(new Address(Checksum.addChecksum(address), false)))
                                addresses.add(new Address(Checksum.addChecksum(address), false));
                        }
                    }
                }

                txDataList.add(new TxData(timestamp, destinationAddress, hash, persistence, totalValue, message, tag));

            }
        }
        return txDataList;
    }

    public List<String> getAddresses() throws ArgumentException {
        List<String> addresses = new ArrayList<String>();

        int i = -1;
        while (true) {
            i++;
            String newAddress = this.getAddress(i);
            if (iotaAPI.findTransactionsByAddresses(new String[] { newAddress }).getHashes().length == 0) {
                return addresses;
            } else {
                addresses.add(newAddress);
            }
        }
    }

    public List<Transaction> findTransactionsObjectsByHashes(String[] hashes) throws ArgumentException {
        return iotaAPI.findTransactionsObjectsByHashes(hashes);
    }

    public Integer getAvailableAddressIndex(Integer lastKnownAddressIndex) throws ArgumentException {
        int i = lastKnownAddressIndex == null ? -1 : lastKnownAddressIndex;

        while (true) {
            i++;
            String newAddress = this.getAddress(i);
            System.out.println(i + "   " + newAddress);
            if (iotaAPI.findTransactionsByAddresses(new String[] { newAddress }).getHashes().length == 0) {
                return i;
            }
        }
    }

    public void reattachTx(String hash) throws ArgumentException, IOException {
        iotaAPI.replayBundle(hash, depth, minWeightMagnitude, null);

//		GetTransactionsToApproveResponse getTransactionsToApproveResponse = iotaAPI.getTransactionsToApprove(depth, hash);
//		GetTrytesResponse getTrytesResponse = iotaAPI.getTrytes(hash);
//		System.out.println("getTrunkTransaction: " + getTransactionsToApproveResponse.getTrunkTransaction());

//		iotaAPI.attachToTangle(
//				getTransactionsToApproveResponse.getTrunkTransaction(),
//				getTransactionsToApproveResponse.getBranchTransaction(),
//				minWeightMagnitude,
//				getTrytesResponse.getTrytes()
//		);


//		Powsrvio powsrvio = new Powsrvio();
//		String res = powsrvio.post(
//				getTransactionsToApproveResponse.getTrunkTransaction(),
//				getTransactionsToApproveResponse.getBranchTransaction(),
//				minWeightMagnitude,
//				getTrytesResponse.getTrytes()
//		);
//		System.out.println(res);



    }

    private String getAddress(int index) throws ArgumentException {
        boolean checksum = true;

        return IotaAPIUtils.newAddress(seed, security, index, checksum, null);
    }
}
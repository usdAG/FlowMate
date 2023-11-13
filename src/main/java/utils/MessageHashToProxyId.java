package utils;

import burp.api.montoya.MontoyaApi;

import java.util.HashMap;

// This class keeps track of which messageHash belongs to which id in the burp history to avoid iterating over the
// proxy history over and over
public class MessageHashToProxyId {

    private static MessageHashToProxyId instance;
    private MontoyaApi api;
    private HashMap<String, Integer> hashAndIdMap;

    private MessageHashToProxyId(MontoyaApi api) {
        this.api = api;
        this.hashAndIdMap = new HashMap<>();
    }
    public synchronized int calculateId(String messageHash) {

        var history = this.api.proxy().history();
        int historySize = history.size();

        int diff = calculateSizeDifference();
        if (diff > 0) {
            putNewHashesInHashmap(diff, historySize);
        }

        return this.hashAndIdMap.getOrDefault(messageHash, -1);
    }

    /**
     * Returns the difference between the size of the API history and the size of the hash and ID map.
     * @return the difference between the size of the API history and the size of the hash and ID map
     */
    private int calculateSizeDifference() {
        int hashMapSize = this.hashAndIdMap.size();
        int historySize = this.api.proxy().history().size();
        return historySize - hashMapSize;
    }

    // If the Hashmap is not up-to-date, put the new values into it
    private synchronized void putNewHashesInHashmap(int diff, int historySize) {
        var history = this.api.proxy().history();
        int start = historySize - diff;
        for (int i = start; i < history.size(); i++) {
            String proxyHash = Hashing.sha1(history.get(i).finalRequest().toByteArray().getBytes());
            this.hashAndIdMap.put(proxyHash, i+1);
        }
    }
    public static MessageHashToProxyId getInstance(MontoyaApi api) {
        if (instance == null) {
            instance = new MessageHashToProxyId(api);
        }
        return instance;
    }
}

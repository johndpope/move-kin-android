package org.kinecosystem.sampleReceiverApp;

import android.support.annotation.NonNull;
import android.util.Log;

import org.kinecosystem.appsdiscovery.receiver.service.ReceiveKinServiceBase;

public class ReceiveKinService extends ReceiveKinServiceBase {
    private static final String TAG = ReceiveKinService.class.getSimpleName();

    public ReceiveKinService() {
        super();
    }

    @Override
    public void onTransactionCompleted(@NonNull String fromAddress, @NonNull String toAddress, int amount, @NonNull String transactionId, @NonNull String memo) {
        Log.d(TAG, "Transaction completed from " + fromAddress + " to " + toAddress+ " amount " + amount+ " transactionId " + transactionId+ " memo " + memo);
    }

    //the error is for the developers - put info as much as possible so the other side app can understand the transfer problem
    @Override
    public void onTransactionFailed(@NonNull String error, @NonNull String fromAddress, @NonNull String toAddress, int amount, @NonNull String memo) {
        Log.d(TAG, "Transaction failed from " + fromAddress + " to " + toAddress+ " amount " + amount+ " error " + error + " memo " + memo);
    }

}

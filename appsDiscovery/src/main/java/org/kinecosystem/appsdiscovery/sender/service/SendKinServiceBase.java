package org.kinecosystem.appsdiscovery.sender.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.NonNull;

public abstract class SendKinServiceBase extends Service {

    //Developer needs to implements
    //these is called from background thread so can be called async
    public abstract @NonNull
    KinTransferComplete transferKin(@NonNull final String toAddress, final int amount, @NonNull final String memo) throws KinTransferException;

    public abstract int getCurrentBalance() throws BalanceException;

    public final IBinder binder = new KinTransferServiceBinder();

    public class KinTransferServiceBinder extends Binder {
        public SendKinServiceBase getService() {
            return SendKinServiceBase.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class BalanceException extends Exception {
        public BalanceException(String getBalanceError) {
            super(getBalanceError);
        }

    }

    public class KinTransferException extends Exception {
        private String senderAddress;

        public KinTransferException(@NonNull String senderAddress, @NonNull String transferError) {
            super(transferError);
            this.senderAddress = senderAddress;
        }

        public String getSenderAddress() {
            return senderAddress;
        }
    }

    public class KinTransferComplete {
        private String senderAddress;
        private String transactionId;

        public KinTransferComplete(@NonNull String senderAddress, @NonNull String transactionId) {
            this.senderAddress = senderAddress;
            this.transactionId = transactionId;
        }

        public String getSenderAddress() {
            return senderAddress;
        }

        public String getTransactionId() {
            return transactionId;
        }
    }

}

package org.kinecosystem.transfer.receiver.manager;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;

import org.kinecosystem.transfer.TransferIntent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


public class AccountInfoResponder implements IAccountInfoResponder {
    private static final String FILE_NAME = "accountInfo.txt";
    private static final String FILE_PROVIDER_NAME = "KinTransferAccountInfoFileProvider";
    private static final String FILE_PROVIDER_DIR_NAME = "kintransfer_account_info";
    private File file;
    private Activity activity;

    public AccountInfoResponder(@NonNull Activity activity) {
        this.activity = activity;
    }

    public void onDestroy() {
        activity = null;
    }

    public boolean init(@NonNull final String accountInfo) {
        if (accountInfo.isEmpty() || activity == null) {
            return false;
        }
        File dir = new File(getFileProviderDir());
        if (!dir.exists()) {
            if (!dir.mkdir()) {
                return false;
            }
        }
        try {
            file = new File(getFileProviderFullPath());
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    file = null;
                    return false;
                }
            }
            FileWriter writer = new FileWriter(file);
            writer.append(accountInfo);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            file = null;
            return false;
        }
        return true;
    }

    //Can be called anytime (before init returns), when there is some error
    public void respondError(String errorMessage) {
        respondCancel(errorMessage);
    }


    //Can be called anytime (before init returns), when user decline transfer
    public void respondCancel() {
        respondCancel(null);
    }

    //Can be called only after init returns true, hence file is not null
    public void respondOk() {
        if (file != null && activity != null) {
            Intent intent = new Intent();
            String authority = activity.getPackageName() + "." + FILE_PROVIDER_NAME;
            final Uri uri = FileProvider.getUriForFile(activity, authority, file);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(uri, activity.getContentResolver().getType(uri));
            activity.setResult(Activity.RESULT_OK, intent);
        }
    }

    private void respondCancel(String errorMessage) {
        Intent intent = new Intent();
        if (errorMessage != null) {
            intent.putExtra(TransferIntent.EXTRA_HAS_ERROR, true);
            intent.putExtra(TransferIntent.EXTRA_ERROR_MESSAGE, errorMessage);
        }
        if (activity != null) {
            activity.setResult(Activity.RESULT_CANCELED, intent);
        }
    }

    //for testing
    String readFile(@NonNull String fullPathFileProvider) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(fullPathFileProvider));
            return in.readLine();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return "";
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    String getFileProviderDir() {
        if (activity != null) {
            return activity.getFilesDir().getAbsolutePath() + File.separator + FILE_PROVIDER_DIR_NAME;
        }
        return "";
    }

    String getFileProviderFullPath() {
        return getFileProviderDir() + File.separator + FILE_NAME;
    }
}

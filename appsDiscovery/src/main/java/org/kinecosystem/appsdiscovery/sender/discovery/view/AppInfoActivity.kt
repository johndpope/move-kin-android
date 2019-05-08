package org.kinecosystem.appsdiscovery.sender.discovery.view

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.TextView
import org.kinecosystem.appsdiscovery.R
import org.kinecosystem.appsdiscovery.receiver.service.TransferKinServiceException
import org.kinecosystem.appsdiscovery.receiver.service.ReceiveKinNotifier
import org.kinecosystem.appsdiscovery.sender.discovery.presenter.AppInfoPresenter
import org.kinecosystem.appsdiscovery.sender.discovery.view.customView.ReceiverAppStateView
import org.kinecosystem.appsdiscovery.sender.model.EcosystemApp
import org.kinecosystem.appsdiscovery.sender.model.name
import org.kinecosystem.appsdiscovery.sender.repositories.DiscoveryAppsLocal
import org.kinecosystem.appsdiscovery.sender.repositories.DiscoveryAppsRemote
import org.kinecosystem.appsdiscovery.sender.repositories.DiscoveryAppsRepository
import org.kinecosystem.appsdiscovery.sender.service.SendKinServiceBase
import org.kinecosystem.appsdiscovery.sender.transfer.TransferManager
import org.kinecosystem.appsdiscovery.utils.navigateToUrl
import java.util.concurrent.Executors

class AppInfoActivity : AppCompatActivity(), IAppInfoView {
    private var presenter: AppInfoPresenter? = null
    private var receiverAppStateView: ReceiverAppStateView? = null
    private var isBound = false
    private var transferService: SendKinServiceBase? = null
    private var exceutorService = Executors.newCachedThreadPool()
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as SendKinServiceBase.KinTransferServiceBinder
            transferService = binder.service
            isBound = true
            updateBalance()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appName = intent.getStringExtra(PARAM_APP_NAME)
        if (appName.isNullOrBlank()) {
            finish()
        }
        setContentView(R.layout.app_info_activity)
        val discoveryAppsRepository = DiscoveryAppsRepository.getInstance(packageName, DiscoveryAppsLocal(this), DiscoveryAppsRemote(), Handler(Looper.getMainLooper()))
        presenter = AppInfoPresenter(appName, discoveryAppsRepository, TransferManager(this))
        presenter?.onAttach(this)
        presenter?.onStart()
    }

    override fun onResume() {
        super.onResume()
        presenter?.onResume(baseContext)
    }


    override fun startSendKin(receiverAddress: String, amount: Int, memo: String, receiverPackage: String) {
        if (isBound) {
            try {
                val kinTransferComplete: SendKinServiceBase.KinTransferComplete = transferService?.transferKin(receiverAddress, amount, memo)!!
                //TODO notify the transaction bar of complete
                try {
                    ReceiveKinNotifier.notifyTransactionCompleted(baseContext, receiverPackage, kinTransferComplete.senderAddress, receiverAddress, amount, kinTransferComplete.transactionId, memo)
                } catch (transferKinServiceException: TransferKinServiceException) {
                    Log.d("####", "#### error notify receiver of transaction success ${transferKinServiceException.message}")
                }
            } catch (kinTransferException: SendKinServiceBase.KinTransferException) {
                //TODO notify the transaction bar of failed
                Log.d("####", "#### kinTransferException ${kinTransferException.message}")
                try {
                    ReceiveKinNotifier.notifyTransactionFailed(baseContext, receiverPackage, kinTransferException.toString(), kinTransferException.senderAddress, receiverAddress, amount, memo)
                } catch (transferKinServiceException: TransferKinServiceException) {
                    Log.d("####", "#### error notify receiver of transaction failed ${transferKinServiceException.message}")
                }
            }
        }
    }

    override fun unbindToSendService() {
        unbindService(connection)
        isBound = false
    }


    override fun bindToSendService() {
        val intent = Intent()

        var senderPackageName = packageName
        var serviceName = "SendKinService"
        intent.component = ComponentName(senderPackageName, "$senderPackageName.$serviceName")
        intent.`package` = senderPackageName
        val resolveInfos: MutableList<ResolveInfo> = packageManager.queryIntentServices(intent, 0)
        if (!resolveInfos.any()) {
            throw TransferKinServiceException("$senderPackageName.$serviceName", "Service not found")
        }
        if (resolveInfos.filter { it.serviceInfo.exported }.any()) {
            throw TransferKinServiceException("$senderPackageName.$serviceName", "Service should not be exported")
        }

        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onRequestAmountError() {
        //TODO update user
    }

    override fun startAmountChooserActivity(receiverAppIcon: String, balance: Int, requestCode: Int) {
        startActivityForResult(AmountChooserActivity.getIntent(this, receiverAppIcon, balance), requestCode)
    }

    override fun onRequestReceiverPublicAddressCanceled() {
        Log.d("###", "onRequestReceiverPublicAddressCanceled")
    }

    override fun onRequestReceiverPublicAddressError(error: AppInfoPresenter.RequestReceiverPublicAddressError) {
        //TODO update user
        Log.d("###", "onRequestReceiverPublicAddressError")
    }

    override fun onStartRequestReceiverPublicAddress() {
        //TODO show progress
        Log.d("###", "onStartRequestReceiverPublicAddress")
    }

    override fun initViews(app: EcosystemApp?) {
        if (app == null) {
            finish()
        }
        findViewById<TextView>(R.id.name).setText(app?.name)
        findViewById<TextView>(R.id.pkg).setText(app?.identifier)
        findViewById<Button>(R.id.approveBtn).setOnClickListener {
            presenter?.onRequestReceiverPublicAddress()
        }
        receiverAppStateView = findViewById(R.id.receiver_app_state)
        receiverAppStateView?.setListener(object : ReceiverAppStateView.IActionClickListener {
            override fun onActionButtonClicked() {
                presenter?.onActionButtonClicked()
            }
        })
    }


    companion object {
        private const val PARAM_APP_NAME = "PARAM_APP_NAME"

        fun getIntent(context: Context, appName: String): Intent {
            val intent = Intent(context, AppInfoActivity::class.java)
            intent.putExtra(PARAM_APP_NAME, appName)
            return intent
        }
    }

    private fun updateBalance() {
        if (isBound) {
            exceutorService.execute {
                try {
                    transferService?.let {
                        val currentBalance = it.currentBalance
                        presenter?.updateBalance(currentBalance)
                    }
                } catch (balanceException: SendKinServiceBase.BalanceException) {
                    //TODO show error retrieve balance
                    Log.d("####", "#### balanceException ${balanceException.message}")
                }
            }
        }
    }

    override fun navigateTo(downloadUrl: String) {
        navigateToUrl(downloadUrl)
    }

    override fun updateAppState(state: ReceiverAppStateView.State) {
        receiverAppStateView?.update(state)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        presenter?.processResponse(requestCode, resultCode, data)
    }

    //need to think if need to be in onstop - what happen if get response when you are in background
    override fun onDestroy() {
        super.onDestroy()
        presenter?.onDestroy()
    }

//    override fun onStop() {
//        super.onStop()
//        unbindService(connection)
//        isBound = false
//    }
}
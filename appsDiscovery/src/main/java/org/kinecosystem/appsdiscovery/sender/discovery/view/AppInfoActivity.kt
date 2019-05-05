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
import org.kinecosystem.appsdiscovery.receiver.service.ReceiveKinServiceBase
import org.kinecosystem.appsdiscovery.sender.model.EcosystemApp
import org.kinecosystem.appsdiscovery.sender.model.launchActivity
import org.kinecosystem.appsdiscovery.sender.model.name
import org.kinecosystem.appsdiscovery.sender.repositories.DiscoveryAppsLocal
import org.kinecosystem.appsdiscovery.sender.repositories.DiscoveryAppsRemote
import org.kinecosystem.appsdiscovery.sender.repositories.DiscoveryAppsRepository
import org.kinecosystem.appsdiscovery.sender.service.SendKinServiceBase
import org.kinecosystem.appsdiscovery.sender.transfer.TransferManager
import java.math.BigDecimal

class AppInfoActivity : AppCompatActivity() {

    private var app: EcosystemApp? = null
    private lateinit var transferManager : TransferManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appName = intent.getStringExtra(PARAM_APP_NAME)
        if (appName.isNullOrBlank()) {
            finish()
        }
        val discoveryAppsRepository = DiscoveryAppsRepository.getInstance(DiscoveryAppsLocal(this), DiscoveryAppsRemote(), Handler(Looper.getMainLooper()))
        app = discoveryAppsRepository.getAppByName(appName)
        if (app == null) {
            finish()
        }
        transferManager = TransferManager(this)
        setContentView(R.layout.app_info_activity)
        findViewById<TextView>(R.id.name).setText(app?.name)
        findViewById<TextView>(R.id.pkg).setText(app?.identifier)
        findViewById<Button>(R.id.sendBtn).setOnClickListener {
            startSendKinRequest()
        }

        findViewById<Button>(R.id.approveBtn).setOnClickListener {
            requestPublicAddress()
        }

    }

    private fun requestPublicAddress() {
        //TODO temp pass to model
        app?.launchActivity?.let{ activityPath ->
            app?.identifier?.let { receiverPkg->
                val started = transferManager.startTransferRequestActivity(receiverPkg, activityPath)
                if (!started) {
                   // status.set(CONNECTION_ERROR)
                } else {
                   // status.set(START_CONNECTION)
                }
            } ?: kotlin.run {
                //status.set(CONNECTION_ERROR)
            }
        } ?: kotlin.run {
            //status.set(CONNECTION_ERROR)
        }
    }

    companion object {
        private const val PARAM_APP_NAME = "PARAM_APP_NAME"

        fun getIntent(context: Context, appName: String): Intent {
            val intent = Intent(context, AppInfoActivity::class.java)
            intent.putExtra(PARAM_APP_NAME, appName)
            return intent
        }
    }

    private var isBound = false
    private var transferService: SendKinServiceBase? = null
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as SendKinServiceBase.KinTransferServiceBinder
            transferService = binder.service
            isBound = true
            //TODO call only when try to send
           //startSendKinRequest()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }

    fun startSendKinRequest() {
        //TODO update the transaction bar of the start of the transaction
        //TODO clear local store shredpref from prev adddress recieved

        if (isBound) {
            val thread = Thread(Runnable {
                val receiver = "SwellyAddress"
                val amount = BigDecimal(3)
                val memo = "lean"

                try {
                    val currentBalance = transferService?.currentBalance
                    Log.d("####", "#### current balanbe is $currentBalance")
                } catch (balanceException: SendKinServiceBase.BalanceException) {
                    //TODO need to handle??
                    Log.d("####", "#### balanceException ${balanceException.message}")
                }

                try {
                    val kinTransferComplete = transferService?.transferKin(receiver, amount, memo)!!
                    //TODO notify the transaction bar of complete
                    //notify the receiver of the transaction
                    app?.identifier?.let { receiverPackage ->
                        try {
                            ReceiveKinServiceBase.notifyTransactionCompleted(baseContext, receiverPackage, kinTransferComplete.senderAddress, receiver, amount, kinTransferComplete.transactionId, memo)
                        }catch (kinReceiverServiceException:ReceiveKinServiceBase.KinReceiverServiceException){
                            Log.d("####", "#### error notify receiver of transaction success ${kinReceiverServiceException.message}")
                        }
                    }
                } catch (kinTransferException: SendKinServiceBase.KinTransferException) {
                    //TODO notify the transaction bar of error
                    //notify the receiver of the error
                    Log.d("####", "#### kinTransferException ${kinTransferException.message}")
                    app?.identifier?.let { receiverPackage ->
                        try {
                            ReceiveKinServiceBase.notifyTransactionFailed(baseContext, receiverPackage, kinTransferException.toString(), kinTransferException.senderAddress, receiver, amount, memo)
                        }catch (kinReceiverServiceException:ReceiveKinServiceBase.KinReceiverServiceException){
                            Log.d("####", "#### error notify receiver of transaction failed ${kinReceiverServiceException.message}")
                        }
                    }
                }
            })
            thread.start()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        transferManager.processResponse(requestCode, resultCode, intent, object : TransferManager.AccountInfoResponseListener{
            override fun onCancel() {
                //TODO on cancel
            }

            override fun onError(error: String) {
                //TODO on onError
            }

            override fun onAddressReceived(data: String) {
                //TODO put on local store shredpref
              Log.d("####", "####onAddressReceived $data")
            }

        })



//        if (requestCode == AppViewModel.AmountChooserRequestCode) {
//            processResponse(requestCode, resultCode, intent)
//        } else {
//            transferManager.processResponse(requestCode, resultCode, intent, this)
//        }
    }


    override fun onStart() {
        super.onStart()
        bindSendService()
    }

    private fun bindSendService() {
        val intent = Intent()
        var receiverPackageName = app?.identifier

        //TODO temp change to local sample
        receiverPackageName = packageName
        intent.component = ComponentName(receiverPackageName, "$receiverPackageName.SendKinService")
        intent.setPackage(receiverPackageName)
        val resolveInfos: MutableList<ResolveInfo> = packageManager.queryIntentServices(intent, 0)
        if (!resolveInfos.any()) {
            //TODO throw exception service not implemented
            Log.d("####", "#### cant find the service ${receiverPackageName}.SendKinService")
        }
        if (resolveInfos.filter {
                    it.serviceInfo.exported
                }.any()) {
            //TODO throw exception service is exported andr remove the else
            Log.d("####", "####  service ${receiverPackageName}.SendKinService export must be declared on manifest false")
        } else {
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    //need to think if need to be in onstop - what happen if get response when you are in background
    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)
        isBound = false
    }

//    override fun onStop() {
//        super.onStop()
//        unbindService(connection)
//        isBound = false
//    }
}
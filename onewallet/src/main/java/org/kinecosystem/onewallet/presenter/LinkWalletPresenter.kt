package org.kinecosystem.onewallet.presenter

import android.os.Handler
import android.view.View
import org.kinecosystem.common.base.BasePresenter
import org.kinecosystem.common.base.LocalStore
import org.kinecosystem.onewallet.model.OneWalletActionModel
import org.kinecosystem.onewallet.view.LinkWalletViewHolder

class LinkWalletPresenter(localStore: LocalStore, private val uiHandler: Handler) : BasePresenter<LinkWalletViewHolder>(), ILinkWalletPresenter {

    var oneWalletActionModel: OneWalletActionModel = OneWalletActionModel(localStore)
        private set

    override fun onAttach(view: LinkWalletViewHolder) {
        super.onAttach(view)
        view.actionButton.update(oneWalletActionModel)
        view.actionButton.isEnabled = true
        view.progressBar.visibility = View.INVISIBLE
    }

    override fun onLinkWalletStarted() {
        view?.progressBar?.startFlipAnimation(uiHandler)
        view?.actionButton?.isEnabled = false
        view?.progressBar?.visibility = View.VISIBLE
    }

    override fun onLinkWalletSucceeded() {
        view?.progressBar?.stopFlipAnimation()
        view?.actionButton?.isEnabled = true
        oneWalletActionModel.type = OneWalletActionModel.Type.TOP_UP
        view?.actionButton?.update(oneWalletActionModel)
    }

    override fun onLinkWalletCancelled() {
        view?.progressBar?.stopFlipAnimation()
        view?.actionButton?.isEnabled = true
    }

    override fun onLinkWalletError(errorMessage: String) {
        view?.progressBar?.stopFlipAnimation()
        view?.actionButton?.isEnabled = true
    }

    override fun onDetach() {
        view?.progressBar?.stopFlipAnimation()
        super.onDetach()
    }

}
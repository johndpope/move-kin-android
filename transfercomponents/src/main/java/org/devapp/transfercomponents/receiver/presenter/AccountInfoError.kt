package org.devapp.transfercomponents.receiver.presenter

class AccountInfoError(val actionType: IErrorActionClickListener.ActionType, val errorMessage: String)

interface IErrorActionClickListener {
    enum class ActionType {
        None,
        LaunchMainActivity
    }

    fun onOkClicked(errorType: ActionType)
}
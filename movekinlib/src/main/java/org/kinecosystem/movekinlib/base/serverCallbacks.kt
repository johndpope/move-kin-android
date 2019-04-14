package org.kinecosystem.movekinlib.base

interface OperationCompletionCallback {
    fun onSuccess()
    fun onError(errorCode: Int)
}

interface OperationResultCallback<in T> {
    fun onResult(result: T)
    fun onError(errorCode: Int)
}
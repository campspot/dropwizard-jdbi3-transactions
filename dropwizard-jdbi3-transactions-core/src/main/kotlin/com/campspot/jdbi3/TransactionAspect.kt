package com.campspot.jdbi3

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi

class TransactionAspect(private val dbis: Map<String, Jdbi>, private val daoManager: DAOManager) {
  private var inTransaction: InTransaction? = null
  private var handle: Handle? = null

  fun beforeStart(inTransaction: InTransaction) {
    this.inTransaction = inTransaction
    val jdbi = dbis[inTransaction.name]
    handle = jdbi?.open()
    try {
      handle?.let {
        it.begin()
        daoManager.setupWithTransaction(it)
      }
    } catch (t: Throwable) {
      handle?.close()
    }
  }

  fun afterEnd() {
    try {
      if (handle?.isClosed == false) {
        handle!!.commit()
      }
    } catch (e: Exception) {
      onError()
      throw e
    }
  }

  fun onError() {
    try {
      if (handle?.isClosed == false) {
        handle!!.rollback()
      }
      daoManager.clear()
    } finally {
      onFinish()
    }
  }

  fun onFinish() {
    if (handle?.isClosed == false) {
      handle?.close()
    }
  }
}

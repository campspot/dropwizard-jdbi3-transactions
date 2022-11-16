package com.campspot.jdbi3

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

class TransactionAspect(private val dbis: Map<String, Jdbi>, private val daoManager: DAOManager) {
  private var inTransaction: InTransaction? = null
  private var handle: Handle? = null
  private var logger = LoggerFactory.getLogger(this.javaClass)

  fun beforeStart(inTransaction: InTransaction) {
    this.inTransaction = inTransaction
    val jdbi = dbis[inTransaction.name]
    handle = jdbi?.open()
    try {
      handle?.let {
        it.begin()
        daoManager.setupWithTransaction(it)
      }
    } catch (t: Exception) {
      logger.error("Error starting the transaction", t)
      handle?.close()
    }
  }

  fun afterEnd() {
    try {
      if (handle?.isClosed == false) {
        handle!!.commit()
      }
    } catch (e: Exception) {
      logger.error("Error committing transaction", e)
      onError()
    }
  }

  fun onError() {
    try {
      if (handle?.isClosed == false) {
        handle!!.rollback()
      }
      daoManager.clear()
    } catch (e: Exception) {
      logger.error("Error rolling back transaction", e)
    } finally {
      onFinish()
    }
  }

  fun onFinish() {
    try {
      handle?.close()
    } catch (e: Exception) {
      logger.error("Error finishing transaction", e)
    }
  }
}

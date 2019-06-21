package com.campspot.jdbi3

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import kotlin.reflect.KClass

open class DAOManager(
  private val jdbi: Jdbi
) {
  protected val transaction = ThreadLocal<Handle>()
  protected val daoInstances = ThreadLocal<HashMap<String, DAO>>()

  fun setupWithTransaction(transaction: Handle): Handle {
    this.transaction.set(transaction)
    this.daoInstances.set(HashMap())

    return transaction
  }

  fun setupWithTransaction(): Handle {
    val transaction: Handle = jdbi.open()
    return setupWithTransaction(transaction)
  }

  open operator fun <T : DAO> get(dao: KClass<T>): T {
    val daoMap = daoInstances.get()
    var daoInstance = daoMap[dao.qualifiedName]
    val transactionInstance = transaction.get()

    if (daoInstance == null) {
      daoInstance = transactionInstance.attach(dao.java)
      daoMap[dao.qualifiedName ?: "none"] = daoInstance
    }

    @Suppress("UNCHECKED_CAST")
    return daoInstance as T
  }

  open fun <T : DAO> getWithoutTransaction(dao: KClass<T>): T {
    return jdbi.onDemand(dao.java)
  }

  fun clear() {
    daoInstances.remove()
    transaction.remove()
  }
}


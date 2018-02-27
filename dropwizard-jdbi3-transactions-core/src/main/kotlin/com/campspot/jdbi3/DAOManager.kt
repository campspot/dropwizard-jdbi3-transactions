package com.campspot.jdbi3

import org.jdbi.v3.core.Handle
import kotlin.reflect.KClass

open class DAOManager {
  private val transaction = ThreadLocal<Handle>()
  private val daoInstances = ThreadLocal<HashMap<String, DAO>>()

  fun setupWithTransaction(transaction: Handle): Handle {
    this.transaction.set(transaction)
    this.daoInstances.set(HashMap())

    return transaction
  }

  open operator fun <T: DAO> get(dao: KClass<T>): T {
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

  fun clear() {
    transaction.remove()
    daoInstances.remove()
  }
}


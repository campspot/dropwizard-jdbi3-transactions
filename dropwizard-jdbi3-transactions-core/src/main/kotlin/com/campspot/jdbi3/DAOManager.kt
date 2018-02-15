package com.campspot.jdbi3

import org.jdbi.v3.core.Handle
import kotlin.reflect.KClass

open class DAOManager(private vararg val daos: KClass<out DAO>) {
  private val transaction = ThreadLocal<Handle>()
  private val daoInstances = ThreadLocal<HashMap<String, DAO>>()

  fun setupWithTransaction(transaction: Handle): Handle {
    this.transaction.set(transaction)
    val daoMap = HashMap<String, DAO>()
    daos.forEach { dao ->
      val createdDao = transaction.attach(dao.java)
      daoMap[dao.qualifiedName ?: "none"] = createdDao
    }

    daoInstances.set(daoMap)
    return transaction
  }

  open operator fun <T: DAO> get(dao: KClass<T>): T {
    @Suppress("UNCHECKED_CAST")
    return daoInstances.get()[dao.qualifiedName] as T
  }

  fun clear() {
    transaction.remove()
    daoInstances.remove()
  }
}


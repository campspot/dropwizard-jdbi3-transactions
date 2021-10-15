package com.campspot.jdbi3

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import kotlin.reflect.KClass

const val DEFAULT = "db"

open class DAOManager {
  private val jdbis: Map<String, Jdbi>
  val transaction = ThreadLocal<Handle>()
  val daoInstances = ThreadLocal<HashMap<String, DAO>>()

  constructor(jdbis: Map<String, Jdbi>) {
    this.jdbis = jdbis
  }

  constructor(jdbi: Jdbi) {
    jdbis = mapOf(DEFAULT to jdbi)
  }

  fun setupWithTransaction(transaction: Handle): Handle {
    this.transaction.set(transaction)
    this.daoInstances.set(HashMap())

    return transaction
  }

  fun setupWithTransaction(): Handle {
    if (jdbis.size > 1) {
      throw RuntimeException("name required for multiple databases")
    }

    return setupWithTransaction(DEFAULT)
  }

  fun setupWithTransaction(name: String): Handle {
    val transaction: Handle = jdbis.getValue(name).open()
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
    if (jdbis.size > 1) {
      throw RuntimeException("name required for multiple databases")
    }

    return getWithoutTransaction(DEFAULT, dao)
  }

  open fun <T : DAO> getWithoutTransaction(name: String, dao: KClass<T>): T {
    return jdbis.getValue(name).onDemand(dao.java)
  }

  fun clear() {
    daoInstances.remove()
    transaction.remove()
  }

  fun<T> executeInTransaction(callback: () -> T): T {
    val handle = setupWithTransaction()
    val result: T;
    try {
      result = callback()
      handle.commit();
      return result;
    } catch (exception: Exception) {
      handle.rollback();
      throw exception;
    } finally {
      handle.close();
    }
  }
}

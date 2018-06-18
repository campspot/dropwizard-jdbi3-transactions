package com.campspot.jdbi3.test

import com.campspot.jdbi3.DAO
import com.campspot.jdbi3.DAOManager
import org.jdbi.v3.core.Jdbi
import org.mockito.Mockito
import kotlin.reflect.KClass

class TestDAOManager(jdbi: Jdbi): DAOManager(jdbi) {
  private val mocks = HashMap<KClass<out DAO>, DAO>()

  override operator fun <T: DAO> get(dao: KClass<T>): T {
    if (!mocks.containsKey(dao)) {
      mocks[dao] = Mockito.mock(dao.java)
    }

    @Suppress("UNCHECKED_CAST")
    return mocks[dao] as T
  }
}

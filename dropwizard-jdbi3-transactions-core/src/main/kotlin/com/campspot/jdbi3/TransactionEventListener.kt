package com.campspot.jdbi3

import org.glassfish.jersey.server.model.ResourceMethod
import org.glassfish.jersey.server.monitoring.ApplicationEvent
import org.glassfish.jersey.server.monitoring.ApplicationEventListener
import org.glassfish.jersey.server.monitoring.RequestEvent
import org.glassfish.jersey.server.monitoring.RequestEventListener
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.util.HashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import javax.ws.rs.ext.Provider

@Provider
class TransactionApplicationListener<out T: Throwable> @JvmOverloads constructor(private val daoManager: DAOManager, private val ignoredExceptionClasses: List<Class<T>> = emptyList()) : ApplicationEventListener {
  private val methodMap = ConcurrentHashMap<ResourceMethod, InTransaction?>()
  private val dbis = HashMap<String, Jdbi>()

  fun registerDbi(name: String, jdbi: Jdbi) {
    dbis[name] = jdbi
  }

  private class TransactionEventListener<out T: Throwable>(
    private val methodMap: ConcurrentMap<ResourceMethod, InTransaction?>,
    daoManager: DAOManager,
    dbis: Map<String, Jdbi>,
    private val ignoredExceptionClasses: List<Class<T>>
  ) : RequestEventListener {
    private val transactionAspect: TransactionAspect = TransactionAspect(dbis, daoManager)
    private var logger = LoggerFactory.getLogger(this.javaClass)

    override fun onEvent(event: RequestEvent) {
      val eventType = event.type
      if (eventType == RequestEvent.Type.RESOURCE_METHOD_START) {
        val inTransaction = methodMap.computeIfAbsent(event.uriInfo.matchedResourceMethod, { registerInTransactionAnnotations(it) })
        inTransaction?.let { transactionAspect.beforeStart(it) }
      } else if (eventType == RequestEvent.Type.RESP_FILTERS_START) {
        transactionAspect.afterEnd()
      } else if (eventType == RequestEvent.Type.ON_EXCEPTION) {
        try {
          var exceptionClass: Class<T>? = event.exception.javaClass as Class<T>
          if (event.exception.cause != null) {
            exceptionClass = event.exception.cause?.javaClass as Class<T>
          }

          if (exceptionClass != null && ignoredExceptionClasses.contains(exceptionClass)) {
            logger.error("exception message {}", event.exception.message);
          } else {
            logger.error("exception event", event.exception);
          }
        } catch (e: Exception) {
          // Just in case any of the above logging blows up, we still want to invoke the `onError` below
          e.printStackTrace()
        }

        transactionAspect.onError()
      } else if (eventType == RequestEvent.Type.FINISHED) {
        transactionAspect.onFinish()
      }
    }

    private fun registerInTransactionAnnotations(method: ResourceMethod): InTransaction? {
      var annotation: InTransaction? = method.invocable.definitionMethod.getAnnotation(InTransaction::class.java)
      if (annotation == null) {
        annotation = method.invocable.handlingMethod.getAnnotation(InTransaction::class.java)
      }
      return annotation
    }
  }

  override fun onEvent(event: ApplicationEvent) {}

  override fun onRequest(event: RequestEvent): RequestEventListener {
    return TransactionEventListener(methodMap, daoManager, dbis, ignoredExceptionClasses)
  }
}


package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.config

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.PersistenceException
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.springframework.beans.BeansException
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.BeanFactoryAware
import org.springframework.beans.factory.ListableBeanFactory
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.orm.jpa.EntityManagerFactoryUtils
import org.springframework.orm.jpa.EntityManagerHolder
import org.springframework.stereotype.Component
import org.springframework.test.context.event.annotation.AfterTestClass
import org.springframework.test.context.event.annotation.AfterTestMethod
import org.springframework.test.context.event.annotation.BeforeTestClass
import org.springframework.test.context.event.annotation.BeforeTestMethod
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.util.Assert
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.config.IntegrationTestDbManager.recreateDatabaseFromTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.ApplicationContextProvider
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.isPerClass

/**
 * A simplified version of [org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor]
 * used to ensure we can lazy load relationships within the integration test scope, without
 * having to set `enable_lazy_load_no_trans` in application-test.yml which will also apply to the
 * code under test.
 *
 * This means we can effectively test code where the Open Entity in View Interceptor
 * is disabled.
 */
@Component
class OpenEntityInTestManager : BeanFactoryAware {

  private val logger: Log = LogFactory.getLog(this.javaClass)

  private var entityManagerFactory: EntityManagerFactory? = null
  private var persistenceUnitName: String? = null

  fun clear() {
    val emf = obtainEntityManagerFactory()
    if (TransactionSynchronizationManager.hasResource(emf)) {
      val emHolder =
        TransactionSynchronizationManager.unbindResource(this.obtainEntityManagerFactory()) as EntityManagerHolder
      emHolder.entityManager.clear()
    }
  }

  fun setup(): Boolean {
    val emf = obtainEntityManagerFactory()
    if (!TransactionSynchronizationManager.hasResource(emf)) {
      logger.debug("Opening JPA EntityManager in OpenEntityManagerInTestUtil")

      try {
        val em = createEntityManager()
        val emHolder = EntityManagerHolder(em)
        TransactionSynchronizationManager.bindResource(emf, emHolder)
      } catch (e: PersistenceException) {
        throw DataAccessResourceFailureException("Could not create JPA EntityManager", e)
      }
      return true
    }

    return false
  }

  fun teardown() {
    val emf = obtainEntityManagerFactory()
    if (TransactionSynchronizationManager.hasResource(emf)) {
      val emHolder =
        TransactionSynchronizationManager.unbindResource(emf) as EntityManagerHolder
      logger.debug("Closing JPA EntityManager in OpenEntityManagerInViewInterceptor")
      EntityManagerFactoryUtils.closeEntityManager(emHolder.entityManager)
    }
  }

  private fun obtainEntityManagerFactory(): EntityManagerFactory {
    val emf = entityManagerFactory
    Assert.state(emf != null, "No EntityManagerFactory set")
    return emf!!
  }

  @Throws(BeansException::class)
  override fun setBeanFactory(beanFactory: BeanFactory) {
    if (entityManagerFactory == null) {
      check(beanFactory is ListableBeanFactory) { "Cannot retrieve EntityManagerFactory by persistence unit name in a non-listable BeanFactory: $beanFactory" }

      entityManagerFactory =
        EntityManagerFactoryUtils.findEntityManagerFactory(beanFactory, this.persistenceUnitName)
    }
  }

  private fun createEntityManager(): EntityManager {
    val emf = obtainEntityManagerFactory()
    return emf.createEntityManager()
  }

  class IntegrationTestListener : BeforeAllCallback, BeforeEachCallback, AfterAllCallback, AfterEachCallback {
    override fun beforeAll(context: ExtensionContext?) {
      if (isPerClass(context)) {
        getManager().setup()
      }
    }

    override fun beforeEach(context: ExtensionContext?) {
      if (!isPerClass(context)) {
        getManager().setup()
      }
    }

    override fun afterAll(context: ExtensionContext?) {
      if (isPerClass(context)) {
        getManager().teardown()
      }
    }

    override fun afterEach(context: ExtensionContext?) {
      if (!isPerClass(context)) {
        getManager().teardown()
      }
    }

    private fun getManager(): OpenEntityInTestManager = ApplicationContextProvider.get().getBean(OpenEntityInTestManager::class.java)

  }

}

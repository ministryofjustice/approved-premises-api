package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.config

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.PersistenceException
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.beans.BeansException
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.BeanFactoryAware
import org.springframework.beans.factory.ListableBeanFactory
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.orm.jpa.EntityManagerFactoryUtils
import org.springframework.orm.jpa.EntityManagerHolder
import org.springframework.stereotype.Component
import org.springframework.test.context.event.annotation.AfterTestClass
import org.springframework.test.context.event.annotation.BeforeTestClass
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.util.Assert

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

  @BeforeTestClass
  fun beforeTest() {
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
    }
  }

  @AfterTestClass
  fun afterCompletion() {
    if(TransactionSynchronizationManager.hasResource(this.obtainEntityManagerFactory())) {
      val emHolder =
        TransactionSynchronizationManager.unbindResource(this.obtainEntityManagerFactory()) as EntityManagerHolder
      logger.debug("Closing JPA EntityManager in OpenEntityManagerInViewInterceptor")
      EntityManagerFactoryUtils.closeEntityManager(emHolder.entityManager)
    }
  }

  private fun obtainEntityManagerFactory(): EntityManagerFactory? {
    val emf = entityManagerFactory
    Assert.state(emf != null, "No EntityManagerFactory set")
    return emf
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
    return emf!!.createEntityManager()
  }
}

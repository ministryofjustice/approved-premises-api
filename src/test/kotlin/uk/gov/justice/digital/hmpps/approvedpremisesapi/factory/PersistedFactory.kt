package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.jpa.repository.JpaRepository

class PersistedFactory<EntityType : Any, PrimaryKeyType : Any, FactoryType : Factory<EntityType>>(private val factoryProducer: () -> FactoryType, private val repository: JpaRepository<EntityType, PrimaryKeyType>) {
  fun produceAndPersist(): EntityType {
    (1..5).forEach {
      try {
        return repository.saveAndFlush(factoryProducer().produce())
      } catch (dataIntegrityViolationException: DataIntegrityViolationException) {
        if (it == 5) {
          throw dataIntegrityViolationException
        }
      }
    }

    throw RuntimeException("Unreachable")
  }

  fun produceAndPersist(configuration: FactoryType.() -> Unit): EntityType {
    val factory = factoryProducer()
    configuration(factory)
    return repository.saveAndFlush(factory.produce())
  }

  fun produceAndPersistMultiple(amount: Int) = (1..amount).map { produceAndPersist() }
  fun produceAndPersistMultiple(amount: Int, configuration: FactoryType.() -> Unit): List<EntityType> {
    val factory = factoryProducer()
    configuration(factory)
    return (1..amount).map {
      repository.saveAndFlush(factory.produce())
    }
  }

  fun produceAndSortAndPersistMultiple(amount: Int, configuration: FactoryType.() -> Unit, comparator: Comparator<EntityType>): List<EntityType> {
    val factory = factoryProducer()
    configuration(factory)
    val producedAndSortedEntities = (1..amount).map {
      factory.produce()
    }.sortedWith(comparator)
    return repository.saveAllAndFlush(producedAndSortedEntities)
  }

  fun produceAndPersistMultipleIndexed(amount: Int, configuration: FactoryType.(Int) -> Unit): List<EntityType> {
    val factory = factoryProducer()
    return (1..amount).map {
      configuration(factory, it)
      repository.saveAndFlush(factory.produce())
    }
  }
}

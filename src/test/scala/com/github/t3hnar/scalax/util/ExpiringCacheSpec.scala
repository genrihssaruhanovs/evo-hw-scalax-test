package com.github.t3hnar.scalax.util

import org.specs2.mutable.Specification
import java.util.concurrent.TimeUnit

import org.specs2.specification.Scope

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

/**
 * @author Yaroslav Klymko
 */
class ExpiringCacheSpec extends Specification {
  "ExpiringCache" should {
    "clean expired values if get enough queries" in new ExpiringCacheScope {
      cache.map must haveSize(0)
      cache.queryCount mustEqual 0

      cache.put(0, "0")
      cache.get(0) must beSome("0")
      cache.map must haveSize(1)
      cache.queryCount mustEqual 1

      current = cache.unit.toMillis(cache.duration)

      cache.put(1, "1")
      cache.get(1) must beSome("1")
      cache.queryCount mustEqual 2

      (0 to cache.queryOverflow).foreach(_ => cache.get(3))

      cache.map.size must eventually(beEqualTo(1))
      cache.get(1) must beSome("1")
    }

    "not return expired values which are not cleaned" in new ExpiringCacheScope {
      cache.map must haveSize(0)
      cache.queryCount mustEqual 0

      cache.put(0, "0")
      cache.get(0) must beSome("0")
      cache.map.size must eventually(beEqualTo(1))

      current = cache.unit.toMillis(cache.duration)

      cache.get(0) must beNone
      cache.map.size must eventually(beEqualTo(1))
    }

    "remove elements on request" in new ExpiringCacheScope {
      cache.put(0, "0")
      cache.put(1, "1")
      cache.map must haveSize(2)

      cache.remove(0)
      cache.map must haveSize(1)
      cache.get(0) must beNone
      cache.get(1) must beSome("1")

      cache.remove(1)
      cache.get(1) must beNone
      cache.map must haveSize(0)
    }

    //below test does not make much sense, but the task was to increase coverage, so.. :)
    "be constructed with FiniteDuration, work with non-default execution context and default currentMillis" in {
      implicit val execContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
      val cache = new ExpiringCache[Int, String](FiniteDuration(1, TimeUnit.MILLISECONDS), 5)
      cache.duration mustEqual 1
      cache.unit mustEqual TimeUnit.MILLISECONDS
      cache.ec mustEqual execContext

      cache.put(0, "0")
      cache.put(0, "5")

      cache.map must haveSize(1)
      cache.get(0) must beNone
    }
  }

  class ExpiringCacheScope extends Scope {
    var current = 0L
    val cache = new ExpiringCache[Int, String](1, TimeUnit.MILLISECONDS, 5) {
      override def currentMillis = current
    }
  }
}


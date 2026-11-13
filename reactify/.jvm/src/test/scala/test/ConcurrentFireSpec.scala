package test

import java.util.concurrent.{CountDownLatch, TimeUnit}
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import reactify.{Channel, Val, Var}

import scala.language.implicitConversions

class ConcurrentFireSpec extends AnyWordSpec with Matchers {
  private val threads = 8
  private val fires = 20000

  // each thread runs `body` at once with the others, and every one finishes or the first failure is rethrown
  private def together(body: Int => Unit): Unit = {
    val start = new CountDownLatch(1)
    val failure = new AtomicReference[Throwable](null)
    val running = (0 until threads).map { t =>
      val thread = new Thread(new Runnable {
        override def run(): Unit = {
          start.await()
          try body(t) catch {
            case e: Throwable => failure.compareAndSet(null, e)
          }
          ()
        }
      })
      thread.start()
      thread
    }
    start.countDown()
    running.foreach(_.join(TimeUnit.MINUTES.toMillis(2)))
    running.exists(_.isAlive) should be(false)
    Option(failure.get()).foreach(e => throw e)
  }

  "Values fired on many threads at once" should {
    "keep each thread's propagation of one value to itself" in {
      // a stop on one thread must stop only that thread's propagation: the even values stop, the odd reach the end
      val c = Channel[Int]
      val reachedEnd = new AtomicInteger(0)
      val wrong = new AtomicInteger(0)
      c.attach(i => if (i % 2 == 0) c.stopPropagation())
      c.attach { i =>
        if (i % 2 == 0) wrong.incrementAndGet()
        if (!c.status.contains(reactify.reaction.ReactionStatus.Continue)) wrong.incrementAndGet()
        reachedEnd.incrementAndGet()
      }
      together { t =>
        (0 until fires).foreach(i => c @= i)
        if (c.status.nonEmpty) wrong.incrementAndGet()
      }
      wrong.get() should be(0)
      reachedEnd.get() should be(threads * fires / 2)
    }
    "fire different values on each thread, each through its own chain" in {
      val ends = new AtomicInteger(0)
      together { t =>
        val root = Var(0)
        val chain = (0 until 20).foldLeft[Val[Int]](root)((p, _) => Val(p() + 1))
        var last = -1
        chain.attach { i =>
          last = i
          ends.incrementAndGet()
        }
        (1 to fires / 10).foreach { i =>
          root @= i
          if (last != i + 20) throw new AssertionError(s"thread $t: $last after $i")
        }
        if (root.status.nonEmpty || chain.status.nonEmpty) throw new AssertionError(s"thread $t: a status left behind")
      }
      ends.get() should be(threads * fires / 10)
    }
  }
}

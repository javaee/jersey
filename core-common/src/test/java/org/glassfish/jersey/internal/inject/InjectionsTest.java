package org.glassfish.jersey.internal.inject;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.glassfish.hk2.api.Immediate;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.junit.Test;

/**
 * Test for the {@link Injections} class.
 *
 * @author Jord Sonneveld (jord@moz.com)
 *
 */
public class InjectionsTest {

  /**
   * Verify that services marked with the HK2 Immediate annotation are indeed
   * created "immediately" (or at least "soon").
   *
   * Because Immediate services are instantiated in a separate thread, we use a
   * {@link CountDownLatch} to wait for the service to be created.
   *
   * After the {@link ServiceLocator} is created, we specifically do not call
   * any more methods on it: the locator must instantiate the Immediate service
   * without any further prompting to the locator.
   *
   * @throws InterruptedException
   *           if awaiting on the latch is interrupted.
   */
  @Test
  public void testHK2ImmediateAnnotation() throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);

    @SuppressWarnings("unused") // It is unused by design
    ServiceLocator sl = Injections.createLocator(new AbstractBinder() {
      @Override
      protected void configure() {
        bind(latch).to(CountDownLatch.class);
        bind(ImmediateMe.class).to(ImmediateMe.class).in(Immediate.class);
      }
    });

    // 10 seconds is a LONG time. It should be faster than that. However, 10
    // seconds gives us a reasonable upper limit to wait in case the test fails.
    assertTrue("Latch should be unlocked within 10 seconds.",
        latch.await(10, TimeUnit.SECONDS));
  }

  /**
   * Helper class for testing Immediate services.
   *
   */
  public static final class ImmediateMe {
    @Inject
    public ImmediateMe(CountDownLatch latch) {
      latch.countDown();
    }
  }

}

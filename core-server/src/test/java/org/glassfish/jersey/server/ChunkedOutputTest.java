package org.glassfish.jersey.server;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ChunkedOutputTest {

  @Rule
  public final ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testBatchClose() throws Exception {
    final ChunkedOutput<Object> subject = new ChunkedOutput<Object>(Object.class);

    final Object o1 = new Object(), o2 = new Object();

    ChunkedOutput.Batch<Object> ref;
    try (final ChunkedOutput.Batch<Object> batch = subject.batch()) {
      ref = batch;

      batch.add(o1);
      batch.add(o2);
    }

    assertThat(subject.queue, contains(o1, o2));
    assertTrue(ref.isClosed());
  }

  @Test
  public void testBatchDoubleClose() throws Exception {
    final ChunkedOutput<Object> subject = new ChunkedOutput<Object>(Object.class);

    try (final ChunkedOutput.Batch<Object> batch = subject.batch()) {
      batch.close();

      expectedException.expect(IOException.class);
      expectedException.expectMessage(is(LocalizationMessages.CHUNKED_OUTPUT_BATCH_CLOSED()));
    }
  }

  @Test
  public void testBatchAddAfterClose() throws Exception {
    final ChunkedOutput<Object> subject = new ChunkedOutput<Object>(Object.class);

    ChunkedOutput.Batch<Object> ref;
    try (final ChunkedOutput.Batch<Object> batch = subject.batch()) {
      ref = batch;
    }

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(is(LocalizationMessages.CHUNKED_OUTPUT_BATCH_CLOSED()));

    ref.add(new Object());
  }

  @Test
  public void testBatchAddNull() throws Exception {
    final ChunkedOutput<Object> subject = new ChunkedOutput<Object>(Object.class);

    try (final ChunkedOutput.Batch<Object> batch = subject.batch()) {
      batch.add(null);
    }

    assertThat(subject.queue, empty());
  }

  @Test
  public void testBatchWriteToClosedChunkedOutput() throws Exception {
    final ChunkedOutput<Object> subject = new ChunkedOutput<Object>(Object.class);

    try (final ChunkedOutput.Batch<Object> batch = subject.batch()) {
      subject.close();

      expectedException.expect(IOException.class);
      expectedException.expectMessage(is(LocalizationMessages.CHUNKED_OUTPUT_CLOSED()));

      batch.add(null);
    }
  }

}

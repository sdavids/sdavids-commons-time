/*
 * Copyright (c) 2017, Sebastian Davids
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.sdavids.commons.time;

import static io.sdavids.commons.test.junit4.DefaultTimeZoneRule.forTimeZone;
import static io.sdavids.commons.time.ClockSupplier.fixedClockSupplier;
import static io.sdavids.commons.time.ClockSupplier.fixedUtcClockSupplier;
import static io.sdavids.commons.time.ClockSupplier.systemDefaultZoneClockSupplier;
import static io.sdavids.commons.time.ClockSupplier.systemUtcClockSupplier;
import static io.sdavids.commons.time.TestableClockSupplier.FIXED_INSTANT;
import static io.sdavids.commons.time.TestableClockSupplier.FIXED_ZONE;
import static java.time.Clock.systemDefaultZone;
import static java.time.Clock.systemUTC;
import static java.util.ServiceLoader.load;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.generate;
import static org.assertj.core.api.Assertions.assertThat;

import io.sdavids.commons.test.junit4.DefaultTimeZoneRule;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public final class ClockSupplierTest {

  private static final long COUNT = 100L;

  private static Function<Future<Clock>, Clock> getFuture() {
    return f -> {
      try {
        return f.get();
      } catch (InterruptedException | ExecutionException e) {
        throw new IllegalStateException(e);
      }
    };
  }

  private static Supplier<Instant> supplier(Callable<Instant> supplier) {
    return () -> {
      try {
        return supplier.call();
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }
    };
  }

  @Rule public ExpectedException expectedException = ExpectedException.none();

  @Rule public DefaultTimeZoneRule timeZone = forTimeZone(TimeZone.getTimeZone("Europe/Berlin"));

  @Test
  public void systemUtcClockSupplier_() throws InterruptedException {
    Supplier<Clock> supplier = systemUtcClockSupplier();

    assertThat(supplier.toString()).isEqualTo("ClockSupplier.systemUtcClockSupplier()");

    ExecutorService service = newFixedThreadPool(5);

    List<Future<Clock>> result =
        service.invokeAll(
            generate(() -> (Callable<Clock>) supplier::get).limit(COUNT).collect(toList()));

    service.shutdown();
    service.awaitTermination(1L, MINUTES);

    Set<Clock> clocks = result.stream().map(getFuture()).collect(toSet());

    assertThat(clocks).hasSize(1);
    assertThat(clocks).doesNotContainNull();

    Clock clock = clocks.iterator().next();

    // noinspection ConstantConditions
    assertThat(clock.getZone()).isEqualTo(systemUTC().getZone());

    Set<Instant> instants =
        generate(
                supplier(
                    () -> {
                      MILLISECONDS.sleep(10L);
                      return clock.instant();
                    }))
            .limit(COUNT)
            .collect(toSet());

    assertThat(instants).hasSize((int) COUNT);
    assertThat(instants).doesNotContainNull();
  }

  @Test
  public void systemDefaultZoneClockSupplier_() throws InterruptedException {
    Supplier<Clock> supplier = systemDefaultZoneClockSupplier();

    assertThat(supplier.toString()).isEqualTo("ClockSupplier.systemDefaultZoneClockSupplier()");

    ExecutorService service = newFixedThreadPool(5);

    List<Future<Clock>> result =
        service.invokeAll(
            generate(() -> (Callable<Clock>) supplier::get).limit(COUNT).collect(toList()));

    service.shutdown();
    service.awaitTermination(1L, MINUTES);

    Set<Clock> clocks = result.stream().map(getFuture()).collect(toSet());

    assertThat(clocks).hasSize(1);
    assertThat(clocks).doesNotContainNull();

    Clock clock = clocks.iterator().next();

    // noinspection ConstantConditions
    assertThat(clock.getZone()).isEqualTo(systemDefaultZone().getZone());

    Set<Instant> instants =
        generate(
                supplier(
                    () -> {
                      MILLISECONDS.sleep(10L);
                      return clock.instant();
                    }))
            .limit(COUNT)
            .collect(toSet());

    assertThat(instants).hasSize((int) COUNT);
    assertThat(instants).doesNotContainNull();
  }

  @Test
  public void fixedClockSupplier_fixedInstant_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("fixedInstant");

    // noinspection ConstantConditions
    fixedClockSupplier(null, FIXED_ZONE);
  }

  @Test
  public void fixedClockSupplier_() throws InterruptedException {
    Supplier<Clock> supplier = fixedClockSupplier(FIXED_INSTANT, FIXED_ZONE);

    assertThat(supplier.toString())
        .isEqualTo("ClockSupplier.fixedClockSupplier(2017-10-02T17:03:00Z, America/Chicago)");

    ExecutorService service = newFixedThreadPool(5);

    List<Future<Clock>> result =
        service.invokeAll(
            generate(() -> (Callable<Clock>) supplier::get).limit(COUNT).collect(toList()));

    service.shutdown();
    service.awaitTermination(1L, MINUTES);

    Set<Clock> clocks = result.stream().map(getFuture()).collect(toSet());

    assertThat(clocks).hasSize(1);
    assertThat(clocks).doesNotContainNull();

    Clock clock = clocks.iterator().next();

    // noinspection ConstantConditions
    assertThat(clock.getZone()).isEqualTo(FIXED_ZONE);

    Set<Instant> instants =
        generate(
                supplier(
                    () -> {
                      MILLISECONDS.sleep(10L);
                      return clock.instant();
                    }))
            .limit(COUNT)
            .collect(toSet());

    assertThat(instants).hasSize(1);
    assertThat(instants).doesNotContainNull();

    assertThat(instants.iterator().next()).isEqualTo(FIXED_INSTANT);
  }

  @Test
  public void fixedClockSupplier_zone_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("zone");

    // noinspection ConstantConditions
    fixedClockSupplier(FIXED_INSTANT, null);
  }

  @Test
  public void fixedUtcClockSupplier_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("fixedInstant");

    // noinspection ConstantConditions
    fixedUtcClockSupplier(null);
  }

  @Test
  public void fixedUtcClockSupplier_() throws InterruptedException {
    Supplier<Clock> supplier = fixedUtcClockSupplier(FIXED_INSTANT);

    assertThat(supplier.toString())
        .isEqualTo("ClockSupplier.fixedClockSupplier(2017-10-02T17:03:00Z, Etc/UTC)");

    ExecutorService service = newFixedThreadPool(5);

    List<Future<Clock>> result =
        service.invokeAll(
            generate(() -> (Callable<Clock>) supplier::get).limit(COUNT).collect(toList()));

    service.shutdown();
    service.awaitTermination(1L, MINUTES);

    Set<Clock> clocks = result.stream().map(getFuture()).collect(toSet());

    assertThat(clocks).hasSize(1);
    assertThat(clocks).doesNotContainNull();

    Clock clock = clocks.iterator().next();

    // noinspection ConstantConditions
    assertThat(clock.getZone()).isEqualTo(ZoneId.of("Etc/UTC"));

    Set<Instant> instants =
        generate(
                supplier(
                    () -> {
                      MILLISECONDS.sleep(10L);
                      return clock.instant();
                    }))
            .limit(COUNT)
            .collect(toSet());

    assertThat(instants).hasSize(1);
    assertThat(instants).doesNotContainNull();

    assertThat(instants.iterator().next()).isEqualTo(FIXED_INSTANT);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void fixedClockSupplier_serialization() throws IOException, ClassNotFoundException {
    byte[] serialized;
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = new ObjectOutputStream(bos)) {

      Supplier<Clock> original = fixedClockSupplier(FIXED_INSTANT, FIXED_ZONE);

      out.writeObject(original);

      serialized = bos.toByteArray();
    }

    Supplier<Clock> deserialized;
    try (ByteArrayInputStream bis = new ByteArrayInputStream(serialized);
        ObjectInputStream in = new ObjectInputStream(bis)) {

      deserialized = (Supplier<Clock>) in.readObject();
    }

    Clock clock = deserialized.get();

    assertThat(clock.getZone()).isEqualTo(FIXED_ZONE);
    assertThat(clock.instant()).isEqualTo(FIXED_INSTANT);
  }

  @Test
  public void getDefault_default_impl() {
    Iterator<ClockSupplier> providers = load(ClockSupplier.class).iterator();

    assertThat(providers.hasNext()).isFalse();

    Supplier<Clock> first = ClockSupplier.getDefault();

    assertThat(first).isNotNull();

    Supplier<Clock> second = ClockSupplier.getDefault();

    assertThat(second).isNotNull();

    assertThat(first).isSameAs(second);
  }

  @Test
  public void getDefault_get() {
    assertThat(ClockSupplier.getDefault().get()).isNotNull();
  }
}

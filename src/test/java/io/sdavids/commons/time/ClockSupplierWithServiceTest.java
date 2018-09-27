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

import static io.sdavids.commons.test.MockServices.setServices;
import static io.sdavids.commons.time.TestableClockSupplier.FIXED_INSTANT;
import static io.sdavids.commons.time.TestableClockSupplier.FIXED_ZONE;
import static java.util.ServiceLoader.load;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.util.Iterator;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;

public final class ClockSupplierWithServiceTest {

  @Before
  public void setUp() {
    setServices(TestableClockSupplier.class);
  }

  @Test
  public void getDefault_() {
    Iterator<ClockSupplier> providers = load(ClockSupplier.class).iterator();

    assertThat(providers.hasNext()).isTrue();

    Supplier<Clock> first = ClockSupplier.getDefault();
    Clock firstClock = first.get();

    assertThat(first).isNotNull();
    assertThat(first.toString())
        .isEqualTo("NonCachingUuidSupplier(io.sdavids.commons.time.TestableClockSupplier)");
    assertThat(firstClock.instant()).isEqualTo(FIXED_INSTANT);
    assertThat(firstClock.getZone()).isEqualTo(FIXED_ZONE);

    setServices();

    providers = load(ClockSupplier.class).iterator();

    assertThat(providers.hasNext()).isFalse();

    Supplier<Clock> second = ClockSupplier.getDefault();
    Clock secondClock = first.get();

    assertThat(second).isNotNull();
    assertThat(second.toString())
        .isEqualTo(
            "NonCachingUuidSupplier(io.sdavids.commons.time.ClockSupplier$SystemUtcClockSupplier)");
    assertThat(secondClock.instant()).isNotEqualTo(FIXED_INSTANT);
    assertThat(secondClock.getZone()).isNotEqualTo(FIXED_ZONE);

    assertThat(first).isSameAs(second);
  }

  @Test
  public void getDefault_get() {
    Supplier<Clock> supplier = ClockSupplier.getDefault();

    Clock clock = supplier.get();
    assertThat(clock.instant()).isEqualTo(FIXED_INSTANT);
    assertThat(clock.getZone()).isEqualTo(FIXED_ZONE);
  }
}

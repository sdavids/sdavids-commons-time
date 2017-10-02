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

import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public final class TestableClockSupplier extends ClockSupplier {

  private static final String FIXED_ZONE_ID = "America/Chicago";

  static final Instant FIXED_INSTANT = OffsetDateTime.of(2017, 10, 2, 17, 3, 0, 0, UTC).toInstant();
  static final ZoneId FIXED_ZONE = ZoneId.of(FIXED_ZONE_ID);

  @Override
  public Clock get() {
    return Clock.fixed(FIXED_INSTANT, FIXED_ZONE);
  }

  @Override
  public String toString() {
    return "TestableClockSupplier";
  }
}

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

import static java.security.AccessController.doPrivileged;
import static java.util.Objects.requireNonNull;
import static java.util.ServiceLoader.load;
import static org.apiguardian.api.API.Status.STABLE;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.PrivilegedAction;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Iterator;
import java.util.function.Supplier;
import org.apiguardian.api.API;

/**
 * Suppliers for clocks.
 *
 * <p>The default instance returns a clock in the UTC time-zone.
 *
 * @see Clock#systemUTC()
 * @see java.util.ServiceLoader
 * @see <a href="http://wiki.apidesign.org/wiki/Injectable_Singleton">Injectable Singleton</a>
 * @since 1.0
 */
@API(status = STABLE, since = "1.0")
public abstract class ClockSupplier implements Supplier<Clock> {

  static final String CACHED_PROPERTY_KEY = "io.sdavids.commons.time.clock.supplier.default.cached";

  private enum SystemUtcClockSupplier implements Supplier<Clock> {
    INSTANCE;

    @Override
    public String toString() {
      return "ClockSupplier.systemUtcClockSupplier()";
    }

    @Override
    public Clock get() {
      return Clock.systemUTC();
    }
  }

  private enum SystemDefaultZoneClockSupplier implements Supplier<Clock> {
    INSTANCE;

    @Override
    public String toString() {
      return "ClockSupplier.systemDefaultZoneClockSupplier()";
    }

    @Override
    public Clock get() {
      return Clock.systemDefaultZone();
    }
  }

  private static final class FixedClockSupplier implements Supplier<Clock>, Serializable {

    private static final long serialVersionUID = 1662342342420281297L;

    private final Instant fixedInstant;

    private transient ZoneId zone;
    private transient Clock clock;

    FixedClockSupplier(Instant fixedInstant, ZoneId zone) {
      this.fixedInstant = requireNonNull(fixedInstant, "fixedInstant");
      this.zone = requireNonNull(zone, "zone");
      clock = Clock.fixed(fixedInstant, zone);
    }

    @Override
    public String toString() {
      return "ClockSupplier.fixedClockSupplier(" + fixedInstant + ", " + zone + ')';
    }

    @Override
    public Clock get() {
      return clock;
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
      stream.defaultWriteObject();
      stream.writeUTF(zone.getId());
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
      stream.defaultReadObject();
      String zoneId = stream.readUTF();

      zone = ZoneId.of(zoneId);

      requireNonNull(fixedInstant, "fixedInstant");
      requireNonNull(zone, "zone");

      clock = Clock.fixed(fixedInstant, zone);
    }
  }

  private static final class NonCachingUuidSupplier implements Supplier<Clock>, Serializable {

    private static final long serialVersionUID = 937273036612993297L;

    private volatile String supplierClassName = "unitialized - call get() first";

    @Override
    public Clock get() {
      Iterator<ClockSupplier> providers = load(ClockSupplier.class).iterator();

      Supplier<Clock> supplier =
          providers.hasNext() ? providers.next() : SystemUtcClockSupplier.INSTANCE;

      supplierClassName = supplier.getClass().getName();

      return supplier.get();
    }

    @Override
    public String toString() {
      return "NonCachingUuidSupplier(" + supplierClassName + ')';
    }
  }

  private static final class SingletonHolder {

    private static Supplier<Clock> initialize() {
      String cached = getCachedSystemProperty();

      if (cached == null || Boolean.parseBoolean(cached)) {
        Iterator<ClockSupplier> providers = load(ClockSupplier.class).iterator();

        return providers.hasNext() ? providers.next() : SystemUtcClockSupplier.INSTANCE;
      }

      return new NonCachingUuidSupplier();
    }

    private static String getCachedSystemProperty() {
      return System.getSecurityManager() == null
          ? System.getProperty(CACHED_PROPERTY_KEY)
          : doPrivileged((PrivilegedAction<String>) () -> System.getProperty(CACHED_PROPERTY_KEY));
    }

    static final Supplier<Clock> INSTANCE = initialize();
  }

  /**
   * Obtains the default instance of the clock supplier.
   *
   * <p>The first instance of type {@code ClockSupplier} obtained by the {@code ServiceLoader} is
   * used. Otherwise, a supplier returning a clock in the UTC timezone is used.
   *
   * <p>By default the instance returned by the {@code ServiceLoader} is cached. In order to turn
   * off caching set the system property {@code
   * io.sdavids.commons.time.clock.supplier.default.cached} to {@code false}. <em>Note:</em> The
   * system property is evaluated once, i.e. caching cannot be dynamically enabled/disabled.
   *
   * @return some Clock supplier; never null
   * @see #systemUtcClockSupplier()
   * @since 1.0
   */
  public static Supplier<Clock> getDefault() {
    return SingletonHolder.INSTANCE;
  }

  /**
   * Returns a supplier returning a clock in the UTC time-zone.
   *
   * @return a {@code Clock.systemUTC()} supplier
   * @since 1.0
   */
  public static Supplier<Clock> systemUtcClockSupplier() {
    return SystemUtcClockSupplier.INSTANCE;
  }

  /**
   * Returns a supplier returning a clock in the system's default time-zone.
   *
   * @return a {@code Clock.systemDefaultZone()} supplier
   * @since 1.0
   */
  public static Supplier<Clock> systemDefaultZoneClockSupplier() {
    return SystemDefaultZoneClockSupplier.INSTANCE;
  }

  /**
   * Returns a supplier returning a fixed clock.
   *
   * @param fixedInstant the instant to use as the clock, not null
   * @param zone the time-zone to use to convert the instant to date-time, not null
   * @return a fixed clock supplier
   * @since 1.0
   */
  public static Supplier<Clock> fixedClockSupplier(Instant fixedInstant, ZoneId zone) {
    return new FixedClockSupplier(fixedInstant, zone);
  }

  /**
   * Returns a supplier returning a fixed clock in the UTC time-zone.
   *
   * @param fixedInstant the instant to use as the clock, not null
   * @return a fixed clock supplier
   * @since 1.0
   */
  public static Supplier<Clock> fixedUtcClockSupplier(Instant fixedInstant) {
    return new FixedClockSupplier(fixedInstant, ZoneId.of("Etc/UTC"));
  }

  protected ClockSupplier() {
    // injectable singleton
  }
}

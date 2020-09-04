package eu.interop.federationgateway.utils;

import org.slf4j.MDC;

/**
 * Wrapper for MDC to escape values for better log files.
 */
public class EfgsMDC {

  /**
   * Put a diagnostic context value (the <code>value</code> parameter) as identified with the
   * <code>key</code> parameter into the current thread's diagnostic context map. The
   * <code>key</code> parameter cannot be null. The <code>value</code> parameter
   * can be null only if the underlying implementation supports it.
   *
   * <p>
   * This method delegates all work to the MDC of the underlying logging system.
   *
   * @param key   non-null key
   * @param value value to put in the map
   * @throws IllegalArgumentException in case the "key" parameter is null
   */
  public static void put(String key, String value) {
    value = value == null ? ">>null<<" : value;

    value = value.replace("\"", "'");
    MDC.put(key, "\"" + value + "\"");
  }

  /**
   * Put a diagnostic context value (the <code>value</code> parameter) as identified with the
   * <code>key</code> parameter into the current thread's diagnostic context map. The
   * <code>key</code> parameter cannot be null. The <code>value</code> parameter
   * can be null only if the underlying implementation supports it.
   *
   * <p>
   * This method delegates all work to the MDC of the underlying logging system.
   *
   * @param key   non-null key
   * @param value value to put in the map
   * @throws IllegalArgumentException in case the "key" parameter is null
   */
  public static void put(String key, long value) {
    put(key, String.valueOf(value));
  }

  /**
   * Remove the diagnostic context identified by the <code>key</code> parameter using
   * the underlying system's MDC implementation. The <code>key</code> parameter
   * cannot be null. This method does nothing if there is no previous value
   * associated with <code>key</code>.
   *
   * @param key non-null key
   * @throws IllegalArgumentException in case the "key" parameter is null
   */
  public static void remove(String key) {
    MDC.remove(key);
  }

  /**
   * Clear all entries in the MDC of the underlying implementation.
   */
  public static void clear() {
    MDC.clear();
  }
}

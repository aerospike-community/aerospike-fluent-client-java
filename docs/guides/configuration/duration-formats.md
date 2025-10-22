# Duration Formats

This document provides a comprehensive list of the supported duration formats for use in YAML configuration files.

## Overview

When specifying time durations in YAML `behavior` configuration files, you can use a variety of human-readable formats. These formats are parsed by the client and converted into `java.time.Duration` objects.

**Example YAML Configuration:**
```yaml
behaviors:
  - name: "my-behavior"
    parent: "default"
    allOperations:
      abandonCallAfter: "10s"
      delayBetweenRetries: "50ms"
```

---

## Supported Units

The duration format consists of a numeric value followed by a unit suffix. There should be no space between the number and the unit.

The following table lists all supported units and their variations.

| Unit        | Suffixes (Case-Insensitive)                             | Example       |
|-------------|---------------------------------------------------------|---------------|
| Nanoseconds | `ns`, `nanos`, `nanosecond`, `nanoseconds`              | `500ns`       |
| Microseconds| `us`, `micros`, `microsecond`, `microseconds`           | `100us`       |
| Milliseconds| `ms`, `millis`, `millisecond`, `milliseconds`           | `50ms`        |
| Seconds     | `s`, `sec`, `second`, `seconds`                         | `10s`         |
| Minutes     | `m`, `min`, `minute`, `minutes`                         | `5m`          |
| Hours       | `h`, `hr`, `hour`, `hours`                              | `1h`          |
| Days        | `d`, `day`, `days`                                      | `7d`          |

---

## ISO-8601 Format

In addition to the short human-readable formats, the standard ISO-8601 duration format is also supported.

**Format:** `PTnHnMnS`

- `P` is the duration designator (for period).
- `T` is the time designator.
- `nH`, `nM`, and `nS` are the number of hours, minutes, and seconds respectively.

**Examples:**
- `PT10S` = 10 seconds
- `PT1M30S` = 1 minute and 30 seconds
- `PT2H` = 2 hours
- `P3D` = 3 days

---

## Examples in YAML

Here is a more comprehensive example of a `behavior-config.yml` file showcasing various duration formats.

```yaml
behaviors:
  - name: "fast-reads"
    parent: "default"
    availabilityModeReads:
      abandonCallAfter: "500ms"  # Milliseconds
      withSocketTimeout: "250ms"

  - name: "reliable-writes"
    parent: "default"
    retryableWrites:
      abandonCallAfter: "30s"      # Seconds
      delayBetweenRetries: "100ms"
      maximumNumberOfCallAttempts: 5

  - name: "long-running-query"
    parent: "default"
    query:
      abandonCallAfter: "5m"      # Minutes

  - name: "batch-processing"
    parent: "default"
    batchReads:
      abandonCallAfter: "PT1H"    # 1 hour in ISO-8601 format
```

---

## Next Steps

- **[YAML Configuration](./yaml-configuration.md)**: Learn more about how to structure and use YAML files for behavior configuration.
- **[Dynamic Reloading](./dynamic-reloading.md)**: Discover how to reload configuration changes at runtime.

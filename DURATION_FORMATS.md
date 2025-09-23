# Duration Formats in YAML Configuration

The YAML configuration system supports human-readable duration formats for all time-based settings. This makes the configuration more intuitive and easier to read.

## Supported Formats

### Basic Units
- **`ns`** - Nanoseconds (e.g., `100ns`)
- **`us`** - Microseconds (e.g., `500us`)
- **`ms`** - Milliseconds (e.g., `100ms`, `5ms`)
- **`s`** - Seconds (e.g., `10s`, `30s`)
- **`m`** - Minutes (e.g., `1m`, `5m`)
- **`h`** - Hours (e.g., `1h`, `2h`)
- **`d`** - Days (e.g., `1d`, `7d`)

### Alternative Unit Names
Each unit also supports alternative names for better readability:

| Unit | Alternative Names |
|------|------------------|
| `ns` | `nanos`, `nanosecond`, `nanoseconds` |
| `us` | `micros`, `microsecond`, `microseconds` |
| `ms` | `millis`, `millisecond`, `milliseconds` |
| `s` | `sec`, `second`, `seconds` |
| `m` | `min`, `minute`, `minutes` |
| `h` | `hr`, `hour`, `hours` |
| `d` | `day`, `days` |

## Examples

### YAML Configuration Examples
```yaml
behaviors:
  - name: "high-performance"
    allOperations:
      abandonCallAfter: "10s"           # 10 seconds
      delayBetweenRetries: "5ms"        # 5 milliseconds
      waitForCallToComplete: "500ms"    # 500 milliseconds
      waitForConnectionToComplete: "100ms"  # 100 milliseconds
      waitForSocketResponseAfterCallFails: "50ms"  # 50 milliseconds
    
    retryableWrites:
      delayBetweenRetries: "5ms"        # 5 milliseconds
```

### More Examples
```yaml
# Various duration formats
abandonCallAfter: "30s"        # 30 seconds
delayBetweenRetries: "100ms"   # 100 milliseconds
waitForCallToComplete: "1m"    # 1 minute
waitForConnectionToComplete: "2s"  # 2 seconds
waitForSocketResponseAfterCallFails: "500ms"  # 500 milliseconds

# Alternative unit names
abandonCallAfter: "30seconds"  # Same as "30s"
delayBetweenRetries: "100milliseconds"  # Same as "100ms"
waitForCallToComplete: "1minute"  # Same as "1m"
```

## Fallback to ISO-8601

If the human-readable format cannot be parsed, the system will fall back to trying to parse the value as an ISO-8601 duration format (e.g., `PT30S` for 30 seconds).

## Error Handling

If a duration value cannot be parsed using either format, you'll get a clear error message like:
```
Cannot parse duration: invalid-value. Expected format: <number><unit> (e.g., '10s', '20ms', '1m') or ISO-8601 duration
```

## Best Practices

1. **Use human-readable formats** for better readability: `"10s"` instead of `"PT10S"`
2. **Be consistent** with your unit choices throughout the configuration
3. **Use appropriate precision** - don't use nanoseconds when milliseconds are sufficient
4. **Consider readability** - `"1m"` is clearer than `"60s"` for one minute

## Supported Duration Fields

The following fields in the YAML configuration support these duration formats:

- `abandonCallAfter`
- `delayBetweenRetries`
- `waitForCallToComplete`
- `waitForConnectionToComplete`
- `waitForSocketResponseAfterCallFails`

All these fields are found in the `allOperations`, `retryableWrites`, `nonRetryableWrites`, `batchReads`, `batchWrites`, `query`, and `info` sections of the behavior configuration. 
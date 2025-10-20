# InfoParser Class

The `InfoParser` class is an internal utility for parsing the string-based results of Aerospike info commands into structured Java objects.

## Overview

**This is an internal class and is not intended for public use.**

The `InfoParser` is responsible for:
- Executing info commands on the cluster.
- Parsing the semicolon-delimited key-value pair strings returned by the server.
- Mapping the parsed data to annotated Java objects (like `NamespaceDetail`).
- Aggregating results from multiple nodes.

Its functionality is exposed to the end-user through the much simpler `InfoCommands` class.

---

## Next Steps

- **[InfoCommands](./info-commands.md)**: The public API for accessing info command results.

---
layout: documentation
menu: yugabytedb
subtitle: YugabyteDB
---

# YugabyteDB

## Supported Versions

- `2.7`
- `2.6`
- `2.5`
- `2.4`

## Driver

<table class="table">
<tr>
<th>URL format</th>
<td><code>jdbc:postgresql://<i>host</i>:<i>port</i>/<i>database</i></code></td>
</tr>
<tr>
<th>SSL support</th>
<td>Yes - add <code>?ssl=true</code></td>
</tr>
<tr>
<th>Maven Central coordinates</th>
<td><code>org.postgresql:postgresql:42.2.14</code></td>
</tr>
<tr>
<th>Supported versions</th>
<td><code>9.3-1104-jdbc4</code> and later</td>
</tr>
<tr>
<th>Default Java class</th>
<td><code>org.postgresql.Driver</code></td>
</tr>
</table>

## Notes

YugabyteDB is a variant of PostgreSQL and MigrateDB usage is the same for the two databases. For more details,
please refer to the [PostgreSQL](/migratedb/documentation/database/postgresql) page.

<p class="next-steps">
    <a class="btn btn-primary" href="/migratedb/documentation/database/aurora-postgresql">Aurora PostgreSQL ➡️</a>
</p>
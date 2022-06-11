---
layout: documentation
menu: authentication
subtitle: Authentication
---

# Authentication

In order to log in to your database, the typical approach is to set your username and password in the
MigrateDB [configuration file](/migratedb/documentation/configuration/configfile). This however has some concerns:

- These properties are stored in plain text - anyone can see your credentials
- Your credentials must be supplied in every configuration file you use
- You may not have access to these credentials, and someone else needs to configure them securely

MigrateDB comes with additional authentication mechanisms that tackle these concerns.

## Environment Variables

By storing your username and password in the environment variables `MIGRATEDB_USER` and `MIGRATEDB_PASSWORD`
respectively, they can be configured once and used across multiple MigrateDB configurations. They can also be set by
someone who has the relevant access, so they do not end up being leaked to any unwarranted parties.

## Database Specific Authentication

### Oracle

- [Oracle Wallet](/migratedb/documentation/database/oracle#oracle-wallet)

### SQL Server and Azure Synapse

- [Windows Authentication](/migratedb/documentation/database/sqlserver#windows-authentication)
- [Azure Active Directory](/migratedb/documentation/database/sqlserver#azure-active-directory)
- [Kerberos](/migratedb/documentation/database/sqlserver#kerberos)

### MySQL

- [MySQL Option Files](/migratedb/documentation/database/mysql#option-files)

### PostgreSQL

- [SCRAM](/migratedb/documentation/database/postgresql#scram)
- [pgpass](/migratedb/documentation/database/postgresql#pgpass)

### Snowflake

- [Key-based Authentication](/migratedb/documentation/database/snowflake#key-based-authentication)

---
layout: documentation
menu: configuration
pill: oracleWalletLocation
subtitle: migratedb.oracleWalletLocation
---
{% include not-implemented.html %}       

# Oracle Wallet Location

## Description

The location on disk of your Oracle wallet.

## Default

null

## Usage

### API

```java
MigrateDb.configure()
    .extensionConfig(OracleConfig.class, new OracleConfig()
            .walletLocation("/User/db/my_wallet"))
    .load()
```

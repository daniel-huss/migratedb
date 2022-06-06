---
layout: documentation
menu: configuration
pill: oracleWalletLocation
subtitle: migratedb.oracleWalletLocation
---

# Oracle Wallet Location

## Description

The location on disk of your Oracle wallet.

## Default

null

## Usage

### Command line

```powershell
./migratedb -oracle.walletLocation="/User/db/my_wallet" info
```

### Configuration File

```properties
migratedb.oracle.walletLocation=/User/db/my_wallet
```

### Environment Variable

```properties
MIGRATEDB_ORACLE_WALLET_LOCATION=/User/db/my_wallet
```

### API

```java
MigrateDB.configure()
    .oracleWalletLocation("/User/db/my_wallet")
    .load()
```

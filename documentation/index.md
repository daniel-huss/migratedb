---
layout: documentation
menu: overview
title: Documentation
---

# Documentation

<p>Welcome to <strong>MigrateDB</strong>, database migrations made easy.</p>

<div class="well well-small">
    <strong>Tip:</strong>
    If you haven't checked out the <a href="/documentation/getstarted">Get Started</a> section yet, do it now. You'll be up
    and running in no time!
</div>

<h1 class="text--center">
    <span class="icon--migratedb color--red icon--6x display--block spaced-v"></span>
    <span class="text--bigger">MigrateDB</span>
</h1>

<p>MigrateDB is an open-source database migration tool. It strongly favors simplicity and convention over
    configuration.</p>

<p>It is based around just 6 basic commands:
    <a href="/documentation/command/migrate">Migrate</a>,
    <a href="/documentation/command/clean">Clean</a>,
    <a href="/documentation/command/info">Info</a>,
    <a href="/documentation/command/validate">Validate</a>,
    <a href="/documentation/command/baseline">Baseline</a> and
    <a href="/documentation/command/repair">Repair</a>.
</p>

<p>Migrations can be written in <a href="/documentation/concepts/migrations#sql-based-migrations">SQL</a>
    (database-specific syntax (such as PL/SQL, T-SQL, ...) is supported)
    or <a href="/documentation/concepts/migrations#java-based-migrations">Java</a>
    (for advanced data transformations or dealing with LOBs).</p>

<p>It has a <a href="/documentation/usage/commandline">Command-line client</a>.
    If you are on the JVM, we recommend using the <a href="/documentation/usage/api">Java API</a>
    for migrating the database on application startup.</p>

<p class="next-steps">
    <a class="btn btn-primary" href="/documentation/concepts/migrations">Migrations <i class="fa fa-arrow-right"></i></a>
</p>

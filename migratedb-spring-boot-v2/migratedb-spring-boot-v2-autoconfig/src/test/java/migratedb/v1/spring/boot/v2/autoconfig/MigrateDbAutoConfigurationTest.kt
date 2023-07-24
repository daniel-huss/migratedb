/*
 * Copyright 2012-2023 the original author or authors.
 * Copyright 2023 The MigrateDB contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package migratedb.v1.spring.boot.v2.autoconfig

import io.kotest.assertions.assertSoftly
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.collections.*
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.types.shouldBeInstanceOf
import migratedb.v1.core.MigrateDb
import migratedb.v1.core.api.Checksum
import migratedb.v1.core.api.Location.ClassPathLocation
import migratedb.v1.core.api.MigrateDbException
import migratedb.v1.core.api.MigrationPattern
import migratedb.v1.core.api.Version
import migratedb.v1.core.api.callback.Callback
import migratedb.v1.core.api.callback.Event
import migratedb.v1.core.api.migration.Context
import migratedb.v1.core.api.migration.JavaMigration
import migratedb.v1.spring.boot.v2.autoconfig.MigrateDbAutoConfigurationTest.DataSources.EMPTY_TABLE_INSIDE_MARKER_SCHEMA
import migratedb.v1.spring.boot.v2.autoconfig.MigrateDbAutoConfigurationTest.DataSources.MIGRATION_DS_MARKER_SCHEMA
import migratedb.v1.spring.boot.v2.autoconfig.MigrateDbAutoConfigurationTest.DataSources.evaluateUsingDataSource
import org.assertj.core.api.AbstractObjectAssert
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.engine.transaction.jta.platform.internal.NoJtaPlatform
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DefaultDSLContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.ArgumentMatchers
import org.mockito.BDDMockito.given
import org.mockito.Mockito
import org.springframework.beans.factory.BeanCreationException
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.jdbc.SchemaManagement
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder
import org.springframework.boot.test.context.assertj.ApplicationContextAssert
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.core.io.ResourceLoader
import org.springframework.jdbc.core.JdbcOperations
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactory
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType
import org.springframework.jdbc.datasource.init.DatabasePopulator
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.springframework.stereotype.Component
import java.sql.SQLException
import java.util.concurrent.atomic.AtomicLong
import javax.sql.DataSource

/**
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Vedran Pavic
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Dominic Gunn
 * @author András Deák
 * @author Takaaki Shimbo
 * @author Chris Bono
 * @author Daniel Huss
 */
internal class MigrateDbAutoConfigurationTest {
    private val contextRunner =
        ApplicationContextRunner().withConfiguration(AutoConfigurations.of(MigrateDbAutoConfiguration::class.java))

    @Test
    fun `Backs off if there is no data source`() {
        contextRunner.run { context ->
            assertThat(context).doesNotHaveBean<MigrateDb>()
        }
    }

    @Test
    fun `Setting migratedb_dataSource_url is enough to define a properties-based data source`() {
        contextRunner.withPropertyValues("spring.migratedb.dataSource.url=" + DataSources.newMigrationDataSourceUrl())
            .run { context ->
                assertThat(context).singleBean<MigrateDb>().extracting { it.configuration.dataSource }.isNotNull()
            }
    }

    @Test
    fun `Uses the migration DataSource if both application DataSource and a migration DataSource from migratedb_dataSource_url are present`() {
        contextRunner.withUserConfiguration(MultipleUsersApplicationDataSourceConfiguration::class.java)
            .withPropertyValues("spring.migratedb.dataSource.url=" + DataSources.newMigrationDataSourceUrl())
            .run { context ->
                assertThat(context).singleBean<MigrateDb>().isUsingMigrationDataSource()
            }
    }

    @Test
    fun `Uses the derived DataSource if both application DataSource and a derived DataSource are present`() {
        contextRunner.withUserConfiguration(MultipleUsersApplicationDataSourceConfiguration::class.java)
            .withPropertyValues(
                "spring.migratedb.user=${MultipleUsersApplicationDataSourceConfiguration.USER_1}",
                "spring.migratedb.password=${MultipleUsersApplicationDataSourceConfiguration.PASSWORD_1}"
            ).run { context ->
                assertThat(context).singleBean<MigrateDb>()
                    .isUsingDataSourceWithUser(MultipleUsersApplicationDataSourceConfiguration.USER_1)
            }
    }

    @Test
    fun `Uses the migration DataSource if both application DataSource and a migration DataSource from @MigrationDataSource are present`() {
        contextRunner.withUserConfiguration(MultipleDataSourcesAndSingleMigrationDataSourceConfiguration::class.java)
            .run { context ->
                assertThat(context).singleBean<MigrateDb>().isUsingMigrationDataSource()
            }
    }

    @Test
    fun `Fails if a @MigrationDataSource bean and derived DataSource are present`() {
        contextRunner.withUserConfiguration(ApplicationAndMigrationDataSourcesConfiguration::class.java)
            .withPropertyValues("spring.migratedb.user=migration_user") // = "Use a derived DS where the user is different"
            .run { context ->
                assertThat(context).hasFailed()
                assertThat(context).failure.rootCause().isInstanceOf(ConflictingDataSourcesException::class.java)
            }
    }

    @Test
    fun `Fails if a migration DataSource from migratedb_dataSource_url and a derived DataSource are present`() {
        contextRunner.withUserConfiguration(ApplicationAndMigrationDataSourcesConfiguration::class.java)
            .withPropertyValues("spring.migratedb.user=migration_user") // "Use a derived DS where the user is different"
            .run { context ->
                assertThat(context).hasFailed()
                assertThat(context).failure.rootCause().isInstanceOf(ConflictingDataSourcesException::class.java)
            }
    }

    @Test
    fun `Fails if a @MigrationDataSource bean and a migration DataSource from migratedb_dataSource_url are present`() {
        contextRunner.withUserConfiguration(ApplicationAndMigrationDataSourcesConfiguration::class.java)
            .withPropertyValues("spring.migratedb.data-source.url=" + DataSources.newMigrationDataSourceUrl())
            .run { context ->
                assertThat(context).hasFailed()
                assertThat(context).failure.rootCause().isInstanceOf(ConflictingDataSourcesException::class.java)
            }
    }

    @Test
    fun `Works if the only data source is marked as @MigrationDataSource`() {
        contextRunner.withUserConfiguration(MigrationDataSourceOnlyConfiguration::class.java).run { context ->
            assertThat(context).singleBean<MigrateDb>().isUsingMigrationDataSource()
        }
    }

    @Test
    fun `Schema management provider detects data source`() {
        contextRunner.withUserConfiguration(ApplicationAndMigrationDataSourcesConfiguration::class.java)
            .run { context ->
                val schemaManagementProvider = context.bean<MigrateDbSchemaManagementProvider>()
                val applicationDataSource = context.bean<DataSource>()
                val migrationDataSource = context.getBean("migrateDbDataSource") as DataSource

                assertThat(schemaManagementProvider.getSchemaManagement(applicationDataSource)).isEqualTo(
                    SchemaManagement.UNMANAGED
                )
                assertThat(schemaManagementProvider.getSchemaManagement(migrationDataSource)).isEqualTo(SchemaManagement.MANAGED)
            }
    }

    @Test
    fun `Uses default location if no properties are set`() {
        contextRunner.withUserConfiguration(MigrationDataSourceOnlyConfiguration::class.java).run { context ->
            assertThat(context).singleBean<MigrateDb>().all { actual ->
                actual.configuration.locations.shouldBeSingleton().single()
                    .shouldBeInstanceOf<ClassPathLocation>().namePrefix().shouldBeEqual("db/migration")
            }
        }
    }

    @Test
    fun `Can override locations via spring_migratedb_locations`() {
        contextRunner.withUserConfiguration(MultipleUsersApplicationDataSourceConfiguration::class.java)
            .withPropertyValues("spring.migratedb.locations=classpath:db/changelog,classpath:db/migration")
            .run { context ->
                assertThat(context).singleBean<MigrateDb>().all { actual ->
                    actual.configuration.locations.shouldForAll { it.shouldBeInstanceOf<ClassPathLocation>() }
                        .map { (it as ClassPathLocation).namePrefix() }
                        .shouldContainExactlyInAnyOrder("db/changelog", "db/migration")
                }
            }
    }

    @Test
    fun `Can override schemas via spring_migratedb_schemas`() {
        contextRunner.withUserConfiguration(MultipleUsersApplicationDataSourceConfiguration::class.java)
            .withPropertyValues("spring.migratedb.schemas=foobar").run { context ->
                assertThat(context).singleBean<MigrateDb>().all { actual ->
                    actual.configuration.schemas.shouldContainOnly("foobar")
                }
            }
    }

    @ParameterizedTest
    @ValueSource(
        strings = ["classpath:db/missing1,classpath:db/migration2", "db/missing1,db/migration2", "filesystem:no-such-dir"]
    )
    fun `Can validate class locations via spring_migratedb_fail-on-missing-locations`(missingLocations: String) {
        contextRunner.withUserConfiguration(MultipleUsersApplicationDataSourceConfiguration::class.java)
            .withPropertyValues("spring.migratedb.fail-on-missing-locations=true")
            .withPropertyValues("spring.migratedb.locations=$missingLocations").run { context ->
                assertThat(context).hasFailed()
                assertThat(context).failure.isInstanceOf(BeanCreationException::class.java)
                    .hasCauseInstanceOf(MigrateDbException::class.java)
            }
    }

    @ParameterizedTest
    @ValueSource(
        strings = ["classpath:db/changelog,classpath:db/migration", "db/changelog,db/migration", "filesystem:src/test/resources/db/migration"]
    )
    fun `Location validation does not fail if the locations exist`(existingLocations: String) {
        contextRunner.withUserConfiguration(MultipleUsersApplicationDataSourceConfiguration::class.java)
            .withPropertyValues("spring.migratedb.fail-on-missing-locations=true")
            .withPropertyValues("spring.migratedb.locations=$existingLocations")
            .run { context -> assertThat(context).hasNotFailed() }
    }

    @Test
    fun `Can customize the MigrateDB execution which happens automagically on application startup`() {
        contextRunner.withUserConfiguration(
            MultipleUsersApplicationDataSourceConfiguration::class.java, MockMigrateDbExecution::class.java
        ).run { context ->
            assertThat(context).singleBean<MigrateDb>()
            context.bean<MockMigrateDbExecution>().assertCalled()
        }
    }

    @Test
    fun `Java migration beans are auto-registered`() {
        contextRunner.withUserConfiguration(
            MultipleUsersApplicationDataSourceConfiguration::class.java, TwoJavaMigrationsConfiguration::class.java
        ).run { context ->
            assertThat(context).singleBean<MigrateDb>().all { actual ->
                actual.configuration.javaMigrations.shouldHaveSize(2)
                    .onEach { it.shouldBeInstanceOf<TestMigration>() }
            }
        }
    }

    @Test
    fun `Can provide a custom MigrateDbInitializer`() {
        contextRunner.withUserConfiguration(
            MultipleUsersApplicationDataSourceConfiguration::class.java,
            CustomMigrateDbInitializerWithHighestPrecedenceOrder::class.java
        ).run { context ->
            assertThat(context).singleBean<MigrateDb>()
            assertThat(context).singleBean<MigrateDbInitializer>().all { actual ->
                actual.order.shouldBeEqual(Ordered.HIGHEST_PRECEDENCE)
            }
        }
    }

    @Test
    fun `Integrates with JPA`() {
        contextRunner.withUserConfiguration(
            MultipleUsersApplicationDataSourceConfiguration::class.java,
            CustomMigrateDbWithJpaConfiguration::class.java
        ).run { context -> assertThat(context).hasNotFailed() }
    }

    @Test
    fun `Integrates with Spring JDBC`() {
        contextRunner.withUserConfiguration(
            MultipleUsersApplicationDataSourceConfiguration::class.java,
            CustomMigrateDbWithJdbcConfiguration::class.java
        ).run { context -> assertThat(context).hasNotFailed() }
    }

    @Test
    fun `Can use custom MigrateDbInitializer with JPA`() {
        contextRunner.withUserConfiguration(
            MultipleUsersApplicationDataSourceConfiguration::class.java,
            CustomMigrateDbMigrationInitializerWithJpaConfiguration::class.java
        ).run { context -> assertThat(context).hasNotFailed() }
    }

    @Test
    fun `Can use custom MigrateDbInitializer with Spring JDBC`() {
        contextRunner.withUserConfiguration(
            MultipleUsersApplicationDataSourceConfiguration::class.java,
            CustomMigrateDbMigrationInitializerWithJdbcConfiguration::class.java
        ).run { context -> assertThat(context).hasNotFailed() }
    }

    @ParameterizedTest
    @ValueSource(strings = ["0", "1", "5"])
    fun `Can change the baseline version via config properties`(baselineVersion: String) {
        contextRunner.withUserConfiguration(MultipleUsersApplicationDataSourceConfiguration::class.java)
            .withPropertyValues("spring.migratedb.baseline-version=$baselineVersion").run { context ->
                assertThat(context).singleBean<MigrateDb>().all { actual ->
                    actual.configuration.baselineVersion.shouldBeEqual(Version.parse(baselineVersion))
                }
            }
    }

    @Test
    fun `Spring beans implementing Callback are invoked in bean name order by default`() {
        contextRunner.withUserConfiguration(
            MultipleUsersApplicationDataSourceConfiguration::class.java, CallbackConfiguration::class.java
        ).run { context ->
            assertThat(context).singleBean<MigrateDb>().all { migrateDb ->
                val callback1 = context.getBean("callback1", Callback::class.java)
                val callback2 = context.getBean("callback2", Callback::class.java)
                migrateDb.configuration.callbacks.shouldContainExactly(callback1, callback2)

                val anyEvent = { ArgumentMatchers.any(Event::class.java) }
                val anyContext = { ArgumentMatchers.any(migratedb.v1.core.api.callback.Context::class.java) }

                Mockito.inOrder(callback1, callback2).apply {
                    verify(callback1).handle(anyEvent(), anyContext())
                    verify(callback2).handle(anyEvent(), anyContext())
                }
            }
        }
    }

    @Test
    fun `Beans implementing MigrateDbConfigurationCustomizer are invoked in order`() {
        contextRunner.withUserConfiguration(
            MultipleUsersApplicationDataSourceConfiguration::class.java,
            ConfigurationCustomizerConfiguration::class.java
        ).run { context ->
            assertThat(context).singleBean<MigrateDb>().all { actual ->
                actual.configuration.connectRetries.shouldBeEqual(5)
                actual.configuration.baselineDescription.shouldBeEqual("<< Custom baseline >>")
                actual.configuration.baselineVersion.shouldBeEqual(Version.parse("3"))
            }
        }
    }

    @Test
    fun `Uses the class loader provided by the Spring ResourceLoader`() {
        contextRunner.withUserConfiguration(
            MultipleUsersApplicationDataSourceConfiguration::class.java, ResourceLoaderConfiguration::class.java
        ).run { context ->
            assertThat(context).singleBean<MigrateDb>().all { actual ->
                actual.configuration.classLoader.shouldBeInstanceOf<CustomClassLoader>()
            }
        }
    }

    @Test
    fun `Can configure SQL to run on new MigrateDB connections`() {
        val initSql = "SELECT 1 FROM (VALUES(0)); SELECT 2 FROM (VALUES(0))"
        contextRunner.withUserConfiguration(MultipleUsersApplicationDataSourceConfiguration::class.java)
            .withPropertyValues("spring.migratedb.init-sql=$initSql").run { context ->
                assertThat(context).singleBean<MigrateDb>().extracting { it.configuration.initSql }.isEqualTo(initSql)
            }
    }

    @Test
    fun `CherryPick parameter is correctly mapped`() {
        contextRunner.withUserConfiguration(MultipleUsersApplicationDataSourceConfiguration::class.java)
            .withPropertyValues("spring.migratedb.cherry-pick=1.1").run { context ->
                assertThat(context).singleBean<MigrateDb>().all { actual ->
                    actual.configuration.cherryPick.shouldContainExactly(MigrationPattern("1.1"))
                }
            }
    }

    @Test
    fun `When MigrateDB is auto-configured then jOOQ DslContext depends on the MigrateDB initializer`() {
        contextRunner.withUserConfiguration(
            MultipleUsersApplicationDataSourceConfiguration::class.java, JooqConfiguration::class.java
        ).run { context ->
            val beanDefinition = context.beanFactory.getBeanDefinition("dslContext")
            assertThat(beanDefinition.dependsOn).contains("migrateDbInitializer")
        }
    }

    @Test
    fun `When a custom MigrateDB initializer is defined then jOOQ DslContext depends on it`() {
        contextRunner.withUserConfiguration(
            MultipleUsersApplicationDataSourceConfiguration::class.java,
            JooqConfiguration::class.java,
            CustomMigrateDbInitializerWithHighestPrecedenceOrder::class.java
        ).run { context ->
            val beanDefinition = context.beanFactory.getBeanDefinition("dslContext")
            assertThat(beanDefinition.dependsOn).contains("customMigrateDbInitializer")
        }
    }

    @Test
    fun `When a custom MigrateDB instance is defined then jOOQ DslContext depends on it`() {
        contextRunner.withUserConfiguration(
            MultipleUsersApplicationDataSourceConfiguration::class.java,
            JooqConfiguration::class.java,
            CustomMigrateDb::class.java
        ).run { context ->
            val beanDefinition = context.beanFactory.getBeanDefinition("dslContext")
            assertThat(beanDefinition.dependsOn).contains("migrateDbInitializer")
            val initializer = context.getBean("migrateDbInitializer", MigrateDbInitializer::class.java)
            assertThat(initializer.migrateDb).isSameAs(CustomMigrateDb.customMigrateDbInstance)
        }
    }

    @Test
    fun `ScriptPlaceholderPrefix parameter is correctly mapped`() {
        contextRunner.withUserConfiguration(MultipleUsersApplicationDataSourceConfiguration::class.java)
            .withPropertyValues("spring.migratedb.script-placeholder-prefix=SPP").run { context ->
                assertThat(context.bean<MigrateDb>().configuration.scriptPlaceholderPrefix).isEqualTo("SPP")
            }
    }

    @Test
    fun `ScriptPlaceholderSuffix parameter is correctly mapped`() {
        contextRunner.withUserConfiguration(MultipleUsersApplicationDataSourceConfiguration::class.java)
            .withPropertyValues("spring.migratedb.script-placeholder-suffix=SPS").run { context ->
                assertThat(context.bean<MigrateDb>().configuration.scriptPlaceholderSuffix).isEqualTo("SPS")
            }
    }

    @Configuration(proxyBeanMethods = false)
    internal class MultipleUsersApplicationDataSourceConfiguration {
        @Bean(destroyMethod = "shutdown")
        fun applicationDataSource() = DataSources.newApplicationDataSource(USER_1 to PASSWORD_1)

        companion object {
            const val USER_1 = "another_user"
            const val PASSWORD_1 = "some_secret"
        }
    }

    @Configuration(proxyBeanMethods = false)
    internal class ApplicationAndMigrationDataSourcesConfiguration {
        @Bean(destroyMethod = "shutdown")
        @Primary
        fun applicationDataSource() = DataSources.newApplicationDataSource()

        @MigrateDbDataSource
        @Bean(destroyMethod = "shutdown")
        fun migrateDbDataSource() = DataSources.newMigrationDataSource()
    }

    @Configuration(proxyBeanMethods = false)
    internal class MigrationDataSourceOnlyConfiguration {
        @MigrateDbDataSource
        @Bean(destroyMethod = "shutdown")
        fun migrateDbDataSource() = DataSources.newMigrationDataSource()
    }

    @Configuration(proxyBeanMethods = false)
    internal class MultipleDataSourcesAndSingleMigrationDataSourceConfiguration {
        @Bean(destroyMethod = "shutdown")
        fun firstDataSource() = DataSources.newApplicationDataSource()

        @Bean(destroyMethod = "shutdown")
        fun secondDataSource() = DataSources.newApplicationDataSource()

        @MigrateDbDataSource
        @Bean(destroyMethod = "shutdown")
        fun migrateDbDataSource() = DataSources.newMigrationDataSource()
    }

    @Configuration(proxyBeanMethods = false)
    internal class TwoJavaMigrationsConfiguration {
        @Bean
        fun migration1(): TestMigration {
            return TestMigration("2", "M1")
        }

        @Bean
        fun migration2(): TestMigration {
            return TestMigration("3", "M2")
        }
    }

    @Configuration(proxyBeanMethods = false)
    internal class ResourceLoaderConfiguration {
        @Bean
        @Primary
        fun customClassLoader(): ResourceLoader {
            return DefaultResourceLoader(CustomClassLoader(javaClass.classLoader))
        }
    }

    @Configuration(proxyBeanMethods = false)
    internal class CustomMigrateDbInitializerWithHighestPrecedenceOrder {
        @Bean
        fun customMigrateDbInitializer(migrateDb: MigrateDb): MigrateDbInitializer {
            val initializer = MigrateDbInitializer(migrateDb, DefaultMigrateDbExecution())
            initializer.order = Ordered.HIGHEST_PRECEDENCE
            return initializer
        }
    }

    @Configuration(proxyBeanMethods = false)
    internal class CustomMigrateDb {
        @Bean
        fun customMigrateDb(): MigrateDb {
            return customMigrateDbInstance
        }

        @Bean
        fun doNothingOnMigrate(): MigrateDbExecution {
            return MigrateDbExecution { }
        }

        companion object {
            val customMigrateDbInstance: MigrateDb =
                MigrateDb.configure().dataSource { throw UnsupportedOperationException("Not implemented") }.load()
        }
    }

    @Configuration(proxyBeanMethods = false)
    internal class CustomMigrateDbMigrationInitializerWithJpaConfiguration {
        @Bean
        fun customMigrateDbMigrationInitializer(migrateDb: MigrateDb): MigrateDbInitializer {
            return MigrateDbInitializer(migrateDb, DefaultMigrateDbExecution())
        }

        @Bean
        fun entityManagerFactoryBean(dataSource: DataSource): LocalContainerEntityManagerFactoryBean {
            val properties: MutableMap<String, Any?> = HashMap()
            properties["configured"] = "manually"
            properties["hibernate.transaction.jta.platform"] = NoJtaPlatform.INSTANCE
            return EntityManagerFactoryBuilder(HibernateJpaVendorAdapter(), properties, null).dataSource(dataSource)
                .build()
        }
    }

    @Configuration(proxyBeanMethods = false)
    internal class CustomMigrateDbWithJpaConfiguration(private val dataSource: DataSource) {
        @Bean
        fun customMigrateDb(): MigrateDb {
            return MigrateDb.configure().dataSource(dataSource).load()
        }

        @Bean
        fun entityManagerFactoryBean(): LocalContainerEntityManagerFactoryBean {
            val properties: MutableMap<String, Any?> = HashMap()
            properties["configured"] = "manually"
            properties["hibernate.transaction.jta.platform"] = NoJtaPlatform.INSTANCE
            return EntityManagerFactoryBuilder(HibernateJpaVendorAdapter(), properties, null).dataSource(dataSource)
                .build()
        }
    }

    @Configuration
    internal class CustomMigrateDbWithJdbcConfiguration(private val dataSource: DataSource) {
        @Bean
        fun customMigrateDb(): MigrateDb {
            return MigrateDb.configure().dataSource(dataSource).load()
        }

        @Bean
        fun jdbcOperations(): JdbcOperations {
            return JdbcTemplate(dataSource)
        }

        @Bean
        fun namedParameterJdbcOperations(): NamedParameterJdbcOperations {
            return NamedParameterJdbcTemplate(dataSource)
        }
    }

    @Configuration
    internal class CustomMigrateDbMigrationInitializerWithJdbcConfiguration(private val dataSource: DataSource) {
        @Bean
        fun customMigrateDbMigrationInitializer(migrateDb: MigrateDb): MigrateDbInitializer {
            return MigrateDbInitializer(migrateDb, DefaultMigrateDbExecution())
        }

        @Bean
        fun jdbcOperations(): JdbcOperations {
            return JdbcTemplate(dataSource)
        }

        @Bean
        fun namedParameterJdbcOperations(): NamedParameterJdbcOperations {
            return NamedParameterJdbcTemplate(dataSource)
        }
    }

    @Component
    internal class MockMigrateDbExecution : MigrateDbExecution {
        private var called = false
        override fun run(migrateDb: MigrateDb) {
            called = true
        }

        fun assertCalled() {
            assertThat(called).isTrue()
        }
    }

    @Configuration(proxyBeanMethods = false)
    internal class CallbackConfiguration {
        @Bean
        fun callback1(): Callback {
            return mockCallback("callbackOne")
        }

        @Bean
        fun callback2(): Callback {
            return mockCallback("callbackTwo")
        }

        private fun mockCallback(name: String): Callback {
            val callback = Mockito.mock(Callback::class.java)
            given(
                callback.supports(
                    ArgumentMatchers.any(Event::class.java),
                    ArgumentMatchers.any(migratedb.v1.core.api.callback.Context::class.java)
                )
            ).willReturn(true)
            given(callback.callbackName).willReturn(name)
            return callback
        }
    }

    @Configuration(proxyBeanMethods = false)
    internal class ConfigurationCustomizerConfiguration {
        @Bean
        @Order(0)
        fun customizer1(): MigrateDbConfigurationCustomizer {
            return MigrateDbConfigurationCustomizer {
                it.connectRetries = 10
                it.baselineDescription = "<< Custom baseline >>"
            }
        }

        @Bean
        @Order(1)
        fun customizer2(): MigrateDbConfigurationCustomizer {
            return MigrateDbConfigurationCustomizer {
                it.connectRetries = 5
                it.baselineVersion = Version.parse("3")
            }
        }
    }

    @Configuration(proxyBeanMethods = false)
    internal class JooqConfiguration {
        @Bean
        fun dslContext(): DSLContext {
            return DefaultDSLContext(SQLDialect.HSQLDB)
        }
    }

    private class CustomClassLoader(parent: ClassLoader) : ClassLoader(parent)

    internal class TestMigration(version: String, private val description: String) : JavaMigration {
        private val version: Version = Version.parse(version)

        override fun getVersion() = version

        override fun getDescription() = description

        override fun getChecksum(configuration: migratedb.v1.core.api.configuration.Configuration): Checksum {
            return Checksum.builder().addNumber(1L).build()
        }

        override fun canExecuteInTransaction() = true

        override fun migrate(context: Context) {}

        override fun isBaselineMigration() = false
    }

    private inline fun <reified T : Any> ApplicationContext.bean(): T = getBean(T::class.java)
    private inline fun <reified T : Any> ApplicationContextAssert<*>.doesNotHaveBean() = doesNotHaveBean(T::class.java)
    private inline fun <reified T : Any> ApplicationContextAssert<*>.singleBean() =
        hasSingleBean(T::class.java).getBean(T::class.java)

    private fun AbstractObjectAssert<*, MigrateDb>.isUsingDataSourceWithUser(expectedUser: String) =
        extracting { actual ->
            actual.evaluateUsingDataSource("select current_user from (values(0))")?.lowercase()
        }.isNotNull().isEqualTo(expectedUser.lowercase())

    private fun <T : Any> AbstractObjectAssert<*, T>.all(assertions: (actual: T) -> Unit) = satisfies({
        assertSoftly {
            assertions(it)
        }
    })

    /**
     * Asserts that MigrateDB uses a data source created by [DataSources.newMigrationDataSource] or by [DataSources.newMigrationDataSourceUrl].
     */
    private fun AbstractObjectAssert<*, MigrateDb>.isUsingMigrationDataSource() = extracting { actual ->
        try {
            actual.evaluateUsingDataSource("select 1 from $MIGRATION_DS_MARKER_SCHEMA.$EMPTY_TABLE_INSIDE_MARKER_SCHEMA")
            true
        } catch (e: SQLException) {
            false
        }
    }.isEqualTo(true)

    internal object DataSources {
        private val counter = AtomicLong()
        const val MIGRATION_DS_MARKER_SCHEMA = "this_is_a_migration_data_source"
        const val EMPTY_TABLE_INSIDE_MARKER_SCHEMA = "empty_table"

        /**
         * @return The first column of the first result row cast to String, null if no results.
         */
        fun MigrateDb.evaluateUsingDataSource(sql: String): String? {
            return configuration.dataSource?.connection?.use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery(sql).use { resultSet ->
                        if (resultSet.next()) resultSet.getString(1) else return null
                    }
                }
            } ?: fail { "No data source was configured" }
        }

        fun newMigrationDataSourceUrl() =
            "jdbc:h2:mem:migration_ds_${counter.incrementAndGet()}" + ";INIT=create schema if not exists $MIGRATION_DS_MARKER_SCHEMA\\;" + "create table if not exists $MIGRATION_DS_MARKER_SCHEMA.$EMPTY_TABLE_INSIDE_MARKER_SCHEMA (id int primary key)"

        fun newMigrationDataSource(): EmbeddedDatabase {
            return EmbeddedDatabaseFactory().apply {
                setDatabaseName("migration_ds_${counter.incrementAndGet()}")
                setDatabaseType(EmbeddedDatabaseType.HSQL)
                setDatabasePopulator(
                    executeStatements(
                        "create schema $MIGRATION_DS_MARKER_SCHEMA",
                        "set schema $MIGRATION_DS_MARKER_SCHEMA",
                        "create table $EMPTY_TABLE_INSIDE_MARKER_SCHEMA (id int primary key)"
                    )
                )
            }.database
        }

        fun newApplicationDataSource(vararg additionalUsers: Pair<String, String>): EmbeddedDatabase {
            return EmbeddedDatabaseFactory().apply {
                setDatabaseName("application_ds_${counter.incrementAndGet()}")
                setDatabaseType(EmbeddedDatabaseType.HSQL)
                setDatabasePopulator(executeStatements(additionalUsers.flatMap { (user, password) ->
                    assertThat(user).doesNotContain("\"")
                    assertThat(password).doesNotContain("'")
                    listOf(
                        "create user \"$user\" password '$password'", "grant DBA to \"$user\""
                    )
                }))
            }.database
        }

        private fun executeStatements(vararg statements: String) = executeStatements(statements.toList())

        private fun executeStatements(statements: List<String>) = DatabasePopulator { connection ->
            connection.autoCommit = true
            connection.createStatement().use {
                statements.forEach(it::execute)
            }
        }
    }
}

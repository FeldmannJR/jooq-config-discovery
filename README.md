# JOOQ Config Discovery

A simple maven plugin to discover jOOQ generators inside the classpath. 


## How to use
The plugin will look for classes extending the `dev.feldmann.jooqConfigDiscovery.jooq.JOOQGenerator` class with the `dev.feldmann.jooqConfigDiscovery.jooq.EnableJOOQGenerator` annotation  
### Example
```java
@EnableJOOQGenerator
public class JOOQGenerator extends JOOQGenerator {
    @Override
    public Generator getGenerator() {
        return new Generator().withDatabase(
                createDatabase()
                        .withOutputCatalogToDefault(true)
                        .withOutputSchemaToDefault(true)
                        .withIncludes("users")

        ).withTarget(createTarget());
    }

    @Override
    public String getSchema() {
        return "constellation_common";
    }
    @Override
    protected String getDatabaseURL(String s) {
        return "jdbc:mysql://localhost:3306/";
    }

    @Override
    protected String getDatabaseUser() {
        return "root";
    }

    @Override
    protected String getDatabasePassword() {
        return "123";
    }
}
```

To scan for configurations just run the plugin like this
`mvn dev.feldmann:jooq-config-discovery-maven-plugin:generate
`

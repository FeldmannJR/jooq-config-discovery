package dev.feldmann.jooqConfigDiscovery.jooq;

import org.jooq.codegen.GenerationTool;
import org.jooq.meta.jaxb.*;
import org.jooq.meta.mariadb.MariaDBDatabase;

/**
 * This is not supposed to run in runtime
 * Its probably best to create a maven plugin for generate based on this config pattern
 */
public abstract class JOOQGenerator {

    protected abstract String getDatabaseURL(String schema);

    protected abstract String getDatabaseUser();

    protected abstract String getDatabasePassword();

    public abstract Generator getGenerator();

    public abstract String getSchema();

    protected String getDatabaseName() {
        return MariaDBDatabase.class.getName();
    }

    protected String getTargetPackage() {
        return getClass().getPackage().getName() + ".jooq";
    }

    private Configuration createConfig() {

        Configuration config = new Configuration()
                .withJdbc(new Jdbc()
                        .withUrl(getDatabaseURL(getSchema()))
                        .withUser(getDatabaseUser())
                        .withPassword(getDatabasePassword())
                );
        config = config.withGenerator(getGenerator());
        return config;
    }

    protected Database createDatabase() {
        return new Database()
                .withName(getDatabaseName())
                .withInputSchema(getSchema());
    }


    protected Target createTarget() {
        String packageName = getTargetPackage();
        return new Target()
                .withDirectory("src/main/java/")
                .withPackageName(packageName);
    }


    public Configuration run() throws Exception {
        Configuration config = createConfig();
        GenerationTool.generate(config);
        return config;
    }

}

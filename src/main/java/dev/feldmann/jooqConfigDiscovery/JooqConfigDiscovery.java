package dev.feldmann.jooqConfigDiscovery;

import dev.feldmann.jooqConfigDiscovery.jooq.EnableJOOQGenerator;
import dev.feldmann.jooqConfigDiscovery.jooq.JOOQGenerator;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.jooq.meta.jaxb.Configuration;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import static java.net.URLClassLoader.newInstance;

@Mojo(name = "generate", requiresDependencyResolution = ResolutionScope.COMPILE)
public class JooqConfigDiscovery extends AbstractMojo {

    /**
     * project
     *
     * @parameter expression = "${project}";
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject mavenProject;


    public void execute() throws MojoExecutionException {
        try {
            URLClassLoader classloader = configureContextClassLoader();
            // Set the class loader of the current thread to the created one
            Thread.currentThread().setContextClassLoader(classloader);
            // Discovery generators in classpath
            List<JOOQGenerator> generators = generators(classloader);
            for (JOOQGenerator generator : generators) {
                try {
                    getLog().info("Running generator " + generator.getClass().getName());
                    Configuration config = generator.run();
                    String directory = config.getGenerator().getTarget().getDirectory();
                    String packageName = config.getGenerator().getTarget().getPackageName();
                    getLog().info("Generator generate classes in " + directory + " directory to package " + packageName);
                } catch (Exception e) {
                    throw new MojoExecutionException("Failed to run generator " + generator.getClass().getName(), e);
                }
            }
        } catch (DependencyResolutionRequiredException | MalformedURLException e) {
            getLog().error(e);
            return;
        }

    }

    private List<JOOQGenerator> generators(URLClassLoader classloader) throws MojoExecutionException {
        List<JOOQGenerator> list = new ArrayList<>();
        Reflections reflect = new Reflections(
                new ConfigurationBuilder()
                        .addClassLoader(classloader)
                        .addUrls(classloader.getURLs())
                        .setScanners(
                                new SubTypesScanner(false),
                                new TypeAnnotationsScanner()
                        )

        );
        getLog().info("Scanning for classes with EnableJOOQGenerator Annotation");
        // Search for classes with the annotation
        Set<Class<?>> classes = reflect.getTypesAnnotatedWith(EnableJOOQGenerator.class);
        if (!classes.isEmpty()) {
            for (Class<?> aClass : classes) {
                if (Modifier.isAbstract(aClass.getModifiers())) {
                    getLog().info(aClass.getName() + " is abstract");
                    continue;
                }
                getLog().info("Found " + aClass.getName() + "");
                // If the class implements IJOOQGenerator add to the list of loaders
                if (JOOQGenerator.class.isAssignableFrom(aClass)) {
                    try {
                        Class<?> claz = classloader.loadClass(aClass.getName());
                        Constructor<?> cons = claz.getConstructor();
                        Object obj = cons.newInstance();
                        list.add((JOOQGenerator) obj);
                    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                        getLog().info(aClass.getName() + " doesn't have a non argument constructor!");
                        continue;
                    }
                }
            }
        } else {
            getLog().info("Not found any annotated class");
        }

        return list;
    }

    private URLClassLoader configureContextClassLoader() throws DependencyResolutionRequiredException, MalformedURLException {
        // Configure a classpath loader with all the dependencies of the project
        List<URL> urls = new ArrayList<>();
        List<String> elements = mavenProject.getRuntimeClasspathElements();
        for (String element : elements) {
            getLog().info("Found " + element);
            urls.add(new File(element).toURI().toURL());
        }
        return new URLClassLoader(urls.toArray(new URL[0]), JooqConfigDiscovery.class.getClassLoader());
    }
}
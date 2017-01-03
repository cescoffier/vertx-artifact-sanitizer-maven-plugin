package me.escoffier.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.Properties;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
@Mojo(name = "build-with-sanitized-pom")
public class BuildMojo extends AbstractSanitizerMojo {

    @Parameter(property = "sanitizer.phase")
    private String phase;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (phase == null) {
            phase = "install";
        }

        if (!pom.exists()) {
            throw new MojoExecutionException("Unable to find the given pom file: " + pom.getAbsolutePath());
        }

        Properties props = new Properties();
        props.put("skipTests", "true");

        new MavenBuildExecutor()
            .setContainer(container)
            .setExecutor(lifecycleExecutor)
            .setLog(getLog())
            .setProjectBuilder(projectBuilder)
            .setRepoSession(repoSession)
            .setSession(session)
            .execute(pom, phase, props);
    }
}

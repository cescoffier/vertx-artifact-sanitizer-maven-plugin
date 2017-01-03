package me.escoffier.maven;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.eclipse.aether.RepositorySystemSession;

import java.io.File;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public abstract class AbstractSanitizerMojo extends AbstractMojo implements Contextualizable {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    MavenSession session;

    /**
     * The session to access the repository system.
     */
    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    RepositorySystemSession repoSession;

    PlexusContainer container;

    @Component
    ProjectBuilder projectBuilder;
    
    /**
     * The component used to execute the second Maven execution.
     */
    @Component
    LifecycleExecutor lifecycleExecutor;


    @Parameter(property = "sanitizer.pom", defaultValue = "pom.xml.sanitized")
    File pom;
    
    /**
     * Retrieves the Plexus container.
     *
     * @param context the context
     * @throws ContextException if the container cannot be retrieved.
     */
    @Override
    public void contextualize(Context context) throws ContextException {
        container = (PlexusContainer) context.get(PlexusConstants.PLEXUS_KEY);
    }
}

package me.escoffier.maven;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.*;
import org.codehaus.plexus.PlexusContainer;
import org.eclipse.aether.RepositorySystemSession;

import java.io.File;
import java.util.*;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class MavenBuildExecutor {


    private MavenSession session;
    private PlexusContainer container;
    private LifecycleExecutor executor;
    private RepositorySystemSession repoSession;
    private Log log;
    private ProjectBuilder projectBuilder;

    public MavenBuildExecutor setSession(MavenSession session) {
        this.session = session;
        return this;
    }

    public MavenBuildExecutor setContainer(PlexusContainer container) {
        this.container = container;
        return this;
    }

    public MavenBuildExecutor setExecutor(LifecycleExecutor executor) {
        this.executor = executor;
        return this;
    }

    public MavenBuildExecutor setLog(Log log) {
        this.log = log;
        return this;
    }

    public MavenBuildExecutor setRepoSession(RepositorySystemSession repoSession) {
        this.repoSession = repoSession;
        return this;
    }

    public MavenBuildExecutor setProjectBuilder(ProjectBuilder projectBuilder) {
        this.projectBuilder = projectBuilder;
        return this;
    }

    public void execute(File pom, String phase, Properties properties)
        throws MojoExecutionException {
        if (!pom.isFile()) {
            throw new MojoExecutionException("Unable to find the pom file: " + pom.getAbsolutePath());
        }

        MavenProject project = null;
        try {
            ProjectBuildingResult result = loadMavenProject(pom, properties, session);
            project = result.getProject();
        } catch (ProjectBuildingException exception) {
            log.error("Error(s) detected in the pom file: " + exception.getMessage());
            throw new MojoExecutionException("Invalid pom file, check log", exception);
        }

        MavenExecutionRequest execRequest = getMavenExecutionRequest(phase);
        MavenSession newSession = getMavenSession(project, execRequest);

        executor.execute(newSession);
    }

    private ProjectBuildingResult loadMavenProject(File pom, Properties sys, MavenSession session) throws
        ProjectBuildingException {

        Properties properties = session.getUserProperties();
        if (properties == null) {
            properties = new Properties();
        }
        if (sys != null) {
            properties.putAll(sys);
        }

        DefaultProjectBuildingRequest request = new DefaultProjectBuildingRequest();
        request.setRepositorySession(repoSession);
        request.setUserProperties(properties);
        request.setSystemProperties(session.getSystemProperties());
        request.setProfiles(session.getRequest().getProfiles());
        request.setActiveProfileIds(session.getRequest().getActiveProfiles());
        request.setRemoteRepositories(session.getRequest().getRemoteRepositories());
        request.setBuildStartTime(session.getRequest().getStartTime());
        request.setInactiveProfileIds(session.getRequest().getInactiveProfiles());
        request.setPluginArtifactRepositories(session.getRequest().getPluginArtifactRepositories());
        request.setLocalRepository(session.getRequest().getLocalRepository());

        return projectBuilder.build(pom, request);
    }

    private MavenSession getMavenSession(final MavenProject project, MavenExecutionRequest request) {
        MavenSession newSession = new MavenSession(container,
            session.getRepositorySession(),
            request,
            session.getResult());
        newSession.setAllProjects(session.getAllProjects());
        newSession.setCurrentProject(project);
        newSession.setParallel(session.isParallel());
        // Update project map to update the current project
        Map<String, MavenProject> projectMaps = new LinkedHashMap<>(session.getProjectMap());
        projectMaps.put(ArtifactUtils.key(project.getGroupId(), project.getArtifactId(),
            project.getVersion()), project);
        newSession.setProjectMap(projectMaps);

        List<MavenProject> list = new ArrayList<>();
        list.add(project);
        /**
         * Fake implementation of the project dependency graph, as we don't support reactor.
         */
        ProjectDependencyGraph graph = new ProjectDependencyGraph() {

            @Override
            public List<MavenProject> getSortedProjects() {
                return list;
            }

            @Override
            public List<MavenProject> getDownstreamProjects(MavenProject project, boolean transitive) {
                return Collections.emptyList();
            }

            @Override
            public List<MavenProject> getUpstreamProjects(MavenProject project, boolean transitive) {
                return Collections.emptyList();
            }
        };
        newSession.setProjectDependencyGraph(graph);
        newSession.setProjects(list);
        return newSession;
    }


    private MavenExecutionRequest getMavenExecutionRequest(String phase) {
        MavenExecutionRequest request = DefaultMavenExecutionRequest.copy(session.getRequest());
        request.setStartTime(session.getStartTime());
        request.setExecutionListener(null);
        List<String> goals = new ArrayList<>();
        goals.add(phase);
        request.setGoals(goals);
        return request;
    }
}

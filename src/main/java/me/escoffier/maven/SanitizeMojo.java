package me.escoffier.maven;

import com.google.common.base.Strings;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.maven.model.*;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingResult;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
@Mojo(name = "generate-sanitized-pom")
public class SanitizeMojo extends AbstractSanitizerMojo {

    @Parameter(property = "sanitizer.module")
    private String module;

    @Parameter(property = "sanitizer.config")
    private File config;
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Configuration configuration = createConfiguration();
        File modifiedPomFile = phaseA(configuration);
        Model model = loadPom(modifiedPomFile);

        if (Strings.isNullOrEmpty(project.getOriginalModel().getName())) {
            model.setName(null);
        }
        if (Strings.isNullOrEmpty(project.getOriginalModel().getDescription())) {
           model.setDescription(null);
        }
        if (Strings.isNullOrEmpty(project.getOriginalModel().getUrl())) {
           model.setUrl(null);
        }

        File out = phaseB(model, configuration);
        phaseC(out);

        getLog().info(out.getAbsolutePath() + " has been generated");
    }

    private void phaseC(File out) throws MojoExecutionException {
        try {
            String content = FileUtils.readFileToString(out, StandardCharsets.UTF_8);
            String path = project.getBasedir().getAbsolutePath();
            content = content.replace(path, "${project.basedir}");
            FileUtils.write(out, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to update path in pom file", e);
        }
    }

    private File phaseB(Model model, Configuration configuration) throws MojoExecutionException {
        getLog().info("Starting transformation - Phase B");

        // We are working on the effective pom, so we can delete the dependency management section and parent
        getLog().info("Removing parent - working on effective pom");
        model.setParent(null);
        getLog().info("Removing dependency management - working on effective pom");
        model.setDependencyManagement(null);

        fixDependencies(configuration, model);

        getLog().info("Excluding resources");
        excludeResources(configuration.getModuleName(), model, configuration.getExcludedResources());

        getLog().info("Removing excluded plugins");
        removePlugins(model, configuration.getExcludedPlugins());

        for (Profile profile : model.getProfiles()) {
            if (configuration.getProfiles().contains(profile.getId())) {
                getLog().info("Activating profile " + profile.getId());
                Activation activation = profile.getActivation();
                if (activation == null) {
                    activation = new Activation();
                    profile.setActivation(activation);
                }
                profile.getActivation().setActiveByDefault(true);
            }
        }

        return write(pom, model);
    }

    private Model loadPom(File file) throws MojoExecutionException {
        DefaultProjectBuildingRequest request = new DefaultProjectBuildingRequest();
        request.setRepositorySession(repoSession);
        request.setUserProperties(session.getUserProperties());
        request.setSystemProperties(session.getSystemProperties());
        request.setProfiles(session.getRequest().getProfiles());
        request.setActiveProfileIds(session.getRequest().getActiveProfiles());
        request.setRemoteRepositories(session.getRequest().getRemoteRepositories());
        request.setBuildStartTime(session.getRequest().getStartTime());
        request.setInactiveProfileIds(session.getRequest().getInactiveProfiles());
        request.setPluginArtifactRepositories(session.getRequest().getPluginArtifactRepositories());
        request.setLocalRepository(session.getRequest().getLocalRepository());

        try {
            ProjectBuildingResult result = projectBuilder.build(file, request);
            return result.getProject().getModel();
        } catch (ProjectBuildingException e) {
            throw new MojoExecutionException("Unable to read the modified pom file", e);
        }
    }

    private File phaseA(Configuration configuration) throws MojoExecutionException {
        getLog().info("Starting transformation - Phase A");
        Model model = project.getOriginalModel();

        getLog().info("Setting coordinates");
        model.setGroupId(configuration.getGroupId());
        model.setArtifactId(configuration.getArtifactId());
        model.setVersion(configuration.getVersion());

        if (model.getParent() != null && configuration.getParentVersion() != null) {
            model.getParent().setVersion(configuration.getParentVersion());
        }

        fixDependencies(configuration, model);

        File out = new File(project.getBasedir(), "pom.xml.modified");
        return write(out, model);
    }

    private void fixDependencies(Configuration configuration, Model model) throws MojoExecutionException {
        getLog().info("Removing excluded dependencies");
        List<Exclusion> excludedDependencies = configuration.getExcludedDependencies().stream().map(s -> {
            String[] seg = s.split(":");
            Exclusion dependency = new Exclusion();
            dependency.setGroupId(seg[0]);
            dependency.setArtifactId(seg[1]);
            return dependency;
        }).collect(Collectors.toList());
        cleanupDependencies(model, excludedDependencies);
        excludeDependencies(model, excludedDependencies);
    }

    private Configuration createConfiguration() throws MojoExecutionException {
        Configuration configuration = loadConfiguration();
        if (module != null) {
            configuration.setModuleName(module);
        } else if (configuration.getModuleName() == null) {
            configuration.setModuleName(ModuleUtils.findModuleName(getLog(), project));
        }
        getLog().info("Set module name to " + configuration.getModuleName());
        return configuration;
    }

    private Configuration loadConfiguration() throws MojoExecutionException {
        Yaml yaml = new Yaml();
        String content;

        if (config == null) {
            config = new File(project.getBasedir(), "sanitizer-config.yml");
        }

        if (config.isFile()) {
            try {
                content = FileUtils.readFileToString(config, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to read the configuration file", e);
            }
        } else {
            getLog().info("Using default configuration...");
            URL url = this.getClass().getClassLoader().getResource("default-configuration.yml");
            if (url == null) {
                throw new MojoExecutionException("Unable to find the default configuration file");
            }

            try {
                content = IOUtils.toString(url, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to read the configuration file", e);
            }
        }

        Map data = (Map) yaml.load(content);

        List<String> deps = (List<String>) data.get("excluded-dependencies");
        List<String> res = (List<String>) data.get("excluded-resources");
        List<String> plugins = (List<String>) data.get("excluded-plugins");
        List<String> profiles = (List<String>) data.get("profiles");
        String module = (String) data.get("module");
        String groupId = (String) data.get("groupId");
        String artifactId = (String) data.get("artifactId");
        String version = (String) data.get("version");
        String parentVersion = (String) data.get("parent-version");

        if (groupId == null) {
            groupId = project.getGroupId();
        }

        if (artifactId == null) {
            artifactId = project.getArtifactId();
        }

        if (version == null) {
            version = project.getVersion();
        } else {
            version = version.replace("${version}", project.getVersion());
        }

        return new Configuration().setModuleName(module)
            .setExcludedDependencies(new LinkedHashSet<>(deps))
            .setExcludedPlugins(new LinkedHashSet<>(plugins))
            .setExcludedResources(new LinkedHashSet<>(res))
            .setProfiles(new LinkedHashSet<>(profiles))
            .setGroupId(groupId)
            .setArtifactId(artifactId)
            .setVersion(version)
            .setParentVersion(parentVersion);
    }

    private void removePlugins(Model model, Set<String> pluginsToRemove) {
        Build build = model.getBuild();
        if (build != null) {
            for (String artifactId : pluginsToRemove) {
                Plugin plugin = getPlugin(artifactId, build.getPlugins());
                if (plugin != null) {
                    build.removePlugin(plugin);
                }
            }
        }
    }

    private Plugin getPlugin(String artifact, List<Plugin> plugins) {
        if (plugins == null) {
            return null;
        }

        for (Plugin plugin : plugins) {
            if (plugin.getArtifactId().equals(artifact)) {
                return plugin;
            }
        }
        return null;
    }

    private void excludeResources(String module, Model model, Set<String> resourcesToExclude) {
        List<String> exclusions = resourcesToExclude.stream()
            .map(s -> s.replace("${module}", module))
            .collect(Collectors.toList());
        Build build = model.getBuild();
        if (build != null) {
            List<Resource> resources = build.getResources();
            if (resources != null) {
                for (Resource resource : resources) {
                    resource.setExcludes(exclusions);
                }
            }
        }
    }

    private void excludeDependencies(Model model, List<Exclusion> depsToExclude) throws MojoExecutionException {
        for (Dependency dependency : model.getDependencies()) {
            if (dependency.getScope() == null || dependency.getScope().equalsIgnoreCase("compile")) {
                for (Exclusion ex : depsToExclude) {
                    dependency.addExclusion(ex);
                }
            }
        }
    }

    private void cleanupDependencies(Model model, List<Exclusion> excluded) {
        List<String> names = excluded.stream().map(Exclusion::getArtifactId).collect(Collectors.toList());
        getLog().info("Old set of dependency: " + model.getDependencies().size());
        List<Dependency> dependencies = model.getDependencies().stream()
            .filter(dependency -> !names.contains(dependency.getArtifactId()))
            .collect(Collectors.toList());

        model.setDependencies(dependencies);
        getLog().info("New set of dependency: " + model.getDependencies().size());
    }

    private File write(File pomFile, Model model) throws MojoExecutionException {
        try {
            MavenXpp3Writer xpp3Writer = new MavenXpp3Writer();
            final FileWriter pomFileWriter = new FileWriter(pomFile);
            xpp3Writer.write(pomFileWriter, model);

            pomFileWriter.flush();
            pomFileWriter.close();
            return pomFile;
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot write the new pom.xml file", e);
        }
    }

    private boolean isDirEmpty(File dir) {
        return FileUtils.listFiles(dir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE).isEmpty();
    }
}

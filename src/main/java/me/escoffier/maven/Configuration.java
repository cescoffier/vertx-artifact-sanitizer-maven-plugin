package me.escoffier.maven;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class Configuration {

    private Set<String> excludedDependencies = new HashSet<>();

    private Set<String> excludedPlugins = new HashSet<>();

    private Set<String> profiles = new HashSet<>();

    private Set<String> excludedResources = new HashSet<>();

    private String moduleName;

    private String artifactId;

    private String groupId;

    private String version;

    private String parentVersion;

    public Configuration addExcludedDependency(String name) {
        excludedDependencies.add(name);
        return this;
    }

    public Configuration addExcludedResource(String name) {
        excludedResources.add(name);
        return this;
    }

    public Configuration addExcludedPlugin(String name) {
        excludedPlugins.add(name);
        return this;
    }

    public Configuration addProfile(String name) {
        profiles.add(name);
        return this;
    }


    public Set<String> getExcludedDependencies() {
        return excludedDependencies;
    }

    public Configuration setExcludedDependencies(Set<String> excludedDependencies) {
        this.excludedDependencies = excludedDependencies;
        return this;
    }

    public Set<String> getExcludedPlugins() {
        return excludedPlugins;
    }

    public Configuration setExcludedPlugins(Set<String> excludedPlugins) {
        this.excludedPlugins = excludedPlugins;
        return this;
    }

    public Set<String> getProfiles() {
        return profiles;
    }

    public Configuration setProfiles(Set<String> profiles) {
        this.profiles = profiles;
        return this;
    }

    public Set<String> getExcludedResources() {
        return excludedResources;
    }

    public Configuration setExcludedResources(Set<String> excludedResources) {
        this.excludedResources = excludedResources;
        return this;
    }

    public String getModuleName() {
        return moduleName;
    }

    public Configuration setModuleName(String moduleName) {
        this.moduleName = moduleName;
        return this;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public Configuration setArtifactId(String artifactId) {
        this.artifactId = artifactId;
        return this;
    }

    public String getGroupId() {
        return groupId;
    }

    public Configuration setGroupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public Configuration setVersion(String version) {
        this.version = version;
        return this;
    }

    public String getParentVersion() {
        return parentVersion;
    }

    public Configuration setParentVersion(String parentVersion) {
        this.parentVersion = parentVersion;
        return this;
    }
}

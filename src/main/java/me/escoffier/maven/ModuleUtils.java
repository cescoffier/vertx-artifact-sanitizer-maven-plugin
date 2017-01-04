package me.escoffier.maven;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class ModuleUtils {

    private static final Pattern PATTERN = Pattern.compile(".*name\\s*=\\s*\"([a-zA-Z0-9\\-]*)\".*");
    
    static String findModuleName(Log log, MavenProject project) throws MojoExecutionException {
        try {
            String name = extractModuleNameFromPackageInfo(log, project);
            if (name != null) return name;
            log.warn("Cannot extract module name from the package-info files, using directory name recognition");

            name = extractModuleNameFromDirectoryPattern(log, project);
            if (name != null) return name;
            
            log.warn("Unable to detect the module name from the resource directories, using artifact id");
            return project.getArtifactId();

        } catch (IOException e) {
            throw new MojoExecutionException("Cannot extract the module name", e);
        }
    }

    private static String extractModuleNameFromDirectoryPattern(Log log, MavenProject project) {
        String name;
        File res = new File(project.getBasedir(), "src/main/resources");
        if (! res.isDirectory()) {
            return null;
        }
        Collection<File> dirs = FileUtils.listFilesAndDirs(res, FalseFileFilter.INSTANCE,
            new WildcardFileFilter("*-js"));
        if (!dirs.isEmpty()) {
            for (File file : dirs) {
                if (file.isDirectory()) {
                    name = file.getName().substring(0, file.getName().length() - 3);
                    File maybe = new File(res, name);
                    if (maybe.isDirectory()) {
                        log.info("Module name found using directory name recognition: " + name);
                        return name;
                    }
                }
            }
        }
        return null;
    }

    private static String extractModuleNameFromPackageInfo(Log log, MavenProject project) throws IOException {
        Collection<File> files = FileUtils.listFiles(new File(project.getBasedir(), "src/main/java"),
            new NameFileFilter("package-info.java"), TrueFileFilter.INSTANCE);
        if (files.isEmpty()) {
            log.debug("No package-info.java file");
        }
        for (File file : files) {
            log.debug("Reading " + file.getAbsolutePath());
            List<String> lines = FileUtils.readLines(file, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line.contains("@ModuleGen")  || line.contains("@io.vertx.codegen.annotations.ModuleGen")) {
                    log.debug("Line with @ModuleGen found: " + line);
                    Matcher matcher = PATTERN.matcher(line);
                    if (matcher.matches()) {
                        log.info("Module name extracted from " + file.getAbsolutePath()
                            + ": " + matcher.group(1));
                        return matcher.group(1);
                    }
                }
            }
        }
        return null;
    }
}

package com.aaron1011.crashreproducer;

import com.aaron1011.crashreproducer.detector.ErrorStringFinder;
import com.aaron1011.crashreproducer.detector.SimpleStringFinder;
import com.aaron1011.crashreproducer.detector.StdoutStrategy;
import com.aaron1011.crashreproducer.detector.Strategy;
import com.google.common.collect.Lists;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;

import javax.annotation.Nullable;

public class CrashReproducer {

    public static Logger LOGGER = Logger.getLogger("CrashReproducer");

    private static final String CLASS_EXTENSION = ".class";
    private static final String JAR_EXTENSION = ".jar";
    private static final String MOD_ANNOTATION = "Lnet/minecraftforge/fml/common/Mod;";


    private static final PathMatcher CLASS_FILE = path -> path.toString().endsWith(CLASS_EXTENSION);
    private static final PathMatcher JAR_FILE = path -> path.toString().endsWith(JAR_EXTENSION);
    private static final DirectoryStream.Filter<Path> JAR_FILTER = path -> path.toString().endsWith(JAR_EXTENSION);

    public final String serverCommand;
    public final File serverDir;
    public final long timeout = 20 * 1000;
    //public final List<Strategy> strategies = Lists.newArrayList(new StdoutStrategy());
    //public final List<ErrorStringFinder> finders = Lists.newArrayList(new SimpleStringFinder("CyclopsCore", true));

    public static void main(String[] args) {
        new CrashReproducer(args);
    }

    public CrashReproducer(String[] args) {
       LOGGER.setLevel(Level.ALL);

        final OptionParser parser = new OptionParser();
        OptionSpec<File> dirOpt = parser.accepts("server-dir").withRequiredArg().defaultsTo(".").ofType(File.class);
        OptionSpec<String> serverCommandOpt = parser.accepts("server-command").withRequiredArg();

        OptionSet options = parser.parse(args);

        serverDir = options.valueOf(dirOpt);
        serverCommand = options.valueOf(serverCommandOpt);
        this.run();
    }

    public void run() {
        Path modsDir = this.serverDir.toPath().resolve("mods");

        Map<String, ModData> data = this.scanDirectory(modsDir);
        List<ModData> sorted = ModParser.sortMods(data);
        System.err.println("Sorted");
        System.err.println(sorted);

        ModNarrower narrower = new ModNarrower();
        narrower.mainLoop(this, sorted);
    }

    // Taken from SpongeVanilla
    private Map<String, ModData> scanDirectory(Path path) {
        Map<String, ModData> mods = new HashMap<>();
        try (DirectoryStream<Path> dir = Files.newDirectoryStream(path, JAR_FILTER)) {
            for (Path jar : dir) {
                ModData data = scanJar(jar);
                if (data != null) {
                    mods.put(data.id, data);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, String.format("Failed to search for plugins in %s", path), e);
        }
        return mods;
    }

    @Nullable
    private ModData scanJar(Path path) throws IOException {
        try (JarInputStream jar = new JarInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
            ZipEntry entry = jar.getNextEntry();
            if (entry == null) {
                return null;
            }

            while ((entry = jar.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                if (entry.getName().endsWith(CLASS_EXTENSION)) {
                    ModData data = scanClassFile(jar, path);
                    if (data != null) {
                        return data;
                    }
                }
            }

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, String.format("Failed to scan plugin JAR: %s", path), e);
        }
        LOGGER.log(Level.SEVERE, String.format("Skipping jar %s - no @Mod annotation found", path));
        return null;
    }

    private ModData scanClassFile(InputStream in, Path path) throws IOException {
        ClassReader reader = new ClassReader(in);
        ClassNode node = new ClassNode(Opcodes.ASM5);
        reader.accept(node, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE);

        if (node.visibleAnnotations == null) {
            return null;
        }

        for (AnnotationNode annotation: node.visibleAnnotations) {
            if (annotation.desc.equals(MOD_ANNOTATION)) {
                try {
                    String id = this.findValue(annotation.values, "modid");
                    String rawDependencies = this.findValue(annotation.values, "dependencies");
                    return new ModData(id, rawDependencies, path);
                } catch (IllegalStateException e) {
                    throw new RuntimeException("Error parsing @Mod annotation for jar " + path, e);
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T> T findValue(List<Object> values, String key) throws IllegalStateException {
        for (int i = 0; i < values.size(); i += 2) {
            if (values.get(i).equals(key)) {
                return (T) values.get(i + 1);
            }
        }
        throw new IllegalStateException("Key " + key + " was not found!");
    }

}

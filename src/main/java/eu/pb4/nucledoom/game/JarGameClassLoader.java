package eu.pb4.nucledoom.game;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class JarGameClassLoader extends ClassLoader {

    private final FileSystem[] filesystem;

    public JarGameClassLoader(List<Path> jarpath) throws IOException {
        super(JarGameClassLoader.class.getClassLoader());
        this.filesystem = new FileSystem[jarpath.size()];

        for (int i = 0; i < jarpath.size(); i++) {
            this.filesystem[i] =  FileSystems.newFileSystem(jarpath.get(i));
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        for (var fs : this.filesystem) {
            var path = fs.getPath(name.replace('.', '/') + ".class");
            if (Files.exists(path)) {
                try {
                    var data = Files.readAllBytes(path);
                    return defineClass(name, data, 0, data.length);
                } catch (Throwable throwable) {
                    throw new ClassNotFoundException(name, throwable);
                }
            }
        }
        throw new ClassNotFoundException(name);
    }

    @Override
    protected URL findResource(String name) {
        for (var fs : this.filesystem) {
            var path = fs.getPath(name);
            if (Files.exists(path)) {
                try {
                    return path.toUri().toURL();
                } catch (MalformedURLException e) {
                    return null;
                }
            }
        }

        return null;
    }

    public void close() throws IOException {
        for (var fs : this.filesystem) {
            fs.close();
        }
    }

    static {
        JarGameClassLoader.registerAsParallelCapable();
    }
}

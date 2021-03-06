package weiner.noah.blogpusher;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class Git {
    private static final String gitCmdPath = "/system/bin/git";
    private static final String TAG = "Git";


    //example of usage
    private static void initAndAddFile() throws IOException, InterruptedException {
        Path directory = Paths.get("c:\\temp\\example");
        Files.createDirectories(directory);
        gitInit(directory);
        Files.write(directory.resolve("example.txt"), new byte[0]);
        gitStageAll(directory);
        gitCommit(directory, "Add example.txt");
    }

    //example of usage
    private static void cloneAndAddFile() throws IOException, InterruptedException {
        String originUrl = "https://github.com/Crydust/TokenReplacer.git";
        Path directory = Paths.get("c:\\temp\\TokenReplacer");
        gitClone(directory, originUrl);
        Files.write(directory.resolve("example.txt"), new byte[0]);
        gitStageAll(directory);
        gitCommit(directory, "Add example.txt");
        gitPush(directory);
    }

    public static void gitInit(Path directory) throws IOException, InterruptedException {
        runCommand(directory, gitCmdPath, "init");
    }

    public static void gitStageAll(Path directory) throws IOException, InterruptedException {
        runCommand(directory, gitCmdPath, "add", "-A");
    }

    public static void gitCommit(Path directory, String message) throws IOException, InterruptedException {
        runCommand(directory, gitCmdPath, "commit", "-m", message);
    }

    public static void gitPush(Path directory) throws IOException, InterruptedException {
        runCommand(directory, gitCmdPath, "push");
    }

    public static void gitClone(Path directory, String originUrl) throws IOException, InterruptedException {
        runCommand(directory.getParent(), gitCmdPath, "clone", originUrl, directory.getFileName().toString());
    }

    public static void setHome(Path directory, String newHome) throws IOException, InterruptedException {
        runCommand(directory, "export", "HOME=" + newHome);
    }

    public static void runCommand(Path directory, String... command) throws IOException, InterruptedException {
        Objects.requireNonNull(directory, "directory");
        if (!Files.exists(directory)) {
            throw new RuntimeException("can't run command in non-existing directory '" + directory + "'");
        }
        ProcessBuilder pb = new ProcessBuilder()
                .command(command)
                .directory(directory.toFile());
        //set HOME shell var so that git knows where to look for the config file. This isn't necessary.
        /*
        Map<String, String> envMap = pb.environment();
        envMap.put("HOME", Constants.gitConfigDir);
        Log.i(TAG, "got new HOME:" + envMap.get("HOME"));*/
        Process p = pb.start();
        StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), "ERROR");
        StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(), "OUTPUT");
        outputGobbler.start();
        errorGobbler.start();
        int exit = p.waitFor();
        errorGobbler.join();
        outputGobbler.join();
        if (exit != 0) {
            throw new AssertionError(String.format("runCommand() cmd returned %d", exit));
        }
        else {
            Log.i(TAG, "command ran successfully");
        }
    }

    private static class StreamGobbler extends Thread {

        private final InputStream is;
        private final String type;

        private StreamGobbler(InputStream is, String type) {
            this.is = is;
            this.type = type;
        }

        @Override
        public void run() {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is));) {
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println(type + "> " + line);
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

}

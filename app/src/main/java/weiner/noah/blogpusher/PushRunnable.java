package weiner.noah.blogpusher;

import android.util.Log;
import android.widget.Toast;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PushRunnable implements Runnable {
    private org.eclipse.jgit.api.Git repo;
    private final String TAG = "PushRunnable";

    private final String branch = "master";
    private final MainActivity mainActivity;

    private final Path rootDirPath = Paths.get(Constants.rootDir);

    CredentialsProvider cp;

    public PushRunnable(Git repo, MainActivity mainActivity, String username, String passwd) {
        Log.i(TAG, "username is " + username + ", passwd is " + passwd);
        this.repo = repo;
        this.mainActivity = mainActivity;
        cp = new UsernamePasswordCredentialsProvider(username, passwd);
    }

    @Override
    public void run() {
        try {
            Log.i(TAG, "Running stage cmd...");
            mainActivity.showToastMessage("Staging...");
            weiner.noah.blogpusher.Git.gitStageAll(rootDirPath);

            Log.i(TAG, "Running commit cmd...");
            mainActivity.showToastMessage("Committing...");
            weiner.noah.blogpusher.Git.gitCommit(rootDirPath, "new blog post");

            Log.i(TAG, "Running push cmd...");
            mainActivity.showToastMessage("Pushing...");

            Iterable<PushResult> pushResults = repo.push().setCredentialsProvider(cp).setRemote("origin").add(branch).call();
            for (PushResult result : pushResults) {
                final String compiledResultInfo = "Pushed. Messages: " + result.getMessages() + " | URI: " + result.getURI() + " | Branch: " + branch +
                        " | Updates: " + result.getRemoteUpdates().toString();
                Log.i(TAG, compiledResultInfo);
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mainActivity, compiledResultInfo, Toast.LENGTH_LONG).show();
                    }
                });
            }

            mainActivity.clearPostFields();
        } catch (TransportException transportException) {
            mainActivity.showToastMessage(transportException.getMessage());
            transportException.printStackTrace();
        }
        catch (GitAPIException | InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }
}

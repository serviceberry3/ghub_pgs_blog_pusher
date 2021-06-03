package weiner.noah.blogpusher;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private Button submitButton;
    private EditText postContentText;
    private EditText postTitleText;
    private final String TAG = "MainActivity";

    private final String rootDir = "/storage/emulated/0/Documents/serviceberry3.github.io/";
    private final String postDir = rootDir + "_posts/";
    private final String binDir = "/data/data/com.termux/files/usr/bin/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        verifyStoragePermissions(this);

        submitButton = (Button) findViewById(R.id.submit);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, postContentText.getText().toString());
                try {
                    pushPost();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        postContentText = (EditText) findViewById(R.id.postContent);
        postTitleText = (EditText) findViewById(R.id.postTitle);
    }

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    public void pushPost() throws IOException {
        Date currentDate = Calendar.getInstance().getTime();
        Log.i(TAG, currentDate.toString());

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String formattedDate = df.format(currentDate);

        Log.i(TAG, formattedDate);
        String titleText = postTitleText.getText().toString();

        String fileName = postDir + formattedDate + "-" + titleText.toLowerCase().replace(" ", "") + ".markdown";

        String postContent = postContentText.getText().toString();

        Log.i(TAG, fileName);

        String frontMatter = "---\nlayout: post\ntitle: '" + titleText +
                "'\ndate: " + currentDate.toString() +
                "\ncategories: \nblog: at\n---\n";

        String fileContent = frontMatter + postContent;

        //create the new File
        File file = new File(fileName);
        if (!file.createNewFile()) {
            //the file already exists
            return;
        }

        //write the content to the file
        if (file.exists())
        {
            OutputStream fo = new FileOutputStream(file);
            OutputStreamWriter osw = new OutputStreamWriter(fo);
            osw.write(fileContent);
            osw.flush();
            osw.close();
            System.out.println("File created: " + file);
        }



        Intent intent2 = new Intent();
        intent2.setClassName("com.termux", "com.termux.app.RunCommandService");
        intent2.setAction("com.termux.RUN_COMMAND");
        intent2.putExtra("com.termux.RUN_COMMAND_PATH", binDir + "git");
        intent2.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"-C", rootDir, "add", "."});
        startService(intent2);

        Intent intent3 = new Intent();
        intent3.setClassName("com.termux", "com.termux.app.RunCommandService");
        intent3.setAction("com.termux.RUN_COMMAND");
        intent3.putExtra("com.termux.RUN_COMMAND_PATH", binDir + "git");
        intent3.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"-C", rootDir, "commit", "-m", "\"new blog post\""});
        startService(intent3);

        Intent intent4 = new Intent();
        intent4.setClassName("com.termux", "com.termux.app.RunCommandService");
        intent4.setAction("com.termux.RUN_COMMAND");
        intent4.putExtra("com.termux.RUN_COMMAND_PATH", binDir + "git");
        intent4.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"-C", rootDir, "push", "origin", "master"});
        startService(intent4);
    }
}
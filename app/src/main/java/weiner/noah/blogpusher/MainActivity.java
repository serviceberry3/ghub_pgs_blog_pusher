package weiner.noah.blogpusher;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import me.sheimi.android.utils.BasicFunctions;
import me.sheimi.sgit.database.RepoDbManager;
import me.sheimi.sgit.database.models.Repo;
import me.sheimi.sgit.dialogs.DummyDialogListener;
import me.sheimi.sgit.repo.tasks.repo.PushTask;

import static weiner.noah.blogpusher.Constants.relImgPath;
import static weiner.noah.blogpusher.Constants.repoName;
import static weiner.noah.blogpusher.Constants.rootDir;
import static weiner.noah.blogpusher.Constants.siteImgAssetsDir;

public class MainActivity extends AppCompatActivity {
    private Button submitButton, attachImgButton;
    private EditText postContentText;
    private EditText postTitleText;
    private final String TAG = "MainActivity";

    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    private RecyclerView.LayoutManager layoutManager;

    private ImageView imageView;

    private final String postDir = rootDir + "_posts/";

    private final File rootDirFile = new File(rootDir);

    org.eclipse.jgit.api.Git blogRepo;

    //Repo class from Sheimi
    private Repo blogRepoSheimi;

    //A dummy Sheimi PushTask obj, since it extends RepoRemoteOpTask, which is where we get OnPasswordEntered implementation
    private PushTask dummyPushTask;

    private String blogName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BasicFunctions.setActiveActivity(this);

        Log.i(TAG, "Number of rows in tab: " + RepoDbManager.getNumRowsInTab());

        //set up view
        recyclerView = findViewById(R.id.recycler_view);
        adapter = new BlogsAdapter(this);

        //pass reference to activity
        layoutManager = new LinearLayoutManager(this);

        //set up the RecyclerView
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(layoutManager);
        //now we've instantiated the recyclerview, set the adapter for it to be adapter we wrote, and so now it knows what data to display

        //select the ImageView
        imageView = (ImageView) findViewById(R.id.img_attachment);

        //if the repo hasn't been created in the sqlite table yet, create it now

        if (RepoDbManager.getNumRowsInTab() == 0) {
            blogRepoSheimi = Repo.createRepo("serviceberry3.github.io",
                    "https://github.com/serviceberry3/serviceberry3.github.io", "testStatus");
        }
        else {
            //get sqlite cursor and move it to first row of repo table
            Cursor c = RepoDbManager.queryAllRepo();
            c.moveToFirst();
            blogRepoSheimi = new Repo(c);
        }

        dummyPushTask = new PushTask(blogRepoSheimi, null, false, false, null);

        verifyStoragePermissions(this);

        checkForStoredCredentials();

        try {
            blogRepo = org.eclipse.jgit.api.Git.open(rootDirFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //set up submit post btn
        submitButton = (Button) findViewById(R.id.submit_post_button);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, postContentText.getText().toString());

                if (!(((BlogsAdapter)adapter).isAnyBlogChecked())) {
                    showToastMessage("Please select a blog first.");
                    return;
                }

                if (postContentText.getText().toString().equals("") || postTitleText.getText().toString().equals("")) {
                    showToastMessage("Title or content is empty.");
                    return;
                }
                try {
                    pushPost();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        //set up add img button
        attachImgButton = (Button) findViewById(R.id.add_img_button);
        attachImgButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage(MainActivity.this);
            }
        });

        postContentText = (EditText) findViewById(R.id.postContent);
        postTitleText = (EditText) findViewById(R.id.postTitle);
    }

    //Storage Permissions
    private final int REQUEST_EXTERNAL_STORAGE = 1;
    private String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public void selectBlog(String name) {
        Log.i(TAG, "selectBlog: new name is " + name);
        this.blogName = name;
    }

    //On opening the app, make sure we have username and passwd credentials in the db. If not, prompt use for them now.
    private void checkForStoredCredentials() {
            //FIXME: check to see if persistent credentials
            String username = blogRepoSheimi.getUsername();
            String password = blogRepoSheimi.getPassword();

            Log.i(TAG, "checkforstoredcreds(): From blogRepoSheimi found username " + username + ", password " + password);

            //we ain't got nothin. Prompt and then make a recursive call to this fxn
            if (username == null || password == null) {
                //this will end up calling setUsername() and setPassword() on the Repo
                promptForPassword(dummyPushTask, null);
            }

            //at this point should always have appropriate username and passwd filled
    }

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public void verifyStoragePermissions(Activity activity) {
        //Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            //We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
        else {
            Log.i(TAG, "Already have perm to write ext storage");
        }
    }

    public void clearPostFields() {
        postTitleText.getText().clear();
        postContentText.getText().clear();
    }

    String currentPhotoPath;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        //this will resolve to something like /storage/emulated/0/Pictures
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        Log.i(TAG, "Pictures location is: " + storageDir.toString());

        /**Creates new empty file in specified dir, using given prefix and suffix strings to generate name. If success, is guaranteed that:
         - file denoted by the returned abstract pathname did not exist before this method was invoked, and
         - This method nor its variants will return same abstract pathname again in current invocation of the VM.*/
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        //Save the file path of the captured image so that we can copy img to site assets and ref it in the blog post
        currentPhotoPath = image.getAbsolutePath();

        //return reference to the new empty file created in Pictures
        return image;
    }

    private void copyImgToSiteAssets(String imgPath) {
        try {
            Git.runCommand(getApplication().getDataDir().toPath(), "cp", imgPath, Constants.siteImgAssetsDir);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void selectImage(Context context) {
        //options for the image
        final CharSequence[] options = { "Take photo", "Choose from Gallery", "Cancel" };

        //create a new AlertDialog builder, and set title of the pop-up dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Choose image source");

        //set onClick actions for the items
        builder.setItems(options, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (options[item].equals("Take photo")) {
                    //start new Activity for image capture, will be placed at top of activity stack
                    Intent takePicture = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

                    Log.i(TAG, "resolving activity...");

                    // Ensure that there's a camera activity to handle the intent
                    if (takePicture.resolveActivity(getPackageManager()) != null) {
                        Log.i(TAG, "creating photo file...");
                        //Create the File where the photo should go
                        File photoFile = null;
                        try {
                            //create empty file to store the image in Pictures
                            photoFile = createImageFile();
                        } catch (IOException e) {
                            //Error occurred while creating the File
                            e.printStackTrace();
                        }
                        //Continue only if the File was successfully created
                        if (photoFile != null) {
                            /**get the URI for the File we created, where the photo will be stored
                             FileProvider subclass of ContentProvider that facilitates secure sharing of files assoc w/app by creating content:// Uri for file instead of file:/// Uri.

                             Content URI allows you to grant read and write access using temp access perms. When you create Intent containing content URI,
                             in order to send content URI to a client app, you can also call Intent.setFlags() to add permissions.
                             These permissions are available to the client app for as long as stack for receiving Activity is active.
                             For Intent going to a Service, permissions are available as long as Service is running.

                             In comparison, to control access to a file:/// Uri you have to modify the file system permissions of the underlying file.
                             The permissions you provide become available to any app, and remain in effect until you change them. This level of access is fundamentally insecure.
                            */
                            //get content URI for the photo, authority is just this app.
                            //will return something like content://weiner.noah.blogpusher/my_images/XXXX.jpg
                            Uri contentURIForPhoto = FileProvider.getUriForFile(MainActivity.this, "weiner.noah.blogpusher", photoFile);

                            Log.i(TAG, "Content URI is: " + contentURIForPhoto.toString());

                            //this extra indicates a content resolver Uri is being used to store the requested image or video
                            takePicture.putExtra(MediaStore.EXTRA_OUTPUT, contentURIForPhoto);
                            takePicture.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivityForResult(takePicture, 0);
                        }
                    }

                } else if (options[item].equals("Choose from Gallery")) {
                    //start new Activity for image selection from Gallery
                    Intent pickPhoto = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(pickPhoto , 1);

                } else if (options[item].equals("Cancel")) {
                    //close the dialog
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //make sure Activity wasn't cancelled
        if (resultCode != RESULT_CANCELED) {
            switch (requestCode) {
                //returning from image capture
                case 0:
                    if (resultCode == RESULT_OK && data != null) {
                        //Bundle extras = data.getExtras();

                        //Log.i(TAG, "EXTRAS: " + extras.size());

                        //Log.i(TAG, "Whether contains: " + data.getExtras().containsKey("data"));

                        //get the image back from the save location and display preview
                        imageView.setImageBitmap(BitmapFactory.decodeFile(currentPhotoPath));

                        copyImgToSiteAssets(currentPhotoPath);
                    }
                    break;

                //returning from Gallery img selection
                case 1:
                    //take the returned URI of the selected image, and load that file as a Bitmap
                    if (resultCode == RESULT_OK && data != null) {
                        //get URI of the image selected from the gallery
                        Uri selectedImage = data.getData();
                        String[] filePathColumn = {MediaStore.Images.Media.DATA};

                        if (selectedImage != null) {
                            /** ContentResolver provides apps access to the content model
                             - query() similar to SQL format
                            @param - this param maps to the table in the provider with this name
                            @param - an array of columns that should be included for each row retrieved
                            @param - 'selection' specifies criteria for selecting rows
                            @param - 'sortOrder' specifies order in which rows appear in returned Cursor
                             */
                            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                            if (cursor != null) {
                                //move to first row
                                cursor.moveToFirst();

                                //get the index of column with name equal to first String in filePathColumn
                                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);

                                //file path of the picture sits here
                                String picturePath = cursor.getString(columnIndex);

                                //decode the file into Bitmap and display it
                                imageView.setImageBitmap(BitmapFactory.decodeFile(picturePath));
                                cursor.close();

                                //copy image into website assets
                                copyImgToSiteAssets(picturePath);

                                currentPhotoPath = picturePath;
                            }
                        }

                    }
                    break;
            }
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

        Log.i(TAG, "TESTSRC: " + relImgPath + currentPhotoPath.substring(currentPhotoPath.lastIndexOf('/') + 1));

        String postContent = postContentText.getText().toString() + "<br><br><br><br>" +
                "<img src=\"" + "{{ '" + relImgPath + currentPhotoPath.substring(currentPhotoPath.lastIndexOf('/') + 1) + "' | prepend: site.baseurl }}" +
       "\" width=\"400\" />";

        Log.i(TAG, fileName);
        Log.i(TAG, "Post content is: " + postContent);

        String frontMatter = "---\nlayout: post\ntitle: '" + titleText +
                "'\ndate: " + currentDate.toString() +
                "\ncategories: \nblog: " + blogName.replace("2021", "") + "\n---\n";

        String footer = "<br><br><br><br><br>" +
                "<span class=\"text-sm\">" +
                    "Posted from " +
                    "<a href=\"https://github.com/serviceberry3/ghub_pgs_blog_pusher\" class=\"text-green-500\">" +
                        "mobile companion app" +
                    "</a>" +
                "</span>";

        String fileContent = frontMatter + postContent + footer;

        String userDir = System.getProperty("user.dir");
        Log.i(TAG, "user dir is " + userDir);

        //create the new File
        File file = new File(fileName);
        if (!file.createNewFile()) {
            //the file already exists
            showToastMessage("A post with this title and date already exists");
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

        PushRunnable pushRunnable = new PushRunnable(blogRepo, this, blogRepoSheimi.getUsername(), blogRepoSheimi.getPassword());
        new Thread(pushRunnable).start();
    }

    /* View Utils Start */
    public void showToastMessage(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, msg,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    public void showToastMessage(int resId) {
        showToastMessage(getString(resId));
    }

    public void showMessageDialog(int title, int msg, int positiveBtn,
                                  DialogInterface.OnClickListener positiveListener) {
        showMessageDialog(title, getString(msg), positiveBtn,
                R.string.label_cancel, positiveListener,
                new DummyDialogListener());
    }

    public void showMessageDialog(int title, String msg, int positiveBtn,
                                  DialogInterface.OnClickListener positiveListener) {
        showMessageDialog(title, msg, positiveBtn, R.string.label_cancel,
                positiveListener, new DummyDialogListener());
    }

    public void showMessageDialog(int title, String msg, int positiveBtn,
                                  int negativeBtn, DialogInterface.OnClickListener positiveListener,
                                  DialogInterface.OnClickListener negativeListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title).setMessage(msg)
                .setPositiveButton(positiveBtn, positiveListener)
                .setNegativeButton(negativeBtn, negativeListener).show();
    }

    public void showMessageDialog(int title, String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title).setMessage(msg)
                .setPositiveButton(R.string.label_ok, new DummyDialogListener()).show();
    }

    public void showEditTextDialog(int title, int hint, int positiveBtn,
                                   final OnEditTextDialogClicked positiveListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.dialog_edit_text, null);
        final EditText editText = (EditText) layout.findViewById(R.id.editText);
        editText.setHint(hint);
        builder.setTitle(title)
                .setView(layout)
                .setPositiveButton(positiveBtn,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(
                                    DialogInterface dialogInterface, int i) {
                                String text = editText.getText().toString();
                                if (text == null || text.trim().isEmpty()) {
                                    showToastMessage(R.string.alert_you_should_input_something);
                                    return;
                                }
                                positiveListener.onClicked(text);
                            }
                        })
                .setNegativeButton(R.string.label_cancel,
                        new DummyDialogListener()).show();
    }

    /**
     * Callback interface to receive credentials entered via UI by the user after being prompted
     * in the UI in order to connect to a remote repo
     */
    public static interface OnPasswordEntered {

        /**
         * Handle retrying a Remote Repo task after user supplies requested credentials
         *
         * @param username
         * @param password
         * @param savePassword
         */
        void onClicked(String username, String password, boolean savePassword);

        void onCanceled();
    }

    public void promptForPassword(OnPasswordEntered onPasswordEntered,
                                  int errorId) {
        promptForPassword(onPasswordEntered, errorId);
    }

    public void promptForPassword(final OnPasswordEntered onPasswordEntered,
                                  final String errorInfo) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                promptForPasswordInner(onPasswordEntered, errorInfo);
            }
        });
    }

    private void promptForPasswordInner(final OnPasswordEntered onPasswordEntered, String errorInfo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();

        View layout = inflater.inflate(R.layout.dialog_prompt_for_password, null);

        final EditText username = (EditText) layout.findViewById(R.id.username);
        final EditText password = (EditText) layout.findViewById(R.id.password);

        final CheckBox checkBox = (CheckBox) layout
                .findViewById(R.id.savePassword);

        if (errorInfo == null) {
            errorInfo = getString(R.string.dialog_prompt_for_password_title_weiner);
        }

        //set title to the string, and set up buttons for popup
        builder.setTitle(errorInfo)
                .setView(layout)
                .setPositiveButton(R.string.label_done,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                String enteredUsername = username.getText().toString();
                                String enteredPasswd = password.getText().toString();

                                //invocation of onClicked here() will set username and password fields of the Repo obj that was used to create
                                //dummyPushTask
                                onPasswordEntered.onClicked(username.getText()
                                        .toString(), password.getText()
                                        .toString(), checkBox.isChecked());
                            }
                        })
                .setNegativeButton(R.string.label_cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(
                                    DialogInterface dialogInterface, int i) {
                                onPasswordEntered.onCanceled();
                            }
                        }).show();
    }

    public static interface onOptionDialogClicked {
        void onClicked();
    }

    public static interface OnEditTextDialogClicked {
        void onClicked(String text);
    }

    public void showOptionsDialog(int title,final int option_names, final onOptionDialogClicked[] option_listeners) {
        CharSequence[] options_values = getResources().getStringArray(option_names);
        showOptionsDialog(title, options_values, option_listeners);
    }

    public void showOptionsDialog(int title, CharSequence[] option_values, final onOptionDialogClicked[] option_listeners) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setItems(option_values,new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                option_listeners[which].onClicked();
            }
        }).create().show();
    }

    public void forwardTransition() {
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
    }

    public void backTransition() {
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
    }
}
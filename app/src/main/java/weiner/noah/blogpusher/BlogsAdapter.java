package weiner.noah.blogpusher;

import android.content.Intent;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import org.w3c.dom.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


//RecyclerView class is a generic class
public class BlogsAdapter extends RecyclerView.Adapter<BlogsAdapter.BlogsViewHolder> {
    private final MainActivity mainActivity;
    private List<String> blogList = new ArrayList<>();
    private final String TAG = "BlogsAdapter";

    //keep track of the one blog's RadioBox that needs to be checked
    private final ArrayList<Integer> selectCheck = new ArrayList<>();

    public BlogsAdapter(MainActivity mainActivity) {
        this.mainActivity = mainActivity;

        fillBlogList();
        clearAllCheckBoxes();
    }

    //check to see if a blog has been selected yet
    public boolean isAnyBlogChecked() {
        for (Integer thisInteger : selectCheck) {
            if (thisInteger == 1) {
                return true;
            }
        }

        return false;
    }

    private void clearAllCheckBoxes() {
        //initialize selectCheck to all zeroes (unchecked)
        for (int i = 0; i < blogList.size(); i++) {
            selectCheck.add(0);
        }
    }

    public static class BlogsViewHolder extends RecyclerView.ViewHolder { //needs to be public because using it in declaration above
        public LinearLayout containerView;
        public TextView textView;
        public RadioButton radioButton;
        private final MainActivity mainActivity;

        //constructor - called in onCreateViewHolder()
        BlogsViewHolder(View view, final MainActivity mainActivity) {
            //use original
            super(view);
            //create fields for the two things we added IDs to: layout and textview

            //take view that's passed in from recyclerview, convert it into something we can use

            //under the hood, Gradle automatically generates unique IDs for all the string IDs we gave stuff, puts in R
            containerView = view.findViewById(R.id.blog_view); //integer that represents the container
            textView = view.findViewById(R.id.blog_row_text_view);
            radioButton = view.findViewById(R.id.blog_checkbox);

            this.mainActivity = mainActivity;
        }
    }


    private void fillBlogList() { ;
        File directory = new File(Constants.rootDir + "/pages/");
        File[] files = directory.listFiles();
        Log.d(TAG, "Size: "+ files.length);

        for (File thisFile : files)
        {
            String fileName = thisFile.getName();
            Log.d(TAG, "FileName:" + fileName);
            if (fileName.contains("2021")) {
                blogList.add(fileName.replace(".markdown", ""));
            }
        }
    }

    //override some methods from RecyclerView.Adapter
    @NonNull
    @Override
    public BlogsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //get our layout file, go from layout to a view. Inflate: go from XML file to a Java View. R.layout is auto generated for us
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.blog_listitem, parent, false);
        //now we've converted XML file into Java View object in memory

        //return a new view holder containing this view
        return new BlogsViewHolder(view, mainActivity);
    }

    //second method: onBind()--called whenever a view scrolls into screen and we say we need to set the values inside of this row
    //set the different properties of the view we created

    //go from this model to our view (a controller goes from a model to a view)
    @Override
    public void onBindViewHolder(@NonNull BlogsViewHolder holder, final int position) {
        //grab element out of array to display data
        String current = blogList.get(position);

        //take name of the test, set that to be text of row
        holder.textView.setText(current);

        //pass along the test to the ViewHolder
        holder.containerView.setTag(current); //now ViewHolder has access to current test

        //each time onBindViewHolder() is called on each notifyDataSetChanged(), the correct checkbox will be automatically filled
        //based on the selectCheck array
        if (selectCheck.get(position) == 1) {
            holder.radioButton.setChecked(true);
        } else {
            holder.radioButton.setChecked(false);
        }

        //add event handler that can be executed when row is tapped
        //call setOnClickListener method on a LINEARLAYOUT
        holder.containerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //save blog selection
                LinearLayout vLinear = (LinearLayout)v;
                mainActivity.selectBlog(((TextView)vLinear.getChildAt(1)).getText().toString());

                for (int k = 0; k < selectCheck.size(); k++) {
                    if (k == position) {
                        selectCheck.set(k, 1);
                    }
                    else {
                        selectCheck.set(k, 0);
                    }
                }
                //force check box graphics to update
                notifyDataSetChanged();
            }
        });
    }

    //method that tells RecyclerView how many rows to display
    @Override
    public int getItemCount() {
        return blogList.size();
    }
}

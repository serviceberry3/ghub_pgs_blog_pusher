package me.sheimi.android.views;

import android.content.Context;
import android.content.DialogInterface;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import weiner.noah.blogpusher.MainActivity;

public class SheimiDialogFragment extends DialogFragment {

    @SuppressWarnings("NullableProblems") // It's safe to assume onAttach is called before other code.
    @NonNull
    private MainActivity mActivity;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mActivity = (MainActivity) context;
    }

    @NonNull
    public MainActivity getRawActivity() {
        return mActivity;
    }

    public void showMessageDialog(int title, int msg, int positiveBtn,
            DialogInterface.OnClickListener positiveListener) {
        getRawActivity().showMessageDialog(title, msg, positiveBtn,
                positiveListener);
    }

    public void showMessageDialog(int title, String msg, int positiveBtn,
            DialogInterface.OnClickListener positiveListener) {
        getRawActivity().showMessageDialog(title, msg, positiveBtn,
                positiveListener);
    }

    public void showToastMessage(int resId) {
        getRawActivity().showToastMessage(getString(resId));
    }

    public void showToastMessage(String msg) {
        getRawActivity().showToastMessage(msg);
    }

    public void promptForPassword(MainActivity.OnPasswordEntered onPasswordEntered,
                                  int errorId) {
        getRawActivity().promptForPassword(onPasswordEntered, errorId);
    }

    public void promptForPassword(MainActivity.OnPasswordEntered onPasswordEntered) {
        getRawActivity().promptForPassword(onPasswordEntered, null);
    }
}

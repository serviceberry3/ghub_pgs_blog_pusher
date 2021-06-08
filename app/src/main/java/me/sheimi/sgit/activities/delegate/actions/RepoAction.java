package me.sheimi.sgit.activities.delegate.actions;

import me.sheimi.sgit.database.models.Repo;
import weiner.noah.blogpusher.MainActivity;

public abstract class RepoAction {

    protected Repo mRepo;
    protected MainActivity mActivity;

    public RepoAction(Repo repo, MainActivity activity) {
        mRepo = repo;
        mActivity = activity;
    }

    public abstract void execute();
}

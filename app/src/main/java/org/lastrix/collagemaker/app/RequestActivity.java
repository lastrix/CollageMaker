package org.lastrix.collagemaker.app;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import org.lastrix.collagemaker.app.api.User;
import org.lastrix.collagemaker.app.api.UserSearchTask;


/**
 * Main activity. Performs user searching and navigation
 * to image selection activity {@link CollageActivity}.
 */
public class RequestActivity extends ActionBarActivity implements UserListFragment.Listener {

    public static final String LOG_TAG = RequestActivity.class.getSimpleName();
    private final static boolean LOG_ALL = BuildConfig.LOG_ALL;

    private UserListFragment mUserListFragment;
    private boolean mTwoPane = false;
    private User mSelectedUser = null;
    private UserSearchTask mSearchTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request);


        mUserListFragment = (UserListFragment) getSupportFragmentManager().findFragmentById(R.id.user_list);

        if (findViewById(R.id.user_photos) != null) {
            //install photos fragment
            mTwoPane = true;
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (mSearchTask != null) {
            mSearchTask.cancel(true);
            mSearchTask = null;
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_request, menu);

        final SearchView searchText = getSearchView(menu);
        searchText.setQueryHint(getString(R.string.hint_search_user));
        searchText.setOnQueryTextListener(mUserListFragment.getQueryTextListener());

        return true;
    }

    private SearchView getSearchView(Menu menu) {
        final SearchView searchText;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            final MenuItem menuItem = menu.findItem(R.id.action_search);
            searchText = (SearchView) MenuItemCompat.getActionView(menuItem);
        } else {
            searchText = (SearchView) menu.findItem(R.id.action_search).getActionView();
        }
        return searchText;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                //let ActionBar expand SearchView
                return false;

            default:
                return false;
        }
    }

    @Override
    protected void onDestroy() {
        this.mUserListFragment = null;
        super.onDestroy();
    }

    @Override
    public void onUserSelected(User user) {
        //forbid abusing
        if (mSelectedUser == user) return;

        mSelectedUser = user;
        if (mTwoPane) {
            //simply replace old one
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.user_photos, UserPhotosFragment.newInstance(user))
                    .commit();
        } else {
            //start intent
            Intent intent = new Intent(this, UserPhotosActivity.class);
            intent.putExtras(user.asBundle());
            startActivity(intent);
        }
    }
}

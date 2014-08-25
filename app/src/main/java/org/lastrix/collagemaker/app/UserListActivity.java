package org.lastrix.collagemaker.app;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import org.lastrix.collagemaker.app.content.User;


/**
 * Main activity. Performs user searching and navigation
 * to image selection activity {@link CollageActivity}.
 */
public class UserListActivity extends ActionBarActivity implements UserListFragment.Listener {

    public static final String LOG_TAG = UserListActivity.class.getSimpleName();
    private final static boolean LOG_ALL = BuildConfig.LOG_ALL;

    private boolean mTwoPane = false;
    private UserSearchOnQueryTextListener mQueryListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users_list);

        mQueryListener = new UserSearchOnQueryTextListener(this);

        FragmentManager manager = getSupportFragmentManager();
        if (savedInstanceState == null) {

            manager.beginTransaction()
                    .add(R.id.fragment_container_user_list, UserListFragment.newInstance(null))
                    .commit();

        }

        if (findViewById(R.id.fragment_container) != null) {
            //install photos fragment
            mTwoPane = true;

            if (savedInstanceState == null) {
                manager.beginTransaction()
                        .add(R.id.fragment_container, UserPhotosFragment.newInstance(null))
                        .commit();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_user_list, menu);

        final SearchView searchText = getSearchView(menu);
        searchText.setQueryHint(getString(R.string.hint_search_user));
        searchText.setOnQueryTextListener(mQueryListener);

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

            case R.id.action_collage:
                startActivity(new Intent(this, CollageActivity.class));
                return true;

            default:
                return false;
        }
    }

    @Override
    protected void onDestroy() {
        mQueryListener.mActivity = null;
        mQueryListener = null;
        super.onDestroy();
    }

    @Override
    public void onUserSelected(User user) {
        if (mTwoPane) {
            //simply replace old one
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, UserPhotosFragment.newInstance(user))
                    .commit();
        } else {
            //start intent
            Intent intent = new Intent(this, UserPhotosActivity.class);
            intent.putExtras(user.asBundle());
            startActivity(intent);
        }
    }

    private void searchFor(String username) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container_user_list, UserListFragment.newInstance(username))
                .commit();
    }

    private static class UserSearchOnQueryTextListener implements SearchView.OnQueryTextListener {
        private UserListActivity mActivity;

        public UserSearchOnQueryTextListener(UserListActivity activity) {
            mActivity = activity;
        }

        @Override
        public boolean onQueryTextSubmit(String s) {
            mActivity.searchFor(s);
            return true;
        }

        @Override
        public boolean onQueryTextChange(String s) {
            if (LOG_ALL) {
                Log.v(LOG_TAG, "onQueryTextChange: " + s);
            }
            return false;
        }
    }


}

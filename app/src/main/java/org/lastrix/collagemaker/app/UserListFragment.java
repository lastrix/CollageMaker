package org.lastrix.collagemaker.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.nostra13.universalimageloader.core.ImageLoader;
import org.lastrix.collagemaker.app.api.UserSearchTask;
import org.lastrix.collagemaker.app.content.ContentHelper;
import org.lastrix.collagemaker.app.content.User;

import java.util.Collections;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link org.lastrix.collagemaker.app.UserListFragment.Listener} interface
 * to handle interaction events.
 */
public class UserListFragment extends Fragment implements AdapterView.OnItemClickListener, UserSearchTask.Listener {
    public final static boolean LOG_ALL = BuildConfig.LOG_ALL;
    public final static String LOG_TAG = UserListFragment.class.getSimpleName();
    public static final String ARG_SEARCH = "search";
    public static final String CONFIG_SELECTED = "selected";


    private Listener mListener;
    private UserListViewAdapter mAdapter;
    private ListView mListView;
    private UserSearchTask mSearchTask;
    private ProgressDialog mProgressDialog;
    private String mSearch;
    private long mSelected = -1;
    private SetupRunnable mSetupRunnable;

    public static UserListFragment newInstance(String username){
        UserListFragment fragment = new UserListFragment();
        Bundle bundle = new Bundle();
        bundle.putString(ARG_SEARCH, username!=null?username:UserSearchTask.SENTINEL);
        fragment.setArguments(bundle);
        return fragment;
    }

    public UserListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSearch = getArguments().getString(ARG_SEARCH);

        if ( savedInstanceState != null ){
            mSelected = savedInstanceState.getLong(CONFIG_SELECTED);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_user_list, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mListView = (ListView) view.findViewById(R.id.list);
        mAdapter = new UserListViewAdapter(getActivity().getLayoutInflater(), getActivity().getContentResolver());
        mListView.setOnItemClickListener(this);
        mListView.setAdapter(mAdapter);

        mProgressDialog = new ProgressDialog(getActivity());
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setTitle(R.string.title_loading);
        mProgressDialog.setCancelable(true);

        mSearchTask = new UserSearchTask(this, mProgressDialog, getActivity().getContentResolver());
        mSearchTask.execute(mSearch);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (Listener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mSearchTask != null) {
            mSearchTask.cancel(true);
            mSearchTask = null;
        }
        mListView.removeCallbacks(mSetupRunnable);
        mSetupRunnable = null;
        if ( mProgressDialog.isShowing() ){
            mProgressDialog.dismiss();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mListView = null;
        mAdapter = null;
        mProgressDialog = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        User user = (User) mAdapter.getItem(position);
        mListener.onUserSelected(user);
        mSelected = user.getId();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mSelected != -1) {
            outState.putLong(CONFIG_SELECTED, mSelected);
        }
    }


    @Override
    public void onSearchCompleted(final List<User> users) {
        mAdapter.mUsers = users;
        mAdapter.notifyDataSetChanged();
        mSearchTask = null;
        mSetupRunnable = new SetupRunnable();
        mListView.postDelayed(mSetupRunnable, 100L);
    }

    @Override
    public void onSearchFailed(Throwable e) {
        Log.e(LOG_TAG, "Failed to check user.", e);
        Toast.makeText(getActivity(), R.string.error_search_failed, Toast.LENGTH_LONG).show();
        mSearchTask = null;
        mSelected = -1;
    }


    public interface Listener {

        public void onUserSelected(User user);
    }

    private static class UserListViewAdapter extends BaseAdapter implements View.OnClickListener {
        public static final int VIEW_TYPE_COUNT = 2;
        public static final int VIEW_TYPE_EMPTY = 1;
        public static final int VIEW_TYPE_ITEM = 0;
        private final LayoutInflater mInflater;
        private List<User> mUsers;
        private ContentResolver mContentResolver;

        private UserListViewAdapter(LayoutInflater inflater, ContentResolver contentResolver) {
            mContentResolver = contentResolver;
            mUsers = Collections.emptyList();
            this.mInflater = inflater;
        }

        @Override
        public int getCount() {
            final int count = mUsers.size();
            return count > 0 ? count : 1;
        }

        @Override
        public Object getItem(int position) {
            if (mUsers.size() == 0) return null;
            return mUsers.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (getItemViewType(position) == VIEW_TYPE_ITEM) {
                convertView = getItemView(position, convertView);
            } else {
                //no results found
                convertView = mInflater.inflate(android.R.layout.simple_list_item_1, null);
                TextView textView = (TextView) convertView;
                textView.setText(R.string.info_no_results);
            }
            return convertView;
        }

        private View getItemView(int position, View convertView) {
            final ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.list_item_user, null);

                holder = new ViewHolder();
                holder.photo = (ImageView) convertView.findViewById(R.id.photo);
                holder.name = (TextView) convertView.findViewById(R.id.name);
                holder.nick = (TextView) convertView.findViewById(R.id.nick);
                holder.favorite = (ImageView) convertView.findViewById(R.id.favorite);
                holder.favorite.setOnClickListener(this);
                holder.position = position;
                holder.loaded = false;

                convertView.setTag(holder);

            } else {
                holder = (ViewHolder) convertView.getTag();

            }

            //check state
            User user = mUsers.get(position);
            if (holder.position != position || user != holder.user) {
                holder.loaded = false;
            }

            //if data changed
            if (!holder.loaded) {
                holder.loaded = true;

                holder.position = position;
                holder.user = user;
                holder.photo.setImageResource(R.drawable.ic_loading_user);
                holder.name.setText(user.getName());
                holder.nick.setText(user.getUsername());

                holder.favorite.setTag(position);
                if ( holder.user.isFavorite() ) {
                    holder.favorite.setImageResource(android.R.drawable.btn_star_big_on);
                } else {
                    holder.favorite.setImageResource(android.R.drawable.btn_star_big_off);
                }

                //now load image
                holder.url = user.getPhotoUrl();
                ImageLoader loader = ImageLoader.getInstance();
                loader.cancelDisplayTask(holder.photo);
                loader.displayImage(holder.url, holder.photo);

            }
            return convertView;
        }

        @Override
        public int getItemViewType(int position) {
            if (mUsers.size() == 0) return VIEW_TYPE_EMPTY;
            return VIEW_TYPE_ITEM;
        }

        @Override
        public int getViewTypeCount() {
            return VIEW_TYPE_COUNT;
        }

        @Override
        public boolean isEnabled(int position) {
            return mUsers.size() > 0;
        }

        @Override
        public void onClick(View v) {
            ImageView image = (ImageView) v;
            Integer position = (Integer) v.getTag();
            User user = mUsers.get(position);
            user.setFavorite(!user.isFavorite());
            if ( user.isFavorite() ) {
                image.setImageResource(android.R.drawable.btn_star_big_on);
            } else {
                image.setImageResource(android.R.drawable.btn_star_big_off);
            }
            new FavoriteChangeTask(mContentResolver).execute(user);
        }

        private static class ViewHolder {
            int position;
            boolean loaded;
            User user;
            String url;
            ImageView photo;
            TextView name;
            TextView nick;
            ImageView favorite;
        }

        private static class FavoriteChangeTask extends AsyncTask<User, Void, Void>{

            private ContentResolver mContentResolver;

            private FavoriteChangeTask(ContentResolver contentResolver) {
                this.mContentResolver = contentResolver;
            }

            @Override
            protected Void doInBackground(User... params) {
                ContentValues values = new ContentValues(1);
                for( User user : params){
                    values.clear();
                    values.put(User.COLUMN_FAVORITE, user.isFavorite());
                    mContentResolver.update(ContentHelper.getUserUri(user),
                            values,
                            String.format("%s = ?", User.COLUMN_ID),
                            new String[]{Long.toString(user.getId())}
                    );
                }
                return null;
            }
        }

    }

    private class SetupRunnable implements Runnable {
        @Override
        public void run() {
            if (mSelected != -1) {
                final int size = mAdapter.mUsers.size();
                for (int i = 0; i < size; i++) {
                    if (mAdapter.mUsers.get(i).getId() == mSelected) {
                        mListView.setSelection(i);
                        mListView.smoothScrollToPosition(i);
                        break;
                    }
                }
            }
        }
    }
}

package org.lastrix.collagemaker.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.nostra13.universalimageloader.core.ImageLoader;
import org.lastrix.collagemaker.app.api.User;
import org.lastrix.collagemaker.app.api.UserSearchTask;

import java.util.Collections;
import java.util.List;

import static android.support.v7.widget.SearchView.OnQueryTextListener;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link org.lastrix.collagemaker.app.UserListFragment.Listener} interface
 * to handle interaction events.
 */
public class UserListFragment extends Fragment implements AdapterView.OnItemClickListener, UserSearchTask.Listener {
    public final static boolean LOG_ALL = BuildConfig.LOG_ALL;
    public final static String LOG_TAG = UserListFragment.class.getSimpleName();

    private Listener mListener;
    private UserListViewAdapter mAdapter;
    private ListView mListView;
    private OnQueryTextListener mQueryTextListener;
    private UserSearchTask mSearchTask;
    private ProgressDialog mProgressDialog;


    public UserListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mQueryTextListener = new UserSearchOnQueryTextListener(this);
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
        mAdapter = new UserListViewAdapter(getActivity().getLayoutInflater());
        mListView.setOnItemClickListener(this);
        mListView.setAdapter(mAdapter);

        mProgressDialog = new ProgressDialog(getActivity());
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setTitle(R.string.title_loading);
        mProgressDialog.setCancelable(true);
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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mListView = null;
        mAdapter = null;
        mProgressDialog = null;
        mQueryTextListener = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mListener.onUserSelected((User) mAdapter.getItem(position));
    }

    OnQueryTextListener getQueryTextListener() {
        return mQueryTextListener;
    }

    @Override
    public void onSearchCompleted(List<User> users) {
        mAdapter.mUsers = users;
        mAdapter.notifyDataSetChanged();
        mSearchTask = null;
    }

    @Override
    public void onSearchFailed(Throwable e) {
        Log.e(LOG_TAG, "Failed to check user.", e);
        Toast.makeText(getActivity(), R.string.error_user_check_failed, Toast.LENGTH_LONG).show();
        mSearchTask = null;
    }

    public interface Listener {

        public void onUserSelected(User user);
    }

    private static class UserSearchOnQueryTextListener implements OnQueryTextListener {
        private UserListFragment mFragment;

        public UserSearchOnQueryTextListener(UserListFragment fragment) {
            mFragment = fragment;
        }

        @Override
        public boolean onQueryTextSubmit(String s) {
            if (mFragment.mSearchTask != null) {
                mFragment.mSearchTask.cancel(true);
                mFragment.mSearchTask = null;
            }
            mFragment.mSearchTask = new UserSearchTask(mFragment, mFragment.mProgressDialog);
            mFragment.mSearchTask.execute(s);
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


    private static class UserListViewAdapter extends BaseAdapter {
        private final LayoutInflater mInflater;
        private List<User> mUsers;

        private UserListViewAdapter(LayoutInflater inflater) {
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
            if (getItemViewType(position) == 0) {
                ViewHolder holder;
                if (convertView == null) {
                    convertView = mInflater.inflate(R.layout.list_item_user, null);
                    holder = new ViewHolder();
                    holder.photo = (ImageView) convertView.findViewById(R.id.photo);
                    holder.name = (TextView) convertView.findViewById(R.id.name);
                    holder.nick = (TextView) convertView.findViewById(R.id.nick);
                    holder.position = position;
                    holder.loaded = false;
                    convertView.setTag(holder);
                } else {
                    holder = (ViewHolder) convertView.getTag();
                }
                User user = mUsers.get(position);
                if (holder.position != position || !user.getPhotoUrl().equals(holder.url)) {
                    holder.loaded = false;
                }
                holder.position = position;

                //if data changed
                if (!holder.loaded) {
                    holder.loaded = true;

                    holder.name.setText(user.getName());
                    holder.nick.setText(user.getUsername());

                    //now load image
                    holder.url = user.getPhotoUrl();
                    ImageLoader loader = ImageLoader.getInstance();
                    loader.cancelDisplayTask(holder.photo);
                    loader.displayImage(holder.url, holder.photo);
                }
            } else {
                //no results found
                convertView = mInflater.inflate(android.R.layout.simple_list_item_1, null);
                TextView textView = (TextView) convertView;
                textView.setText(R.string.info_no_results);
            }
            return convertView;
        }

        @Override
        public int getItemViewType(int position) {
            if (mUsers.size() == 0) return 1;
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public boolean isEnabled(int position) {
            return mUsers.size() > 0;
        }

        private static class ViewHolder {
            int position;
            boolean loaded;
            String url;
            ImageView photo;
            TextView name;
            TextView nick;
        }
    }
}

package org.lastrix.collagemaker.app;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.nostra13.universalimageloader.core.ImageLoader;
import org.lastrix.collagemaker.app.api.PopularPhotosTask;
import org.lastrix.collagemaker.app.content.ContentHelper;
import org.lastrix.collagemaker.app.content.Photo;
import org.lastrix.collagemaker.app.content.User;

import java.util.Collections;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link UserPhotosFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class UserPhotosFragment extends Fragment implements AdapterView.OnItemClickListener, PopularPhotosTask.Listener {

    public static final String LOG_TAG = UserPhotosFragment.class.getSimpleName();
    public static final boolean LOG_ALL = BuildConfig.LOG_ALL;
    private User mUser;

    private GridView mGridView;
    private boolean mCanceled = true;
    private PhotoListViewAdapter mAdapter;
    private PopularPhotosTask mPopularPhotosTask;
    private ProgressDialog mProgressDialog;
    private SetupRunnable mSetupRunnable;


    public UserPhotosFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param user -- for whom images should be displayed.
     * @return A new instance of fragment UserPhotosFragment.
     */
    public static UserPhotosFragment newInstance(User user) {
        UserPhotosFragment fragment = new UserPhotosFragment();
        if (user != null) {
            fragment.setArguments(user.asBundle());
        }
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mUser = User.fromBundle(getArguments());
        } else {
            mUser = null;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_user_photos, container, false);
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mGridView = (GridView) view.findViewById(R.id.grid_view_photos);
        mGridView.setOnItemClickListener(this);
        mAdapter = new PhotoListViewAdapter(getActivity().getLayoutInflater());
        mGridView.setAdapter(mAdapter);

        mProgressDialog = new ProgressDialog(getActivity());
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setTitle(R.string.title_loading);
        mProgressDialog.setCancelable(true);

    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mPopularPhotosTask != null) {
            mPopularPhotosTask.cancel(true);
            mPopularPhotosTask = null;
        }
        mGridView.setOnItemClickListener(null);
        mGridView = null;
        mAdapter = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mCanceled && mUser != null) {
            load();
            mCanceled = false;
        }
    }

    private void load() {
        mCanceled = false;
        mPopularPhotosTask = new PopularPhotosTask(this, mProgressDialog, getActivity().getContentResolver());
        mPopularPhotosTask.execute(mUser);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mPopularPhotosTask != null) {
            mPopularPhotosTask.cancel(true);
            mPopularPhotosTask = null;
        }
        if ( mSetupRunnable != null ) {
            mGridView.removeCallbacks(mSetupRunnable);
            mSetupRunnable = null;
        }
        if ( mProgressDialog.isShowing() ){
            mProgressDialog.dismiss();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //TODO:
        Photo photo = mAdapter.mPhotos.get(position);
        photo.setChecked(!photo.isChecked());
        new UpdatePhotoTask(getActivity().getContentResolver()).execute(photo);
    }


    @Override
    public void onLoadingFailed(Throwable e) {
        Log.e(LOG_TAG, "Failed to load index", e);
        mPopularPhotosTask = null;
        Toast.makeText(getActivity(), R.string.error_index_loading_failed, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onLoadingCompleted(List<Photo> photos) {
        mAdapter.mPhotos = photos;
        mAdapter.notifyDataSetChanged();
        mPopularPhotosTask = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mSetupRunnable = new SetupRunnable();
            mGridView.postDelayed(mSetupRunnable, 100L);
        }
        //in pre honeycomb version this job belongs
        // to mAdapter and manual background setting.
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void resetSelection() {
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ) {
            final int size = mAdapter.getCount();
            for (int i = 0; i < size; i++) {
                mGridView.setItemChecked(i, false);
            }
        } else {
            for( Photo photo : mAdapter.mPhotos){
                photo.setChecked(false);
            }
            mAdapter.notifyDataSetChanged();
        }
    }

    private static class PhotoListViewAdapter extends BaseAdapter {
        private LayoutInflater mInflater;
        private List<Photo> mPhotos;

        private PhotoListViewAdapter(LayoutInflater mInflater) {
            this.mInflater = mInflater;
            mPhotos = Collections.emptyList();
        }

        @Override
        public int getCount() {
            return mPhotos.size();
        }

        @Override
        public Object getItem(int position) {
            return mPhotos.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = mInflater.inflate(R.layout.list_item_photo, null);
                holder.thumbnail = (ImageView) convertView.findViewById(R.id.thumbnail);
                holder.position = position;
                holder.loaded = false;
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            if (holder.position != position) {
                holder.loaded = false;
            }
            holder.position = position;

            if (!holder.loaded) {
                holder.loaded = true;
                holder.thumbnail.setImageResource(android.R.drawable.progress_indeterminate_horizontal);
                Photo photo = mPhotos.get(position);

                ImageLoader loader = ImageLoader.getInstance();
                loader.cancelDisplayTask(holder.thumbnail);
                loader.displayImage(photo.getThumbnailUrl(), holder.thumbnail);

                //TODO: background setting in pre honeycomb versions
            }
            return convertView;
        }

        private static class ViewHolder {
            int position;
            boolean loaded;
            ImageView thumbnail;
        }
    }

    private static class UpdatePhotoTask extends AsyncTask<Photo, Void, Boolean> {

        private ContentResolver mContentResolver;

        private UpdatePhotoTask(ContentResolver contentResolver) {
            this.mContentResolver = contentResolver;
        }

        @Override
        protected Boolean doInBackground(Photo... params) {
            ContentValues values = new ContentValues(1);
            for( Photo photo : params){
                values.clear();
                values.put(Photo.COLUMN_CHECKED, photo.isChecked()?1:0);
                mContentResolver.update(ContentHelper.getPhotoUri(photo),
                        values,
                        String.format("%s = ?", Photo.COLUMN_ID),
                        new String[]{Long.toString(photo.getId())}
                );
            }
            return null;
        }
    }

    private class SetupRunnable implements Runnable {
        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        @Override
        public void run() {
            final int size = mAdapter.mPhotos.size();
            for (int i = 0; i < size; i++) {
                if (mAdapter.mPhotos.get(i).isChecked()) {
                    mGridView.setItemChecked(i, true);
                }
            }
            mSetupRunnable = null;
        }
    }
}

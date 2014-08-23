package org.lastrix.collagemaker.app;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import org.lastrix.collagemaker.app.api.*;
import org.lastrix.collagemaker.app.gfx.GFXSurfaceView;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by lastrix on 8/21/14.
 */
public class PhotoPickerActivity extends ActionBarActivity implements AdapterView.OnItemClickListener, GFXSurfaceView.ScreenShotListener {
    public static final String LOG_TAG = PhotoPickerActivity.class.getSimpleName();
    private static final boolean LOG_ALL = false;
    public static final float ZOOM_STEP = 0.5f;
    private User mUser;
    private Subscription mIndexSubscription = null;
    private ProgressDialog mProgressDialog;
    private PhotoPickerListViewAdapter mAdapter;
    private ListView mList;
    private GFXSurfaceView mGfxSurfaceView;
    private List<String> mImages;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_picker);
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            mUser = User.fromBundle(bundle);
        } else {
            //no user means bad hands. User is required.
            // that's why we stop now.
            return;
        }

        mAdapter = new PhotoPickerListViewAdapter(Collections.EMPTY_LIST, getLayoutInflater());

        mGfxSurfaceView = (GFXSurfaceView) findViewById(R.id.preview);

        mList = (ListView) findViewById(R.id.photos);
        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(this);

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
                .build();
        ImageLoader.getInstance().init(config);

        loadIndex();
    }

    private void loadIndex() {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setMessage(getResources().getString(R.string.message_downloading_popular_photos_list));
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();

        // load index into bitmap manager
        mIndexSubscription = AndroidObservable.bindActivity(this, Observable.create(new PopularPhotoRequest(mUser)))
                .subscribeOn(Schedulers.io())
                .subscribe(new PhotosSubscriber(this, mProgressDialog));
    }

    void setThumbnails(List<String> list){
        mAdapter.setThumbnails(new ArrayList<String>(list));
    }

    public void setImages(List<String> mImages) {
        mGfxSurfaceView.setImages(new ArrayList<String>(mImages));
    }

    @Override
    protected void onResume() {
        super.onResume();
        //anyway we can't do it.
        if (mUser == null) finish();
        ImageLoader.getInstance().resume();
        mGfxSurfaceView.onResume();
        mGfxSurfaceView.setScreenShotListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ImageLoader.getInstance().pause();
        mGfxSurfaceView.onPause();
        //to prevent possible activity leak
        mGfxSurfaceView.setScreenShotListener(null);
    }

    @Override
    protected void onStop() {
        super.onStop();
        ImageLoader.getInstance().stop();
    }

    @Override
    protected void onDestroy() {
        mGfxSurfaceView.onDestroy();
        mList.setOnItemClickListener(null);
        if (mIndexSubscription != null) {
            mIndexSubscription.unsubscribe();
            mIndexSubscription = null;
        }

        mAdapter = null;
        mProgressDialog = null;
        mUser = null;
        mList = null;
        mGfxSurfaceView = null;
        ImageLoader.getInstance().destroy();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_photo_picker, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.preview:
                //do preview
                mGfxSurfaceView.takeScreen();
                return true;

            case R.id.select_all:
                setCheckedStateAll(true);
                return true;

            case R.id.select_none:
                setCheckedStateAll(false);
                return true;

            case R.id.zoom_in:
                mGfxSurfaceView.zoom(-ZOOM_STEP);
                return true;

            case R.id.zoom_out:
                mGfxSurfaceView.zoom(ZOOM_STEP);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setCheckedStateAll(boolean state) {
        final int size = mAdapter.getCount();
        for (int i = 0; i < size; i++) {
            mList.setItemChecked(i, state);
            mGfxSurfaceView.setChecked(i, state);
        }
        mGfxSurfaceView.requestRender();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //TODO: rework it
        final boolean newState = mList.isItemChecked(position);
        view.setSelected(newState);
        mList.setItemChecked(position, newState);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            PhotoPickerListViewAdapter.ViewHolder holder = (PhotoPickerListViewAdapter.ViewHolder) view.getTag();
            holder.checked.setChecked(newState);
        }
        mGfxSurfaceView.setChecked(position, newState);
    }

    @Override
    public void screenShot(final Bitmap bmp) {
        //TODO: pass bmp somewhere else
    }

    /**
     * ListViewAdapter to display image thumbnails in side ListView.
     */
    private static class PhotoPickerListViewAdapter extends BaseAdapter {

        private final LayoutInflater mInflater;
        private List<String> mThumbnails;

        private PhotoPickerListViewAdapter(List<String> thumbnails, LayoutInflater inflater) {
            this.mThumbnails = thumbnails;
            this.mInflater = inflater;
        }

        @Override
        public int getCount() {
            return mThumbnails.size();
        }

        @Override
        public Object getItem(int position) {
            return mThumbnails.get(position);
        }

        /**
         * Set thumbnails list and notify about data set change.
         * @param mThumbnails -- new list of urls
         */
        void setThumbnails(List<String> mThumbnails) {
            this.mThumbnails = mThumbnails;
            notifyDataSetChanged();
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.list_item_photo_picker, null);
                holder = new ViewHolder();
                holder.image = (ImageView) convertView.findViewById(R.id.image);

                //HONEYCOMB workaround
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                    holder.checked = (CheckBox) convertView.findViewById(R.id.checked);
                }

                convertView.setTag(holder);

            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            if ( holder.position != position){
                holder.loading = false;
            }

            holder.position = position;
            //get thumbnail
            if ( !holder.loading ) {
                // forbid future calls on same item
                holder.loading = true;
                ImageLoader.getInstance()
                        .loadImage(
                                mThumbnails.get(position),
                                new ThumbnailLoadingListener(position, holder)
                        );
            }

            //honeycomb workaround
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                holder.checked.setChecked(convertView.isSelected());
            }
            return convertView;
        }

        private static class ViewHolder {
            int position;
            ImageView image;
            CheckBox checked;
            public boolean loading;
        }

        private static class ThumbnailLoadingListener extends SimpleImageLoadingListener {
            private final ViewHolder mHolder;
            private int mPosition;

            public ThumbnailLoadingListener(int position, ViewHolder holder) {
                this.mHolder = holder;
                this.mPosition = position;
            }

            @Override
            public void onLoadingStarted(String imageUri, View view) {
                if ( mHolder.position == mPosition ){
                    mHolder.image.setImageResource(android.R.drawable.ic_menu_report_image);
                }
            }

            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                if ( mHolder.position == mPosition) {
                    mHolder.image.setImageBitmap(loadedImage);
                }
            }
        }
    }

    /**
     * Listens {@link PopularPhotoRequest} observable
     * and on success passes all photos to Bitmap manager.
     */
    static class PhotosSubscriber extends Subscriber<Photo> {
        private PhotoPickerActivity mActivity;
        private ProgressDialog mProgressDialog;
        private List<String> mThumbnails = new LinkedList<String>();
        private List<String> mImages = new LinkedList<String>();


        public PhotosSubscriber(PhotoPickerActivity activity, ProgressDialog progressDialog) {
            this.mActivity = activity;
            mProgressDialog = progressDialog;
        }

        @Override
        public void onCompleted() {
            //index loading complete!
            mActivity.setThumbnails(mThumbnails);
            mActivity.setImages(mImages);
            mActivity = null;

            mProgressDialog.dismiss();
            mProgressDialog = null;
            unsubscribe();
        }

        @Override
        public void onError(Throwable e) {
            Log.w(LOG_TAG, "Failed to load photos.", e);
    //        Toast.makeText(this, R.string.error_index_loading_failed, Toast.LENGTH_LONG).show();
            mActivity = null;

            mProgressDialog.dismiss();
            mProgressDialog = null;
            unsubscribe();
        }

        @Override
        public void onNext(Photo photo) {
            mThumbnails.add(photo.getThumbnailUrl());
            mImages.add(photo.getStandardResolutionUrl());
        }
    }

    /**
     * Observable for fetching user popular image urls.
     * Created by lastrix on 8/21/14.
     */
    static class PopularPhotoRequest implements Observable.OnSubscribe<Photo> {
        private final User mUser;

        public PopularPhotoRequest(User user) {
            mUser = user;
        }

        @Override
        public void call(Subscriber<? super Photo> subscriber) {
            //temporary holder
            Photos photos;

            //pagination
            String nextUrl = null;
            try {
                do {
                    //check we're needed
                    if ( subscriber.isUnsubscribed() ) return;

                    //do the lengthy job
                    photos = InstagramApi.popularPhotos(mUser, nextUrl);
                    if (photos == null) {
                        //no images
                        if ( !subscriber.isUnsubscribed()) {
                            subscriber.onCompleted();
                        }
                        return;
                    }
                    nextUrl = photos.getNextUrl();

                    //check again
                    if ( subscriber.isUnsubscribed() ) return;

                    //pass to subscriber
                    for (Photo photo : photos.getPhotos()) {
                        subscriber.onNext(photo);
                    }
                } while (nextUrl != null && !subscriber.isUnsubscribed());

                //notify complete
                if ( !subscriber.isUnsubscribed()) {
                    subscriber.onCompleted();
                }

            } catch (InstagramApiException e) {
                //notify error
                if ( !subscriber.isUnsubscribed()) {
                    subscriber.onError(e);
                }
            }
        }
    }
}

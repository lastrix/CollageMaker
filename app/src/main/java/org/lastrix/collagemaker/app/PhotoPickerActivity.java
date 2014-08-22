package org.lastrix.collagemaker.app;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.*;
import android.widget.*;
import org.lastrix.collagemaker.app.api.Photos;
import org.lastrix.collagemaker.app.api.User;
import org.lastrix.collagemaker.app.gfx.GFXSurfaceView;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.android.schedulers.AndroidSchedulers;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lastrix on 8/21/14.
 */
public class PhotoPickerActivity extends ActionBarActivity implements AdapterView.OnItemClickListener, GFXSurfaceView.ScreenShotListener {
    public static final String LOG_TAG = PhotoPickerActivity.class.getSimpleName();
    private static final boolean LOG_ALL = false;
    public static final float ZOOM_STEP = 0.5f;
    private User mUser;
    private Subscription mSubscription = null;
    private ProgressDialog mProgressDialog;
    private PhotoPickerListViewAdapter mAdapter;
    private List<BitmapDrawable> mDrawables;
    private List<Photos.Photo> mPhotos;
    private ListView mList;
    private GFXSurfaceView mGfxSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_picker);
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            mUser = User.fromBundle(bundle);
            Log.d(PhotoPickerActivity.class.getSimpleName(), "User=" + mUser);
        } else {
            //no user means bad hands. User is required.
            // that's why we stop now.
            return;
        }

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setMessage(getResources().getString(R.string.message_downloading_popular_photos));

        mPhotos = new ArrayList<Photos.Photo>();


        mProgressDialog.show();
        //for background tasks
        mSubscription = AndroidObservable.bindActivity(this, Observable.create(new PopularPhotoRequest(mUser)))
                .subscribeOn(AppScheduler.getScheduler())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new PhotosSubscriber(this, mPhotos));

        mDrawables = new ArrayList<BitmapDrawable>();

        mList = (ListView) findViewById(R.id.photos);
        mAdapter = new PhotoPickerListViewAdapter(mDrawables, getLayoutInflater(), mList);
        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(this);

        mGfxSurfaceView = (GFXSurfaceView) findViewById(R.id.preview);
        mGfxSurfaceView.setScreenShotListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //anyway we can't do it.
        if (mUser == null) finish();
        mGfxSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGfxSurfaceView.onPause();
    }

    @Override
    protected void onDestroy() {
        mGfxSurfaceView.setScreenShotListener(null);
        mGfxSurfaceView.onDestroy();
        mPhotos.clear();
        for (Drawable drawable : mDrawables) {
            //don't trust gc, recycle them manually.
            if (drawable instanceof BitmapDrawable) {
                ((BitmapDrawable) drawable).getBitmap().recycle();
            }
        }
        mDrawables.clear();
        if (mSubscription != null) {
            mSubscription.unsubscribe();
            mSubscription = null;
        }
        mList.setOnItemClickListener(null);

        mDrawables = null;
        mPhotos = null;
        mAdapter = null;
        mProgressDialog = null;
        mUser = null;
        mList = null;
        mGfxSurfaceView = null;
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
                setCheckedState(true);
                return true;

            case R.id.select_none:
                setCheckedState(false);
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

    private void setCheckedState(boolean state) {
        final int size = mAdapter.getCount();
        for (int i = 0; i < size; i++) {
            mList.setItemChecked(i, state);
            mGfxSurfaceView.setChecked(i, state);
        }
        mGfxSurfaceView.requestRender();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final boolean newState = mList.isItemChecked(position);
        view.setSelected(newState);
        mList.setItemChecked(position, newState);
        //TODO: something really strange here. I'm starting to hate damn pre-honeycomb!
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.HONEYCOMB) {
            ViewHolder holder = (ViewHolder) view.getTag();
            holder.checked.setChecked(newState);
        }
        mGfxSurfaceView.setChecked(position, newState);
    }

    @Override
    public void screenShot(final Bitmap bmp) {
        mDrawables.add(new BitmapDrawable(getResources(), bmp));
        mGfxSurfaceView.add(bmp);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    private static class PhotosSubscriber extends Subscriber<Photos.Photo> {
        private final WeakReference<PhotoPickerActivity> mContext;
        private final List<String> mUrls = new ArrayList<String>();
        private final List<Photos.Photo> mPhotos;

        private PhotosSubscriber(PhotoPickerActivity context, List<Photos.Photo> photos) {
            mPhotos = photos;
            this.mContext = new WeakReference<PhotoPickerActivity>(context);
        }

        @Override
        public void onCompleted() {
            PhotoPickerActivity activity = mContext.get();
            activity.mSubscription.unsubscribe();
            activity.mSubscription = null;

            //now we may start another subscriber, which should download each
            //image
            activity.mSubscription = AndroidObservable
                    .bindActivity(activity, Observable.create(
                            new ImageLoadingRequest(mUrls)
                    ))
                    .subscribeOn(AppScheduler.getScheduler())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new ImageSubscriber(mContext, activity.mDrawables));
        }

        @Override
        public void onError(Throwable e) {
            PhotoPickerActivity activity = mContext.get();
            activity.mSubscription.unsubscribe();
            activity.mSubscription = null;
            activity.mProgressDialog.dismiss();
            Toast.makeText(activity, R.string.error_index_loading_failed, Toast.LENGTH_LONG).show();
            if (LOG_ALL) {
                Log.w(LOG_TAG, "Failed to load photos.", e);
            }
        }

        @Override
        public void onNext(Photos.Photo photo) {
            mUrls.add(photo.getStandardResolutionUrl());
            mPhotos.add(photo);
        }

    }

    private static class ImageSubscriber extends Subscriber<Bitmap> {
        private final WeakReference<PhotoPickerActivity> mContext;
        private final List<BitmapDrawable> mDrawables;

        public ImageSubscriber(WeakReference<PhotoPickerActivity> mContext, List<BitmapDrawable> mDrawables) {
            this.mContext = mContext;
            this.mDrawables = mDrawables;
        }

        @Override
        public void onCompleted() {
            PhotoPickerActivity activity = mContext.get();
            activity.mSubscription.unsubscribe();
            activity.mSubscription = null;
            activity.mProgressDialog.dismiss();

            activity.mAdapter.notifyDataSetChanged();
            //that's all now, c u!

            activity.mGfxSurfaceView.requestRender();
        }

        @Override
        public void onError(Throwable e) {
            PhotoPickerActivity activity = mContext.get();
            activity.mSubscription.unsubscribe();
            activity.mSubscription = null;
            activity.mProgressDialog.dismiss();
            Toast.makeText(activity, R.string.error_image_loading_failed, Toast.LENGTH_LONG).show();
            if (LOG_ALL) {
                Log.w(LOG_TAG, "Failed to load image.", e);
            }
        }

        @Override
        public void onNext(Bitmap o) {
            PhotoPickerActivity activity = mContext.get();
            activity.mGfxSurfaceView.add(o);
            mDrawables.add(new BitmapDrawable(mContext.get().getResources(), o));
        }
    }


    private static class PhotoPickerListViewAdapter extends BaseAdapter {

        private final List<BitmapDrawable> mDrawables;
        private final LayoutInflater mInflater;
        private final WeakReference<ListView> mList;

        private PhotoPickerListViewAdapter(List<BitmapDrawable> drawables, LayoutInflater inflater, ListView list) {
            this.mDrawables = drawables;
            mInflater = inflater;
            this.mList = new WeakReference<ListView>(list);
        }

        @Override
        public int getCount() {
            return mDrawables.size();
        }

        @Override
        public Object getItem(int position) {
            return mDrawables.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.list_item_photo_picker, null);
                holder = new ViewHolder();
                holder.image = (ImageView) convertView.findViewById(R.id.image);
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.HONEYCOMB) {
                    holder.checked = (CheckBox) convertView.findViewById(R.id.checked);
                }
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.image.setImageDrawable((Drawable) getItem(position));
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.HONEYCOMB) {
                holder.checked.setChecked(mList.get().isItemChecked(position));
            }

            return convertView;
        }

    }

    private static class ViewHolder {
        ImageView image;
        CheckBox checked;
    }
}

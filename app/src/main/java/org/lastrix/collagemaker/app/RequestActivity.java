package org.lastrix.collagemaker.app;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import org.lastrix.collagemaker.app.api.User;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;


/**
 * Main activity. Performs user searching and navigation
 * to image selection activity {@link org.lastrix.collagemaker.app.PhotoPickerActivity}.
 */
public class RequestActivity extends ActionBarActivity {

    public static final String LOG_TAG = RequestActivity.class.getSimpleName();
    private final static boolean LOG_ALL = false;

    private EditText mEtUser;
    private Subscription mSubscription = null;
    private Button mBtnCollage;
    private List<User> mUsersFound = new LinkedList<User>();
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request);
        this.mEtUser = (EditText) findViewById(R.id.user);
        this.mBtnCollage = (Button) findViewById(R.id.collage);

        //setup progress dialog
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setMessage(getResources().getString(R.string.message_searching_for_user));
        mProgressDialog.setCancelable(false);
    }


    public void onCollageClick(View view) {
        //actually this should can't happen, but who knows?
        if (mSubscription != null) {
            throw new RuntimeException("Previous call was not finished.");
        }

        //do checks
        final String user = mEtUser.getText().toString();
        if (TextUtils.isEmpty(user)) {
            Toast.makeText(this, R.string.error_no_username_specified, Toast.LENGTH_LONG).show();
            return;
        }

        if (LOG_ALL) {
            Log.v(LOG_TAG, "Checks started for user " + user);
        }

        mBtnCollage.setEnabled(false);

        //clean up state
        mUsersFound.clear();
        mProgressDialog.show();

        //run fetch
        mSubscription = AndroidObservable.bindActivity(this, Observable.create(new UserSearchRequest(user)))
                .subscribeOn(Schedulers.io())
                .subscribe(new UserSearchSubscriber(this));
    }

    @Override
    protected void onDestroy() {
        //there is no reason to wait termination.
        mUsersFound.clear();
        if (mSubscription != null) {
            mSubscription.unsubscribe();
            mSubscription = null;
        }

        //nullify links
        mUsersFound = null;
        mEtUser = null;
        mProgressDialog = null;
        mBtnCollage = null;
        super.onDestroy();
    }

    /**
     * Processes user search request results.
     * See {@link org.lastrix.collagemaker.app.UserSearchRequest} .
     */
    private static class UserSearchSubscriber extends Subscriber<User> {
        private final RequestActivity mActivity;

        private UserSearchSubscriber(RequestActivity context) {
            this.mActivity = context;
        }

        @Override
        public void onCompleted() {
            if (LOG_ALL) {
                Log.d(LOG_TAG, "User check completed!");
            }
            // make state clean
            cleanup();

            //now we may decide what to do with results
            final int size = mActivity.mUsersFound.size();
            if (size == 0) {
                noResult();
            } else if (size == 1) {
                singleResult();
            } else {
                multipleResult();
            }
        }

        private void multipleResult() {
            if (LOG_ALL) {
                Log.v(LOG_TAG, "Starting activity to select user from returned list.");
            }
            //start new user pick activity
            //TODO: add user picker activity instead of error toast.
            Toast.makeText(mActivity, R.string.error_user_selection_not_implemented, Toast.LENGTH_LONG).show();
        }

        private void noResult() {
            if (LOG_ALL) {
                Log.i(LOG_TAG, "No users found!");
            }
            //just notify user about such sad results.
            Toast.makeText(mActivity, R.string.error_no_user_found, Toast.LENGTH_LONG).show();
        }

        private void singleResult() {
            //do image fetch
            if (LOG_ALL) {
                Log.v(LOG_TAG, "User found, starting photo picker.");
            }
            //launch PhotoPickerActivity
            final Bundle bundle = mActivity.mUsersFound.get(0).asBundle();
            final Intent intent = new Intent(mActivity, PhotoPickerActivity.class);
            intent.putExtras(bundle);
            mActivity.startActivity(intent);
        }

        @Override
        public void onError(Throwable e) {
            if (LOG_ALL) {
                Log.d(LOG_TAG, "Failed to check user.", e);
            }

            //anyway cleanup our state.
            cleanup();
            //and make user know about our problems
            Toast.makeText(mActivity, R.string.error_user_check_failed, Toast.LENGTH_LONG).show();
        }

        private void cleanup() {
            mActivity.mProgressDialog.dismiss();

            mActivity.mSubscription.unsubscribe();
            mActivity.mSubscription = null;

            mActivity.mBtnCollage.setEnabled(true);
        }

        @Override
        public void onNext(User user) {
            mActivity.mUsersFound.add(user);
        }
    }
}

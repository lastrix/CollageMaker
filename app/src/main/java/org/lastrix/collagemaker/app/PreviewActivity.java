package org.lastrix.collagemaker.app;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import com.nostra13.universalimageloader.core.ImageLoader;


public class PreviewActivity extends ActionBarActivity {

    public static final String FILE_URL = "FILE_URL";
    public static final String LOG_TAG = PreviewActivity.class.getSimpleName();
    public static final boolean LOG_ALL = true;
    private String mUri;
    private ImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Show the Up button in the action bar.
            getActionBar().setDisplayHomeAsUpEnabled(true);
        } else {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Bundle bundle = getIntent().getExtras();
        mUri = bundle.getString(FILE_URL);
        if (mUri == null) {
            finish();
        } else {
            mImageView = (ImageView) findViewById(R.id.preview);
            mImageView.setImageBitmap(ImageLoader.getInstance().getMemoryCache().get(mUri));
        }
        if (LOG_ALL) {
            Log.v(LOG_TAG, "onCreate()");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_preview, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_apply:
                //send mail
//                final Intent intent = new Intent(Intent.ACTION_SEND);
//                intent.setType("image/png");
//                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(mUri)));
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                startActivity(Intent.createChooser(intent, getString(R.string.send_email_using)));
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

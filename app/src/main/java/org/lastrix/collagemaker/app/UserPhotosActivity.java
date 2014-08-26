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
import org.lastrix.collagemaker.app.content.ResetSelectionTask;
import org.lastrix.collagemaker.app.content.User;


public class UserPhotosActivity extends ActionBarActivity {


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_photos);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Show the Up button in the action bar.
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }  else {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        User user = User.fromBundle(getIntent().getExtras());
        if (user == null) {
            finish();
            return;
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, UserPhotosFragment.newInstance(user))
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_user_photo, menu);
        return true;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_collage:
                startActivity(new Intent(this, CollageActivity.class));
                return true;

            case R.id.action_reset:
                new ResetSelectionTask(getContentResolver()).execute();
                return true;

            default:
                return false;
        }
    }
}

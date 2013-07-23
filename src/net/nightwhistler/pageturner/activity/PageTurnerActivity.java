package net.nightwhistler.pageturner.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.github.rtyley.android.sherlock.roboguice.activity.RoboSherlockFragmentActivity;
import com.google.inject.Inject;
import com.limecreativelabs.sherlocksupport.ActionBarDrawerToggleCompat;
import net.nightwhistler.pageturner.Configuration;
import net.nightwhistler.pageturner.PageTurner;
import net.nightwhistler.pageturner.R;
import roboguice.RoboGuice;

/**
 * Created with IntelliJ IDEA.
 * User: alex
 * Date: 7/14/13
 * Time: 10:14 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class PageTurnerActivity extends RoboSherlockFragmentActivity
        implements AdapterView.OnItemClickListener {

    private DrawerLayout mDrawer;
    private ListView mDrawerOptions;
    private ActionBarDrawerToggleCompat mToggle;

    private ArrayAdapter<String> adapter;

    private CharSequence originalTitle;

    @Inject
    private Configuration config;

    @Override
    protected final void onCreate(Bundle savedInstanceState) {

        Configuration config = RoboGuice.getInjector(this).getInstance(Configuration.class);
        PageTurner.changeLanguageSetting(this, config);

        setTheme( getTheme(config) );
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        super.onCreate(savedInstanceState);

        setContentView(getMainLayoutResource());

        mDrawerOptions = (ListView) findViewById(R.id.left_drawer);
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawer.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener

        // enable ActionBar app icon to behave as action to toggle nav drawer
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        initDrawerItems();
        mDrawerOptions.setOnItemClickListener(this);

        mToggle = new ActionBarDrawerToggleCompat(this, mDrawer, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {
            public void onDrawerClosed(View view) {
                PageTurnerActivity.this.onDrawerClosed(view);
            }

            public void onDrawerOpened(View drawerView) {
                PageTurnerActivity.this.onDrawerOpened(drawerView);
            }
        };


        mToggle.setDrawerIndicatorEnabled(true);
        mDrawer.setDrawerListener(mToggle);
        mDrawer.setDrawerLockMode( DrawerLayout.LOCK_MODE_LOCKED_CLOSED );

        onCreatePageTurnerActivity(savedInstanceState);
    }

    private void initDrawerItems() {
        if ( mDrawerOptions != null ) {
            mDrawerOptions.setAdapter( new ArrayAdapter<String>( this, R.layout.drawer_list_item, getMenuItems(config) ));
        }
    }

    protected abstract int getMainLayoutResource();

    protected void onCreatePageTurnerActivity( Bundle savedInstanceState ) {

    }

    protected void beforeLaunchActivity() {

    }

    protected int getTheme( Configuration config ) {
        return config.getTheme();
    }

    protected String[] getMenuItems( Configuration config ) {
        return array(config.getLastReadTitle(), getString(R.string.library), getString(R.string.download));
    }

    public void onDrawerClosed(View view) {
        getSupportActionBar().setTitle(originalTitle);
        invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
    }

    public void onDrawerOpened(View drawerView) {

        this.originalTitle = getSupportActionBar().getTitle();

        getSupportActionBar().setTitle(R.string.app_name);
        invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        initDrawerItems();
        return super.onPrepareOptionsMenu(menu);    //To change body of overridden methods use File | Settings | File Templates.
    }

    protected static String[] array( String... items ) {
        return items;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setSupportProgressBarIndeterminate(true);
        setSupportProgressBarIndeterminateVisibility(false);

        mToggle.syncState();
    }

    public void launchActivity(Class<?> activityClass) {
        Intent intent = new Intent(this, activityClass);

        beforeLaunchActivity();

        startActivity(intent);
        finish();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // I think there's a bug in mToggle.onOptionsItemSelected, because it always returns false.
        // The item id testing is a fix.
        if (mToggle.onOptionsItemSelected(item) || item.getItemId() == android.R.id.home) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

        if ( i == 0 ) {
            launchActivity( ReadingActivity.class );
        } else if ( i == 1 ) {
            launchActivity( LibraryActivity.class );
        } else if ( i == 2 ) {
            launchActivity( CatalogActivity.class );
        }

        mDrawer.closeDrawers();
    }



}

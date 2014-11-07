package net.nightwhistler.pageturner.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.github.rtyley.android.sherlock.roboguice.activity.RoboSherlockFragmentActivity;
import com.google.inject.Inject;
import com.limecreativelabs.sherlocksupport.ActionBarDrawerToggleCompat;
import jedi.option.Option;
import net.nightwhistler.pageturner.Configuration;
import net.nightwhistler.pageturner.PageTurner;
import net.nightwhistler.pageturner.R;
import net.nightwhistler.pageturner.view.NavigationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import roboguice.RoboGuice;
import roboguice.inject.InjectView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static net.nightwhistler.pageturner.CollectionUtil.listElement;

/**
 * Superclass for all PageTurner activity classes.
 */
public abstract class PageTurnerActivity extends RoboSherlockFragmentActivity {

    @InjectView(R.id.drawer_layout)
    private DrawerLayout mDrawer;

    @InjectView(R.id.left_drawer)
    private ExpandableListView mDrawerOptions;

    private ActionBarDrawerToggleCompat mToggle;

    private NavigationAdapter adapter;

    private CharSequence originalTitle;

    private boolean drawerIsOpen;

    @Inject
    private Configuration config;

    private static Logger LOG = LoggerFactory.getLogger("PageTurnerActivity");

    @Override
    protected final void onCreate(Bundle savedInstanceState) {

        Configuration config = RoboGuice.getInjector(this).getInstance(Configuration.class);
        PageTurner.changeLanguageSetting(this, config);

        setTheme( getTheme(config) );
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        super.onCreate(savedInstanceState);

        setContentView(getMainLayoutResource());

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawer.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener

        // enable ActionBar app icon to behave as action to toggle nav drawer
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        initDrawerItems( mDrawerOptions );

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

        onCreatePageTurnerActivity(savedInstanceState);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        int action = event.getAction();
        int keyCode = event.getKeyCode();

        if ( action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK && isDrawerOpen() ) {
            closeNavigationDrawer();
            return true;
        }

        return super.dispatchKeyEvent(event);
    }

    protected void closeNavigationDrawer() {
        mDrawer.closeDrawers();
    }

    protected NavigationAdapter getAdapter() {
        return this.adapter;
    }

    protected void initDrawerItems( ExpandableListView expandableListView ) {
        if ( expandableListView != null ) {

            this.adapter = new NavigationAdapter( this, getMenuItems(config) );

            expandableListView.setAdapter( this.adapter );
            expandableListView.setOnGroupClickListener(this::onGroupClick);
            expandableListView.setOnChildClickListener(this::onChildClick);
            expandableListView.setOnItemLongClickListener( this::onItemLongClick );

            expandableListView.setGroupIndicator(null);
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

    protected List<NavigationCallback> getMenuItems( Configuration config ) {

        List result = new ArrayList<>();

        result.add( navigate(getString(R.string.open_library), LibraryActivity.class) );
        result.add( navigate(getString(R.string.download), CatalogActivity.class));

        if ( new File(config.getLastOpenedFile()).exists() ) {
            result.add( navigate(config.getLastReadTitle(), ReadingActivity.class));
        }

        result.add( new NavigationCallback(getString(R.string.prefs)).setOnClick(this::startPreferences));

        return result;
    }

    protected void startPreferences() {
        Intent intent = new Intent(this, PageTurnerPrefsActivity.class);
        startActivity(intent);
    }

    protected NavigationCallback navigate( String title, Class<? extends PageTurnerActivity> classToStart ) {
        return new NavigationCallback( title ).setOnClick(
                () -> launchActivity(classToStart));
    }

    public void onDrawerClosed(View view) {
        this.drawerIsOpen = false;
        getSupportActionBar().setTitle(originalTitle);
        invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
    }

    public void onDrawerOpened(View drawerView) {

        this.drawerIsOpen = true;
        this.originalTitle = getSupportActionBar().getTitle();

        getSupportActionBar().setTitle(R.string.app_name);
        invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
    }

    protected boolean isDrawerOpen() {
        return drawerIsOpen;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        initDrawerItems( mDrawerOptions );
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setSupportProgressBarIndeterminate(true);
        setSupportProgressBarIndeterminateVisibility(false);

        mToggle.syncState();
    }

    public void launchActivity(Class<? extends  PageTurnerActivity> activityClass) {
        Intent intent = new Intent(this, activityClass);

        beforeLaunchActivity();

        config.setLastActivity( activityClass );

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

    protected boolean onGroupClick(ExpandableListView expandableListView, View view, int i, long l) {

        Option<Boolean> group = getAdapter().findGroup(i).map( g -> {
            if ( g.hasChildren() ) {
                return false; //Let the superclass handle it and expand the group
            } else {
                g.onClick();
                closeNavigationDrawer();
                return true;
            }
        });

        return group.getOrElse( false );
    }

    protected boolean onChildClick(ExpandableListView expandableListView, View view, int i, int i2, long l) {

        Option<NavigationCallback> childItem = getAdapter().findChild( i, i2 );
        childItem.forEach(item -> item.onClick());

        closeNavigationDrawer();
        return false;
    }

    protected boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

        LOG.debug("Got long click");

        if (ExpandableListView.getPackedPositionType(id) == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            int groupPosition = ExpandableListView.getPackedPositionGroup(id);
            int childPosition = getAdapter().getIndexForChildId( groupPosition,
                    ExpandableListView.getPackedPositionChild(id) );

            Option<NavigationCallback> childItem = getAdapter().findChild( groupPosition, childPosition );

            LOG.debug("Long-click on " + groupPosition + ", " + childPosition );
            LOG.debug("Child-item: " + childItem );

            childItem.match(
                    i -> i.onLongClick(),
                    () -> LOG.error("Could not get child-item for " + position + " and id " + id )
            );

            closeNavigationDrawer();
            return true;
        }

        return false;
    }

}

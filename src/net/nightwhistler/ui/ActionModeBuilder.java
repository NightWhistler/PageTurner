package net.nightwhistler.ui;


import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Builder class for ActionMode callbacks.
 *
 * Allows a more functional way of building action modes.
 *
 * @author Alex Kuiper
 */
public class ActionModeBuilder {

    public static interface ActionModeAction<A> {
        boolean perform( ActionMode actionMode, A item );
    }

    public static interface DestroyActionModeCommand {
        void perform( ActionMode mode );
    }

    private ActionModeAction<Menu> createAction;
    private ActionModeAction<Menu> prepareAction;
    private ActionModeAction<MenuItem> clickedAction;
    private DestroyActionModeCommand destroyAction;

    /**
     * Creates a blank builder
     */
    public ActionModeBuilder() {

    }

    /**
     * Creates an ActionModeBuilder which uses the given string
     * as a title for the ActionMode
     * @param title
     */
    public ActionModeBuilder( String title ) {
        setTitle( title );
    }

    /**
     * Creates an ActionModeBuilder which uses the given
     * resource id for the title.
     *
     * @param titleResourceId
     */
    public ActionModeBuilder( int titleResourceId ) {
        setTitle( titleResourceId );
    }

    public ActionModeBuilder setTitle( String title ) {
        this.prepareAction = (actionMode, menu ) -> {
            actionMode.setTitle( title );
            return true;
        };

        return this;
    }

    public ActionModeBuilder setTitle( int titleResourceId ) {
        this.prepareAction = (actionMode, menu ) -> {
            actionMode.setTitle( titleResourceId );
            return true;
        };

        return this;
    }

    public ActionModeBuilder setOnActionItemClickedAction(ActionModeAction<MenuItem> clickedAction) {
        this.clickedAction = clickedAction;
        return this;
    }

    public ActionModeBuilder setOnCreateAction(ActionModeAction<Menu> createAction) {
        this.createAction = createAction;
        return this;
    }

    public ActionModeBuilder setOnDestroyAction(DestroyActionModeCommand destroyAction) {
        this.destroyAction = destroyAction;
        return this;
    }

    public ActionModeBuilder setOnPrepareAction(ActionModeAction<Menu> prepareAction) {
        this.prepareAction = prepareAction;
        return this;
    }

    public void build( ActionBarActivity sherlockActivity ) {
        sherlockActivity.startSupportActionMode( new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                if ( createAction != null ) {
                    return createAction.perform(actionMode, menu);
                }
                return false;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                if ( prepareAction != null ) {
                    return prepareAction.perform( actionMode, menu );
                }
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                if ( clickedAction != null ) {
                    return clickedAction.perform( actionMode, menuItem );
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {
                if ( destroyAction != null ) {
                    destroyAction.perform(actionMode);
                }
            }
        });
    }


}

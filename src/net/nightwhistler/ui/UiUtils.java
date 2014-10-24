package net.nightwhistler.ui;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;

/**
 * Created by alex on 10/11/14.
 */
public class UiUtils {

    public static interface Operation<A> {
        void thenDo(A arg);
    }

    public static interface Action {
        void perform();
    }

    public static Operation<Action> onMenuPress( Menu menu, int elementName ) {
        return onMenuPress( menu.findItem(elementName) );
    }

    public static Operation<Action> onMenuPress( MenuItem menuItem ) {
        return action -> menuItem.setOnMenuItemClickListener(item -> {
            action.perform();
            return true;
        });
    }

    public static Operation<Action> onMenuPress( android.view.MenuItem menuItem ) {
        return action -> menuItem.setOnMenuItemClickListener( item -> {
            action.perform();
            return true;
        });
    }

    public static SearchView.OnQueryTextListener onQuery( Operation<String> op ) {
        return new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                op.thenDo(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                return  false;
            }
        };
    }

    public static MenuItem.OnActionExpandListener onActionExpandListener( Action onExpand, Action onCollapse ) {
        return new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem menuItem) {
                onExpand.perform();
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                onCollapse.perform();
                return false;
            }
        };
    }

}

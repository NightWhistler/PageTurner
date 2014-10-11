package net.nightwhistler.pageturner;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.google.common.base.Function;

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
        return action -> menuItem.setOnMenuItemClickListener( item -> {
            action.perform();
            return true;
        });
    }

}

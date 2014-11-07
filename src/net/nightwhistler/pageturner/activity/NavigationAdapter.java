package net.nightwhistler.pageturner.activity;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import jedi.option.Option;
import net.nightwhistler.pageturner.PlatformUtil;
import net.nightwhistler.pageturner.R;
import net.nightwhistler.pageturner.view.NavigationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static jedi.functional.FunctionalPrimitives.*;
import static net.nightwhistler.pageturner.CollectionUtil.listElement;
import static net.nightwhistler.ui.UiUtils.getImageView;
import static net.nightwhistler.ui.UiUtils.getTextView;

/**
 * Created with IntelliJ IDEA.
 * User: alex
 * Date: 8/12/13
 * Time: 11:10 AM
 * To change this template use File | Settings | File Templates.
 */
public class NavigationAdapter extends BaseExpandableListAdapter {

    private List<NavigationCallback> items;
    private Context context;

    private static final Logger LOG = LoggerFactory.getLogger("NavigationAdapter");

    public NavigationAdapter( Context context, List<NavigationCallback> items ) {
        this.context = context;
        this.items = new ArrayList<>(items);

        LOG.debug( "Initialized new adapter with " + items.size() + " items");
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean b, View view, ViewGroup viewGroup) {

        LOG.debug("Looking for child-view for group " + groupPosition + " and child " + childPosition );

        View layout = view;

        if ( layout == null ) {
            layout = PlatformUtil.getLayoutInflater(context).inflate(
                    R.layout.drawer_list_subitem, null );
        }

        TextView titleTextView = (TextView) layout.findViewById( R.id.itemText );
        TextView subTitleView = (TextView) layout.findViewById( R.id.subtitleText );

        Option<NavigationCallback> childItem = findChild( groupPosition, childPosition );

        childItem.match( c -> {
            titleTextView.setText( c.getTitle() );
            subTitleView.setText( c.getSubtitle() );
        }, () -> LOG.error("Could not find childView with index " + childPosition + " for group " + groupPosition ));

        return layout;

    }

    @Override
    public int getGroupCount() {
        LOG.debug("Returning group count: " + items.size() );
        return items.size();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public boolean isChildSelectable(int i, int i2) {
        return true;
    }

    @Override
    public View getGroupView(int i, boolean isExpanded, View view, ViewGroup viewGroup) {

        View layout;

        if ( view != null ) {
            layout = view;
        } else {
            layout = PlatformUtil.getLayoutInflater(context).inflate(
                    R.layout.drawer_list_item, null );
        }

        Option<TextView> textView = getTextView( layout, R.id.groupName );
        Option<ImageView> indicator = getImageView( layout, R.id.explist_indicator );

        indicator.forEach( ind -> {
            if ( getChildrenCount( i ) == 0 ) {
                ind.setVisibility( View.INVISIBLE );
            } else {
                ind.setVisibility( View.VISIBLE );
                ind.setImageResource( isExpanded ? R.drawable.arrowhead_up : R.drawable.arrowhead_down );
            }
        });

        NavigationCallback item = items.get(i);

        LOG.debug( "Getting view for index " + i + " with title "
                + item.getTitle() + " and subtitle " + item.getSubtitle() );
        LOG.debug( "Is expanded? " + isExpanded );

        textView.match(
                t -> t.setText( item.getTitle() ),
                () -> LOG.error("View for title not found!")
        );

        return layout;
    }

    @Override
    public int getChildrenCount(int i) {
        Option<Integer> count = listElement( items, i ).map(NavigationCallback::getChildCount);

        LOG.debug( "ChildrenCount for element " + i + " is " + count );

        return count.getOrElse(0);
    }

    public Option<NavigationCallback> findGroup( int i ) {
        return listElement(items, i);
    }

    public Option<NavigationCallback> findChild(int i, int i2) {

        Option<Option<NavigationCallback>> childItem =
                listElement(items, i).map(g -> g.getChild(i2));

        return headOption( flatten(childItem) );
    }

    @Override
    public Object getChild(int i, int i1) {
        return findChild(i,i1).unsafeGet();
    }

    @Override
    public NavigationCallback getGroup(int i) {
        return findGroup(i).unsafeGet();
    }

    @Override
    public long getGroupId(int i) {
        return i;
    }

    @Override
    public long getChildId(int i, int i2) {
        return i * 100 + i2;
    }

    public int getIndexForChildId( int groupIndex, int childId ) {
        return childId - groupIndex * 100;
    }

}

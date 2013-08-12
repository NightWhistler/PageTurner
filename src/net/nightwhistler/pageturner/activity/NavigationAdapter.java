package net.nightwhistler.pageturner.activity;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;
import net.nightwhistler.pageturner.PlatformUtil;
import net.nightwhistler.pageturner.R;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: alex
 * Date: 8/12/13
 * Time: 11:10 AM
 * To change this template use File | Settings | File Templates.
 */
public class NavigationAdapter extends BaseExpandableListAdapter {

    private List<String> groups;
    private Map<Integer, List<String>> children;

    private Context context;

    public NavigationAdapter( Context context, String... items ) {
        this.context = context;
        this.groups = Arrays.asList(items);
        this.children = new HashMap<Integer, List<String>>();
    }

    public void setChildren( int groupId, List<String> childItems ) {
        this.children.put( groupId, childItems );
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean b, View view, ViewGroup viewGroup) {

        TextView textView;

        if ( view != null ) {
            textView = (TextView) view;
        } else {
            textView = (TextView) PlatformUtil.getLayoutInflater(context).inflate(
                    R.layout.drawer_list_subitem, null );
        }

        if ( children.containsKey( groupPosition ) ) {
            List<String> childStrings = children.get( groupPosition );

            textView.setText( childStrings.get( childPosition ) );
        }

        return textView;
    }

    @Override
    public int getGroupCount() {
        return groups.size();
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
    public View getGroupView(int i, boolean b, View view, ViewGroup viewGroup) {

        TextView textView;

        if ( view != null ) {
            textView = (TextView) view;
        } else {
            textView = (TextView) PlatformUtil.getLayoutInflater(context).inflate(
                    R.layout.drawer_list_item, null );
        }

        textView.setText(groups.get(i));

        return textView;
    }

    @Override
    public int getChildrenCount(int i) {
        if ( children.containsKey(i) ) {
            return children.get(i).size();
        }

        return 0;
    }

    @Override
    public Object getChild(int i, int i2) {

        if ( children.containsKey(i) ) {
            return children.get(i);
        }

        return null;
    }

    @Override
    public Object getGroup(int i) {
        return groups.get( i );
    }

    @Override
    public long getGroupId(int i) {
        return i;
    }

    @Override
    public long getChildId(int i, int i2) {
        return i * 100 + i2;
    }


}

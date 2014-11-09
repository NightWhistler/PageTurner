package net.nightwhistler.pageturner.view;

import jedi.option.Option;
import net.nightwhistler.ui.TreeNode;
import net.nightwhistler.ui.UiUtils;

import java.util.ArrayList;
import java.util.List;

import static net.nightwhistler.pageturner.CollectionUtil.listElement;

/**
 * Created with IntelliJ IDEA.
 * User: alex
 * Date: 9/1/13
 * Time: 9:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class NavigationCallback extends TreeNode<NavigationCallback> {

    private String title;
    private String subtitle;

    private UiUtils.Action onClickAction;
    private UiUtils.Action onLongClickAction;

    private List<NavigationCallback> children = new ArrayList<>();

    public NavigationCallback( String title ) {
        this( title, "" );
    }

    public NavigationCallback( String title, String subtitle ) {
        this.title = title;
        this.subtitle = subtitle;
    }

    public NavigationCallback( String title, String subtitle, UiUtils.Action onClickAction ) {
        this.title = title;
        this.subtitle = subtitle;
        this.onClickAction = onClickAction;
    }

    public NavigationCallback setOnClick(UiUtils.Action onClickAction) {
        this.onClickAction = onClickAction;
        return this;
    }

    public NavigationCallback setOnLongClick(UiUtils.Action onLongClickAction) {
        this.onLongClickAction = onLongClickAction;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void onClick() {
        if ( this.onClickAction != null ) {
            this.onClickAction.perform();
        }
    }

    public void onLongClick() {
        if ( this.onLongClickAction != null ) {
            this.onLongClickAction.perform();
        }
    }


}

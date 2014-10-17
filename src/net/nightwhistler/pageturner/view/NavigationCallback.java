package net.nightwhistler.pageturner.view;

import net.nightwhistler.pageturner.UiUtils;

/**
 * Created with IntelliJ IDEA.
 * User: alex
 * Date: 9/1/13
 * Time: 9:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class NavigationCallback {

    private String title;
    private String subtitle;

    private UiUtils.Action onClickAction;
    private UiUtils.Action onLongClickAction;

    public NavigationCallback( String title, String subtitle ) {
        this.title = title;
        this.subtitle = subtitle;
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

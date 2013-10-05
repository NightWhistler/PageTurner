package net.nightwhistler.pageturner.view;

/**
 * Created with IntelliJ IDEA.
 * User: alex
 * Date: 9/1/13
 * Time: 9:12 PM
 * To change this template use File | Settings | File Templates.
 */
public interface NavigationCallback {

    String getTitle();

    String getSubtitle();

    void onClick();

    void onLongClick();

}

package net.nightwhistler.pageturner.view.bookview;

import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;

/**
 * Created with IntelliJ IDEA.
 * User: alex
 * Date: 5/12/13
 * Time: 2:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class WrappingActionMode extends ActionMode {

    private ActionMode wrappedActionMode;

    public WrappingActionMode(ActionMode wrappedActionMode) {
        this.wrappedActionMode = wrappedActionMode;
    }

    @Override
    public void setTitle(CharSequence charSequence) {
       wrappedActionMode.setTitle(charSequence);
    }

    @Override
    public void setTitle(int i) {
        wrappedActionMode.setTitle(i);
    }

    @Override
    public void setSubtitle(CharSequence charSequence) {
        wrappedActionMode.setSubtitle(charSequence);
    }

    @Override
    public void setSubtitle(int i) {
        wrappedActionMode.setSubtitle(i);
    }

    @Override
    public void setCustomView(View view) {
        wrappedActionMode.setCustomView(view);
    }

    @Override
    public void invalidate() {
        wrappedActionMode.invalidate();
    }

    @Override
    public void finish() {
        //wrappedActionMode.finish();
    }

    public void close() {
        wrappedActionMode.finish();
    }

    @Override
    public Menu getMenu() {
        return wrappedActionMode.getMenu();
    }

    @Override
    public CharSequence getTitle() {
        return wrappedActionMode.getTitle();
    }

    @Override
    public CharSequence getSubtitle() {
        return wrappedActionMode.getSubtitle();
    }

    @Override
    public View getCustomView() {
        return wrappedActionMode.getCustomView();
    }

    @Override
    public MenuInflater getMenuInflater() {
        return wrappedActionMode.getMenuInflater();
    }
}

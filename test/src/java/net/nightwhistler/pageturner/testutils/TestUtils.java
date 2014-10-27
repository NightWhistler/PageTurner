package net.nightwhistler.pageturner.testutils;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

public class TestUtils {
	
	public static void startFragment( Fragment fragment ) {
        startFragment(new FragmentActivity(), fragment);
    }
	
	public static void startFragment( FragmentActivity activity, Fragment fragment ) {
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add( fragment, null );
        fragmentTransaction.commit();
	}

}

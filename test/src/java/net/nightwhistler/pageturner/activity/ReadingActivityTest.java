package net.nightwhistler.pageturner.activity;

import android.annotation.TargetApi;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import com.google.inject.AbstractModule;
import com.google.inject.util.Modules;
import jedi.option.Option;
import net.nightwhistler.pageturner.scheduling.TaskQueue;
import net.nightwhistler.pageturner.testutils.SynchronousTaskQueue;
import net.nightwhistler.ui.UiUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import roboguice.RoboGuice;
import roboguice.inject.RoboInjector;

import static jedi.option.Options.none;

/**
 * Created by alex on 10/26/14.
 */
@TargetApi(16)
@Config(emulateSdk = 16, reportSdk = 10)
@RunWith(RobolectricTestRunner.class)
public class ReadingActivityTest {

  //  @Inject
//    private NetworkUtil networkUtil;

    //private ErrorDialog errorDialog = mock(ErrorDialog.class, Mockito.RETURNS_DEEP_STUBS);

    private FragmentActivity activity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        activity = Robolectric.buildActivity(CatalogActivity.class).create().get();
        RoboGuice.setBaseApplicationInjector(Robolectric.application, RoboGuice.DEFAULT_STAGE,
                Modules.override(RoboGuice.newDefaultRoboModule(Robolectric.application)).with(new MyTestModule()));

        RoboInjector injector = RoboGuice.getInjector(activity);
        injector.injectMembersWithoutViews(this);
    }

    @Test
    public void testNothing() {

    }


    private class MyTestModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(FragmentManager.class).toInstance(activity.getSupportFragmentManager());
            bind(TaskQueue.class).to(SynchronousTaskQueue.class);

        }
    }
}




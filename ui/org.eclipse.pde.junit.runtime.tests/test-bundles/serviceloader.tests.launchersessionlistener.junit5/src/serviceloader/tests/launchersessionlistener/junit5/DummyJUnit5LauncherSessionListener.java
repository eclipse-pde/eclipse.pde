package serviceloader.tests.launchersessionlistener.junit5;

import org.junit.platform.launcher.LauncherSessionListener;

public class DummyJUnit5LauncherSessionListener implements LauncherSessionListener {

	{
		System.out.println(String.format("Called %s", getClass().getSimpleName()));
	}

}

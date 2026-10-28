package serviceloader.tests.launchersessionlistener.junit6;

import org.junit.platform.launcher.LauncherSessionListener;

public class DummyJUnit6LauncherSessionListener implements LauncherSessionListener {

	{
		System.out.println(String.format("Called %s", getClass().getSimpleName()));
	}

}

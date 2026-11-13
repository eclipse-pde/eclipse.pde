package serviceloader.tests.launcherdiscoverylistener.junit5;

import org.junit.platform.launcher.LauncherDiscoveryListener;

public class DummyJUnit5LauncherDiscoveryListener implements LauncherDiscoveryListener {

	{
		System.out.println(String.format("Called %s", getClass().getSimpleName()));
	}

}

package serviceloader.tests.launcherdiscoverylistener.junit6;

import org.junit.platform.launcher.LauncherDiscoveryListener;

public class DummyJUnit6LauncherDiscoveryListener implements LauncherDiscoveryListener {

	{
		System.out.println(String.format("Called %s", getClass().getSimpleName()));
	}

}

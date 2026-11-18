package serviceloader.tests.testexecutionlistener.junit5;

import org.junit.platform.launcher.TestExecutionListener;

public class DummyJUnit5TestExecutionListener implements TestExecutionListener {

	{
		System.out.println(String.format("Called %s", getClass().getSimpleName()));
	}

}

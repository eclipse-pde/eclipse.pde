package serviceloader.tests.testexecutionlistener.junit6;

import org.junit.platform.launcher.TestExecutionListener;

public class DummyJUnit6TestExecutionListener implements TestExecutionListener {

	{
		System.out.println(String.format("Called %s", getClass().getSimpleName()));
	}

}

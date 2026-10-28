package serviceloader.tests.launcherinterceptor.junit5;

import org.junit.platform.launcher.LauncherInterceptor;

public class DummyJUnit5LauncherInterceptor implements LauncherInterceptor {

	{
		System.out.println(String.format("Called %s", getClass().getSimpleName()));
	}

	@Override
	public <T> T intercept(Invocation<T> invocation) {
		return invocation.proceed();
	}

	@Override
	public void close() {
	}

}

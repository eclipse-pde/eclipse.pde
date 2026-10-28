package serviceloader.tests.launcherinterceptor.junit6;

import org.junit.platform.launcher.LauncherInterceptor;

public class DummyJUnit6LauncherInterceptor implements LauncherInterceptor {

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

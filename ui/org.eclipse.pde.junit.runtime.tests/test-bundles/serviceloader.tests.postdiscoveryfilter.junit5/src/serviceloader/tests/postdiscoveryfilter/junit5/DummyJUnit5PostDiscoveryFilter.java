package serviceloader.tests.postdiscoveryfilter.junit5;

import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.launcher.PostDiscoveryFilter;

public class DummyJUnit5PostDiscoveryFilter implements PostDiscoveryFilter {

	{
		System.out.println(String.format("Called %s", getClass().getSimpleName()));
	}

	@Override
	public FilterResult apply(TestDescriptor object) {
		return FilterResult.included("");
	}
}

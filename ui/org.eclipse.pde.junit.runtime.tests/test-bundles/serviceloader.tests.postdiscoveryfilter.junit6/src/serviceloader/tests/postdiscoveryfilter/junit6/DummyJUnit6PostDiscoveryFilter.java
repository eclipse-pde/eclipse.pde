package serviceloader.tests.postdiscoveryfilter.junit6;
import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.launcher.PostDiscoveryFilter;

public class DummyJUnit6PostDiscoveryFilter implements PostDiscoveryFilter {

	{
		System.out.println(String.format("Called %s", getClass().getSimpleName()));
	}

	@Override
	public FilterResult apply(TestDescriptor object) {
		return FilterResult.included("");
	}
}

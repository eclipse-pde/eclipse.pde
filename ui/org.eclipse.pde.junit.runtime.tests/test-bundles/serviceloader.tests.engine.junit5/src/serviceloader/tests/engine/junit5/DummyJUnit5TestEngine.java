package serviceloader.tests.engine.junit5;

import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.junit.platform.engine.support.hierarchical.EngineExecutionContext;
import org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine;

public class DummyJUnit5TestEngine extends HierarchicalTestEngine<EngineExecutionContext> {

	{
		System.out.println(String.format("Called %s", getClass().getSimpleName()));
	}

	@Override
	public String getId() {
		return DummyJUnit5TestEngine.class.getSimpleName();
	}

	@Override
	public TestDescriptor discover(EngineDiscoveryRequest req, UniqueId uniqueId) {
		return new EngineDescriptor(uniqueId, DummyJUnit5TestEngine.class.getSimpleName());
	}

	@Override
	protected EngineExecutionContext createExecutionContext(ExecutionRequest request) {
		return new EngineExecutionContext() {
		};
	}
}

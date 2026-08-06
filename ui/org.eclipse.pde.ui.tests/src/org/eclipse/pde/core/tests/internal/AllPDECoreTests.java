package org.eclipse.pde.core.tests.internal;

import org.eclipse.pde.core.tests.internal.core.builders.DependencyLoopFinderTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({ //
	DependencyManagerTest.class, //
	DependencyLoopFinderTest.class, //
	StaleDependencyResolutionTest.class, //
	WorkspaceModelManagerTest.class, //
	WorkspaceProductModelManagerTest.class, //
})
public class AllPDECoreTests {
}

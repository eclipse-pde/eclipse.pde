package org.eclipse.pde.core.tests.internal;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({ //
	DependencyManagerTest.class, //
	WorkspaceModelManagerTest.class, //
	WorkspaceProductModelManagerTest.class, //
})
public class AllPDECoreTests {
}

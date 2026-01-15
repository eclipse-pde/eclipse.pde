package org.eclipse.pde.ds.internal.annotations.tests;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Test suite for all DS annotations tests.
 * <p>
 * Workspace setup is handled by {@link WorkspaceSetupExtension} which is
 * registered on {@link TestBase} that all test classes extend.
 * </p>
 */
@Suite
@SelectClasses({
	ManagedProjectTest.class,
	UnmanagedProjectTest.class,
	ErrorProjectTest.class,
	DefaultComponentTest.class,
	FullComponentTestV1_2.class,
	FullComponentTest.class,
	ExtendedReferenceMethodComponentTest.class,
	ExtendedLifeCycleMethodComponentTest.class,
})
public class AllDSAnnotationsTests {
	// Suite class - no setup needed here
	// Setup is handled by WorkspaceSetupExtension
}

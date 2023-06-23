package org.eclipse.pde.core.tests.internal;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ //
	XmlDocumentBuilderFactoryTest.class, //
	DependencyManagerTest.class, //
	XmlTransformerTest.class, //
	WorkspaceModelManagerTest.class, //
	WorkspaceProductModelManagerTest.class, //
	XmlParserFactoryTest.class //
})
public class AllPDECoreTests {
}

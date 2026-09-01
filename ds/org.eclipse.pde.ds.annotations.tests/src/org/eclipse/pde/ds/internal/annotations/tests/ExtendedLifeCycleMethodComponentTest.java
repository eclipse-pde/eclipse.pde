package org.eclipse.pde.ds.internal.annotations.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

@SuppressWarnings("restriction")
public class ExtendedLifeCycleMethodComponentTest extends AnnotationProcessorTest {

	@Override
	protected String getTestProjectName() {
		return "ds.annotations.test1";
	}

	@Override
	protected String getComponentDescriptorPath() {
		return "OSGI-INF/ds.annotations.test1.ExtendedLifeCycleMethodComponent.xml";
	}

	@Test
	public void componentNamespace() throws Exception {
		assertEquals(dsModel.getDSComponent().getXMLAttributeValue("xmlns:" + dsModel.getDSComponent().getNamespacePrefix()), "http://www.osgi.org/xmlns/scr/v1.3.0");
	}

	@Test
	public void componentActivateMethod() throws Exception {
		assertNull(dsModel.getDSComponent().getActivateMethod());
	}

	@Test
	public void componentModifiedMethod() throws Exception {
		assertEquals(dsModel.getDSComponent().getModifiedMethod(), "modified");
	}

	@Test
	public void componentDeactivateMethod() throws Exception {
		assertEquals(dsModel.getDSComponent().getDeactivateMethod(), "deactivate");
	}
}

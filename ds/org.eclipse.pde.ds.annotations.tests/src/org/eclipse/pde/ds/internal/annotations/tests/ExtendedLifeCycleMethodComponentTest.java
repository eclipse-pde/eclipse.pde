package org.eclipse.pde.ds.internal.annotations.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

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
		assertEquals("http://www.osgi.org/xmlns/scr/v1.3.0", dsModel.getDSComponent().getXMLAttributeValue("xmlns:" + dsModel.getDSComponent().getNamespacePrefix()));
	}

	@Test
	public void componentActivateMethod() throws Exception {
		assertEquals("activate", dsModel.getDSComponent().getActivateMethod());
	}

	@Test
	public void componentModifiedMethod() throws Exception {
		assertEquals("modified", dsModel.getDSComponent().getModifiedMethod());
	}

	@Test
	public void componentDeactivateMethod() throws Exception {
		assertEquals("deactivate", dsModel.getDSComponent().getDeactivateMethod());
	}
}

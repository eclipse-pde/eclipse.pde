package org.eclipse.pde.ds.internal.annotations.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.Executor;

import org.eclipse.pde.internal.ds.core.IDSReference;
import org.junit.Test;

@SuppressWarnings("restriction")
public class ExtendedReferenceMethodComponentTest extends AnnotationProcessorTest {

	@Override
	protected String getTestProjectName() {
		return "ds.annotations.test1";
	}

	@Override
	protected String getComponentDescriptorPath() {
		return "OSGI-INF/ds.annotations.test1.ExtendedReferenceMethodComponent.xml";
	}

	@Test
	public void componentNamespace() throws Exception {
		assertEquals("http://www.osgi.org/xmlns/scr/v1.3.0", dsModel.getDSComponent().getXMLAttributeValue("xmlns:" + dsModel.getDSComponent().getNamespacePrefix()));
	}

	@Test
	public void componentReference1() throws Exception {
		IDSReference[] references = dsModel.getDSComponent().getReferences();
		assertNotNull(references);
		assertEquals(1, references.length);
		IDSReference reference = references[0];
		assertEquals(Executor.class.getName(), reference.getReferenceInterface());
	}
}

package org.eclipse.pde.ds.internal.annotations.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.concurrent.Executor;

import org.eclipse.pde.internal.ds.core.IDSConstants;
import org.eclipse.pde.internal.ds.core.IDSImplementation;
import org.eclipse.pde.internal.ds.core.IDSProvide;
import org.eclipse.pde.internal.ds.core.IDSReference;
import org.eclipse.pde.internal.ds.core.IDSService;
import org.junit.Test;

@SuppressWarnings("restriction")
public class DefaultComponentTest extends AnnotationProcessorTest {

	private static final String DEFAULT_COMPONENT_CLASS = "ds.annotations.test1.DefaultComponent";

	@Override
	protected String getTestProjectName() {
		return "ds.annotations.test1";
	}

	@Override
	protected String getComponentDescriptorPath() {
		return "OSGI-INF/" + DEFAULT_COMPONENT_CLASS + ".xml";
	}

	@Test
	public void componentNamespace() throws Exception {
		assertEquals(IDSConstants.NAMESPACE, dsModel.getDSComponent().getXMLAttributeValue("xmlns:" + dsModel.getDSComponent().getNamespacePrefix()));
	}

	@Test
	public void componentName() throws Exception {
		assertEquals(DEFAULT_COMPONENT_CLASS, dsModel.getDSComponent().getName());
	}

	@Test
	public void componentImplementationClass() throws Exception {
		IDSImplementation impl = dsModel.getDSComponent().getImplementation();
		assertNotNull(impl);
		assertEquals(DEFAULT_COMPONENT_CLASS, impl.getClassName());
	}

	@Test
	public void componentServiceProviderInterface() throws Exception {
		IDSService service = dsModel.getDSComponent().getService();
		assertNotNull(service);
		assertFalse(service.getServiceFactory());
		IDSProvide[] provides = service.getProvidedServices();
		assertNotNull(provides);
		assertEquals(1, provides.length);
		assertEquals(Runnable.class.getName(), provides[0].getInterface());
	}

	@Test
	public void componentReference() throws Exception {
		IDSReference[] references = dsModel.getDSComponent().getReferences();
		assertNotNull(references);
		assertEquals(1, references.length);
		IDSReference reference = references[0];
		assertEquals(Executor.class.getName(), reference.getReferenceInterface());
		assertEquals(Executor.class.getSimpleName(), reference.getReferenceName());
		assertNull(reference.getReferencePolicy());
		assertNull(reference.getXMLAttributeValue("policy-option"));
		assertNull(reference.getReferenceCardinality());
		assertEquals("setExecutor", reference.getReferenceBind());
		assertEquals("unsetExecutor", reference.getReferenceUnbind());
		assertNull(reference.getReferenceTarget());
		assertNull(reference.getXMLAttributeValue("updated"));
	}
}

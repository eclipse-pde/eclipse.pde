package org.eclipse.pde.ds.internal.annotations.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;

import org.eclipse.pde.internal.ds.core.IDSConstants;
import org.eclipse.pde.internal.ds.core.IDSImplementation;
import org.eclipse.pde.internal.ds.core.IDSProperties;
import org.eclipse.pde.internal.ds.core.IDSProperty;
import org.eclipse.pde.internal.ds.core.IDSProvide;
import org.eclipse.pde.internal.ds.core.IDSReference;
import org.eclipse.pde.internal.ds.core.IDSService;
import org.junit.jupiter.api.Test;

@SuppressWarnings("restriction")
public class FullComponentTestV1_2 extends AnnotationProcessorTest {

	@Override
	protected String getTestProjectName() {
		return "ds.annotations.test1";
	}

	@Override
	protected String getComponentDescriptorPath() {
		return "OSGI-INF/test.fullComponent-v1_2.xml";
	}

	@Test
	public void componentNamespace() throws Exception {
		assertEquals(dsModel.getDSComponent().getXMLAttributeValue("xmlns:" + dsModel.getDSComponent().getNamespacePrefix()), "http://www.osgi.org/xmlns/scr/v1.2.0");
	}

	@Test
	public void componentName() throws Exception {
		assertEquals(dsModel.getDSComponent().getName(), "test.fullComponent-v1_2");
	}

	@Test
	public void componentConfigurationPid() throws Exception {
		assertEquals(dsModel.getDSComponent().getXMLAttributeValue("configuration-pid"), "test.configurationPid-v1_2");
	}

	@Test
	public void componentConfigurationPolicy() throws Exception {
		assertEquals(IDSConstants.VALUE_CONFIGURATION_POLICY_REQUIRE, dsModel.getDSComponent().getConfigurationPolicy());
	}

	@Test
	public void componentEnabled() throws Exception {
		assertFalse(dsModel.getDSComponent().getEnabled());
	}

	@Test
	public void componentImmediate() throws Exception {
		assertFalse(dsModel.getDSComponent().getImmediate());
	}

	@Test
	public void componentFactory() throws Exception {
		assertEquals(dsModel.getDSComponent().getFactory(), "test.componentFactory");
	}

	@Test
	public void componentActivateMethod() throws Exception {
		assertEquals(dsModel.getDSComponent().getActivateMethod(), "putAll");
	}

	@Test
	public void componentModifiedMethod() throws Exception {
		assertEquals(dsModel.getDSComponent().getModifiedMethod(), "putAll");
	}

	@Test
	public void componentDeactivateMethod() throws Exception {
		assertEquals(dsModel.getDSComponent().getDeactivateMethod(), "clear");
	}

	@Test
	public void componentImplementationClass() throws Exception {
		IDSImplementation impl = dsModel.getDSComponent().getImplementation();
		assertNotNull(impl);
		assertEquals(impl.getClassName(), "ds.annotations.test1.FullComponentV1_2");
	}

	@Test
	public void componentServiceProviderInterface() throws Exception {
		IDSService service = dsModel.getDSComponent().getService();
		assertNotNull(service);
		assertFalse(service.getServiceFactory());
		IDSProvide[] provides = service.getProvidedServices();
		assertNotNull(provides);
		assertEquals(1, provides.length);
		assertEquals(Map.class.getName(), provides[0].getInterface());
	}

	@Test
	public void componentProperties() throws Exception {
		IDSProperty[] properties = dsModel.getDSComponent().getPropertyElements();
		assertNotNull(properties);
		assertEquals(11, properties.length);
	}

	@Test
	public void componentPropertyImplicitString() throws Exception {
		IDSProperty[] properties = dsModel.getDSComponent().getPropertyElements();
		assertNotNull(properties);
		final int PROPERTY_INDEX = 0;
		assertTrue(properties.length > PROPERTY_INDEX);
		assertEquals(properties[PROPERTY_INDEX].getPropertyName(), "implicitStringProperty");
		assertEquals(properties[PROPERTY_INDEX].getPropertyValue(), "implicitStringValue");
		assertNull(properties[PROPERTY_INDEX].getPropertyType());
		assertNull(properties[PROPERTY_INDEX].getPropertyElemBody());
	}

	@Test
	public void componentPropertyExplicitString() throws Exception {
		IDSProperty[] properties = dsModel.getDSComponent().getPropertyElements();
		assertNotNull(properties);
		final int PROPERTY_INDEX = 1;
		assertTrue(properties.length > PROPERTY_INDEX);
		assertEquals(properties[PROPERTY_INDEX].getPropertyName(), "explicitStringProperty");
		assertEquals(properties[PROPERTY_INDEX].getPropertyValue(), "explicitStringValue");
		assertEquals(properties[PROPERTY_INDEX].getPropertyType(), "String");
		assertNull(properties[PROPERTY_INDEX].getPropertyElemBody());
	}

	@Test
	public void componentPropertyInteger() throws Exception {
		IDSProperty[] properties = dsModel.getDSComponent().getPropertyElements();
		assertNotNull(properties);
		final int PROPERTY_INDEX = 2;
		assertTrue(properties.length > PROPERTY_INDEX);
		assertEquals(properties[PROPERTY_INDEX].getPropertyName(), "integerProperty");
		assertEquals(properties[PROPERTY_INDEX].getPropertyValue(), "1");
		assertEquals(properties[PROPERTY_INDEX].getPropertyType(), "Integer");
		assertNull(properties[PROPERTY_INDEX].getPropertyElemBody());
	}

	@Test
	public void componentPropertyLong() throws Exception {
		IDSProperty[] properties = dsModel.getDSComponent().getPropertyElements();
		assertNotNull(properties);
		final int PROPERTY_INDEX = 3;
		assertTrue(properties.length > PROPERTY_INDEX);
		assertEquals(properties[PROPERTY_INDEX].getPropertyName(), "longProperty");
		assertEquals(properties[PROPERTY_INDEX].getPropertyValue(), "2");
		assertEquals(properties[PROPERTY_INDEX].getPropertyType(), "Long");
		assertNull(properties[PROPERTY_INDEX].getPropertyElemBody());
	}

	@Test
	public void componentPropertyShort() throws Exception {
		IDSProperty[] properties = dsModel.getDSComponent().getPropertyElements();
		assertNotNull(properties);
		final int PROPERTY_INDEX = 4;
		assertTrue(properties.length > PROPERTY_INDEX);
		assertEquals(properties[PROPERTY_INDEX].getPropertyName(), "shortProperty");
		assertEquals(properties[PROPERTY_INDEX].getPropertyValue(), "3");
		assertEquals(properties[PROPERTY_INDEX].getPropertyType(), "Short");
		assertNull(properties[PROPERTY_INDEX].getPropertyElemBody());
	}

	@Test
	public void componentPropertyByte() throws Exception {
		IDSProperty[] properties = dsModel.getDSComponent().getPropertyElements();
		assertNotNull(properties);
		final int PROPERTY_INDEX = 5;
		assertTrue(properties.length > PROPERTY_INDEX);
		assertEquals(properties[PROPERTY_INDEX].getPropertyName(), "byteProperty");
		assertEquals(properties[PROPERTY_INDEX].getPropertyValue(), "4");
		assertEquals(properties[PROPERTY_INDEX].getPropertyType(), "Byte");
		assertNull(properties[PROPERTY_INDEX].getPropertyElemBody());
	}

	@Test
	public void componentPropertyCharacter() throws Exception {
		IDSProperty[] properties = dsModel.getDSComponent().getPropertyElements();
		assertNotNull(properties);
		final int PROPERTY_INDEX = 6;
		assertTrue(properties.length > PROPERTY_INDEX);
		assertEquals(properties[PROPERTY_INDEX].getPropertyName(), "characterProperty");
		assertEquals(properties[PROPERTY_INDEX].getPropertyValue(), "53");
		assertEquals(properties[PROPERTY_INDEX].getPropertyType(), "Character");
		assertNull(properties[PROPERTY_INDEX].getPropertyElemBody());
	}

	@Test
	public void componentPropertyFloat() throws Exception {
		IDSProperty[] properties = dsModel.getDSComponent().getPropertyElements();
		assertNotNull(properties);
		final int PROPERTY_INDEX = 7;
		assertTrue(properties.length > PROPERTY_INDEX);
		assertEquals(properties[PROPERTY_INDEX].getPropertyName(), "floatProperty");
		assertEquals(properties[PROPERTY_INDEX].getPropertyValue(), "6.7");
		assertEquals(properties[PROPERTY_INDEX].getPropertyType(), "Float");
		assertNull(properties[PROPERTY_INDEX].getPropertyElemBody());
	}

	@Test
	public void componentPropertyDouble() throws Exception {
		IDSProperty[] properties = dsModel.getDSComponent().getPropertyElements();
		assertNotNull(properties);
		final int PROPERTY_INDEX = 8;
		assertTrue(properties.length > PROPERTY_INDEX);
		assertEquals(properties[PROPERTY_INDEX].getPropertyName(), "doubleProperty");
		assertEquals(properties[PROPERTY_INDEX].getPropertyValue(), "8.9");
		assertEquals(properties[PROPERTY_INDEX].getPropertyType(), "Double");
		assertNull(properties[PROPERTY_INDEX].getPropertyElemBody());
	}

	@Test
	public void componentPropertyImplicitStringArray() throws Exception {
		IDSProperty[] properties = dsModel.getDSComponent().getPropertyElements();
		assertNotNull(properties);
		final int PROPERTY_INDEX = 9;
		assertTrue(properties.length > PROPERTY_INDEX);
		assertEquals(properties[PROPERTY_INDEX].getPropertyName(), "implicitStringArrayProperty");
		assertNull(properties[PROPERTY_INDEX].getPropertyValue());
		assertNull(properties[PROPERTY_INDEX].getPropertyType());
		assertEquals(properties[PROPERTY_INDEX].getPropertyElemBody(), "implicitStringArrayValue1\nimplicitStringArrayValue2");
	}

	@Test
	public void componentPropertyExplicitStringArray() throws Exception {
		IDSProperty[] properties = dsModel.getDSComponent().getPropertyElements();
		assertNotNull(properties);
		final int PROPERTY_INDEX = 10;
		assertTrue(properties.length > PROPERTY_INDEX);
		assertEquals(properties[PROPERTY_INDEX].getPropertyName(), "explicitStringArrayProperty");
		assertNull(properties[PROPERTY_INDEX].getPropertyValue());
		assertEquals(properties[PROPERTY_INDEX].getPropertyType(), "String");
		assertEquals(properties[PROPERTY_INDEX].getPropertyElemBody(), "explicitStringArrayValue1\nexplicitStringArrayValue2\nexplicitStringArrayValue3");
	}

	@Test
	public void componentPropertyFiles() throws Exception {
		IDSProperties[] properties = dsModel.getDSComponent().getPropertiesElements();
		assertNotNull(properties);
		assertEquals(2, properties.length);
		assertEquals(properties[0].getEntry(), "/fullComponent1.properties");
		assertEquals(properties[1].getEntry(), "/fullComponent2.properties");
	}

	@Test
	public void componentReference1() throws Exception {
		IDSReference[] references = dsModel.getDSComponent().getReferences();
		assertNotNull(references);
		assertEquals(2, references.length);
		IDSReference reference = references[0];
		assertEquals(Set.class.getName(), reference.getReferenceInterface());
		assertEquals(reference.getReferenceName(), "Entries");
		assertEquals(IDSConstants.VALUE_REFERENCE_POLICY_DYNAMIC, reference.getReferencePolicy());
		assertEquals(reference.getXMLAttributeValue("policy-option"), "greedy");
		assertEquals(IDSConstants.VALUE_REFERENCE_CARDINALITY_ZERO_ONE, reference.getReferenceCardinality());
		assertEquals(reference.getReferenceBind(), "assignEntrySet");
		assertEquals(reference.getReferenceUnbind(), "unassignEntrySet");
		assertEquals(reference.getReferenceTarget(), "(!(component.name=test.fullComponent-v1_2))");
		assertEquals(reference.getXMLAttributeValue("updated"), "updateEntrySet");
	}

	@Test
	public void componentReference2() throws Exception {
		IDSReference[] references = dsModel.getDSComponent().getReferences();
		assertNotNull(references);
		assertEquals(2, references.length);
		IDSReference reference = references[1];
		assertEquals(reference.getReferenceInterface(), "java.util.Map$Entry");
		assertEquals(reference.getReferenceName(), "Entry");
		assertEquals(IDSConstants.VALUE_REFERENCE_POLICY_DYNAMIC, reference.getReferencePolicy());
		assertNull(reference.getXMLAttributeValue("policy-option"));
		assertEquals(IDSConstants.VALUE_REFERENCE_CARDINALITY_ZERO_N, reference.getReferenceCardinality());
		assertEquals(reference.getReferenceBind(), "addEntry");
		assertEquals(reference.getReferenceUnbind(), "removeEntry");
		assertNull(reference.getReferenceTarget());
		assertNull(reference.getXMLAttributeValue("updated"));
	}
}

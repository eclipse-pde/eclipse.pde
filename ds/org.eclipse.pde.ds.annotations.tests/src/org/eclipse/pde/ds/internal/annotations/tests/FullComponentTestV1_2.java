package org.eclipse.pde.ds.internal.annotations.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

import java.util.Map;
import java.util.Set;

import org.eclipse.pde.internal.ds.core.IDSConstants;
import org.eclipse.pde.internal.ds.core.IDSImplementation;
import org.eclipse.pde.internal.ds.core.IDSProperties;
import org.eclipse.pde.internal.ds.core.IDSProperty;
import org.eclipse.pde.internal.ds.core.IDSProvide;
import org.eclipse.pde.internal.ds.core.IDSReference;
import org.eclipse.pde.internal.ds.core.IDSService;
import org.junit.Test;

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
		assertEquals("http://www.osgi.org/xmlns/scr/v1.2.0", dsModel.getDSComponent().getXMLAttributeValue("xmlns:" + dsModel.getDSComponent().getNamespacePrefix()));
	}

	@Test
	public void componentName() throws Exception {
		assertEquals("test.fullComponent-v1_2", dsModel.getDSComponent().getName());
	}

	@Test
	public void componentConfigurationPid() throws Exception {
		assertEquals("test.configurationPid-v1_2", dsModel.getDSComponent().getXMLAttributeValue("configuration-pid"));
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
		assertEquals("test.componentFactory", dsModel.getDSComponent().getFactory());
	}

	@Test
	public void componentActivateMethod() throws Exception {
		assertEquals("putAll", dsModel.getDSComponent().getActivateMethod());
	}

	@Test
	public void componentModifiedMethod() throws Exception {
		assertEquals("putAll", dsModel.getDSComponent().getModifiedMethod());
	}

	@Test
	public void componentDeactivateMethod() throws Exception {
		assertEquals("clear", dsModel.getDSComponent().getDeactivateMethod());
	}

	@Test
	public void componentImplementationClass() throws Exception {
		IDSImplementation impl = dsModel.getDSComponent().getImplementation();
		assertNotNull(impl);
		assertEquals("ds.annotations.test1.FullComponentV1_2", impl.getClassName());
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
		assumeNotNull((Object) properties);
		final int PROPERTY_INDEX = 0;
		assumeTrue(properties.length > PROPERTY_INDEX);
		assertEquals("implicitStringProperty", properties[PROPERTY_INDEX].getPropertyName());
		assertEquals("implicitStringValue", properties[PROPERTY_INDEX].getPropertyValue());
		assertNull(properties[PROPERTY_INDEX].getPropertyType());
		assertNull(properties[PROPERTY_INDEX].getPropertyElemBody());
	}

	@Test
	public void componentPropertyExplicitString() throws Exception {
		IDSProperty[] properties = dsModel.getDSComponent().getPropertyElements();
		assumeNotNull((Object) properties);
		final int PROPERTY_INDEX = 1;
		assumeTrue(properties.length > PROPERTY_INDEX);
		assertEquals("explicitStringProperty", properties[PROPERTY_INDEX].getPropertyName());
		assertEquals("explicitStringValue", properties[PROPERTY_INDEX].getPropertyValue());
		assertEquals("String", properties[PROPERTY_INDEX].getPropertyType());
		assertNull(properties[PROPERTY_INDEX].getPropertyElemBody());
	}

	@Test
	public void componentPropertyInteger() throws Exception {
		IDSProperty[] properties = dsModel.getDSComponent().getPropertyElements();
		assumeNotNull((Object) properties);
		final int PROPERTY_INDEX = 2;
		assumeTrue(properties.length > PROPERTY_INDEX);
		assertEquals("integerProperty", properties[PROPERTY_INDEX].getPropertyName());
		assertEquals("1", properties[PROPERTY_INDEX].getPropertyValue());
		assertEquals("Integer", properties[PROPERTY_INDEX].getPropertyType());
		assertNull(properties[PROPERTY_INDEX].getPropertyElemBody());
	}

	@Test
	public void componentPropertyLong() throws Exception {
		IDSProperty[] properties = dsModel.getDSComponent().getPropertyElements();
		assumeNotNull((Object) properties);
		final int PROPERTY_INDEX = 3;
		assumeTrue(properties.length > PROPERTY_INDEX);
		assertEquals("longProperty", properties[PROPERTY_INDEX].getPropertyName());
		assertEquals("2", properties[PROPERTY_INDEX].getPropertyValue());
		assertEquals("Long", properties[PROPERTY_INDEX].getPropertyType());
		assertNull(properties[PROPERTY_INDEX].getPropertyElemBody());
	}

	@Test
	public void componentPropertyShort() throws Exception {
		IDSProperty[] properties = dsModel.getDSComponent().getPropertyElements();
		assumeNotNull((Object) properties);
		final int PROPERTY_INDEX = 4;
		assumeTrue(properties.length > PROPERTY_INDEX);
		assertEquals("shortProperty", properties[PROPERTY_INDEX].getPropertyName());
		assertEquals("3", properties[PROPERTY_INDEX].getPropertyValue());
		assertEquals("Short", properties[PROPERTY_INDEX].getPropertyType());
		assertNull(properties[PROPERTY_INDEX].getPropertyElemBody());
	}

	@Test
	public void componentPropertyByte() throws Exception {
		IDSProperty[] properties = dsModel.getDSComponent().getPropertyElements();
		assumeNotNull((Object) properties);
		final int PROPERTY_INDEX = 5;
		assumeTrue(properties.length > PROPERTY_INDEX);
		assertEquals("byteProperty", properties[PROPERTY_INDEX].getPropertyName());
		assertEquals("4", properties[PROPERTY_INDEX].getPropertyValue());
		assertEquals("Byte", properties[PROPERTY_INDEX].getPropertyType());
		assertNull(properties[PROPERTY_INDEX].getPropertyElemBody());
	}

	@Test
	public void componentPropertyCharacter() throws Exception {
		IDSProperty[] properties = dsModel.getDSComponent().getPropertyElements();
		assumeNotNull((Object) properties);
		final int PROPERTY_INDEX = 6;
		assumeTrue(properties.length > PROPERTY_INDEX);
		assertEquals("characterProperty", properties[PROPERTY_INDEX].getPropertyName());
		assertEquals("5", properties[PROPERTY_INDEX].getPropertyValue());
		assertEquals("Character", properties[PROPERTY_INDEX].getPropertyType());
		assertNull(properties[PROPERTY_INDEX].getPropertyElemBody());
	}

	@Test
	public void componentPropertyFloat() throws Exception {
		IDSProperty[] properties = dsModel.getDSComponent().getPropertyElements();
		assumeNotNull((Object) properties);
		final int PROPERTY_INDEX = 7;
		assumeTrue(properties.length > PROPERTY_INDEX);
		assertEquals("floatProperty", properties[PROPERTY_INDEX].getPropertyName());
		assertEquals("6.7", properties[PROPERTY_INDEX].getPropertyValue());
		assertEquals("Float", properties[PROPERTY_INDEX].getPropertyType());
		assertNull(properties[PROPERTY_INDEX].getPropertyElemBody());
	}

	@Test
	public void componentPropertyDouble() throws Exception {
		IDSProperty[] properties = dsModel.getDSComponent().getPropertyElements();
		assumeNotNull((Object) properties);
		final int PROPERTY_INDEX = 8;
		assumeTrue(properties.length > PROPERTY_INDEX);
		assertEquals("doubleProperty", properties[PROPERTY_INDEX].getPropertyName());
		assertEquals("8.9", properties[PROPERTY_INDEX].getPropertyValue());
		assertEquals("Double", properties[PROPERTY_INDEX].getPropertyType());
		assertNull(properties[PROPERTY_INDEX].getPropertyElemBody());
	}

	@Test
	public void componentPropertyImplicitStringArray() throws Exception {
		IDSProperty[] properties = dsModel.getDSComponent().getPropertyElements();
		assumeNotNull((Object) properties);
		final int PROPERTY_INDEX = 9;
		assumeTrue(properties.length > PROPERTY_INDEX);
		assertEquals("implicitStringArrayProperty", properties[PROPERTY_INDEX].getPropertyName());
		assertNull(properties[PROPERTY_INDEX].getPropertyValue());
		assertNull(properties[PROPERTY_INDEX].getPropertyType());
		assertEquals("implicitStringArrayValue1\nimplicitStringArrayValue2", properties[PROPERTY_INDEX].getPropertyElemBody());
	}

	@Test
	public void componentPropertyExplicitStringArray() throws Exception {
		IDSProperty[] properties = dsModel.getDSComponent().getPropertyElements();
		assumeNotNull((Object) properties);
		final int PROPERTY_INDEX = 10;
		assumeTrue(properties.length > PROPERTY_INDEX);
		assertEquals("explicitStringArrayProperty", properties[PROPERTY_INDEX].getPropertyName());
		assertNull(properties[PROPERTY_INDEX].getPropertyValue());
		assertEquals("String", properties[PROPERTY_INDEX].getPropertyType());
		assertEquals("explicitStringArrayValue1\nexplicitStringArrayValue2\nexplicitStringArrayValue3", properties[PROPERTY_INDEX].getPropertyElemBody());
	}

	@Test
	public void componentPropertyFiles() throws Exception {
		IDSProperties[] properties = dsModel.getDSComponent().getPropertiesElements();
		assertNotNull(properties);
		assertEquals(2, properties.length);
		assertEquals("/fullComponent1.properties", properties[0].getEntry());
		assertEquals("/fullComponent2.properties", properties[1].getEntry());
	}

	@Test
	public void componentReference1() throws Exception {
		IDSReference[] references = dsModel.getDSComponent().getReferences();
		assertNotNull(references);
		assertEquals(2, references.length);
		IDSReference reference = references[0];
		assertEquals(Set.class.getName(), reference.getReferenceInterface());
		assertEquals("Entries", reference.getReferenceName());
		assertEquals(IDSConstants.VALUE_REFERENCE_POLICY_DYNAMIC, reference.getReferencePolicy());
		assertEquals("greedy", reference.getXMLAttributeValue("policy-option"));
		assertEquals(IDSConstants.VALUE_REFERENCE_CARDINALITY_ZERO_ONE, reference.getReferenceCardinality());
		assertEquals("assignEntrySet", reference.getReferenceBind());
		assertEquals("unassignEntrySet", reference.getReferenceUnbind());
		assertEquals("(!(component.name=test.fullComponent-v1_2))", reference.getReferenceTarget());
		assertEquals("updateEntrySet", reference.getXMLAttributeValue("updated"));
	}

	@Test
	public void componentReference2() throws Exception {
		IDSReference[] references = dsModel.getDSComponent().getReferences();
		assertNotNull(references);
		assertEquals(2, references.length);
		IDSReference reference = references[1];
		assertEquals("java.util.Map$Entry", reference.getReferenceInterface());
		assertEquals("Entry", reference.getReferenceName());
		assertEquals(IDSConstants.VALUE_REFERENCE_POLICY_DYNAMIC, reference.getReferencePolicy());
		assertNull(reference.getXMLAttributeValue("policy-option"));
		assertEquals(IDSConstants.VALUE_REFERENCE_CARDINALITY_ZERO_N, reference.getReferenceCardinality());
		assertEquals("addEntry", reference.getReferenceBind());
		assertEquals("removeEntry", reference.getReferenceUnbind());
		assertNull(reference.getReferenceTarget());
		assertNull(reference.getXMLAttributeValue("updated"));
	}
}

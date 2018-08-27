/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ds.tests;

import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.ds.core.IDSComponent;
import org.eclipse.pde.internal.ds.core.IDSDocumentFactory;
import org.eclipse.pde.internal.ds.core.IDSImplementation;
import org.eclipse.pde.internal.ds.core.IDSProperties;
import org.eclipse.pde.internal.ds.core.IDSProperty;
import org.eclipse.pde.internal.ds.core.IDSReference;
import org.eclipse.pde.internal.ds.core.IDSService;

public class DSComponentTestCase extends AbstractDSModelTestCase {

	public void testAddDefaultComponent() {
		StringBuilder buffer = new StringBuilder();
		setXMLContents(buffer, LF);
		load();

		IDSComponent component = fModel.getDSComponent();
		assertNotNull(component);

		assertEquals(component.getName(), COMPONENT_NAME);
		assertEquals(component.getActivateMethod(), null);
		assertEquals(component.getDeactivateMethod(), null);
		assertEquals(component.getModifiedMethod(), null);

		assertTrue(component.getEnabled());
	}

	public void testAddComponentwithAllAttributes() {
		StringBuilder buffer = new StringBuilder();
		setXMLContents(buffer, LF);
		load();

		IDSComponent component = fModel.getDSComponent();
		assertNotNull(component);

		assertEquals(component.getName(), COMPONENT_NAME);
		component.setEnabled(false);
		component.setAttributeName("NewName");
		component.setFactory("NewFactory");
		component.setImmediate(true);
		component.setActivateMethod("start");
		component.setDeactivateMethod("stop");
		component.setModifiedeMethod("modified");

		assertFalse(component.getEnabled());
		assertEquals(component.getAttributeName(), "NewName");
		assertEquals(component.getFactory(), "NewFactory");
		assertTrue(component.getImmediate());
		assertEquals(component.getActivateMethod(), "start");
		assertEquals(component.getDeactivateMethod(), "stop");
		assertEquals(component.getModifiedMethod(), "modified");
	}

	public void testAddMoveRemoveChildrens() {
		StringBuilder buffer = new StringBuilder();
		setXMLContents(buffer, LF);
		load();

		IDSComponent component = fModel.getDSComponent();
		assertNotNull(component);

		IDSDocumentFactory factory = fModel.getFactory();

		IDSImplementation implementation = factory.createImplementation();
		implementation.setClassName("ImplementationClassName");
		component.setImplementation(implementation);

		IDSProperties properties = factory.createProperties();
		properties.setEntry("PropertiesEntry");
		component.addPropertiesElement(properties);

		IDSProperty property = factory.createProperty();
		property.setPropertyElemBody("Body Values");
		property.setPropertyType("java.lang.String");
		component.addPropertyElement(property);

		IDSService service = factory.createService();
		service.setServiceFactory(true);
		component.setService(service);

		IDSReference reference = factory.createReference();
		reference.setReferenceBind("methodBind");
		reference.setReferenceUnbind("methodUnBind");
		reference.setReferenceInterface("ReferenceInterface");
		reference.setReferenceName("ReferenceName");
		component.addReference(reference);

		IDocumentElementNode childAt4 = component.getChildAt(4);
		component.moveChildNode(reference, -1, true);

		IDocumentElementNode childAt3 = component.getChildAt(3);

		assertEquals(childAt4, childAt3);

		assertTrue(component.getChildCount() == 5);
		assertTrue(component.getImplementation() != null);
		assertTrue(component.getPropertyElements().length == 1);
		assertTrue(component.getPropertiesElements().length == 1);
		assertTrue(component.getService() != null);
		assertTrue(component.getReferences().length == 1);

		component.removeReference(reference);
		assertTrue(component.getChildCount() == 4);
		assertTrue(component.getImplementation() != null);
		assertTrue(component.getPropertyElements().length == 1);
		assertTrue(component.getPropertiesElements().length == 1);
		assertTrue(component.getService() != null);
		assertTrue(component.getReferences().length == 0);

		component.removeService(service);
		component.removePropertiesElement(properties);
		assertTrue(component.getChildCount() == 2);
		assertTrue(component.getImplementation() != null);
		assertTrue(component.getPropertyElements().length == 1);
		assertTrue(component.getPropertiesElements().length == 0);
		assertTrue(component.getService() == null);
		assertTrue(component.getReferences().length == 0);

		component.removeChildNode(implementation);
		component.removePropertyElement(property);
		assertTrue(component.getChildCount() == 0);
		assertTrue(component.getImplementation() == null);
		assertTrue(component.getPropertyElements().length == 0);
		assertTrue(component.getPropertiesElements().length == 0);
		assertTrue(component.getService() == null);
		assertTrue(component.getReferences().length == 0);

	}

}

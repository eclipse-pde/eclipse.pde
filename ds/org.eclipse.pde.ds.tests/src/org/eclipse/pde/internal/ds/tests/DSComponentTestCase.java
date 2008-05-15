/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ds.tests;

import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.ds.core.IDSComponent;
import org.eclipse.pde.internal.ds.core.IDSDocumentFactory;
import org.eclipse.pde.internal.ds.core.IDSImplementation;
import org.eclipse.pde.internal.ds.core.IDSProperties;
import org.eclipse.pde.internal.ds.core.IDSProperty;
import org.eclipse.pde.internal.ds.core.IDSProvide;
import org.eclipse.pde.internal.ds.core.IDSReference;
import org.eclipse.pde.internal.ds.core.IDSService;

public class DSComponentTestCase extends AbstractDSModelTestCase {
	
	public void testAddDefaultComponent() {
		StringBuffer buffer = new StringBuffer();
		setXMLContents(buffer, LF);
		load();

		IDSComponent component = fModel.getDSComponent();
		assertNotNull(component);
		
		assertEquals(component.getName(), COMPONENT_NAME);
		
		assertTrue(component.getEnabled());
	}

	public void testAddComponentwithAllAttributes() {
		StringBuffer buffer = new StringBuffer();
		setXMLContents(buffer, LF);
		load();

		IDSComponent component = fModel.getDSComponent();
		assertNotNull(component);
		
		assertEquals(component.getName(), COMPONENT_NAME);
		component.setEnabled(false);
		component.setAttributeName("NewName");
		component.setFactory("NewFactory");
		component.setImmediate(true);
		
		assertFalse(component.getEnabled());
		
		assertEquals(component.getAttributeName(), "NewName");
		
		assertEquals(component.getFactory(), "NewFactory");
		
		assertTrue(component.getImmediate());
	}
	

	public void testAddMoveRemoveChildrens() {
		StringBuffer buffer = new StringBuffer();
		setXMLContents(buffer, LF);
		load();

		IDSComponent component = fModel.getDSComponent();
		assertNotNull(component);
		
		IDSDocumentFactory factory = fModel.getFactory();
		
		IDSImplementation implementation = factory.createImplementation();
		implementation.setClassName("ImplementationClassName");
		component.addChild(implementation);
		
		IDSProperties properties = factory.createProperties();
		properties.setEntry("PropertiesEntry");
		component.addChild(properties);
		
		IDSProperty property = factory.createProperty();
		property.setPropertyElemBody("Body Values");
		property.setPropertyType("java.lang.String");
		component.addChild(property);
		
		IDSService service = factory.createService();
		service.setServiceFactory(true);
		component.addChild(service);
		
		IDSReference reference = factory.createReference();
		reference.setReferenceBind("methodBind");
		reference.setReferenceUnbind("methodUnBind");
		reference.setReferenceInterface("ReferenceInterface");
		reference.setReferenceName("ReferenceName");
		component.addChild(reference);
		
		IDocumentElementNode childAt4 = component.getChildAt(4);
		component.moveChild(reference, -1);
		
		IDocumentElementNode childAt3 = component.getChildAt(3);
		
		assertEquals(childAt4, childAt3);
		
		
		assertTrue(component.getChildCount() == 5);
		assertTrue(component.getImplementations().length == 1);
		assertTrue(component.getPropertyElements().length == 1);
		assertTrue(component.getPropertiesElements().length == 1);
		assertTrue(component.getServices().length == 1);
		assertTrue(component.getReferences().length == 1);
		
		component.removeChild(reference);
		assertTrue(component.getChildCount() == 4);
		assertTrue(component.getImplementations().length == 1);
		assertTrue(component.getPropertyElements().length == 1);
		assertTrue(component.getPropertiesElements().length == 1);
		assertTrue(component.getServices().length == 1);
		assertTrue(component.getReferences().length == 0);
		

		component.removeChild(service);
		component.removeChild(properties);
		assertTrue(component.getChildCount() == 2);
		assertTrue(component.getImplementations().length == 1);
		assertTrue(component.getPropertyElements().length == 1);
		assertTrue(component.getPropertiesElements().length == 0);
		assertTrue(component.getServices().length == 0);
		assertTrue(component.getReferences().length == 0);
		

		component.removeChild(implementation);
		component.removeChild(property);
		assertTrue(component.getChildCount() == 0);
		assertTrue(component.getImplementations().length == 0);
		assertTrue(component.getPropertyElements().length == 0);
		assertTrue(component.getPropertiesElements().length == 0);
		assertTrue(component.getServices().length == 0);
		assertTrue(component.getReferences().length == 0);
		
	}

}

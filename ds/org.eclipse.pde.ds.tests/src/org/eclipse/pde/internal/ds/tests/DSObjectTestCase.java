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

import java.util.List;

import org.eclipse.pde.internal.ds.core.IDSComponent;
import org.eclipse.pde.internal.ds.core.IDSDocumentFactory;
import org.eclipse.pde.internal.ds.core.IDSImplementation;
import org.eclipse.pde.internal.ds.core.IDSObject;
import org.eclipse.pde.internal.ds.core.IDSProperties;
import org.eclipse.pde.internal.ds.core.IDSProperty;
import org.eclipse.pde.internal.ds.core.IDSReference;
import org.eclipse.pde.internal.ds.core.IDSService;

public class DSObjectTestCase extends AbstractDSModelTestCase {


	public void testObject(){
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
		
		List children = component.getChildren();
		assertTrue(children.size() == 5);
		assertEquals(component.getModel(), fModel);
		
		IDSObject child = (IDSObject)children.get(0);
		assertEquals(child.getComponent(), component);
		
		assertEquals(child.getParent(), component);
		
		assertFalse(component.canBeRemoved());
		
		assertTrue(child.descendsFrom(component));
	}
}

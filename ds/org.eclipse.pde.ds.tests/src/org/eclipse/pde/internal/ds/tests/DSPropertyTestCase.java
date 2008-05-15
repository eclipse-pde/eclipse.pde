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

import org.eclipse.pde.internal.ds.core.IDSComponent;
import org.eclipse.pde.internal.ds.core.IDSProperty;

public class DSPropertyTestCase extends AbstractDSModelTestCase {
	
	public void testAddProperty() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("<property ");
		buffer.append("/>");
		setXMLContents(buffer, LF);
		load();

		IDSComponent component = fModel.getDSComponent();
		assertNotNull(component);
		
		IDSProperty[] propertyElements = fModel.getDSComponent().getPropertyElements();
		assertTrue(propertyElements.length == 1);
		IDSProperty property = propertyElements[0];

		String name = "propertyName";
		String type = "java.lang.String";
		String value = "propertyValue";
		String body = "propertyBody";
		
		property.setPropertyName(name);
		property.setPropertyType(type);
		property.setPropertyValue(value);
		property.setPropertyElemBody(body);
		
		assertEquals(property.getPropertyName(), name);
		assertEquals(property.getPropertyType(), type);
		assertEquals(property.getPropertyValue(), value);
		assertEquals(property.getPropertyElemBody(), body);
	}
	
	

}

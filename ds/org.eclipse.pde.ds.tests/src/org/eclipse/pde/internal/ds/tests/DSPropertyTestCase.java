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
 *******************************************************************************/
package org.eclipse.pde.internal.ds.tests;

import org.eclipse.pde.internal.ds.core.IDSComponent;
import org.eclipse.pde.internal.ds.core.IDSProperty;

public class DSPropertyTestCase extends AbstractDSModelTestCase {

	public void testAddProperty() {
		StringBuilder buffer = new StringBuilder();
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

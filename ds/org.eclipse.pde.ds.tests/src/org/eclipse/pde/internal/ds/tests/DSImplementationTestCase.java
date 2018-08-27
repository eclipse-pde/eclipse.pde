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
 *     Rafael Oliveira N�brega <rafael.oliveira@gmail.com>
 *******************************************************************************/
package org.eclipse.pde.internal.ds.tests;

import org.eclipse.pde.internal.ds.core.IDSComponent;
import org.eclipse.pde.internal.ds.core.IDSDocumentFactory;
import org.eclipse.pde.internal.ds.core.IDSImplementation;

public class DSImplementationTestCase extends AbstractDSModelTestCase {

	public void testAddImplementationComponent() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("<implementation class=\"");
		buffer.append("org.example.ds.SampleCommandProvider1\">");
		buffer.append("</implementation>");
		setXMLContents(buffer, LF);
		load();

		IDSComponent component = fModel.getDSComponent();
		assertNotNull(component);

		IDSImplementation implementation = component.getImplementation();
		assertTrue(implementation != null);

		String className = implementation.getClassName();
		assertTrue(className.equals("org.example.ds.SampleCommandProvider1"));

		assertEquals(implementation.getName(), className);
	}


	/**
	 * Tests a service component default values
	 */
	public void testDefaultValuesService() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("<implementation>");
		buffer.append(LF);
		buffer.append("</implementation>");

		setXMLContents(buffer, LF);
		load();

		IDSComponent component = fModel.getDSComponent();
		assertNotNull(component);

		IDSImplementation implementation = component.getImplementation();
		assertNotNull(implementation);
		assertTrue(implementation.getClassName() == null);
	}

	/**
	 * Tests to add a implementation by DSDocumentFactory
	 */
	public void testAddImplementationFactory() {
		StringBuilder buffer = new StringBuilder();
		setXMLContents(buffer, LF);
		load();

		IDSDocumentFactory factory = fModel.getFactory();
		IDSImplementation Implementation = factory.createImplementation();
		String className = "org.example.ds.SampleCommandProvider";
		Implementation.setClassName(className);

		IDSComponent component = fModel.getDSComponent();
		component.setImplementation(Implementation);

		String content = component.toString();
		assertTrue(content.indexOf("class=\"" + className + "\"") != -1);

		IDSImplementation Implementation0 = component.getImplementation();
		assertNotNull(Implementation0);
		assertTrue(Implementation0.getClassName().equals(className));

	}

}

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
import org.eclipse.pde.internal.ds.core.IDSObject;
import org.eclipse.pde.internal.ds.core.IDSProvide;
import org.eclipse.pde.internal.ds.core.IDSService;

public class DSServiceTestCase extends AbstractDSModelTestCase {

	/**
	 * Tests a service component with all Attributes and Child values
	 */
	public void testAddCompleteService() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("<service servicefactory=\"true\" >");
		buffer.append(LF);
		buffer.append("<provide interface=\"");
		buffer.append("java.lang.Runnable\">");
		buffer.append("</provide>");
		buffer.append(LF);
		buffer.append("</service>");

		setXMLContents(buffer, LF);
		load();

		IDSObject root = fModel.getDSRoot();

		assertTrue(root.getChildCount() == 1);

		IDocumentElementNode child = root.getChildAt(0);

		assertTrue(child instanceof IDSService);

		IDSService service = (IDSService) child;
		assertTrue(service.getServiceFactory() == true);

		assertTrue(child.getChildCount() == 1);
		IDocumentElementNode grandChild = child.getChildAt(0);

		assertTrue(grandChild instanceof IDSProvide);
		IDSProvide provide = (IDSProvide) grandChild;

		String interface1 = provide.getInterface();

		assertTrue(interface1.equals("java.lang.Runnable"));
	}

	/**
	 * Tests a service component with multiple Childs 
	 */
	public void testMultipleProvideService() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("<service>");

		buffer.append(LF);
		buffer.append("<provide interface=\"");
		buffer.append("java.lang.Runnable1\"/>");

		buffer.append(LF);
		buffer.append("<provide interface=\"");
		buffer.append("java.lang.Runnable2\"/>");

		buffer.append(LF);
		buffer.append("<provide interface=\"");
		buffer.append("java.lang.Runnable3\"/>");

		buffer.append(LF);
		buffer.append("<provide interface=\"");
		buffer.append("java.lang.Runnable4\"/>");

		buffer.append(LF);
		buffer.append("<provide interface=\"");
		buffer.append("java.lang.Runnable5\"/>");

		buffer.append(LF);
		buffer.append("<provide interface=\"");
		buffer.append("java.lang.Runnable6\"/> ");

		buffer.append("</service>");

//		System.out.println(buffer);
		setXMLContents(buffer, LF);
		load();

		IDSObject root = fModel.getDSRoot();

		assertTrue(root.getChildCount() == 1);

		IDocumentElementNode child = root.getChildAt(0);

		assertTrue(child instanceof IDSService);

		IDSService service = (IDSService) child;

		assertTrue(child.getChildCount() == 6);

		for (int i = 0; i < 6; i++) {
			IDocumentElementNode grandChild = child.getChildAt(i);

			IDSProvide provide = (IDSProvide) grandChild;
			String interface1 = provide.getInterface();
			assertTrue(interface1.equals("java.lang.Runnable" + (i+1)));
		}

	}

	/**
	 * Tests a service component default values 
	 */
	public void testDefaultValuesService() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("<service>");
		buffer.append(LF);
		buffer.append("</service>");

		setXMLContents(buffer, LF);
		load();

		IDSObject root = fModel.getDSRoot();

		assertTrue(root.getChildCount() == 1);

		IDocumentElementNode child = root.getChildAt(0);

		assertTrue(child instanceof IDSService);

		IDSService service = (IDSService) child;
		assertTrue(service.getServiceFactory() == false);
	}
}

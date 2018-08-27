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

import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.ds.core.IDSComponent;
import org.eclipse.pde.internal.ds.core.IDSConstants;
import org.eclipse.pde.internal.ds.core.IDSDocumentFactory;
import org.eclipse.pde.internal.ds.core.IDSObject;
import org.eclipse.pde.internal.ds.core.IDSProvide;
import org.eclipse.pde.internal.ds.core.IDSService;

public class DSServiceTestCase extends AbstractDSModelTestCase {

	/**
	 * Tests a service component with all Attributes and Child values
	 */
	public void testAddCompleteService() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("<service servicefactory=\"true\" >");
		buffer.append(LF);
		buffer.append("<provide interface=\"");
		buffer.append("java.lang.Runnable\">");
		buffer.append("</provide>");
		buffer.append(LF);
		buffer.append("</service>");

		setXMLContents(buffer, LF);
		load();

		IDSObject component = fModel.getDSComponent();

		assertTrue(component.getChildCount() == 1);

		IDocumentElementNode child = component.getChildAt(0);

		assertTrue(child instanceof IDSService);

		IDSService service = (IDSService) child;
		assertTrue(service.getServiceFactory() == true);

		assertTrue(child.getChildCount() == 1);
		IDSProvide[] providedServices = service.getProvidedServices();

		IDSProvide provide = providedServices[0];

		String interface1 = provide.getInterface();

		assertTrue(interface1.equals("java.lang.Runnable"));

		service.setServiceFactory(false);

		String string = fModel.getDSComponent().toString();
		assertTrue(string.indexOf("servicefactory=\"false\"") != -1);

		assertTrue(service.getServiceFactory() == false);

		assertEquals(service.getName(), IDSConstants.ELEMENT_SERVICE);
	}

	/**
	 * Tests a service component with multiple Childs
	 */
	public void testMultipleProvideService() {
		StringBuilder buffer = new StringBuilder();
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

		setXMLContents(buffer, LF);
		load();

		IDSObject component = fModel.getDSComponent();

		assertTrue(component.getChildCount() == 1);

		IDocumentElementNode child = component.getChildAt(0);

		assertTrue(child instanceof IDSService);

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
		StringBuilder buffer = new StringBuilder();
		buffer.append("<service>");
		buffer.append(LF);
		buffer.append("</service>");

		setXMLContents(buffer, LF);
		load();

		IDSObject component = fModel.getDSComponent();

		assertTrue(component.getChildCount() == 1);

		IDocumentElementNode child = component.getChildAt(0);

		assertTrue(child instanceof IDSService);

		IDSService service = (IDSService) child;
		assertTrue(service.getServiceFactory() == false);
	}

	/**
	 * Test to remove a provided service element from a service element.
	 */
	public void testRemoveChildService(){
		StringBuilder buffer = new StringBuilder();
		buffer.append("<service servicefactory=\"true\" >");
		buffer.append(LF);
		buffer.append("<provide interface=\"");
		buffer.append("java.lang.Runnable\">");
		buffer.append("</provide>");
		buffer.append(LF);
		buffer.append("</service>");

		setXMLContents(buffer, LF);
		load();

		IDSComponent component = fModel.getDSComponent();

		IDSService service = component.getService();
		assertNotNull(service);

		IDSProvide[] providedServices = service.getProvidedServices();
		assertTrue(providedServices.length==1);

		//Removing Provided Service
		service.removeProvidedService(providedServices[0]);

		service = component.getService();
		assertNotNull(service);

		assertTrue(service.getProvidedServices().length == 0);

	}

	/**
	 * Tests to add a service by DSDocumentFactory
	 */
	public void testAddServiceFactory(){
		StringBuilder buffer = new StringBuilder();
		setXMLContents(buffer , LF);
		load();

		IDSDocumentFactory factory = fModel.getFactory();
		IDSService service = factory.createService();
		service.setServiceFactory(true);

		IDSComponent component = fModel.getDSComponent();
		component.setService(service);

		String content = component.toString();

		assertTrue(content.indexOf("servicefactory=\"true\"") != -1);

		IDSService service0 = component.getService();
		assertNotNull(service0);
		assertTrue(service0.getServiceFactory());

	}

}

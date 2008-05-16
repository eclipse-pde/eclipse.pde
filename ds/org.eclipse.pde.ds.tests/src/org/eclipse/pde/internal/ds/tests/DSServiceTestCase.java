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
import org.eclipse.pde.internal.ds.core.IDSConstants;
import org.eclipse.pde.internal.ds.core.IDSDocumentFactory;
import org.eclipse.pde.internal.ds.core.IDSObject;
import org.eclipse.pde.internal.ds.core.IDSProperty;
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
		assertTrue(string.contains("servicefactory=\"false\""));
		
		assertTrue(service.getServiceFactory() == false);
		
		assertEquals(service.getName(), IDSConstants.ELEMENT_SERVICE);
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

		setXMLContents(buffer, LF);
		load();

		IDSObject component = fModel.getDSComponent();

		assertTrue(component.getChildCount() == 1);

		IDocumentElementNode child = component.getChildAt(0);

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

		IDSComponent component = fModel.getDSComponent();
		
		IDSService[] services = component.getServices();
		assertTrue(services.length == 1);
		
		IDSService service = services[0];
		
		IDSProvide[] providedServices = service.getProvidedServices();
		assertTrue(providedServices.length==1);
		
		//Removing Provided Service
		service.removeChild(providedServices[0]);
		
		services = component.getServices();
		
		assertTrue(services.length == 1);
		
		service = services[0];
		
		assertTrue(service.getProvidedServices().length == 0);

	}
	
	/**
	 * Tests to add a service by DSDocumentFactory
	 */
	public void testAddServiceFactory(){
		StringBuffer buffer = new StringBuffer();
		setXMLContents(buffer , LF);
		load();
		
		IDSDocumentFactory factory = fModel.getFactory();
		IDSService service = factory.createService();
		service.setServiceFactory(true);
		
		IDSComponent component = fModel.getDSComponent();
		component.addChild(service);
		
		String content = component.toString();
		
		assertTrue(content.contains("servicefactory=\"true\""));
		
		IDSService[] services = component.getServices();
		IDSService service0 = services[0];
		assertNotNull(service0);
		assertTrue(service0.getServiceFactory());
		
	}

}

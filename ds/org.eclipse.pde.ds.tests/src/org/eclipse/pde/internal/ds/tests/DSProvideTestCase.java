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
import org.eclipse.pde.internal.ds.core.IDSDocumentFactory;
import org.eclipse.pde.internal.ds.core.IDSProvide;
import org.eclipse.pde.internal.ds.core.IDSService;

public class DSProvideTestCase extends AbstractDSModelTestCase {
	
	public void testAddCompleteProvidedService() {
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
		assertNotNull(component);
		
		IDSService[] services = fModel.getDSComponent().getServices();
		assertTrue(services.length == 1);
		IDSService service = services[0];
		
		IDSProvide[] providedServices = service.getProvidedServices();
		assertTrue(providedServices.length == 1);
		IDSProvide provide = providedServices[0];
		assertTrue(provide.getInterface().equals("java.lang.Runnable"));
		
		String interfaceName = "java.lang.String";
		provide.setInterface(interfaceName);
		
		String content = fModel.getDSComponent().toString();
		assertTrue(content.contains(interfaceName));
		
	}
	
	/**
	 * Tests to add a provided service by DSDocumentFactory
	 */	
	public void testAddProvidedServicebyFactory() {
			StringBuffer buffer = new StringBuffer();
			setXMLContents(buffer , LF);
			load();
			
			IDSDocumentFactory factory = fModel.getFactory();
			IDSService service = factory.createService();
			service.setServiceFactory(true);
			
			IDSComponent component = fModel.getDSComponent();
			component.addChild(service);
			
			IDSProvide provide = factory.createProvide();
			provide.setInterface("java.lang.Runnable");
			service.addChild(provide);
			
			String content = component.toString();
			
			assertTrue(content.contains("interface=\"java.lang.Runnable\""));
			
			IDSService[] services = component.getServices();
			IDSService service0 = services[0];
			assertNotNull(service0);
			assertTrue(service0.getServiceFactory());
			IDSProvide[] providedServices = service.getProvidedServices();
			
			IDSProvide provide0 = providedServices[0];
			assertNotNull(provide0);
			assertTrue(provide0.getInterface().equals("java.lang.Runnable"));
			
			
	}

}

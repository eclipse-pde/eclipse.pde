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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.pde.internal.ds.core.IDSComponent;
import org.eclipse.pde.internal.ds.core.IDSDocumentFactory;
import org.eclipse.pde.internal.ds.core.IDSProvide;
import org.eclipse.pde.internal.ds.core.IDSService;
import org.junit.jupiter.api.Test;

public class DSProvideTestCase extends AbstractDSModelTestCase {
	@Test
	public void testAddCompleteProvidedService() {
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
		assertNotNull(component);

		IDSService service = fModel.getDSComponent().getService();
		assertNotNull(service);

		IDSProvide[] providedServices = service.getProvidedServices();
		assertTrue(providedServices.length == 1);
		IDSProvide provide = providedServices[0];
		assertTrue(provide.getInterface().equals("java.lang.Runnable"));

		String interfaceName = "java.lang.String";
		provide.setInterface(interfaceName);

		String content = fModel.getDSComponent().toString();
		assertTrue(content.contains(interfaceName));

		assertEquals(provide.getName(), interfaceName);

	}

	/**
	 * Tests to add a provided service by DSDocumentFactory
	 */
	@Test
	public void testAddProvidedServicebyFactory() {
		StringBuilder buffer = new StringBuilder();
			setXMLContents(buffer , LF);
			load();

			IDSDocumentFactory factory = fModel.getFactory();
			IDSService service = factory.createService();
			service.setServiceFactory(true);

			IDSComponent component = fModel.getDSComponent();
			component.setService(service);

			IDSProvide provide = factory.createProvide();
			provide.setInterface("java.lang.Runnable");
			service.addProvidedService(provide);

			String content = component.toString();

			assertTrue(content.contains("interface=\"java.lang.Runnable\""));

			IDSService service0 = component.getService();
			assertNotNull(service0);
			assertTrue(service0.getServiceFactory());
			IDSProvide[] providedServices = service.getProvidedServices();

			IDSProvide provide0 = providedServices[0];
			assertNotNull(provide0);
			assertTrue(provide0.getInterface().equals("java.lang.Runnable"));


	}

}

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
import org.eclipse.pde.internal.ds.core.IDSObject;
import org.eclipse.pde.internal.ds.core.IDSProperties;
import org.eclipse.pde.internal.ds.core.IDSProvide;
import org.eclipse.pde.internal.ds.core.IDSService;

public class DSPropertiesTestCase extends AbstractDSModelTestCase {
	
	public void testAddPropertiesComponent() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("<properties entry=\"");
		buffer.append("OSGI-INF/vendor.properties\">");
		buffer.append("</properties>");
		System.out.println(buffer);
		setXMLContents(buffer, LF);
		load();

		IDSComponent component = fModel.getDSComponent();
		assertNotNull(component);
		
		IDSProperties[] propertiesElements = component.getPropertiesElements();
		assertTrue(propertiesElements.length==1);
		
		IDSProperties properties = propertiesElements[0];
		String entry = properties.getEntry();
		assertTrue(entry.equals("OSGI-INF/vendor.properties"));
	}
	
	/**
	 * Tests a  component with multiple properties 
	 */
	public void testMultipleProperties() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("<properties entry=\"");
		buffer.append("OSGI-INF/vendor.properties1\">");
		buffer.append("</properties>");

		buffer.append(LF);
		buffer.append("<properties entry=\"");
		buffer.append("OSGI-INF/vendor.properties2\">");
		buffer.append("</properties>");

		buffer.append(LF);
		buffer.append("<properties entry=\"");
		buffer.append("OSGI-INF/vendor.properties3\">");
		buffer.append("</properties>");

		buffer.append(LF);
		buffer.append("<properties entry=\"");
		buffer.append("OSGI-INF/vendor.properties4\">");
		buffer.append("</properties>");

		setXMLContents(buffer, LF);
		load();

		IDSComponent component = fModel.getDSComponent();

		IDSProperties[] propertiesElements = component.getPropertiesElements();

		assertTrue(propertiesElements.length == 4);
		
		for (int i = 0; i < 4; i++) {
			IDSProperties properties = propertiesElements[i];

			String entry = properties.getEntry();
			assertTrue(entry.equals("OSGI-INF/vendor.properties" + (i+1)));
		}

	}

	/**
	 * Tests a properties component default values 
	 */
	public void testDefaultValuesService() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("<properties>");
		buffer.append(LF);
		buffer.append("</properties>");

		setXMLContents(buffer, LF);
		load();

		IDSComponent component = fModel.getDSComponent();
		assertNotNull(component);
		
		IDSProperties[] propertiesElements = component.getPropertiesElements();
		assertTrue(propertiesElements.length==1);
		
		IDSProperties properties = propertiesElements[0];
		assertTrue(properties.getEntry() == null);
	}
	

	
	/**
	 * Tests to add a properties by DSDocumentFactory
	 */
	public void testAddPropertiesFactory(){
		StringBuffer buffer = new StringBuffer();
		setXMLContents(buffer , LF);
		load();
		
		IDSDocumentFactory factory = fModel.getFactory();
		IDSProperties Properties = factory.createProperties();
		String entry = "OSGI-INF/vendor.propertiesFactory";
		Properties.setEntry(entry);
		
		IDSComponent component = fModel.getDSComponent();
		component.addChild(Properties);
		
		String content = component.toString();
		
		assertTrue(content.contains("entry=\""+entry+"\""));
		
		IDSProperties[] PropertiesElements = component.getPropertiesElements();
		IDSProperties Properties0 = PropertiesElements[0];
		assertNotNull(Properties0);
		assertTrue(Properties0.getEntry().equals(entry));
		
	}

}

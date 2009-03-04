/*******************************************************************************
 * Copyright (c) 2009 EclipseSource IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     EclipseSource Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ds.tests;

import org.eclipse.pde.internal.ds.core.IDSComponent;

public class DSv10tov11TestCase extends AbstractDSModelTestCase {
	
	public void testAddDefaultComponent() {
		StringBuffer buffer = new StringBuffer();
		setXMLContents(buffer, LF);
		load();

		IDSComponent component = fModel.getDSComponent();
		assertNotNull(component);
		
		assertEquals(component.getName(), COMPONENT_NAME);
		assertEquals(component.getNamespace(), "http://www.osgi.org/xmlns/scr/v1.1.0");
	}
	
	protected void setXMLContents(StringBuffer body, String newline) {
		StringBuffer sb = new StringBuffer();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		sb.append(newline);
		sb.append("<component name=\"" + COMPONENT_NAME + "\">");
		sb.append(newline);
		if (body != null)
			sb.append(body.toString());
		sb.append(newline);
		sb.append("</component>");
		sb.append(newline);
		fDocument.set(sb.toString());
	}

}

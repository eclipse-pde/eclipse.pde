/*******************************************************************************
 * Copyright (c) 2009, 2017 EclipseSource and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     EclipseSource Corporation - initial API and implementation
 *     IBM Corporation - continued improvements
 *******************************************************************************/
package org.eclipse.pde.internal.ds.tests;

import org.eclipse.pde.internal.ds.core.IDSComponent;

public class DSv10tov11TestCase extends AbstractDSModelTestCase {

	public void testAddDefaultComponent() {
		StringBuilder buffer = new StringBuilder();
		setXMLContents(buffer, LF);
		load();

		IDSComponent component = fModel.getDSComponent();
		assertNotNull(component);

		assertEquals(component.getName(), COMPONENT_NAME);
		assertEquals(component.getNamespace(), "http://www.osgi.org/xmlns/scr/v1.1.0");
	}

	@Override
	protected void setXMLContents(StringBuilder body, String newline) {
		StringBuilder sb = new StringBuilder();
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

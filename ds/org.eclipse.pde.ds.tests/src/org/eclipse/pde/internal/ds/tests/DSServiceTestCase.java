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

import org.eclipse.pde.internal.ds.core.text.DSRoot;

public class DSServiceTestCase extends AbstractDSModelTestCase {
	
	public void testAddService() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("<service>");
		buffer.append(LF);
		buffer.append("<provide interface=\"");
		buffer.append("java.lang.Runnable");
		buffer.append("</provide>");
		buffer.append(LF);
		buffer.append("</service>");
		setXMLContents(buffer, LF);
		load();
		
		// TODO test stuff?
	}

}

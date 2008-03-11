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
package org.eclipse.pde.ui.tests.model.cheatsheet;

import org.eclipse.pde.internal.core.icheatsheet.simple.*;

public class SimpleCSIntroTestCase extends AbstractCheatSheetModelTestCase {

	protected static String INTRO_HREF = "/org.eclipse.platform.doc.user/reference/ref-cheatsheets.htm"; //$NON-NLS-1$
	protected static String DESCRIPTION = "some description"; //$NON-NLS-1$
	
	public void testAddSimpleCSIntro() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("<intro href=\"").append(INTRO_HREF).append("\">");
		buffer.append(LF);
		buffer.append("<description>");
		buffer.append(DESCRIPTION);
		buffer.append("</description>");
		buffer.append(LF);
		buffer.append("</intro>");
		setXMLContents(buffer, LF);
		load();
		
		ISimpleCS model = fModel.getSimpleCS();
		String title = model.getTitle();
		
		// check intro
		ISimpleCSIntro intro = model.getIntro();
		assertNotNull(intro);
		assertEquals(intro.getHref(), INTRO_HREF);
		
		// check description
		ISimpleCSDescription description = intro.getDescription();
		assertNotNull(description);
		assertEquals(DESCRIPTION, description.getContent());
	}
	
}

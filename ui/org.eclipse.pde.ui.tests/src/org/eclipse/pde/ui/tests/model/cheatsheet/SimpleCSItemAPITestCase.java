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

/**
 * Cheatsheet items tests for API-generated models.
 *
 */
public class SimpleCSItemAPITestCase extends CheatSheetModelTestCase {
	
	protected void setUp() throws Exception {
		super.setUp();
		setXMLContents(new StringBuffer(), LF);
		load();
	}

	public void testSimpleCSItemTestCase() {
		simpleCSItemTestCase("");
	}
	
	public void testSimpleCSItemTestCaseSpace() {
		simpleCSItemTestCase(" ");
	}
	
	public void testSimpleCSItemTestCaseCR() {
		simpleCSItemTestCase(CR);
	}
	
	public void testSimpleCSItemTestCaseLF() {
		simpleCSItemTestCase(LF);
	}
	
	public void testSimpleCSItemTestCaseCRLF() {
		simpleCSItemTestCase(CRLF);
	}
	
	public void simpleCSItemTestCase(String newline) {
		ISimpleCSItem item = createComplexCSItem();

		ISimpleCS model = process(item.toString(), newline);
		
		validateItemsCount(1, model);
		item = model.getItems()[0];
		
		validateComplexCSItem(item);
	}
	
	public void testItemActionTestCase() {
		ISimpleCSItem item = createComplexCSItem();
		item.setExecutable(createAction());

		ISimpleCS model = process(item.toString(), LF);
		
		validateItemsCount(1, model);
		item = model.getItems()[0];
		
		validateComplexCSItem(item);
		validateAction(item.getExecutable());
	}
	
	public void testItemCommandTestCase() {
		ISimpleCSItem item = createComplexCSItem();
		item.setExecutable(createCommand());
		
		ISimpleCS model = process(item.toString(), LF);
		
		validateItemsCount(1, model);
		item = model.getItems()[0];
		
		validateComplexCSItem(item);
		validateCommand(item.getExecutable());
	}
	
	public void testItemPerformWhenTestCase() {
		ISimpleCSItem item = createComplexCSItem();
		item.setExecutable(createPerformWhen());
		
		ISimpleCS model = process(item.toString(), LF);
		
		validateItemsCount(1, model);
		item = model.getItems()[0];
		
		validateComplexCSItem(item);
		validatePerformWhen(item.getExecutable());
	}
	
	public ISimpleCS process(String buffer, String newline) {
		setXMLContents(new StringBuffer(buffer), newline);
		load();
		
		return fModel.getSimpleCS();
	}
}

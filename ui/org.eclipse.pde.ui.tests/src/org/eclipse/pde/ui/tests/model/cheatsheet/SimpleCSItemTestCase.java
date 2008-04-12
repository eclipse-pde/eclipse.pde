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

import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem;

/**
 * Cheatsheet items tests for XML-generated models.
 *
 */
public class SimpleCSItemTestCase extends CheatSheetModelTestCase  {
		
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
		StringBuffer buffer = createComplexCSItem("", newline);

		ISimpleCS model = process(buffer, newline);
		
		validateItemsCount(1, model);
		ISimpleCSItem item = model.getItems()[0];
		
		validateComplexCSItem(item);
	}
	
	public void testItemActionTestCase() {
		String action = createAction(LF);
		StringBuffer buffer = createComplexCSItem(action, LF);

		ISimpleCS model = process(buffer, LF);
		
		validateItemsCount(1, model);
		ISimpleCSItem item = model.getItems()[0];
		
		validateComplexCSItem(item);
		validateAction(item.getExecutable());
	}
	
	public void testItemCommandTestCase() {
		String command = createCommand(LF);
		StringBuffer buffer = createComplexCSItem(command, LF);
		
		ISimpleCS model = process(buffer, LF);
		
		validateItemsCount(1, model);
		ISimpleCSItem item = model.getItems()[0];
		
		validateComplexCSItem(item);
		validateCommand(item.getExecutable());
	}
	
	public void testItemPerformWhenTestCase() {
		String command = createPerformWhen("", LF);
		StringBuffer buffer = createComplexCSItem(command, LF);
		
		ISimpleCS model = process(buffer, LF);
		
		validateItemsCount(1, model);
		ISimpleCSItem item = model.getItems()[0];
		
		validateComplexCSItem(item);
		validatePerformWhen(item.getExecutable());
	}
	
	public ISimpleCS process(StringBuffer buffer, String newline) {
		setXMLContents(buffer, newline);
		load();
		
		return fModel.getSimpleCS();
	}
	
}

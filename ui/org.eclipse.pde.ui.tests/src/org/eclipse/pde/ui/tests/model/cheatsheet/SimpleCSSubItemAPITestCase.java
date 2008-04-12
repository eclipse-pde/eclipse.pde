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
import org.eclipse.pde.internal.core.text.cheatsheet.simple.SimpleCSSubItem;

/**
 * Cheatsheet subitems tests for API-generated models.
 *
 */
public class SimpleCSSubItemAPITestCase extends CheatSheetModelTestCase {
	
	protected void setUp() throws Exception {
		super.setUp();
		setXMLContents(new StringBuffer(), LF);
		load();
	}
	
	public void testSimpleSubItemTestCase() {
		simpleSubItemTestCase(1, LF);
	}
	
	public void testSimpleSubItemCommandTestCase() {
		simpleSubItemCommandTestCase(1, LF);
	}
	
	public void testSimpleSubItemActionTestCase() {
		simpleSubItemActionTestCase(1, LF);
	}
	
	public void testSimpleRepeatedSubItemTestCase() {
		simpleRepeatedSubItemTestCase(1, LF);
	}
	
	public void testSimpleConditionalSubItemTestCase() {
		simpleConditionalSubItemTestCase(1, LF);
	}
	
	public void testSimpleSubItemTestCase3() {
		simpleSubItemTestCase(3, LF);
	}
	
	public void testSimpleSubItemCommandTestCase3() {
		simpleSubItemCommandTestCase(3, LF);
	}
	
	public void testSimpleSubItemActionTestCase3() {
		simpleSubItemActionTestCase(3, LF);
	}
	
	public void testSimpleRepeatedSubItemTestCase3() {
		simpleRepeatedSubItemTestCase(3, LF);
	}
	
	public void testSimpleConditionalSubItemTestCase3() {
		simpleConditionalSubItemTestCase(3, LF);
	}
	
	public void simpleSubItemTestCase(int subitemsCount, String newline) {
		ISimpleCSSubItem[] subitems = new ISimpleCSSubItem[subitemsCount];
		
		for (int i =0; i < subitemsCount; i++) {
			subitems[i] = createSubItem();
			subitems[i].setExecutable(createCommand());
		}
		
		ISimpleCSItem item = process(subitems, newline);
		
		validateSubItemsCount(subitemsCount, item);
		
		for (int i = 0; i < subitemsCount; i++) {
			ISimpleCSSubItemObject subitem = item.getSubItems()[i];
			validateSubItem(subitem);
		}
	}
	
	public void simpleSubItemCommandTestCase(int subitemsCount, String newline) {
		ISimpleCSSubItem[] subitems = new ISimpleCSSubItem[subitemsCount];
		
		for (int i =0; i < subitemsCount; i++) {
			subitems[i] = createSubItem();
			subitems[i].setExecutable(createCommand());
		}
		
		ISimpleCSItem item = process(subitems, newline);
		
		validateSubItemsCount(subitemsCount, item);
		
		for (int i = 0; i < subitemsCount; i++) {
			ISimpleCSSubItemObject subitem = item.getSubItems()[i];
			validateSubItem(subitem);
			
			SimpleCSSubItem simpleSubitem = (SimpleCSSubItem) subitem;
			validateCommand(simpleSubitem.getExecutable());
		}
	}
	
	public void simpleSubItemActionTestCase(int subitemsCount, String newline) {
		ISimpleCSSubItem[] subitems = new ISimpleCSSubItem[subitemsCount];
		
		for (int i =0; i < subitemsCount; i++) {
			subitems[i] = createSubItem();
			subitems[i].setExecutable(createAction());
		}
		
		ISimpleCSItem item = process(subitems, newline);
		validateSubItemsCount(subitemsCount, item);
		
		for (int i = 0; i < subitemsCount; i++) {
			ISimpleCSSubItemObject subitem = item.getSubItems()[i];
			validateSubItem(subitem);
			
			SimpleCSSubItem simpleSubitem = (SimpleCSSubItem) subitem;
			validateAction(simpleSubitem.getExecutable());
		}
	}
	
	public void simpleRepeatedSubItemTestCase(int subitemsCount, String newline) {
		ISimpleCSSubItemObject[] subitems = new ISimpleCSSubItemObject[subitemsCount];
		
		for (int i =0; i < subitemsCount; i++) {
			subitems[i] = createRepeatedSubItem();
		}
		
		ISimpleCSItem item = process(subitems, newline);
		
		validateSubItemsCount(subitemsCount, item);
		
		for (int i = 0; i < subitemsCount; i++) {
			validateRepeatedSubItem(item.getSubItems()[i]);
		}
	}
	
	public void simpleConditionalSubItemTestCase(int subitemsCount, String newline) {
		ISimpleCSSubItemObject[] subitems = new ISimpleCSSubItemObject[subitemsCount];
		
		for (int i =0; i < subitemsCount; i++) {
			subitems[i] = createConditionalSubitem();
		}
		
		ISimpleCSItem item = process(subitems, newline);
				
		validateSubItemsCount(subitemsCount, item);
		
		for (int i = 0; i < subitemsCount; i++) {
			validateConditionalSubItem(item.getSubItems()[i]);
		}
	}
	
	public ISimpleCSItem process(ISimpleCSSubItemObject[] subitems, String newline) {
		StringBuffer buffer = createSimpleCSItem(subitems);
		
		setXMLContents(buffer, newline);
		load();
		
		ISimpleCS model = fModel.getSimpleCS();
		
		validateItemsCount(1, model);
		
		return model.getItems()[0];
	}
}

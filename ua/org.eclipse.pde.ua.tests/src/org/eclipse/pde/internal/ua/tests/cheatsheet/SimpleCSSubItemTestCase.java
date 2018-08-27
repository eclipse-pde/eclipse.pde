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
package org.eclipse.pde.internal.ua.tests.cheatsheet;

import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCS;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSItem;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSSubItemObject;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.text.SimpleCSSubItem;
import org.junit.Test;

/**
 * Cheatsheet subitems tests for XML-generated models.
 */
public class SimpleCSSubItemTestCase extends CheatSheetModelTestCase  {

	@Test
	public void testSimpleSubItemTestCase() {
		simpleSubItemTestCase(1, LF);
	}

	@Test
	public void testSimpleSubItemCommandTestCase() {
		simpleSubItemCommandTestCase(1, LF);
	}

	@Test
	public void testSimpleSubItemActionTestCase() {
		simpleSubItemActionTestCase(1, LF);
	}

	@Test
	public void testSimpleRepeatedSubItemTestCase() {
		simpleRepeatedSubItemTestCase(1, LF);
	}

	@Test
	public void testSimpleConditionalSubItemTestCase() {
		simpleConditionalSubItemTestCase(1, LF);
	}

	@Test
	public void testSimpleSubItemTestCase3() {
		simpleSubItemTestCase(3, LF);
	}

	@Test
	public void testSimpleSubItemCommandTestCase3() {
		simpleSubItemCommandTestCase(3, LF);
	}

	@Test
	public void testSimpleSubItemActionTestCase3() {
		simpleSubItemActionTestCase(3, LF);
	}

	@Test
	public void testSimpleRepeatedSubItemTestCase3() {
		simpleRepeatedSubItemTestCase(3, LF);
	}

	@Test
	public void testSimpleConditionalSubItemTestCase3() {
		simpleConditionalSubItemTestCase(3, LF);
	}

	public void simpleSubItemTestCase(int subitemsCount, String newline) {
		StringBuilder buffer = new StringBuilder();

		for (int i =0; i < subitemsCount; i++) {
			String action = createAction(newline);
			buffer.append(createSubItem(action, newline));
		}

		ISimpleCSItem item = process(buffer.toString(), newline);
		validateSubItemsCount(subitemsCount, item);

		for (int i = 0; i < subitemsCount; i++) {
			ISimpleCSSubItemObject subitem = item.getSubItems()[i];
			validateSubItem(subitem);
		}
	}

	public void simpleSubItemCommandTestCase(int subitemsCount, String newline) {
		StringBuilder buffer = new StringBuilder();

		for (int i =0; i < subitemsCount; i++) {
			String action = createCommand(newline);
			buffer.append(createSubItem(action, newline));
		}

		ISimpleCSItem item = process(buffer.toString(), newline);
		validateSubItemsCount(subitemsCount, item);

		for (int i = 0; i < subitemsCount; i++) {
			ISimpleCSSubItemObject subitem = item.getSubItems()[i];
			validateSubItem(subitem);

			SimpleCSSubItem simpleSubitem = (SimpleCSSubItem) subitem;
			validateCommand(simpleSubitem.getExecutable());
		}
	}

	public void simpleSubItemActionTestCase(int subitemsCount, String newline) {
		StringBuilder buffer = new StringBuilder();

		for (int i =0; i < subitemsCount; i++) {
			String action = createAction(newline);
			buffer.append(createSubItem(action, newline));
		}

		ISimpleCSItem item = process(buffer.toString(), newline);
		validateSubItemsCount(subitemsCount, item);

		for (int i = 0; i < subitemsCount; i++) {
			ISimpleCSSubItemObject subitem = item.getSubItems()[i];
			validateSubItem(subitem);

			SimpleCSSubItem simpleSubitem = (SimpleCSSubItem) subitem;
			validateAction(simpleSubitem.getExecutable());
		}
	}

	public void simpleRepeatedSubItemTestCase(int subitemsCount, String newline) {
		StringBuilder buffer = new StringBuilder();

		for (int i =0; i < subitemsCount; i++) {
			buffer.append(createRepeatedSubItem("", newline));
		}

		ISimpleCSItem item = process(buffer.toString(), newline);

		validateSubItemsCount(subitemsCount, item);

		for (int i = 0; i < subitemsCount; i++) {
			validateRepeatedSubItem(item.getSubItems()[i]);
		}
	}

	public void simpleConditionalSubItemTestCase(int subitemsCount, String newline) {
		StringBuilder buffer = new StringBuilder();

		for (int i =0; i < subitemsCount; i++) {
			buffer.append(createConditionalSubItem("", newline));
		}

		ISimpleCSItem item = process(buffer.toString(), newline);

		validateSubItemsCount(subitemsCount, item);

		for (int i = 0; i < subitemsCount; i++) {
			validateConditionalSubItem(item.getSubItems()[i]);
		}
	}

	public ISimpleCSItem process(String subitems, String newline) {
		StringBuilder buffer = createSimpleCSItem(subitems, newline);

		setXMLContents(buffer, newline);
		load();

		ISimpleCS model = fModel.getSimpleCS();

		validateItemsCount(1, model);
		return model.getItems()[0];
	}


}

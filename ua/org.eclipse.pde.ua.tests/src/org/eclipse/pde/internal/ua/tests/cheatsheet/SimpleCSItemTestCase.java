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
import org.junit.Test;

/**
 * Cheatsheet items tests for XML-generated models.
 */
public class SimpleCSItemTestCase extends CheatSheetModelTestCase  {

	@Test
	public void testSimpleCSItemTestCase() {
		simpleCSItemTestCase("");
	}

	@Test
	public void testSimpleCSItemTestCaseSpace() {
		simpleCSItemTestCase(" ");
	}

	@Test
	public void testSimpleCSItemTestCaseCR() {
		simpleCSItemTestCase(CR);
	}

	@Test
	public void testSimpleCSItemTestCaseLF() {
		simpleCSItemTestCase(LF);
	}

	@Test
	public void testSimpleCSItemTestCaseCRLF() {
		simpleCSItemTestCase(CRLF);
	}

	public void simpleCSItemTestCase(String newline) {
		StringBuilder buffer = createComplexCSItem("", newline);

		ISimpleCS model = process(buffer, newline);

		validateItemsCount(1, model);
		ISimpleCSItem item = model.getItems()[0];

		validateComplexCSItem(item);
	}

	@Test
	public void testItemActionTestCase() {
		String action = createAction(LF);
		StringBuilder buffer = createComplexCSItem(action, LF);

		ISimpleCS model = process(buffer, LF);

		validateItemsCount(1, model);
		ISimpleCSItem item = model.getItems()[0];

		validateComplexCSItem(item);
		validateAction(item.getExecutable());
	}

	@Test
	public void testItemCommandTestCase() {
		String command = createCommand(LF);
		StringBuilder buffer = createComplexCSItem(command, LF);

		ISimpleCS model = process(buffer, LF);

		validateItemsCount(1, model);
		ISimpleCSItem item = model.getItems()[0];

		validateComplexCSItem(item);
		validateCommand(item.getExecutable());
	}

	@Test
	public void testItemPerformWhenTestCase() {
		String command = createPerformWhen("", LF);
		StringBuilder buffer = createComplexCSItem(command, LF);

		ISimpleCS model = process(buffer, LF);

		validateItemsCount(1, model);
		ISimpleCSItem item = model.getItems()[0];

		validateComplexCSItem(item);
		validatePerformWhen(item.getExecutable());
	}

	public ISimpleCS process(StringBuilder buffer, String newline) {
		setXMLContents(buffer, newline);
		load();

		return fModel.getSimpleCS();
	}

}

/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
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
import org.junit.Before;
import org.junit.Test;

/**
 * Cheatsheet items tests for API-generated models.
 */
public class SimpleCSItemAPITestCase extends CheatSheetModelTestCase {

	@Before
	public void setUpLocal() {
		setXMLContents(new StringBuilder(), LF);
		load();
	}

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
		ISimpleCSItem item = createComplexCSItem();

		ISimpleCS model = process(item.toString(), newline);

		validateItemsCount(1, model);
		item = model.getItems()[0];

		validateComplexCSItem(item);
	}

	@Test
	public void testItemActionTestCase() {
		ISimpleCSItem item = createComplexCSItem();
		item.setExecutable(createAction());

		ISimpleCS model = process(item.toString(), LF);

		validateItemsCount(1, model);
		item = model.getItems()[0];

		validateComplexCSItem(item);
		validateAction(item.getExecutable());
	}

	@Test
	public void testItemCommandTestCase() {
		ISimpleCSItem item = createComplexCSItem();
		item.setExecutable(createCommand());

		ISimpleCS model = process(item.toString(), LF);

		validateItemsCount(1, model);
		item = model.getItems()[0];

		validateComplexCSItem(item);
		validateCommand(item.getExecutable());
	}

	@Test
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
		setXMLContents(new StringBuilder(buffer), newline);
		load();

		return fModel.getSimpleCS();
	}
}

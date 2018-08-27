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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.pde.internal.ua.core.cheatsheet.simple.*;
import org.junit.Test;

/**
 * Basic tests.
 */
public class SimpleCSIntroTestCase extends AbstractCheatSheetModelTestCase {

	protected static String INTRO_HREF = "/org.eclipse.platform.doc.user/reference/ref-cheatsheets.htm"; //$NON-NLS-1$
	protected static String DESCRIPTION = "some description"; //$NON-NLS-1$

	@Test
	public void testReadSimpleCSIntro() {
		StringBuilder buffer = new StringBuilder();
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
		assertEquals("Incorrect title", "sample cheatsheet", title);
		assertEquals(title, model.getName());
		assertEquals(ISimpleCSConstants.TYPE_CHEAT_SHEET, model.getType());
		assertTrue(model.isRoot());

		// check intro
		ISimpleCSIntro intro = model.getIntro();
		assertNotNull(intro);
		assertEquals(intro.getHref(), INTRO_HREF);
		assertNull(intro.getContextId());
		assertEquals(ISimpleCSConstants.TYPE_INTRO, intro.getType());

		// check description
		ISimpleCSDescription description = intro.getDescription();
		assertNotNull(description);
		assertEquals(DESCRIPTION, description.getContent());
		assertEquals(ISimpleCSConstants.TYPE_DESCRIPTION, description.getType());
	}

	@Test
	public void testSetModelAttributes() {
		setXMLContents(null, LF);
		load();

		ISimpleCS model = fModel.getSimpleCS();
		model.setTitle("Some Title");
		assertEquals("Some Title", model.getTitle());

		model.setTitle(null);
		assertEquals("", model.getTitle());

		ISimpleCSIntro intro = fModel.getFactory().createSimpleCSIntro(model);
		model.setIntro(intro);
		assertEquals(intro, model.getIntro());
	}

	@Test
	public void testSetIntroDescription() {
		setXMLContents(null, LF);
		load();

		ISimpleCS model = fModel.getSimpleCS();

		ISimpleCSIntro intro = fModel.getFactory().createSimpleCSIntro(model);
		model.setIntro(intro);

		ISimpleCSDescription description = fModel.getFactory().createSimpleCSDescription(intro);
		description.setContent("description content");
		intro.setDescription(description);

		assertEquals("description content", model.getIntro().getDescription().getContent());
	}

	@Test
	public void testAddSimpleCSItem() {
		setXMLContents(null, LF);
		load();

		ISimpleCS model = fModel.getSimpleCS();

		assertEquals(0, model.getItemCount());
		assertNotNull(model.getItems());
		assertFalse(model.hasItems());

		ISimpleCSItem item = fModel.getFactory().createSimpleCSItem(model);
		item.setTitle("some title"); //$NON-NLS-1$

		assertEquals(-1, model.indexOfItem(item));
		assertFalse(model.isFirstItem(item));
		assertFalse(model.isLastItem(item));

		model.addItem(item);

		assertEquals(1, model.getItemCount());
		assertNotNull(model.getItems());
		assertEquals(1, model.getItems().length);
		assertTrue(model.hasItems());

		item = model.getItems()[0];

		assertEquals("some title", item.getTitle());
		assertTrue(model.isFirstItem(item));
		assertTrue(model.isLastItem(item));
		assertEquals(0, model.indexOfItem(item));
	}

	@Test
	public void testAddSimpleCSItem2() {
		setXMLContents(null, LF);
		load();

		ISimpleCS model = fModel.getSimpleCS();

		ISimpleCSItem itemA = fModel.getFactory().createSimpleCSItem(model);
		itemA.setTitle("title A"); //$NON-NLS-1$
		ISimpleCSItem itemB = fModel.getFactory().createSimpleCSItem(model);
		itemB.setTitle("title B"); //$NON-NLS-1$

		model.addItem(0, itemA);
		model.addItem(0, itemB);

		assertTrue(model.isFirstItem(itemB));
		assertTrue(model.isLastItem(itemA));
	}

	@Test
	public void testRemoveSimpleCSItem() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("<intro></intro>");
		buffer.append("<item title=\"item1\"></item>");
		setXMLContents(buffer, LF);
		load();

		ISimpleCS model = fModel.getSimpleCS();

		assertEquals(1, model.getItemCount());
		model.removeItem(1);

		assertEquals(0, model.getItemCount());
	}

	@Test
	public void testRemoveSimpleCSItem2() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("<intro></intro>");
		buffer.append("<item title=\"item1\"></item>");
		buffer.append("<item title=\"item2\"></item>");
		setXMLContents(buffer, LF);
		load();

		ISimpleCS model = fModel.getSimpleCS();

		assertEquals(2, model.getItemCount());
		ISimpleCSItem item = model.getItems()[0];
		assertEquals("item1", item.getTitle());
		model.removeItem(item);

		assertEquals(1, model.getItemCount());
		item = model.getItems()[0];
		assertNotNull(item);
		assertEquals("item2", item.getTitle());
		assertEquals(1, model.indexOfItem(model.getItems()[0]));
	}

	@Test
	public void testMoveSimpleCSItem() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("<intro></intro>");
		buffer.append("<item title=\"item1\"></item>");
		buffer.append("<item title=\"item2\"></item>");
		setXMLContents(buffer, LF);
		load();

		ISimpleCS model = fModel.getSimpleCS();

		ISimpleCSItem item1 = model.getItems()[0];
		ISimpleCSItem item2 = model.getItems()[1];

		assertEquals(0, model.indexOf(model.getIntro()));
		assertEquals(1, model.indexOfItem(item1));
		assertEquals(2, model.indexOfItem(item2));
		assertEquals(item2, model.getNextSibling(item1));
		assertEquals(item1, model.getPreviousSibling(item2));

		model.moveItem(item1, 0); // = don't move

		assertEquals(1, model.indexOfItem(item1));
		assertEquals(2, model.indexOfItem(item2));
		assertEquals(item2, model.getNextSibling(item1));
		assertEquals(item1, model.getPreviousSibling(item2));

		model.moveItem(item1, +1);

		assertEquals(2, model.indexOfItem(item1));
		assertEquals(1, model.indexOfItem(item2));
		assertEquals(item1, model.getNextSibling(item2));
		assertEquals(item2, model.getPreviousSibling(item1));

		model.moveItem(item2, -1);

		assertEquals(2, model.indexOfItem(item1));
		assertEquals(0, model.indexOfItem(item2));

		model.moveItem(item2, -1); // effectively no move, because item2 is already at index 0

		assertEquals(2, model.indexOfItem(item1));
		assertEquals(0, model.indexOfItem(item2));
	}

	//bug 285134
	@Test
	public void testSingleQuoteAttributes() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("<intro></intro>");
		buffer.append("<item ");
		buffer.append(LF);
		buffer.append("title='Item'>");
		buffer.append("<description>");
		buffer.append(DESCRIPTION);
		buffer.append("</description>");
		buffer.append(LF);
		buffer.append("</item>");
		setXMLContents(buffer, LF);
		load();

		ISimpleCS model = fModel.getSimpleCS();
		ISimpleCSItem item = model.getItems()[0];
		assertEquals(item.getTitle(), "Item");
	}
}

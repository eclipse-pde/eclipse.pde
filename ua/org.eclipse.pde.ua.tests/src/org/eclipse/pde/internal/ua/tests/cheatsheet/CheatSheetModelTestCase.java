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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCS;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSAction;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSCommand;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSConditionalSubItem;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSConstants;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSDescription;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSItem;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSOnCompletion;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSPerformWhen;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSRepeatedSubItem;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSRunContainerObject;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSSubItem;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSSubItemObject;

/**
 * Provides helper methods to create and validate cheatsheet model elements.
 * Two sets of factory methods are provided: ones that generate valid cheatsheat
 * XML tags and second that build cheatsheet model using API. Both generate the same data
 * that can be validated using validate* methods.
 */
public abstract class CheatSheetModelTestCase extends AbstractCheatSheetModelTestCase {

	protected ISimpleCSAction createAction() {
		ISimpleCSAction action = fModel.getFactory().createSimpleCSAction(null);
		action.setClazz("org.eclipse.some.Clazz");
		action.setPluginId("org.eclipse.pde.plugin.xyz");
		action.setParam("param1.value", 1);
		action.setParam("20", 2);
		return action;
	}

	protected ISimpleCSPerformWhen createPerformWhen() {
		ISimpleCSPerformWhen performWhen = fModel.getFactory().createSimpleCSPerformWhen(null);
		performWhen.setCondition("some.example.condition");
		return performWhen;
	}

	protected ISimpleCSCommand createCommand() {
		ISimpleCSCommand action = fModel.getFactory().createSimpleCSCommand(null);
		action.setRequired(true);
		action.setSerialization("org.eclipse.my.command");
		return action;
	}

	protected ISimpleCSItem createComplexCSItem() {
		ISimpleCSItem item = fModel.getFactory().createSimpleCSItem(null);
		item.setSkip(true);
		item.setTitle("Title");
		item.setDialog(true);

		ISimpleCSDescription description = fModel.getFactory().createSimpleCSDescription(item);
		description.setContent("Description1");
		item.setDescription(description);

		ISimpleCSOnCompletion onCompletion = fModel.getFactory().createSimpleCSOnCompletion(item);
		onCompletion.setContent("On.Completion.Contents");
		item.setOnCompletion(onCompletion);

		return item;
	}

	protected ISimpleCSSubItemObject createConditionalSubitem() {
		ISimpleCSConditionalSubItem subitem = fModel.getFactory().createSimpleCSConditionalSubItem(null);
		subitem.setCondition("please.do");
		return subitem;
	}

	protected StringBuilder createSimpleCSItem(ISimpleCSSubItemObject[] subitems) {
		ISimpleCSItem item = fModel.getFactory().createSimpleCSItem(null);
		item.setTitle("Title");
		for (int i = 0; subitems != null && i < subitems.length; i++) {
			item.addSubItem(subitems[i]);
		}

		return new StringBuilder(item.toString());
	}

	protected ISimpleCSSubItem createSubItem() {
		ISimpleCSSubItem subitem = fModel.getFactory().createSimpleCSSubItem(null);
		subitem.setLabel("label1");
		subitem.setSkip(true);
		subitem.setWhen("always");

		return subitem;
	}

	protected ISimpleCSRepeatedSubItem createRepeatedSubItem() {
		ISimpleCSRepeatedSubItem subitem = fModel.getFactory().createSimpleCSRepeatedSubItem(null);
		subitem.setValues("repeat.value");
		return subitem;
	}

	protected StringBuilder createSimpleCSItem(String subitems, String newline) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("<item title=\"Title\">").append(newline);
		buffer.append(subitems);
		buffer.append("</item>").append(newline);
		return buffer;
	}

	protected StringBuilder createComplexCSItem(String children, String newline) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("<item").append(newline);
		buffer.append(" skip=\"true\"").append(newline);
		buffer.append(" title=\"Title\"").append(newline);
		buffer.append(" dialog=\"true\"").append(newline);
		buffer.append(">").append(newline);
		buffer.append("<description>").append(newline);
		buffer.append("Description1").append(newline);
		buffer.append("</description>").append(newline);
		buffer.append("<onCompletion>").append(newline);
		buffer.append("On.Completion.Contents").append(newline);
		buffer.append("</onCompletion>").append(newline);
		buffer.append(children);
		buffer.append("</item>").append(newline);
		return buffer;
	}

	protected String createAction(String newline) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("<action").append(newline);
		buffer.append(" class=\"org.eclipse.some.Clazz\"").append(newline);
		buffer.append(" pluginId=\"org.eclipse.pde.plugin.xyz\"").append(newline);
		buffer.append(" param1=\"param1.value\"").append(newline);
		buffer.append(" param2=\"20\"").append(newline);
		buffer.append("/>").append(newline);
		return buffer.toString();
	}

	protected String createPerformWhen(String executables, String newline) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("<perform-when").append(newline);
		buffer.append(" condition=\"some.example.condition\"").append(newline);
		buffer.append(">").append(newline);
		buffer.append(executables).append(newline);
		buffer.append("</perform-when>").append(newline);
		return buffer.toString();
	}

	protected String createCommand(String newline) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("<command").append(newline);
		buffer.append("required=\"true\"").append(newline);
		buffer.append("serialization=\"org.eclipse.my.command\"").append(newline);
		buffer.append("/>").append(newline);
		return buffer.toString();
	}

	protected String createSubItem(String children, String newline) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("<subitem").append(newline);
		buffer.append(" label=\"label1\"").append(newline);
		buffer.append(" skip=\"true\"").append(newline);
		buffer.append(" when=\"always\"").append(newline);
		buffer.append(">").append(newline);
		buffer.append(children);
		buffer.append("</subitem>").append(newline);
		return buffer.toString();
	}

	protected String createRepeatedSubItem(String children, String newline) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("<repeated-subitem").append(newline);
		buffer.append(" values=\"repeat.value\"").append(newline);
		buffer.append(">").append(newline);
		buffer.append(children);
		buffer.append("</repeated-subitem>").append(newline);
		return buffer.toString();
	}

	protected String createConditionalSubItem(String children, String newline) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("<conditional-subitem").append(newline);
		buffer.append(" condition=\"please.do\"").append(newline);
		buffer.append(">").append(newline);
		buffer.append(children);
		buffer.append("</conditional-subitem>").append(newline);
		return buffer.toString();
	}

	protected void validateComplexCSItem(ISimpleCSItem item) {
		assertTrue(item.getDialog());
		assertTrue(item.getSkip());
		assertEquals(item.getTitle(), "Title"); //$NON-NLS-1$

		ISimpleCSDescription description = item.getDescription();
		assertNotNull(description);
		assertEquals(ISimpleCSConstants.TYPE_DESCRIPTION, description.getType());
		assertEquals(description.getContent(), description.getName());
		assertEquals(description.getContent(), "Description1"); //$NON-NLS-1$

		ISimpleCSOnCompletion onCompletion = item.getOnCompletion();
		assertNotNull(onCompletion);
		assertEquals(ISimpleCSConstants.TYPE_ON_COMPLETION, onCompletion.getType());
		assertEquals(ISimpleCSConstants.ELEMENT_ONCOMPLETION, onCompletion.getName());
		assertEquals(onCompletion.getContent(), "On.Completion.Contents"); //$NON-NLS-1$
	}

	protected void validateSubItem(ISimpleCSSubItemObject subitem) {
		assertTrue(subitem instanceof ISimpleCSSubItem);
		assertEquals(ISimpleCSConstants.TYPE_SUBITEM, subitem.getType());
		ISimpleCSSubItem simpleSubitem = (ISimpleCSSubItem) subitem;
		assertEquals(simpleSubitem.getLabel(), "label1"); //$NON-NLS-1$
		assertEquals(simpleSubitem.getName(), "label1"); //$NON-NLS-1$
		assertTrue(simpleSubitem.getSkip());
		assertEquals(simpleSubitem.getWhen(), "always"); //$NON-NLS-1$
	}

	protected void validateRepeatedSubItem(ISimpleCSSubItemObject subitem) {
		assertTrue(subitem instanceof ISimpleCSRepeatedSubItem);
		assertEquals(ISimpleCSConstants.TYPE_REPEATED_SUBITEM, subitem.getType());
		assertEquals(((ISimpleCSRepeatedSubItem) subitem).getValues(), "repeat.value"); //$NON-NLS-1$
	}

	protected void validateConditionalSubItem(ISimpleCSSubItemObject subitem) {
		assertTrue(subitem instanceof ISimpleCSConditionalSubItem);
		assertEquals(ISimpleCSConstants.TYPE_CONDITIONAL_SUBITEM, subitem.getType());
		assertEquals(((ISimpleCSConditionalSubItem) subitem).getCondition(), "please.do"); //$NON-NLS-1$
	}

	protected void validateSubItemsCount(int expected, ISimpleCSItem item) {
		assertTrue(item.hasSubItems());
		assertEquals(expected, item.getSubItemCount());
		ISimpleCSSubItemObject[] subitems = item.getSubItems();
		assertNotNull(subitems);
		assertEquals(expected, subitems.length);
	}

	protected void validateItemsCount(int expected, ISimpleCS model) {
		assertTrue(model.hasItems());
		assertEquals(expected, model.getItemCount());
		ISimpleCSItem[] items = model.getItems();
		assertEquals(expected, items.length);
	}

	protected void validateAction(ISimpleCSRunContainerObject executable) {
		assertNotNull(executable);
		assertTrue(executable instanceof ISimpleCSAction);
		ISimpleCSAction action = (ISimpleCSAction) executable;

		String[] params = action.getParams();
		assertNotNull(params);
		assertEquals(2, params.length);
		assertEquals(params[0], "param1.value"); //$NON-NLS-1$
		assertEquals(params[1], "20"); //$NON-NLS-1$

		assertEquals(action.getClazz(), "org.eclipse.some.Clazz"); //$NON-NLS-1$
		assertEquals(action.getPluginId(), "org.eclipse.pde.plugin.xyz"); //$NON-NLS-1$
		assertEquals(null, action.getParam(0)); // params are indexed starting with 1
		assertEquals(action.getParam(1), "param1.value"); //$NON-NLS-1$
		assertEquals(action.getParam(2), "20"); //$NON-NLS-1$
		assertEquals(null, action.getParam(3));
	}

	protected void validateCommand(ISimpleCSRunContainerObject executable) {
		assertNotNull(executable);
		assertTrue(executable instanceof ISimpleCSCommand);
		ISimpleCSCommand command = (ISimpleCSCommand) executable;

		assertTrue(command.getRequired());
		assertEquals(command.getSerialization(), "org.eclipse.my.command"); //$NON-NLS-1$
	}

	protected void validatePerformWhen(ISimpleCSRunContainerObject executable) {
		assertNotNull(executable);
		assertTrue(executable instanceof ISimpleCSPerformWhen);
		ISimpleCSPerformWhen performWhen = (ISimpleCSPerformWhen) executable;

		assertEquals(performWhen.getCondition(), "some.example.condition"); //$NON-NLS-1$
	}

}

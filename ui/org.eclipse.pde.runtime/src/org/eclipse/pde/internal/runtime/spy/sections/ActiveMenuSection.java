/*******************************************************************************
 * Copyright (c) 2009 EclipseSource Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     EclipseSource Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.spy.sections;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.action.*;
import org.eclipse.pde.internal.runtime.PDERuntimeMessages;
import org.eclipse.pde.internal.runtime.spy.SpyFormToolkit;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.internal.IActionSetContributionItem;
import org.eclipse.ui.internal.PluginAction;
import org.eclipse.ui.menus.CommandContributionItem;

/**
 * @since 3.5
 */
public class ActiveMenuSection implements ISpySection {

	public void build(ScrolledForm form, SpyFormToolkit toolkit, Event event) {

		Object object = event.widget.getData();
		if (object != null) {
			Section section = toolkit.createSection(form.getBody(), ExpandableComposite.TITLE_BAR);
			section.clientVerticalSpacing = 9;
			section.setText(PDERuntimeMessages.SpyDialog_activeSelection_title);
			FormText text = toolkit.createFormText(section, true);
			section.setClient(text);

			TableWrapData td = new TableWrapData();
			td.align = TableWrapData.FILL;
			td.grabHorizontal = true;
			section.setLayoutData(td);

			StringBuffer buffer = new StringBuffer();
			buffer.append("<form>"); //$NON-NLS-1$
			if (object instanceof IContributionItem) {
				IContributionItem item = (IContributionItem) object;
				String id = item.getId();
				if (id != null) {
					buffer.append(toolkit.createIdentifierSection(text, PDERuntimeMessages.ActiveMenuSection_0, new String[] {id}));
				}
				scan(item, buffer, toolkit, text);
			}

			buffer.append("</form>"); //$NON-NLS-1$
			text.setText(buffer.toString(), true, false);
		}
	}

	// FIXME this is a bit hackish but works... rearchitect
	private void scan(IContributionItem item, StringBuffer buffer, SpyFormToolkit toolkit, FormText text) {
		// check for action set information
		if (item instanceof IActionSetContributionItem) {
			IActionSetContributionItem actionItem = (IActionSetContributionItem) item;
			buffer.append(toolkit.createIdentifierSection(text, PDERuntimeMessages.ActiveMenuSection_1, new String[] {actionItem.getActionSetId()}));
		}
		if (item instanceof ActionContributionItem) {
			createActionContributionItemText(item, buffer, toolkit, text);
		} else if (item instanceof SubContributionItem) {
			SubContributionItem subItem = (SubContributionItem) item;
			scan(subItem.getInnerItem(), buffer, toolkit, text); // recurse
		} else if (item instanceof CommandContributionItem) { // TODO... this is hard...
			CommandContributionItem contributionItem = (CommandContributionItem) item;
			ICommandService service = (ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class);
			Command command = service.getCommand(contributionItem.getCommand().getId());
			//buffer.append(toolkit.createIdentifierSection(text, "The active command category id:", new String[] {command.getCategory().getId()}));
			buffer.append(toolkit.createClassSection(text, PDERuntimeMessages.ActiveMenuSection_2, new Class[] {command.getClass()}));
			buffer.append(toolkit.createClassSection(text, PDERuntimeMessages.ActiveMenuSection_3, new Class[] {command.getHandler().getClass()}));
			createContributionItemText(item, buffer, toolkit);
		}
	}

	private void createContributionItemText(Object object, StringBuffer buffer, SpyFormToolkit toolkit) {
		// IContributionItem item = (IContributionItem) object;
		// TODO call ICommandService to get the actual command...
	}

	private void createActionContributionItemText(Object object, StringBuffer buffer, SpyFormToolkit toolkit, FormText text) {
		ActionContributionItem actionItem = (ActionContributionItem) object;
		IAction action = actionItem.getAction();

		String id = action.getActionDefinitionId();
		if (id != null) {
			buffer.append(toolkit.createIdentifierSection(text, PDERuntimeMessages.ActiveMenuSection_4, new String[] {action.getActionDefinitionId()}));
		}

		if (action instanceof PluginAction) {
			PluginAction pluginAction = (PluginAction) action;
			Class clazz = pluginAction.getClass();
			createActionContributionItemText(object, buffer, toolkit, text, clazz, pluginAction);

		} else {
			// normal JFace Actions
			Class clazz = action.getClass();
			buffer.append(toolkit.createClassSection(text, PDERuntimeMessages.ActiveMenuSection_5, new Class[] {clazz}));
		}

	}

	private void createActionContributionItemText(Object object, StringBuffer buffer, SpyFormToolkit toolkit, FormText text, Class clazz, PluginAction pluginAction) {
		try {
			Field field = clazz.getDeclaredField("delegate"); //$NON-NLS-1$
			field.setAccessible(true);
			IActionDelegate delegate = (IActionDelegate) field.get(pluginAction);
			if (delegate == null) { // have to invoke createDelegate if we don't have one yet...
				Method method = clazz.getDeclaredMethod("createDelegate", null); //$NON-NLS-1$
				method.setAccessible(true);
				method.invoke(pluginAction, null);
				delegate = (IActionDelegate) field.get(pluginAction);
			}

			buffer.append(toolkit.createClassSection(text, PDERuntimeMessages.ActiveMenuSection_6, new Class[] {delegate.getClass()}));

		} catch (Exception e) {
			createActionContributionItemText(object, buffer, toolkit, text, clazz.getSuperclass(), pluginAction);
		}
	}

	public void build(ScrolledForm form, SpyFormToolkit toolkit, ExecutionEvent event) {
		// do nothing
	}

}

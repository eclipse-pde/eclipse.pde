/*******************************************************************************
 * Copyright (c) 2009, 2010 EclipseSource Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     EclipseSource Corporation - initial API and implementation
 *     Anyware Technologies - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.spy.sections;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.action.*;
import org.eclipse.pde.internal.runtime.PDERuntimeMessages;
import org.eclipse.pde.internal.runtime.PDERuntimePlugin;
import org.eclipse.pde.internal.runtime.spy.SpyFormToolkit;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.actions.RetargetAction;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.internal.*;
import org.eclipse.ui.menus.CommandContributionItem;
import org.osgi.framework.Bundle;
import org.osgi.service.packageadmin.PackageAdmin;

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
				if (object instanceof ContributionItem) {
					createLocationURI(toolkit, object, text, buffer, id);
				}
				scan(item, buffer, toolkit, text);
			}

			buffer.append("</form>"); //$NON-NLS-1$
			text.setText(buffer.toString(), true, false);
		}
	}

	private void createLocationURI(SpyFormToolkit toolkit, Object object, FormText text, StringBuffer buffer, String id) {
		IContributionManager parent = ((ContributionItem) object).getParent();
		if (parent instanceof IMenuManager) {
			String parentId = ((IMenuManager) parent).getId();
			String locationURI = "menu:" + parentId + (id == null ? "?after=additions" : "?after=" + id); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			buffer.append(toolkit.createIdentifierSection(text, PDERuntimeMessages.ActiveMenuSection_7, new String[] {locationURI}));
		} else if (parent instanceof ToolBarManager) {
			ToolBar bar = ((ToolBarManager) parent).getControl();
			if (bar.getParent() instanceof CoolBar) {
				CoolItem[] items = ((CoolBar) bar.getParent()).getItems();
				for (int i = 0; i < items.length; i++) {
					CoolItem coolItem = items[i];
					if (coolItem.getControl() == bar) {
						Object o = coolItem.getData();
						if (o instanceof ToolBarContributionItem) {
							String parentId = ((ToolBarContributionItem) o).getId();
							String locationURI = "toolbar:" + parentId + (id == null ? "?after=additions" : "?after=" + id); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							buffer.append(toolkit.createIdentifierSection(text, PDERuntimeMessages.ActiveMenuSection_7, new String[] {locationURI}));
						}
						continue;
					}
				}
			}
		}
	}

	// FIXME this is a bit hackish but works... need to redo
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
			Command command = contributionItem.getCommand().getCommand();
			buffer.append(toolkit.createClassSection(text, PDERuntimeMessages.ActiveMenuSection_2, new Class[] {command.getClass()}));
			buffer.append(toolkit.createClassSection(text, PDERuntimeMessages.ActiveMenuSection_3, new Class[] {command.getHandler().getClass()}));
		}
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
			PackageAdmin admin = PDERuntimePlugin.getDefault().getPackageAdmin();
			Bundle bundle = admin.getBundle(clazz);
			toolkit.generatePluginDetailsText(bundle, null, "meow", buffer, text); //$NON-NLS-1$
		}

	}

	private void createActionContributionItemText(Object object, StringBuffer buffer, SpyFormToolkit toolkit, FormText text, Class clazz, PluginAction pluginAction) {
		try {
			RetargetAction retargetAction = null;
			IActionDelegate delegate = null;
			if (pluginAction instanceof WWinPluginAction) {
				// such an action *may* have a retarget action
				Field field = clazz.getDeclaredField("retargetAction"); //$NON-NLS-1$
				field.setAccessible(true);
				retargetAction = (RetargetAction) field.get(pluginAction);
			}
			// if there's no retarget action OR if the pluginAction is not a WWinPluginAction, let's try to find the action delegate
			if (retargetAction == null) {
				Field field = clazz.getDeclaredField("delegate"); //$NON-NLS-1$
				field.setAccessible(true);
				delegate = (IActionDelegate) field.get(pluginAction);
				if (delegate == null) { // have to invoke createDelegate if we don't have one yet...
					Method method = clazz.getDeclaredMethod("createDelegate", null); //$NON-NLS-1$
					method.setAccessible(true);
					method.invoke(pluginAction, null);
					delegate = (IActionDelegate) field.get(pluginAction);
				}
			}
			buffer.append(toolkit.createClassSection(text, PDERuntimeMessages.ActiveMenuSection_6, new Class[] {(retargetAction == null) ? delegate.getClass() : retargetAction.getActionHandler().getClass()}));
			PackageAdmin admin = PDERuntimePlugin.getDefault().getPackageAdmin();
			Bundle bundle = admin.getBundle(clazz);
			toolkit.generatePluginDetailsText(bundle, null, "menu item", buffer, text); //$NON-NLS-1$

		} catch (Exception e) {
			Class superclass = clazz.getSuperclass();
			if (superclass != null) {
				createActionContributionItemText(object, buffer, toolkit, text, superclass, pluginAction);
			}
		}
	}

	public void build(ScrolledForm form, SpyFormToolkit toolkit, ExecutionEvent event) {
		// do nothing
	}

}

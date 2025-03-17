/*******************************************************************************
 * Copyright (c) 2009, 2017 EclipseSource Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.SubContributionItem;
import org.eclipse.jface.action.ToolBarContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.pde.internal.runtime.PDERuntimeMessages;
import org.eclipse.pde.internal.runtime.spy.SpyFormToolkit;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.CoolItem;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.actions.RetargetAction;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.menus.CommandContributionItem;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

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

			StringBuilder buffer = new StringBuilder();
			buffer.append("<form>"); //$NON-NLS-1$
			if (object instanceof IContributionItem item) {
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

	private void createLocationURI(SpyFormToolkit toolkit, Object object, FormText text, StringBuilder buffer, String id) {
		IContributionManager parent = ((ContributionItem) object).getParent();
		if (parent instanceof IMenuManager) {
			String parentId = ((IMenuManager) parent).getId();
			String locationURI = "menu:" + parentId + (id == null ? "?after=additions" : "?after=" + id); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			buffer.append(toolkit.createIdentifierSection(text, PDERuntimeMessages.ActiveMenuSection_7, new String[] {locationURI}));
		} else if (parent instanceof ToolBarManager) {
			ToolBar bar = ((ToolBarManager) parent).getControl();
			if (bar.getParent() instanceof CoolBar) {
				CoolItem[] items = ((CoolBar) bar.getParent()).getItems();
				for (CoolItem item : items) {
					if (item.getControl() == bar) {
						Object o = item.getData();
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
	@SuppressWarnings("restriction")
	private void scan(IContributionItem item, StringBuilder buffer, SpyFormToolkit toolkit, FormText text) {
		// check for action set information
		if (item instanceof org.eclipse.ui.internal.IActionSetContributionItem actionItem) {
			buffer.append(toolkit.createIdentifierSection(text, PDERuntimeMessages.ActiveMenuSection_1, new String[] {actionItem.getActionSetId()}));
		}
		if (item instanceof ActionContributionItem) {
			createActionContributionItemText(item, buffer, toolkit, text);
		} else if (item instanceof SubContributionItem subItem) {
			scan(subItem.getInnerItem(), buffer, toolkit, text); // recurse
		} else if (item instanceof CommandContributionItem contributionItem) { // TODO... this is hard...
			Command command = contributionItem.getCommand().getCommand();
			buffer.append(toolkit.createClassSection(text, PDERuntimeMessages.ActiveMenuSection_2, command.getClass()));
			buffer.append(toolkit.createClassSection(text, PDERuntimeMessages.ActiveMenuSection_3,
					command.getHandler().getClass()));
		}
	}

	@SuppressWarnings("restriction")
	private void createActionContributionItemText(Object object, StringBuilder buffer, SpyFormToolkit toolkit, FormText text) {
		ActionContributionItem actionItem = (ActionContributionItem) object;
		IAction action = actionItem.getAction();

		String id = action.getActionDefinitionId();
		if (id != null) {
			buffer.append(toolkit.createIdentifierSection(text, PDERuntimeMessages.ActiveMenuSection_4, new String[] {action.getActionDefinitionId()}));
		}

		if (action instanceof org.eclipse.ui.internal.PluginAction pluginAction) {
			Class<?> clazz = pluginAction.getClass();
			createActionContributionItemText(object, buffer, toolkit, text, clazz, pluginAction);

		} else {
			// normal JFace Actions
			Class<? extends IAction> clazz = action.getClass();
			buffer.append(toolkit.createClassSection(text, PDERuntimeMessages.ActiveMenuSection_5, new Class[] {clazz}));
			Bundle bundle = FrameworkUtil.getBundle(clazz);
			toolkit.generatePluginDetailsText(bundle, null, "meow", buffer, text); //$NON-NLS-1$
		}

	}

	@SuppressWarnings("restriction")
	private void createActionContributionItemText(Object object, StringBuilder buffer, SpyFormToolkit toolkit,
			FormText text, Class<?> clazz, org.eclipse.ui.internal.PluginAction pluginAction) {
		try {
			RetargetAction retargetAction = null;
			IActionDelegate delegate = null;
			if (pluginAction instanceof org.eclipse.ui.internal.WWinPluginAction) {
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
					Method method = clazz.getDeclaredMethod("createDelegate"); //$NON-NLS-1$
					method.setAccessible(true);
					method.invoke(pluginAction);
					delegate = (IActionDelegate) field.get(pluginAction);
				}
			}
			buffer.append(toolkit.createClassSection(text, PDERuntimeMessages.ActiveMenuSection_6,
					(retargetAction == null) ? delegate.getClass() : retargetAction.getActionHandler().getClass()));
			Bundle bundle = FrameworkUtil.getBundle(clazz);
			toolkit.generatePluginDetailsText(bundle, null, "menu item", buffer, text); //$NON-NLS-1$

		} catch (Exception e) {
			Class<?> superclass = clazz.getSuperclass();
			if (superclass != null) {
				createActionContributionItemText(object, buffer, toolkit, text, superclass, pluginAction);
			}
		}
	}

	@Override
	public void build(ScrolledForm form, SpyFormToolkit toolkit, ExecutionEvent event) {
		// do nothing
	}

}

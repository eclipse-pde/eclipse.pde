/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;
import org.eclipse.pde.api.tools.ui.internal.views.APIToolingView;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.actions.ActionFactory;

/**
 * Action to navigate the changes shown in the APITooling View.
 */
public class NavigateAction extends Action {
	private final boolean next;
	private IViewSite site;
	private TreeViewer viewer;
	
	public NavigateAction(APIToolingView view, boolean next) {
		this.site = view.getViewSite();
		this.viewer = view.viewer;
		this.next = next;
		IActionBars bars = site.getActionBars();
		if (next) {
			setText(ActionMessages.NextAction_label);
			setToolTipText(ActionMessages.NextAction_tooltip);
			ImageDescriptor enabledImageDescriptor = ApiUIPlugin.getImageDescriptor(IApiToolsConstants.IMG_ELCL_NEXT_NAV);
			setImageDescriptor(enabledImageDescriptor);
			ImageDescriptor disabledImageDescriptor = ApiUIPlugin.getImageDescriptor(IApiToolsConstants.IMG_DLCL_NEXT_NAV);
			setDisabledImageDescriptor(disabledImageDescriptor);
			setActionDefinitionId(ActionFactory.NEXT.getCommandId());
			if (bars != null)
				bars.setGlobalActionHandler(ActionFactory.NEXT.getId(), this);
		} else {
			setText(ActionMessages.PreviousAction_label);
			setToolTipText(ActionMessages.PreviousAction_tooltip);
			ImageDescriptor enabledImageDescriptor = ApiUIPlugin.getImageDescriptor(IApiToolsConstants.IMG_ELCL_PREV_NAV);
			setImageDescriptor(enabledImageDescriptor);
			ImageDescriptor disabledImageDescriptor = ApiUIPlugin.getImageDescriptor(IApiToolsConstants.IMG_DLCL_PREV_NAV);
			setDisabledImageDescriptor(disabledImageDescriptor);
			setActionDefinitionId(ActionFactory.PREVIOUS.getCommandId());
			if (bars != null)
				bars.setGlobalActionHandler(ActionFactory.PREVIOUS.getId(), this);
		}
	}
	
	/**
	 * Two types of navigation is supported: navigation that is specific to coordinating between a view
	 * and a compare editor and navigation simply using the configured navigator.
 	 */
	public void run() {
		TreeViewerNavigator navigator = new TreeViewerNavigator(this.viewer);
		navigator.navigateNext(this.next);
	}
}

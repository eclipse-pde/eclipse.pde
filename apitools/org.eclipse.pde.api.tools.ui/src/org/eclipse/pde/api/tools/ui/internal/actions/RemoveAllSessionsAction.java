/*******************************************************************************
 * Copyright (c) 2009, 2013 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.ui.internal.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * This action removes the active session.
 */
public class RemoveAllSessionsAction extends Action {

	public RemoveAllSessionsAction() {
		setText(ActionMessages.RemoveAllSessionsAction_label);
		setToolTipText(ActionMessages.RemoveAllSessionsAction_tooltip);
		ImageDescriptor enabledImageDescriptor = PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_REMOVEALL);
		setImageDescriptor(enabledImageDescriptor);
		ImageDescriptor disabledImageDescriptor = PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_REMOVEALL_DISABLED);
		setDisabledImageDescriptor(disabledImageDescriptor);
	}

	@Override
	public void run() {
		ApiPlugin.getDefault().getSessionManager().removeAllSessions();
	}
}
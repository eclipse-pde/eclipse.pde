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
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.ISession;
import org.eclipse.pde.api.tools.internal.provisional.ISessionManager;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;

/**
 * This action removes the active session.
 */
public class RemoveActiveSessionAction extends Action {

	public RemoveActiveSessionAction() {
		setText(ActionMessages.RemoveActiveSessionAction_label);
		setToolTipText(ActionMessages.RemoveActiveSessionAction_tooltip);
		ImageDescriptor enabledImageDescriptor = PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_REMOVE);
		setImageDescriptor(enabledImageDescriptor);
		ImageDescriptor disabledImageDescriptor = PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_REMOVE_DISABLED);
		setDisabledImageDescriptor(disabledImageDescriptor);
		setActionDefinitionId(ActionFactory.DELETE.getCommandId());
	}

	public void run() {
		ISessionManager manager = ApiPlugin.getDefault().getSessionManager();
		ISession session = manager.getActiveSession();
		if (session != null) {
			manager.removeSession(session);
		}
		ISession[] sessions = manager.getSessions();
		int length = sessions.length;
		if (length > 0) {
			manager.activateSession(sessions[length - 1]);
		}
	}
}
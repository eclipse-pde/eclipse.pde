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
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.ISession;
import org.eclipse.pde.api.tools.internal.provisional.ISessionManager;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * Drop-down action to select the active session.
 */
public class SelectSessionAction extends Action implements IMenuCreator {

	private Menu menu;

	public SelectSessionAction() {
		setText(ActionMessages.SelectSessionAction_label);
		setToolTipText(ActionMessages.SelectSessionAction_tooltip);
		setImageDescriptor(ApiUIPlugin.getImageDescriptor(IApiToolsConstants.IMG_ELCL_COMPARE_APIS));
		setDisabledImageDescriptor(ApiUIPlugin.getImageDescriptor(IApiToolsConstants.IMG_ELCL_COMPARE_APIS_DISABLED));
		setMenuCreator(this);
		setEnabled(false);
	}

	public Menu getMenu(Control parent) {
		if (menu != null) {
			menu.dispose();
		}
		menu = new Menu(parent);

		final ISessionManager manager = ApiPlugin.getDefault().getSessionManager();
		ISession[] sessions = manager.getSessions();
		ISession active = manager.getActiveSession();
		for (int i = 0; i < sessions.length; i++) {
			final ISession session = sessions[i];
			MenuItem item = new MenuItem(menu, SWT.RADIO);
			Object[] labelparams = new Object[] { new Integer(i + 1), session.getDescription()};
			item.setText(NLS.bind(ActionMessages.SelectSessionActionEntry_label,
					labelparams));
			item.setSelection(session == active);
			item.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					manager.activateSession(session);
				}
			});
		}
		return menu;
	}

	public Menu getMenu(Menu parent) {
		return null;
	}

	public void dispose() {
	}

}
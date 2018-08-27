/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Chris Aniszczyk <zx@code9.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ua.ui.editor.cheatsheet.comp.actions;

import org.eclipse.osgi.util.NLS;

public class ActionsMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.pde.internal.ua.ui.editor.cheatsheet.comp.actions.messages"; //$NON-NLS-1$

	public static String CompCSAddGroupAction_group;
	public static String CompCSAddTaskAction_task;
	public static String CompCSRemoveTaskObjectAction_delete;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, ActionsMessages.class);
	}

	private ActionsMessages() {
	}
}

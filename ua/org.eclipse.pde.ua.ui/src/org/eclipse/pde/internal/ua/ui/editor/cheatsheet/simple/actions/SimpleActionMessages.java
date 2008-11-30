/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Chris Aniszczyk <zx@code9.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple.actions;

import org.eclipse.osgi.util.NLS;

public class SimpleActionMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple.actions.messages"; //$NON-NLS-1$
	public static String SimpleCSAddStepAction_actionDescription;
	public static String SimpleCSAddStepAction_actionLabel;
	public static String SimpleCSAddStepAction_actionText;
	public static String SimpleCSAddSubStepAction_actionLabel;
	public static String SimpleCSAddSubStepAction_actionText;
	public static String SimpleCSPreviewAction_actionText;
	public static String SimpleCSRemoveRunObjectAction_actionText;
	public static String SimpleCSRemoveStepAction_actionText;
	public static String SimpleCSRemoveSubStepAction_actionText;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, SimpleActionMessages.class);
	}

	private SimpleActionMessages() {
	}
}

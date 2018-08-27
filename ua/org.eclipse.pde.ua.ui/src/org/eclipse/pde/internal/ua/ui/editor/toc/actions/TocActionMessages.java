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
package org.eclipse.pde.internal.ua.ui.editor.toc.actions;

import org.eclipse.osgi.util.NLS;

public class TocActionMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.pde.internal.ua.ui.editor.toc.actions.messages"; //$NON-NLS-1$

	public static String TocAddAnchorAction_anchor;
	public static String TocAddLinkAction_link;
	public static String TocAddTopicAction_topic;
	public static String TocRemoveObjectAction_remove;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, TocActionMessages.class);
	}

	private TocActionMessages() {
	}
}

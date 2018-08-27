/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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

package org.eclipse.pde.internal.ua.ui.editor.toc.actions;

import org.eclipse.pde.internal.ua.core.toc.text.TocLink;
import org.eclipse.pde.internal.ui.util.PDELabelUtility;

public class TocAddLinkAction extends TocAddObjectAction {
	public TocAddLinkAction() {
		setText(TocActionMessages.TocAddLinkAction_link);
	}

	@Override
	public void run() {
		if (fParentObject != null) { //Create a new link
			TocLink link = fParentObject.getModel().getFactory().createTocLink();

			//Generate the name of the link
			String name = PDELabelUtility.generateName(getChildNames(), TocActionMessages.TocAddLinkAction_link);
			link.setFieldTocPath(name);
			//Add the new link to the parent TOC object
			addChild(link);
		}
	}
}

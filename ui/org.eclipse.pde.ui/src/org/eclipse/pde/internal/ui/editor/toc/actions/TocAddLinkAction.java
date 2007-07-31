/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.toc.actions;

import org.eclipse.pde.internal.core.text.toc.TocLink;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.PDELabelUtility;

public class TocAddLinkAction extends TocAddObjectAction {
	public TocAddLinkAction() {
		setText(PDEUIMessages.TocPage_TocLink);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		if (fParentObject != null)
		{	//Create a new link
			TocLink link = 
				fParentObject.getModel().getFactory().createTocLink(); 

			//Generate the name of the link
			String name = PDELabelUtility.generateName(getChildNames(), PDEUIMessages.TocPage_TocLink);
			link.setFieldTocPath(name);
			//Add the new link to the parent TOC object
			addChild(link); 
		}
	}
}

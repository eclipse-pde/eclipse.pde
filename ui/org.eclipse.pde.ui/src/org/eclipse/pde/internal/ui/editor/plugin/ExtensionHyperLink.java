/*******************************************************************************
 *  Copyright (c) 2006, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.jface.text.IRegion;
import org.eclipse.pde.internal.ui.editor.text.AbstractHyperlink;
import org.eclipse.pde.internal.ui.search.ShowDescriptionAction;

public class ExtensionHyperLink extends AbstractHyperlink {

	public ExtensionHyperLink(IRegion region, String pointID) {
		super(region, pointID);
	}

	public void open() {
		new ShowDescriptionAction(fElement).run();
	}

}

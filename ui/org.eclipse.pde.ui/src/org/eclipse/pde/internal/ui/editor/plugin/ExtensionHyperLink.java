/*******************************************************************************
 *  Copyright (c) 2006, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
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

	@Override
	public void open() {
		new ShowDescriptionAction(fElement).run();
	}

}

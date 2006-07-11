/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.site;

import org.eclipse.pde.internal.ui.editor.PDEFormTextEditorContributor;
import org.eclipse.swt.dnd.Clipboard;

public class SiteEditorContributor extends PDEFormTextEditorContributor {

	public SiteEditorContributor() {
		super("Site"); //$NON-NLS-1$
	}

	protected boolean hasKnownTypes(Clipboard clipboard) {
		return true;
	}
}

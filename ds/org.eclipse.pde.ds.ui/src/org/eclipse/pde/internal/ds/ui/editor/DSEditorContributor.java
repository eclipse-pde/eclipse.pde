/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira Nóbrega <rafael.oliveira@gmail.com> - bug 223739
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor;

import org.eclipse.pde.internal.ui.editor.PDEFormTextEditorContributor;

public class DSEditorContributor extends PDEFormTextEditorContributor {

	public DSEditorContributor() {
		super("DS Editor"); //$NON-NLS-1$
	}

	public boolean supportsHyperlinking() {
		return true;
	}
	
	public boolean supportsContentAssist() {
		return true;
	}
}

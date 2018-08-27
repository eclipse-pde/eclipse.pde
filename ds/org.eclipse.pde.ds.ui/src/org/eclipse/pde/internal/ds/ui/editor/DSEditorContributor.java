/*******************************************************************************
 * Copyright (c) 2008, 2015 Code 9 Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira Nobrega <rafael.oliveira@gmail.com> - bug 242028
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor;

import org.eclipse.pde.internal.ui.editor.PDEFormTextEditorContributor;

public class DSEditorContributor extends PDEFormTextEditorContributor {

	public DSEditorContributor() {
		super("DS Editor"); //$NON-NLS-1$
	}

	@Override
	public boolean supportsHyperlinking() {
		return true;
	}

	@Override
	public boolean supportsContentAssist() {
		return true;
	}
}

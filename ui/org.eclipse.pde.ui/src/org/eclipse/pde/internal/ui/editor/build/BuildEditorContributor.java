/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.editor.build;

import org.eclipse.pde.internal.ui.editor.PDEFormTextEditorContributor;
import org.eclipse.swt.dnd.Clipboard;

public class BuildEditorContributor extends PDEFormTextEditorContributor {

	public BuildEditorContributor() {
		super("&Build"); //$NON-NLS-1$
	}

	protected boolean hasKnownTypes(Clipboard clipboard) {
		return true;
	}

	@Override
	public boolean supportsHyperlinking() {
		return true;
	}

	@Override
	public boolean supportsCorrectionAssist() {
		return true;
	}
}

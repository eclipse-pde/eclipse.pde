/*******************************************************************************
.
. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License 2.0 which accompanies this distribution, and is
t https://www.eclipse.org/legal/epl-2.0/
t
t SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.pde.internal.ui.editor.category;

import org.eclipse.pde.internal.ui.editor.PDEFormTextEditorContributor;
import org.eclipse.swt.dnd.Clipboard;

public class CategoryEditorContributor extends PDEFormTextEditorContributor {

	public CategoryEditorContributor() {
		super("Category"); //$NON-NLS-1$
	}

	protected boolean hasKnownTypes(Clipboard clipboard) {
		return true;
	}
}

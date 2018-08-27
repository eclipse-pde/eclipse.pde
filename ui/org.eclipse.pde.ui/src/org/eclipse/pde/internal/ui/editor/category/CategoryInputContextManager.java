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

import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;

public class CategoryInputContextManager extends InputContextManager {
	/**
	 *
	 */
	public CategoryInputContextManager(PDEFormEditor editor) {
		super(editor);
	}

	@Override
	public IBaseModel getAggregateModel() {
		return findSiteModel();
	}

	private IBaseModel findSiteModel() {
		InputContext scontext = findContext(CategoryInputContext.CONTEXT_ID);
		return (scontext != null) ? scontext.getModel() : null;
	}
}

/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ua.ui.editor.ctxhelp;

import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;

/**
 * Input context manager for the context help editor.
 * @since 3.4
 * @see CtxHelpEditor
 */
public class CtxHelpInputContextManager extends InputContextManager {

	public CtxHelpInputContextManager(PDEFormEditor editor) {
		super(editor);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.context.InputContextManager#getAggregateModel()
	 */
	public IBaseModel getAggregateModel() {
		InputContext context = findContext(CtxHelpInputContext.CONTEXT_ID);
		if (context == null) {
			return null;
		}
		return context.getModel();
	}

}

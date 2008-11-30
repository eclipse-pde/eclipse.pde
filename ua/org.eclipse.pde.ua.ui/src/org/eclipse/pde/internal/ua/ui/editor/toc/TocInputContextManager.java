/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.ui.editor.toc;

import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;

/**
 * CompCSInputContextManager
 *
 */
public class TocInputContextManager extends InputContextManager {

	/**
	 * @param editor
	 */
	public TocInputContextManager(PDEFormEditor editor) {
		super(editor);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.context.InputContextManager#getAggregateModel()
	 */
	public IBaseModel getAggregateModel() {
		InputContext context = findContext(TocInputContext.CONTEXT_ID);
		if (context == null) {
			return null;
		}
		return context.getModel();
	}

}

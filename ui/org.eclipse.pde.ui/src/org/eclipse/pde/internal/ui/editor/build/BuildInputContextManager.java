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
package org.eclipse.pde.internal.ui.editor.build;

import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;

public class BuildInputContextManager extends InputContextManager {
	/**
	 * 
	 */
	public BuildInputContextManager(PDEFormEditor editor) {
		super(editor);
	}

	public IBaseModel getAggregateModel() {
		return findBuildModel();
	}

	private IBaseModel findBuildModel() {
		InputContext bcontext = findContext(BuildInputContext.CONTEXT_ID);
		return (bcontext != null) ? bcontext.getModel() : null;
	}
}

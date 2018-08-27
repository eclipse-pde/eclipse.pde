/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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

	@Override
	public IBaseModel getAggregateModel() {
		return findBuildModel();
	}

	private IBaseModel findBuildModel() {
		InputContext bcontext = findContext(BuildInputContext.CONTEXT_ID);
		return (bcontext != null) ? bcontext.getModel() : null;
	}
}

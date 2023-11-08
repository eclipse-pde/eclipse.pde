/*******************************************************************************
 * Copyright (c) 2003, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.editor.site;

import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;

public class SiteInputContextManager extends InputContextManager {
	/**
	 *
	 */
	public SiteInputContextManager(PDEFormEditor editor) {
		super(editor);
	}

	@Override
	public IBaseModel getAggregateModel() {
		return findSiteModel();
	}

	private IBaseModel findSiteModel() {
		InputContext scontext = findContext(SiteInputContext.CONTEXT_ID);
		return (scontext != null) ? scontext.getModel() : null;
	}
}

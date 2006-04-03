/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Created on Mar 1, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.editor.feature;

import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;

public class FeatureInputContextManager extends InputContextManager {
	/**
	 * 
	 */
	public FeatureInputContextManager(PDEFormEditor editor) {
		super(editor);
	}

	public IBaseModel getAggregateModel() {
		return findFeatureModel();
	}

	private IBaseModel findFeatureModel() {
		InputContext fcontext = findContext(FeatureInputContext.CONTEXT_ID);
		return (fcontext != null) ? fcontext.getModel() : null;
	}
}

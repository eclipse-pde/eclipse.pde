/*******************************************************************************
 *  Copyright (c) 2003, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Created on Jan 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.editor.context;

import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.ui.IEditorInput;

public abstract class UTF8InputContext extends InputContext {
	/**
	 * @param editor
	 * @param input
	 */
	public UTF8InputContext(PDEFormEditor editor, IEditorInput input, boolean primary) {
		super(editor, input, primary);
	}

	protected String getDefaultCharset() {
		return "UTF-8"; //$NON-NLS-1$
	}

}

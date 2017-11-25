/*******************************************************************************
 *  Copyright (c) 2003, 2017 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.context;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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

	@Override
	protected Charset getDefaultCharset() {
		return StandardCharsets.UTF_8;
	}

}

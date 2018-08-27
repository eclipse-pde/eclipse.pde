/*******************************************************************************
 *  Copyright (c) 2003, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
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

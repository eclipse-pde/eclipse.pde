/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;
import org.eclipse.core.runtime.IAdaptable;
import java.io.*;

/**
 * Insert the type's description here.
 */
public class SystemFileEditorInputFactory implements IElementFactory {
	/**
	 * The constructor.
	 */
	public SystemFileEditorInputFactory() {
	}
	
	public IAdaptable createElement (IMemento memento) {
		String path = memento.getString("path"); //$NON-NLS-1$
		File file = new File(path);
		SystemFileEditorInput input = new SystemFileEditorInput(file);
		return input;
	}
}

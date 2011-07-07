/*******************************************************************************
 *  Copyright (c) 2006, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.outline;

import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.pde.internal.ui.editor.ISortableContentOutlinePage;

public interface IOutlineSelectionHandler {

	public void updateSelection(SelectionChangedEvent e);

	public void updateSelection(Object object);

	public ISortableContentOutlinePage getContentOutline();

}

/*******************************************************************************
 *  Copyright (c) 2006, 2011 IBM Corporation and others.
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

package org.eclipse.pde.internal.ui.editor.outline;

import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.pde.internal.ui.editor.ISortableContentOutlinePage;

public interface IOutlineSelectionHandler {

	public void updateSelection(SelectionChangedEvent e);

	public void updateSelection(Object object);

	public ISortableContentOutlinePage getContentOutline();

}

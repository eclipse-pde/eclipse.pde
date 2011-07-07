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

import org.eclipse.jface.viewers.*;

public interface IOutlineContentCreator {

	public ViewerComparator createOutlineComparator();

	public ViewerComparator createDefaultOutlineComparator();

	public ILabelProvider createOutlineLabelProvider();

	public ITreeContentProvider createOutlineContentProvider();

	public Object getOutlineInput();

}

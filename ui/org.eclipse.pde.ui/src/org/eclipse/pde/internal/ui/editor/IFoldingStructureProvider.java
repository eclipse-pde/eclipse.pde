/*******************************************************************************
 *  Copyright (c) 2006, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import java.util.Set;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.pde.internal.core.text.IEditingModel;
import org.eclipse.pde.internal.core.text.IReconcilingParticipant;

public interface IFoldingStructureProvider extends IReconcilingParticipant {

	public void initialize();

	public void addFoldingRegions(Set currentRegions, IEditingModel model) throws BadLocationException;

}

/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.model;

import org.eclipse.jface.text.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.ui.editor.text.*;

public interface IEditingModel extends IModel, IModelChangeProvider, IReconcilingParticipant, IEditable {

public IDocument getDocument();
public void setStale(boolean stale);
public boolean isStale();
}

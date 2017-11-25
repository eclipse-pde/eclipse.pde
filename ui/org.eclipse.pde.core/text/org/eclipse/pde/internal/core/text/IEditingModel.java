/*******************************************************************************
 *  Copyright (c) 2000, 2017 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text;

import java.nio.charset.Charset;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.*;

public interface IEditingModel extends IModel, IModelChangeProvider, IReconcilingParticipant, IEditable {

	public IDocument getDocument();

	public void setStale(boolean stale);

	public boolean isStale();

	public Charset getCharset();

	public void setCharset(Charset charset);

}

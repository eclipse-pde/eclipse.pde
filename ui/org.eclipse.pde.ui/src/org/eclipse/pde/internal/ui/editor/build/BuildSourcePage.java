/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.build;

import org.eclipse.ui.views.contentoutline.*;
import org.eclipse.pde.internal.ui.editor.*;

public class BuildSourcePage extends PDESourcePage {

public BuildSourcePage(PDEMultiPageEditor editor) {
	super(editor);
}
public IContentOutlinePage createContentOutlinePage() {
	return new BuildSourceOutlinePage(getEditor().getEditorInput(), getDocumentProvider(), this);
}
}

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
package org.eclipse.pde.internal.ui.editor.standalone;


import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.standalone.parser.*;
import org.eclipse.swt.graphics.*;
import org.w3c.dom.*;

/**
 * Label provider for the plugin.xml model.
 */
public class XMLOutlinePageLabelProvider2 extends LabelProvider {
	
	protected PDELabelProvider provider;
	
	public XMLOutlinePageLabelProvider2() {
		provider = PDEPlugin.getDefault().getLabelProvider();
		provider.connect(this);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.LabelProvider#dispose()
	 */
	public void dispose() {
		super.dispose();
		provider.disconnect(this);
	}
	
	public String getText(Object obj) {
		if (obj instanceof IDocumentNode)
			return ((IDocumentNode)obj).getText();
		return obj.toString();
	}

	public Image getImage(Object obj) {
		if (((IDocumentNode)obj).getContent() instanceof ProcessingInstruction)
			return provider.get(PDEPluginImages.DESC_PROCESSING_INST_OBJ);
		return provider.get(PDEPluginImages.DESC_XML_ELEMENT_OBJ);
	}
}

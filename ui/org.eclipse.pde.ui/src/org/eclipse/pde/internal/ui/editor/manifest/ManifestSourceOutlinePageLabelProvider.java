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
package org.eclipse.pde.internal.ui.editor.manifest;


import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.plugin.PluginDocumentNode;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.graphics.Image;
import org.w3c.dom.Node;


/**
 * Label provider for the plugin.xml model.
 */
public class ManifestSourceOutlinePageLabelProvider extends LabelProvider {
	
	private PDELabelProvider fProvider;
	
	public ManifestSourceOutlinePageLabelProvider() {
		fProvider = PDEPlugin.getDefault().getLabelProvider();
		fProvider.connect(this);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.LabelProvider#dispose()
	 */
	public void dispose() {
		fProvider.disconnect(this);
	}
	
	public String getText(Object obj) {
		if (obj instanceof PluginDocumentNode) {
			IPluginObject pluginObject = ((PluginDocumentNode) obj).getPluginObjectNode();
			if (pluginObject != null)
				return fProvider.getText(pluginObject);
			Node domNode = ((PluginDocumentNode) obj).getDOMNode();
			if (domNode != null)
				return domNode.getNodeName().toLowerCase();

		}
		return "";
	}

	public Image getImage(Object obj) {
		Image image = null;
		int flags = 0;
		
		if (obj instanceof PluginDocumentNode) {
			PluginDocumentNode node = (PluginDocumentNode)obj;
			flags = node.isErrorNode() ?  PDELabelProvider.F_ERROR : 0;
			IPluginObject pluginObject = node.getPluginObjectNode();
			if (pluginObject != null) {
				image = fProvider.getImage( pluginObject );
			} 
		}
		if (image == null)
			image = fProvider.get(PDEPluginImages.DESC_GENERIC_XML_OBJ);
			
		return (flags == 0) ? image : fProvider.get(image, flags);
	}

}

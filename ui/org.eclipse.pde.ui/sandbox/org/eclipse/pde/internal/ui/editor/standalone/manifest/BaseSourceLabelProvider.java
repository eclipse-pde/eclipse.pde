/*
 * Created on Sep 12, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.eclipse.pde.internal.ui.editor.standalone.manifest;

import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.standalone.*;
import org.eclipse.pde.internal.ui.editor.standalone.parser.*;
import org.eclipse.swt.graphics.*;
import org.w3c.dom.*;

/**
 * @author melhem
 */
public class BaseSourceLabelProvider extends XMLOutlinePageLabelProvider2 {
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.XMLOutlinePageLabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object obj) {
		Node node = ((IDocumentNode)obj).getContent();

		if (ManifestEditorUtil.isExtensionNode(node))
			return provider.get(PDEPluginImages.DESC_EXTENSION_OBJ);
	
		if (ManifestEditorUtil.isExtensionPointNode(node))
			return provider.get(PDEPluginImages.DESC_EXT_POINT_OBJ);
			
		if (ManifestEditorUtil.isImportNode(node))
			return provider.get(PDEPluginImages.DESC_REQ_PLUGIN_OBJ);
		
		if (ManifestEditorUtil.isLibraryNode(node))
			return provider.get(PDEPluginImages.DESC_JAVA_LIB_OBJ);
		
		if (ManifestEditorUtil.isRequiresNode(node))
			return provider.get(PDEPluginImages.DESC_REQ_PLUGINS_OBJ);
			
		if (ManifestEditorUtil.isRuntimeNode(node))
			return provider.get(PDEPluginImages.DESC_RUNTIME_OBJ);
			
		return super.getImage(obj);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.XMLOutlinePageLabelProvider#getText(java.lang.Object)
	 */
	public String getText(Object obj) {
		Node node = ((IDocumentNode) obj).getContent();
		String text = null;
		if (ManifestEditorUtil.isImportNode(node)) {
			text = ((Element)node).getAttribute("plugin");
		} else if (ManifestEditorUtil.isExtensionNode(node)) {
			text = ((Element)node).getAttribute("point");
		} else if (ManifestEditorUtil.isExtensionPointNode(node)) {
			text = ((Element)node).getAttribute("id");
		} else if (ManifestEditorUtil.isLibraryNode(node)) {
			text = ((Element)node).getAttribute("name");
		}
		return (text != null && text.length() > 0) ? text : super.getText(obj);
	}

}

/*
 * Created on Oct 7, 2003
 */
package org.eclipse.pde.internal.ui.editor.standalone.extensions;

import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.standalone.*;
import org.eclipse.pde.internal.ui.editor.standalone.parser.*;
import org.eclipse.swt.graphics.*;
import org.w3c.dom.*;

/**
 * @author melhem
 */
public class ExtensionsLabelProvider extends XMLOutlinePageLabelProvider2 {
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.standalone.XMLOutlinePageLabelProvider2#getImage(java.lang.Object)
	 */
	public Image getImage(Object obj) {
		Node node = ((IDocumentNode)obj).getContent();
		
		if (ExtensionsEditorUtil.isTopLevelNode(node))
			return provider.get(PDEPluginImages.DESC_EXTENSIONS_OBJ);

		if (ExtensionsEditorUtil.isExtensionNode(node))
			return provider.get(PDEPluginImages.DESC_EXTENSION_OBJ);
	
		if (ExtensionsEditorUtil.isExtensionPointNode(node))
			return provider.get(PDEPluginImages.DESC_EXT_POINT_OBJ);
		
		return super.getImage(obj);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.standalone.XMLOutlinePageLabelProvider2#getText(java.lang.Object)
	 */
	public String getText(Object obj) {
		Node node = ((IDocumentNode) obj).getContent();
		String text = null;
		if (ExtensionsEditorUtil.isExtensionNode(node)) {
			text = ((Element)node).getAttribute("point");
		} else if (ExtensionsEditorUtil.isExtensionPointNode(node)) {
			text = ((Element)node).getAttribute("id");
		}
		return (text != null && text.length() > 0) ? text : super.getText(obj);
	}

}

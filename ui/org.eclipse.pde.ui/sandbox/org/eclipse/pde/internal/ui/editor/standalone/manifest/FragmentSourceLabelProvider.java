/*
 * Created on Sep 12, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.eclipse.pde.internal.ui.editor.standalone.manifest;

import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.standalone.parser.*;
import org.eclipse.swt.graphics.*;
import org.w3c.dom.*;

/**
 * @author melhem
 */
public class FragmentSourceLabelProvider extends BaseSourceLabelProvider {
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.manifest.BaseSourceLabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object obj) {
		if (ManifestEditorUtil.isFragmentNode(((IDocumentNode)obj).getContent()))
			return provider.get(PDEPluginImages.DESC_FRAGMENT_OBJ);			
		return super.getImage(obj);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.manifest.BaseSourceLabelProvider#getText(java.lang.Object)
	 */
	public String getText(Object obj) {
		Node node = ((IDocumentNode) obj).getContent();
		String text = null;
		if (ManifestEditorUtil.isFragmentNode(node))
			text = ((Element) node).getAttribute("id");
		return (text != null && text.length() > 0) ? text : super.getText(obj);
	}

}

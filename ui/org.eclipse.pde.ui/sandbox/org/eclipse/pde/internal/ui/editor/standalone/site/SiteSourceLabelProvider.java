/*
 * Created on Sep 14, 2003
 */
package org.eclipse.pde.internal.ui.editor.standalone.site;

import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.standalone.*;
import org.eclipse.pde.internal.ui.editor.standalone.parser.*;
import org.eclipse.swt.graphics.*;
import org.w3c.dom.*;

/**
 * @author melhem
 */
public class SiteSourceLabelProvider extends XMLOutlinePageLabelProvider2 {
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.XMLOutlinePageLabelProvider2#getImage(java.lang.Object)
	 */
	public Image getImage(Object obj) {
		Node node = ((IDocumentNode)obj).getContent();
		if (SiteEditorUtil.isArchiveNode(node) || SiteEditorUtil.isFeatureNode(node))
			return provider.get(PDEPluginImages.DESC_JAVA_LIB_OBJ);
		if (SiteEditorUtil.isCategoryNode(node) || SiteEditorUtil.isCategoryDefNode(node))
			return provider.get(PDEPluginImages.DESC_CATEGORY_OBJ);
		if (SiteEditorUtil.isDescriptionNode(node))
			return provider.get(PDEPluginImages.DESC_DOC_SECTION_OBJ, PDELabelProvider.F_EDIT);
		//TODO add icon
		/*if (SiteEditorUtil.isSiteNode(node))
			return provider.get(PDEPluginImages.DESC_SITE_OBJ);*/
		return super.getImage(obj);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.XMLOutlinePageLabelProvider2#getText(java.lang.Object)
	 */
	public String getText(Object obj) {
		Node node = ((IDocumentNode)obj).getContent();
		String text = null;
		if (SiteEditorUtil.isFeatureNode(node)) {
			text = ((Element)node).getAttribute("id");
		} else if (SiteEditorUtil.isCategoryDefNode(node)) {
			text = ((Element)node).getAttribute("label");
			if (text == null || text.length() == 0)
				text = ((Element)node).getAttribute("name");
		} else if (SiteEditorUtil.isCategoryNode(node)) {
			text = ((Element)node).getAttribute("name");
		} else if (SiteEditorUtil.isArchiveNode(node)) {
			text = ((Element)node).getAttribute("path");
		}
		return (text != null && text.length() > 0) ? text : super.getText(obj);
	}

}

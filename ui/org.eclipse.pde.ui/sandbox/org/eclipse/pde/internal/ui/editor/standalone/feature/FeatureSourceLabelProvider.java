/*
 * Created on Sep 13, 2003
 */
package org.eclipse.pde.internal.ui.editor.standalone.feature;

import org.eclipse.jface.resource.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.standalone.*;
import org.eclipse.pde.internal.ui.editor.standalone.parser.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.ui.*;
import org.w3c.dom.*;

/**
 * @author melhem
 */
public class FeatureSourceLabelProvider extends XMLOutlinePageLabelProvider2 {
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.XMLOutlinePageLabelProvider2#getImage(java.lang.Object)
	 */
	public Image getImage(Object obj) {
		Node node = ((IDocumentNode)obj).getContent();
		if (FeatureEditorUtil.isFeatureNode(node)) 
			return provider.get(PDEPluginImages.DESC_FEATURE_OBJ);
		if (FeatureEditorUtil.isPluginNode(node))
			return provider.get(PDEPluginImages.DESC_PLUGIN_OBJ);
		if (FeatureEditorUtil.isIncludesNode(node))
			return provider.get(PDEPluginImages.DESC_FEATURE_OBJ);
		if (FeatureEditorUtil.isImportPluginNode(node))
			return provider.get(PDEPluginImages.DESC_PLUGIN_OBJ);
		if (FeatureEditorUtil.isImportFeatureNode(node))
			return provider.get(PDEPluginImages.DESC_FEATURE_OBJ);
		if (FeatureEditorUtil.isCopyrightNode(node))
			return provider.get(PDEPluginImages.DESC_DOC_SECTION_OBJ, PDELabelProvider.F_EDIT);
		if (FeatureEditorUtil.isLicenseNode(node))
			return provider.get(PDEPluginImages.DESC_DOC_SECTION_OBJ, PDELabelProvider.F_EDIT);
		if (FeatureEditorUtil.isDescriptionNode(node))
			return provider.get(PDEPluginImages.DESC_DOC_SECTION_OBJ, PDELabelProvider.F_EDIT);
		if (FeatureEditorUtil.isDataNode(node)) {
			ImageDescriptor desc =
				PlatformUI.getWorkbench().getEditorRegistry().getImageDescriptor(
					((Element)node).getAttribute("id"));
			return provider.get(desc);
		}
		//TODO add icon
		/*if (FeatureEditorUtil.isDiscoveryNode(node))
			return provider.get(PDEPluginImages.DESC_DISCOVERY_OBJ);
		if (FeatureEditorUtil.isUpdateNode(node))
			return provider.get(PDEPluginImages.DESC_UPDATE_OBJ);
		if (FeatureEditorUtil.isInstallHandlerNode(node))
			return provider.get(PDEPluginImages.DESC_INSTALL_HANDLER_OBJ);
		if (FeatureEditorUtil.isURLNode(node))
			return provider.get(PDEPluginImages.DESC_URL_OBJ);
		if (FeatureEditorUtil.isRequiresNode(node))
			return provider.get(PDEPluginImages.DESC_REQ_PLUGINS_OBJ);*/
		return super.getImage(obj);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.XMLOutlinePageLabelProvider2#getText(java.lang.Object)
	 */
	public String getText(Object obj) {
		Node node = ((IDocumentNode)obj).getContent();
		String text = null;
		if (FeatureEditorUtil.isFeatureNode(node)) {
			text = ((Element)node).getAttribute("id");
		} else if (FeatureEditorUtil.isPluginNode(node)) {
			text = ((Element)node).getAttribute("id");
		} else if (FeatureEditorUtil.isIncludesNode(node)) {
			text = ((Element)node).getAttribute("id");
		} else if (FeatureEditorUtil.isImportFeatureNode(node)) {
			text = ((Element)node).getAttribute("feature");
		} else if (FeatureEditorUtil.isImportPluginNode(node)) {
			text = ((Element)node).getAttribute("plugin");
		} else if (FeatureEditorUtil.isDataNode(node)) {
			text = ((Element)node).getAttribute("id");
		} else if (FeatureEditorUtil.isUpdateNode(node)) {
			text = ((Element)node).getAttribute("label");
		} else if (FeatureEditorUtil.isDiscoveryNode(node)) {
			text = ((Element)node).getAttribute("label");
		}
		return (text != null && text.length() > 0) ? text : super.getText(obj);
	}

}

/*
 * Created on Sep 12, 2003
 */
package org.eclipse.pde.internal.ui.editor.standalone.manifest;

import org.w3c.dom.*;

/**
 * @author melhem
 */
public class PluginSourceContentProvider extends BaseSourceContentProvider {

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.manifest.BaseSourceContentProvider#isValidTopLevelNode(org.w3c.dom.Node)
	 */
	protected boolean isValidTopLevelNode(Node node) {
		return ManifestEditorUtil.isPluginNode(node);
	}

}

/*
 * Created on Sep 12, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.eclipse.pde.internal.ui.editor.standalone.manifest;

import org.eclipse.pde.internal.ui.editor.standalone.*;

/**
 * @author melhem
 */
public class PluginManifestSourceEditor extends StandaloneXMLEditor {
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.XMLEditor#getOutlinePageContentProvider()
	 */
	protected XMLOutlinePageContentProvider2 getOutlinePageContentProvider() {
		return new PluginSourceContentProvider();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.XMLEditor#getOutlinePageLabelProvider()
	 */
	protected XMLOutlinePageLabelProvider2 getOutlinePageLabelProvider() {
		return new PluginSourceLabelProvider();
	}

}

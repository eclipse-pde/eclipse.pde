/*
 * Created on Sep 13, 2003
 */
package org.eclipse.pde.internal.ui.editor.standalone.feature;

import org.eclipse.pde.internal.ui.editor.standalone.*;

/**
 * @author melhem
 */
public class FeatureSourceEditor extends StandaloneXMLEditor {
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.XMLEditor#getOutlinePageLabelProvider()
	 */
	protected XMLOutlinePageLabelProvider2 getOutlinePageLabelProvider() {
		return new FeatureSourceLabelProvider();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.XMLEditor#getOutlinePageContentProvider()
	 */
	protected XMLOutlinePageContentProvider2 getOutlinePageContentProvider() {
		return new FeatureSourceContentProvider();
	}

}

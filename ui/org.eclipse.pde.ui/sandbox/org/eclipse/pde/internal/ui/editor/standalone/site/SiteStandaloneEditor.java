/*
 * Created on Sep 14, 2003
 */
package org.eclipse.pde.internal.ui.editor.standalone.site;

import org.eclipse.pde.internal.ui.editor.standalone.*;

/**
 * @author melhem
 */
public class SiteStandaloneEditor extends StandaloneXMLEditor {
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.XMLEditor#getOutlinePageContentProvider()
	 */
	protected XMLOutlinePageContentProvider2 getOutlinePageContentProvider() {
		return new SiteSourceContentProvider();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.XMLEditor#getOutlinePageLabelProvider()
	 */
	protected XMLOutlinePageLabelProvider2 getOutlinePageLabelProvider() {
		return new SiteSourceLabelProvider();
	}

}

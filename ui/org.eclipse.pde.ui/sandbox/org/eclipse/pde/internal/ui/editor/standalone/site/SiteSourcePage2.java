/*
 * Created on Sep 21, 2003
 */
package org.eclipse.pde.internal.ui.editor.standalone.site;

import org.eclipse.pde.internal.ui.editor.standalone.*;
import org.eclipse.pde.internal.ui.editor.standalone.parser.*;

/**
 * @author melhem
 */
public class SiteSourcePage2 extends AbstractXMLEditor {
	
	public SiteSourcePage2(DocumentModel model) {
		fModel = model;
	}
	
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

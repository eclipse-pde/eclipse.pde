package org.eclipse.pde.internal.core.site;

import org.eclipse.core.runtime.*;


public class ExternalSiteModel extends AbstractSiteModel {

	private static final long serialVersionUID = 1L;

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.AbstractModel#updateTimeStamp()
	 */
	protected void updateTimeStamp() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#isInSync()
	 */
	public boolean isInSync() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.site.AbstractSiteModel#isEditable()
	 */
	public boolean isEditable() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#load()
	 */
	public void load() throws CoreException {
	}

}

package org.eclipse.pde.internal.ui.model.build;

import java.io.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.model.*;

/**
 * @author melhem
 *
 */
public class BuildModel extends AbstractEditingModel {
	/**
	 * @param document
	 * @param isReconciling
	 */
	public BuildModel(IDocument document, boolean isReconciling) {
		super(document, isReconciling);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.AbstractEditingModel#createNLResourceHelper()
	 */
	protected NLResourceHelper createNLResourceHelper() {
		return null;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.AbstractEditingModel#getCharSetName()
	 */
	protected String getCharSetName() {
		return null;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#getUnderlyingResource()
	 */
	public IResource getUnderlyingResource() {
		return null;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#load()
	 */
	public void load() throws CoreException {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#load(java.io.InputStream, boolean)
	 */
	public void load(InputStream source, boolean outOfSync)
			throws CoreException {
	}
}

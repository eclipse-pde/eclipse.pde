package org.eclipse.pde.internal.core.product;

import java.io.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.*;


public class WorkspaceProductModel extends ProductModel implements IEditableModel {

	private static final long serialVersionUID = 1L;
	
	private IFile fFile;

	private boolean fDirty;

	private boolean fEditable;

	public WorkspaceProductModel(IFile file, boolean editable) {
		fFile = file;
		fEditable = editable;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.product.ProductModel#load()
	 */
	public void load() throws CoreException {
		if (fFile.exists()) {
			InputStream stream = null;
			try {
				stream = fFile.getContents(true);
				load(stream, false);
			} catch (CoreException e) {
			} 
		} 
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.product.ProductModel#isInSync()
	 */
	public boolean isInSync() {
		IPath path = fFile.getLocation();
		return path == null ? false : isInSync(path.toFile());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.AbstractModel#getUnderlyingResource()
	 */
	public IResource getUnderlyingResource() {
		return fFile;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.product.ProductModel#getInstallLocation()
	 */
	public String getInstallLocation() {
		return fFile.getLocation().toOSString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IEditableModel#save()
	 */
	public void save() {
		try {
			String contents = getContents();
			ByteArrayInputStream stream =
				new ByteArrayInputStream(contents.getBytes("UTF8")); //$NON-NLS-1$
			if (fFile.exists()) {
				fFile.setContents(stream, false, false, null);
			} else {
				fFile.create(stream, false, null);
			}
			stream.close();
		} catch (CoreException e) {
			PDECore.logException(e);
		} catch (IOException e) {
		}
	}
	
	public String getContents() {
		StringWriter swriter = new StringWriter();
		PrintWriter writer = new PrintWriter(swriter);
		setLoaded(true);
		save(writer);
		writer.flush();
		try {
			swriter.close();
		} catch (IOException e) {
		}
		return swriter.toString();
	}


	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IEditable#isDirty()
	 */
	public boolean isDirty() {
		return fDirty;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IEditable#save(java.io.PrintWriter)
	 */
	public void save(PrintWriter writer) {
		if (isLoaded()) {
			writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); //$NON-NLS-1$
			writer.println("<!-- This file is written by PDE and is not meant to be modified by hand. -->");
			writer.println();
			getProduct().write("", writer); //$NON-NLS-1$
		}
		setDirty(false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IEditable#setDirty(boolean)
	 */
	public void setDirty(boolean dirty) {
		fDirty = dirty;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.AbstractModel#fireModelChanged(org.eclipse.pde.core.IModelChangedEvent)
	 */
	public void fireModelChanged(IModelChangedEvent event) {
		setDirty(true);
		super.fireModelChanged(event);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.product.ProductModel#isEditable()
	 */
	public boolean isEditable() {
		return fEditable;
	}
	
}

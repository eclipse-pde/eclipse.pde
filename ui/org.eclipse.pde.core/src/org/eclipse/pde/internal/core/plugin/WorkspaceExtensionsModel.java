/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.plugin;

import java.io.*;
import java.net.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ibundle.*;

public class WorkspaceExtensionsModel
	extends AbstractExtensionsModel
	implements IEditableModel, IBundlePluginModelProvider {
	private IFile fUnderlyingResource;
	private boolean fDirty;
	private boolean fEditable = true;
	private transient IBundlePluginModelBase fBundleModel;


	protected NLResourceHelper createNLResourceHelper() {
		String name = fUnderlyingResource.getName().equals("plugin.xml") ? "plugin" : "fragment"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return new NLResourceHelper(name, getNLLookupLocations());
	}
	
	public URL getNLLookupLocation() {
		try {
			return new URL("file:" + getInstallLocation() + "/"); //$NON-NLS-1$
		} catch (MalformedURLException e) {
			return null;
		}
	}

	public WorkspaceExtensionsModel(IFile file) {
		fUnderlyingResource = file;
	}
	
	public void fireModelChanged(IModelChangedEvent event) {
		fDirty = true;
		super.fireModelChanged(event);
	}

	public String getContents() {
		StringWriter swriter = new StringWriter();
		PrintWriter writer = new PrintWriter(swriter);
		save(writer);
		writer.flush();
		try {
			swriter.close();
		} catch (IOException e) {
		}
		return swriter.toString();
	}
	
	public String getInstallLocation() {
		return fUnderlyingResource.getLocation().removeLastSegments(1).addTrailingSeparator().toOSString();
	}
	
	public URL getResourceURL(String relativePath) throws MalformedURLException {
		String location = getInstallLocation();
		if (location == null)
			return null;
		
		File file = new File(location);
		URL url = null;
		try {
			if (file.isFile() && file.getName().endsWith(".jar")) {
				url = new URL("jar:file:" + file.getAbsolutePath() + "!/" + relativePath); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				url = new URL("file:" + file.getAbsolutePath() + Path.SEPARATOR + relativePath);
			}
		} catch (MalformedURLException e) {
		}
		return url;
	}

	public IResource getUnderlyingResource() {
		return fUnderlyingResource;
	}

	public boolean isInSync() {
		if (fUnderlyingResource == null)
			return true;
		IPath path = fUnderlyingResource.getLocation();
		if (path == null)
			return false;
		return super.isInSync(path.toFile());
	}

	public boolean isDirty() {
		return fDirty;
	}
	public boolean isEditable() {
		return fEditable;
	}

	public void load() {
		if (fUnderlyingResource == null)
			return;
		if (fUnderlyingResource.exists()) {
			try {
				InputStream stream = fUnderlyingResource.getContents(true);
				load(stream, false);
				stream.close();
			} catch (Exception e) {
				PDECore.logException(e);
			}
		} else {
			getExtensions(true);		
			loaded = true;
		}
	}

	protected void updateTimeStamp() {
		updateTimeStamp(fUnderlyingResource.getLocation().toFile());
	}

	public void save() {
		if (fUnderlyingResource == null)
			return;
		try {
			String contents = getContents();
			ByteArrayInputStream stream =
				new ByteArrayInputStream(contents.getBytes("UTF8")); //$NON-NLS-1$
			if (fUnderlyingResource.exists()) {
				fUnderlyingResource.setContents(stream, false, false, null);
			} else {
				fUnderlyingResource.create(stream, false, null);
			}
			stream.close();
		} catch (CoreException e) {
			PDECore.logException(e);
		} catch (IOException e) {
		}
	}
	public void save(PrintWriter writer) {
		if (isLoaded()) {
			extensions.write("", writer); //$NON-NLS-1$
		}
		fDirty = false;
	}
	public void setDirty(boolean dirty) {
		fDirty = dirty;
	}
	public void setEditable(boolean editable) {
		fEditable = editable;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.plugin.AbstractExtensionsModel#createExtensions()
	 */
	protected Extensions createExtensions() {
		Extensions extensions = super.createExtensions();
		extensions.setIsFragment(fUnderlyingResource.getName().equals("fragment.xml")); //$NON-NLS-1$
		return extensions;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return fUnderlyingResource.getName();
	}
	
	public void setBundleModel(IBundlePluginModelBase model) {
		fBundleModel = model;
	}
	
	public IBundlePluginModelBase getBundlePluginModel() {
		return fBundleModel;
	}
	
	
}

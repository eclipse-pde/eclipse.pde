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
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.internal.core.*;

public abstract class WorkspacePluginModelBase extends AbstractPluginModelBase
		implements IEditableModel {
	
	private IFile fUnderlyingResource;

	private boolean fDirty;

	private boolean fEditable = true;

	private IBuildModel fBuildModel;

	protected NLResourceHelper createNLResourceHelper() {
		String name = isFragmentModel() ? "fragment" : "plugin"; //$NON-NLS-1$ //$NON-NLS-2$
		return new NLResourceHelper(name,getNLLookupLocations());
	}

	public URL getNLLookupLocation() {
		try {
			return new URL("file:" + getInstallLocation() + "/"); //$NON-NLS-1$
		} catch (MalformedURLException e) {
			return null;
		}
	}

	public WorkspacePluginModelBase(IFile file) {
		fUnderlyingResource = file;
		setEnabled(true);
	}

	public void fireModelChanged(IModelChangedEvent event) {
		fDirty = true;
		super.fireModelChanged(event);
	}

	public IBuildModel getBuildModel() {
		return fBuildModel;
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

	public IFile getFile() {
		return fUnderlyingResource;
	}

	public String getInstallLocation() {
		return fUnderlyingResource.getLocation().removeLastSegments(1).addTrailingSeparator().toOSString();
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
			} catch (CoreException e) {
			} catch (IOException e) {
				PDECore.logException(e);
			}
		} else {
			pluginBase = (PluginBase) createPluginBase();
			pluginBase.setModel(this);
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
			ByteArrayInputStream stream = new ByteArrayInputStream(contents
					.getBytes("UTF8")); //$NON-NLS-1$
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
			pluginBase.write("", writer); //$NON-NLS-1$
		}
		fDirty = false;
	}

	public void setBuildModel(IBuildModel buildModel) {
		fBuildModel = buildModel;
	}

	public void setDirty(boolean dirty) {
		fDirty = dirty;
	}

	public void setEditable(boolean editable) {
		fEditable = editable;
	}

}

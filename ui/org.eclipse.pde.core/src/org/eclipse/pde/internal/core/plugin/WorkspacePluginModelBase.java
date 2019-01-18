/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.core.plugin;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.core.IEditableModel;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.internal.core.NLResourceHelper;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDEManager;

/**
 * This class only represents 3.0 style plug-ins
 */
public abstract class WorkspacePluginModelBase extends AbstractPluginModelBase implements IEditableModel {

	private static final long serialVersionUID = 1L;

	private final IFile fUnderlyingResource;

	private boolean fDirty;

	private boolean fEditable = true;

	@Override
	protected NLResourceHelper createNLResourceHelper() {
		return new NLResourceHelper("plugin", PDEManager.getNLLookupLocations(this)); //$NON-NLS-1$
	}

	@Override
	@Deprecated
	public URL getNLLookupLocation() {
		try {
			return new URL("file:" + getInstallLocation() + "/"); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (MalformedURLException e) {
			return null;
		}
	}

	public WorkspacePluginModelBase(IFile file, boolean abbreviated) {
		fUnderlyingResource = file;
		fAbbreviated = abbreviated;
		setEnabled(true);
	}

	@Override
	public void fireModelChanged(IModelChangedEvent event) {
		fDirty = true;
		super.fireModelChanged(event);
	}

	@Override
	@Deprecated
	public IBuildModel getBuildModel() {
		return null;
	}

	public String getContents() {
		try (StringWriter swriter = new StringWriter(); PrintWriter writer = new PrintWriter(swriter)) {
			save(writer);
			writer.flush();
			return swriter.toString();
		} catch (IOException e) {
			PDECore.logException(e);
			return ""; //$NON-NLS-1$
		}
	}

	public IFile getFile() {
		return fUnderlyingResource;
	}

	@Override
	public String getInstallLocation() {
		IPath path = fUnderlyingResource.getLocation();
		return path == null ? null : path.removeLastSegments(1).addTrailingSeparator().toOSString();
	}

	@Override
	public IResource getUnderlyingResource() {
		return fUnderlyingResource;
	}

	@Override
	public boolean isInSync() {
		if (fUnderlyingResource == null) {
			return true;
		}
		IPath path = fUnderlyingResource.getLocation();
		if (path == null) {
			return false;
		}
		return super.isInSync(path.toFile());
	}

	@Override
	public boolean isDirty() {
		return fDirty;
	}

	@Override
	public boolean isEditable() {
		return fEditable;
	}

	@Override
	public void load() {
		if (fUnderlyingResource == null) {
			return;
		}
		if (fUnderlyingResource.exists()) {
			try (InputStream stream = new BufferedInputStream(fUnderlyingResource.getContents(true))) {
				load(stream, false);
			} catch (CoreException | IOException e) {
				PDECore.logException(e);
			}
		} else {
			fPluginBase = createPluginBase();
			setLoaded(true);
		}
	}

	@Override
	protected void updateTimeStamp() {
		updateTimeStamp(fUnderlyingResource.getLocation().toFile());
	}

	@Override
	public void save() {
		if (fUnderlyingResource == null) {
			return;
		}
		String contents = fixLineDelimiter(getContents(), fUnderlyingResource);
		try (ByteArrayInputStream stream = new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8))) {
			if (fUnderlyingResource.exists()) {
				fUnderlyingResource.setContents(stream, false, false, null);
			} else {
				fUnderlyingResource.create(stream, false, null);
			}
			stream.close();
		} catch (CoreException | IOException e) {
			PDECore.logException(e);
		}
	}

	@Override
	public void save(PrintWriter writer) {
		if (isLoaded()) {
			fPluginBase.write("", writer); //$NON-NLS-1$
		}
		fDirty = false;
	}

	@Override
	public void setDirty(boolean dirty) {
		fDirty = dirty;
	}

	public void setEditable(boolean editable) {
		fEditable = editable;
	}

}

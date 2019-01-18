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
package org.eclipse.pde.internal.core.bundle;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.core.IEditableModel;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundleModelFactory;
import org.eclipse.pde.internal.core.text.bundle.BundleModelFactory;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.core.util.ManifestUtils;

/**
 * Represents a bundle in a project in the workspace.
 */
public class WorkspaceBundleModel extends BundleModel implements IEditableModel {
	private static final long serialVersionUID = 1L;

	private final IFile fUnderlyingResource;

	private boolean fDirty;

	private boolean fEditable = true;

	private IBundleModelFactory fFactory;

	public WorkspaceBundleModel(IFile file) {
		fUnderlyingResource = file;
	}

	@Override
	public void fireModelChanged(IModelChangedEvent event) {
		setDirty(event.getChangeType() != IModelChangedEvent.WORLD_CHANGED);
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
			PDECore.logException(e);
		}
		return swriter.toString();
	}

	@Override
	public IResource getUnderlyingResource() {
		return fUnderlyingResource;
	}

	@Override
	public String getInstallLocation() {
		// Ensure we have an underlying resource
		if (fUnderlyingResource == null) {
			return null;
		}
		IPath path = fUnderlyingResource.getLocation();
		if (path == null) {
			return null;
		}
		return path.removeLastSegments(2).addTrailingSeparator().toOSString();
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
			InputStream stream = null;
			try {
				stream = fUnderlyingResource.getContents(true);
				load(stream, false);
			} catch (Exception e) {
				PDECore.logException(e);
			} finally {
				try {
					if (stream != null) {
						stream.close();
					}
				} catch (IOException e) {
					PDECore.logException(e);
				}
			}
		}
	}

	@Override
	public boolean isInSync() {
		// If we have no underlying resource, it probably got deleted from right
		// underneath us; thus, the model is not in sync
		if (fUnderlyingResource == null) {
			return false;
		} else if (fUnderlyingResource.getLocation() == null) {
			return false;
		}
		return isInSync(fUnderlyingResource.getLocation().toFile());
	}

	@Override
	protected void updateTimeStamp() {
		// If we have no underlying resource, it probably got deleted from right
		// underneath us; thus, there is nothing to update the time stamp for
		if (fUnderlyingResource == null) {
			return;
		} else if (fUnderlyingResource.getLocation() == null) {
			return;
		}
		updateTimeStamp(fUnderlyingResource.getLocation().toFile());
	}

	@Override
	public void save() {
		if (fUnderlyingResource == null) {
			return;
		}
		ByteArrayInputStream stream = null;
		try {
			String contents = fixLineDelimiter(getContents(), fUnderlyingResource);
			stream = new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8));
			if (fUnderlyingResource.exists()) {
				fUnderlyingResource.setContents(stream, false, false, null);
			} else {
				// prevents Core Exception when META-INF folder does not exist
				IContainer parent = fUnderlyingResource.getParent();
				if (!parent.exists() && parent instanceof IFolder) {
					CoreUtility.createFolder((IFolder) parent);
				}
				fUnderlyingResource.create(stream, false, null);
			}
			stream.close();
			setLoaded(true);
		} catch (CoreException e) {
			PDECore.logException(e);
		} catch (IOException e) {
			PDECore.logException(e);
		} finally {
			try {
				if (stream != null) {
					stream.close();
				}
			} catch (IOException e) {
				PDECore.logException(e);
			}
		}
	}

	@Override
	public void save(PrintWriter writer) {
		IBundle bundle = getBundle();
		Map<String, String> headers = ((Bundle) bundle).getHeaders();
		// If the bundle doesn't have a user specified manifest version header, use the default 1.0 but don't save it in the model
		boolean addManifestVersion = headers.get(ManifestUtils.MANIFEST_VERSION) == null;
		if (addManifestVersion) {
			headers.put(ManifestUtils.MANIFEST_VERSION, "1.0"); //$NON-NLS-1$
		}
		try {
			ManifestUtils.writeManifest(headers, writer);
		} catch (IOException e) {
			PDECore.logException(e);
		} finally {
			if (addManifestVersion) {
				headers.remove(ManifestUtils.MANIFEST_VERSION);
			}
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

	@Override
	public IBundleModelFactory getFactory() {
		if (fFactory == null) {
			fFactory = new BundleModelFactory(this);
		}
		return fFactory;
	}
}

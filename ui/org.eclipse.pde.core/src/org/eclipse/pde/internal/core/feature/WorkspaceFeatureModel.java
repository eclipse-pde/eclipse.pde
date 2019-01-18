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
package org.eclipse.pde.internal.core.feature;

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
import org.eclipse.pde.internal.core.NLResourceHelper;
import org.eclipse.pde.internal.core.PDECore;

public class WorkspaceFeatureModel extends AbstractFeatureModel implements IEditableModel {
	private static final long serialVersionUID = 1L;
	private boolean dirty;
	private IFile file;
	private boolean editable = true;

	public WorkspaceFeatureModel() {
		super();
	}

	public WorkspaceFeatureModel(IFile file) {
		setFile(file);
	}

	@Override
	public void fireModelChanged(IModelChangedEvent event) {
		setDirty(event.getChangeType() != IModelChangedEvent.WORLD_CHANGED);
		super.fireModelChanged(event);
	}

	@Override
	protected NLResourceHelper createNLResourceHelper() {
		try {
			// TODO revisit this method
			if (file == null || file.getLocation() == null) {
				return null;
			}
			IPath path = file.getLocation().removeLastSegments(1);
			String installLocation = path.toOSString();
			if (installLocation.startsWith("file:") == false) { //$NON-NLS-1$
				installLocation = "file:" + installLocation; //$NON-NLS-1$
			}
			URL url = new URL(installLocation + "/"); //$NON-NLS-1$
			String name = "feature"; //$NON-NLS-1$
			NLResourceHelper helper = new NLResourceHelper(name, new URL[] {url});
			//helper.setFile(file);
			return helper;
		} catch (MalformedURLException e) {
			return null;
		}
	}

	public String getContents() {
		try (StringWriter swriter = new StringWriter(); PrintWriter writer = new PrintWriter(swriter)) {
			setLoaded(true);
			save(writer);
			writer.flush();
			return swriter.toString();
		} catch (IOException e) {
			PDECore.logException(e);
			return ""; //$NON-NLS-1$
		}

	}

	public IFile getFile() {
		return file;
	}

	@Override
	public String getInstallLocation() {
		IPath path = file.getParent().getLocation();
		return path == null ? null : path.toOSString();
	}

	@Override
	public IResource getUnderlyingResource() {
		return file;
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public boolean isEditable() {
		return editable;
	}

	@Override
	public boolean isInSync() {
		return isInSync(file.getLocation().toFile());
	}

	@Override
	protected void updateTimeStamp() {
		updateTimeStamp(file.getLocation().toFile());
	}

	@Override
	public void load() {
		if (file == null) {
			return;
		}
		if (file.exists()) {
			try (InputStream stream = new BufferedInputStream(file.getContents(true))) {
				if (stream.available() > 0) {
					load(stream, false);
				} else {
					// if we have an empty file, then mark as loaded so users changes will be saved
					setLoaded(true);
				}
			} catch (CoreException e) {
				PDECore.logException(e);
			} catch (IOException e) {
				PDECore.logException(e);
			}
		} else {
			this.feature = new Feature();
			feature.model = this;
			setLoaded(true);
		}
	}

	@Override
	public void save() {
		if (file == null) {
			return;
		}
		String contents = fixLineDelimiter(getContents(), file);
		try (ByteArrayInputStream stream = new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8));) {
			if (file.exists()) {
				file.setContents(stream, false, false, null);
			} else {
				file.create(stream, false, null);
			}
		} catch (CoreException | IOException e) {
			PDECore.logException(e);
		}
	}

	@Override
	public void save(PrintWriter writer) {
		if (isLoaded()) {
			writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); //$NON-NLS-1$
			//writer.println("<!DOCTYPE feature SYSTEM \"dtd/feature.dtd\">");
			feature.write("", writer); //$NON-NLS-1$
		}
		setDirty(false);
	}

	@Override
	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	public void setEditable(boolean newEditable) {
		editable = newEditable;
	}

	public void setFile(IFile newFile) {
		file = newFile;
		//setEditable(newFile.isReadOnly()==false);
	}
}

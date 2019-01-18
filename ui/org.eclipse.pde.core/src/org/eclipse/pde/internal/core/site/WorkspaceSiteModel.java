/*******************************************************************************
 *  Copyright (c) 2000, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.site;

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

public class WorkspaceSiteModel extends AbstractSiteModel implements IEditableModel {
	private static final long serialVersionUID = 1L;
	private boolean fDirty;
	private final IFile fFile;
	private boolean fEditable = true;

	public WorkspaceSiteModel(IFile file) {
		fFile = file;
	}

	@Override
	public void fireModelChanged(IModelChangedEvent event) {
		setDirty(event.getChangeType() != IModelChangedEvent.WORLD_CHANGED);
		super.fireModelChanged(event);
	}

	protected NLResourceHelper createNLResourceHelper() {
		try {
			IPath path = fFile.getLocation().removeLastSegments(1);
			String installLocation = path.toOSString();
			if (installLocation.startsWith("file:") == false) { //$NON-NLS-1$
				installLocation = "file:" + installLocation; //$NON-NLS-1$
			}
			URL url = new URL(installLocation + "/"); //$NON-NLS-1$
			String name = "site"; //$NON-NLS-1$
			NLResourceHelper helper = new NLResourceHelper(name, new URL[] {url});
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
			return ""; //$NON-NLS-1$
		}
	}

	public IFile getFile() {
		return fFile;
	}

	@Override
	public String getInstallLocation() {
		return fFile.getParent().getLocation().toOSString();
	}

	@Override
	public IResource getUnderlyingResource() {
		return fFile;
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
	public boolean isInSync() {
		return isInSync(fFile.getLocation().toFile());
	}

	@Override
	protected void updateTimeStamp() {
		updateTimeStamp(fFile.getLocation().toFile());
	}

	@Override
	public void load() {
		if (fFile.exists()) {
			try (InputStream stream = new BufferedInputStream(fFile.getContents(true));) {
					if (stream.available() > 0) {
						load(stream, false);
					} else {
						// if we have an empty file, then mark as loaded so users changes will be saved
						setLoaded(true);
					}
			} catch (CoreException | IOException e) {
			}
		} else {
			this.site = new Site();
			site.model = this;
			setLoaded(true);
		}
	}

	@Override
	public void save() {
		String contents = fixLineDelimiter(getContents(), fFile);
		try (ByteArrayInputStream stream = new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8))) {
			if (fFile.exists()) {
				fFile.setContents(stream, false, false, null);
			} else {
				fFile.create(stream, false, null);
			}
		} catch (CoreException e) {
			PDECore.logException(e);
		} catch (IOException e) {
		}
	}

	@Override
	public void save(PrintWriter writer) {
		if (isLoaded()) {
			writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); //$NON-NLS-1$
			site.write("", writer); //$NON-NLS-1$
		}
		setDirty(false);
	}

	@Override
	public void setDirty(boolean dirty) {
		fDirty = dirty;
	}

	public void setEditable(boolean newEditable) {
		fEditable = newEditable;
	}
}

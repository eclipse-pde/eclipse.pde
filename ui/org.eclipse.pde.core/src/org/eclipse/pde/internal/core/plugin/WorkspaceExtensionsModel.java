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
 *     Simon Scholz <simon.scholz@vogella.com> - Bug 452487
 *******************************************************************************/
package org.eclipse.pde.internal.core.plugin;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.core.IEditableModel;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.NLResourceHelper;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelProvider;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.osgi.framework.Constants;

public class WorkspaceExtensionsModel extends AbstractExtensionsModel implements IEditableModel, IBundlePluginModelProvider {
	private static final long serialVersionUID = 1L;
	private final IFile fUnderlyingResource;
	private boolean fDirty;
	private boolean fEditable = true;
	private transient IBundlePluginModelBase fBundleModel;

	@Override
	protected NLResourceHelper createNLResourceHelper() {
		return new NLResourceHelper(Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME, getNLLookupLocations());
	}

	@Override
	public URL getNLLookupLocation() {
		try {
			return new URL("file:" + getInstallLocation() + "/"); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (MalformedURLException e) {
			return null;
		}
	}

	public WorkspaceExtensionsModel(IFile file) {
		fUnderlyingResource = file;
	}

	@Override
	public void fireModelChanged(IModelChangedEvent event) {
		fDirty = true;
		super.fireModelChanged(event);
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

	@Override
	public String getInstallLocation() {
		return fUnderlyingResource.getLocation().removeLastSegments(1).addTrailingSeparator().toOSString();
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
		getExtensions(true);
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
				adjustBuildPropertiesFile(fUnderlyingResource);
			}
		} catch (CoreException | IOException e) {
			PDECore.logException(e);
		}
	}

	private void adjustBuildPropertiesFile(IFile underlyingResource) throws CoreException {
		IProject project = underlyingResource.getProject();
		IFile buildPropertiesFile = PDEProject.getBuildProperties(project);
		if (buildPropertiesFile.exists()) {
			WorkspaceBuildModel model = new WorkspaceBuildModel(buildPropertiesFile);
			IBuildEntry entry = model.getBuild().getEntry(IBuildEntry.BIN_INCLUDES);
			String relativePath = underlyingResource.getProjectRelativePath().toString();
			if (!entry.contains(relativePath)) {
				entry.addToken(relativePath);
				model.save();
			}
		}
	}

	@Override
	public void save(PrintWriter writer) {
		if (isLoaded()) {
			fExtensions.write("", writer); //$NON-NLS-1$
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
	protected Extensions createExtensions() {
		Extensions extensions = super.createExtensions();
		extensions.setIsFragment(fUnderlyingResource.getName().equals(ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR));
		return extensions;
	}

	@Override
	public String toString() {
		return fUnderlyingResource.getName();
	}

	public void setBundleModel(IBundlePluginModelBase model) {
		fBundleModel = model;
	}

	@Override
	public IBundlePluginModelBase getBundlePluginModel() {
		return fBundleModel;
	}

}

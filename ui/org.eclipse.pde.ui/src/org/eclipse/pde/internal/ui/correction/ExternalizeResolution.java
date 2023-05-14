/*******************************************************************************
 *  Copyright (c) 2005, 2020 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.correction;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDEManager;
import org.eclipse.pde.internal.core.builders.PDEMarkerFactory;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.nls.ExternalizeStringsOperation;
import org.eclipse.pde.internal.ui.nls.ModelChange;
import org.eclipse.pde.internal.ui.nls.ModelChangeElement;
import org.eclipse.pde.internal.ui.util.ModelModification;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;
import org.eclipse.text.edits.MalformedTreeException;
import org.osgi.framework.Constants;

public class ExternalizeResolution extends AbstractXMLMarkerResolution {

	public ExternalizeResolution(int resolutionType, IMarker marker) {
		super(resolutionType, marker);
	}

	@Override
	protected void createChange(IPluginModelBase model) {
		Object node = findNode(model);
		ModelChange change = new ModelChange(model, true);
		ModelChangeElement element = new ModelChangeElement(change, node);
		if (element.updateValue()) {
			String localization = PDEManager.getBundleLocalization(model);
			if (localization == null)
				addLocalization(model, localization = "plugin"); //$NON-NLS-1$
			IProject project = model.getUnderlyingResource().getProject();
			IFile file = PDEProject.getLocalizationFile(project);
			checkPropertiesFile(file);
			try {
				ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
				manager.connect(file.getFullPath(), LocationKind.IFILE, null);
				ITextFileBuffer buffer = manager.getTextFileBuffer(file.getFullPath(), LocationKind.IFILE);
				if (buffer.isDirty())
					buffer.commit(null, true);
				IDocument document = buffer.getDocument();
				ExternalizeStringsOperation.getPropertiesInsertEdit(document, element).apply(document);
				buffer.commit(null, true);
			} catch (CoreException | MalformedTreeException | BadLocationException e) {
				PDEPlugin.log(e);
			} finally {
				try {
					FileBuffers.getTextFileBufferManager().disconnect(file.getFullPath(), LocationKind.IFILE, null);
				} catch (CoreException e) {
					PDEPlugin.log(e);
				}
			}
		}
	}

	@Override
	public String getLabel() {
		String locationPath = null;
		try {
			locationPath = (String) marker.getAttribute(PDEMarkerFactory.MPK_LOCATION_PATH);
		} catch (CoreException e) {
		}
		if (isAttrNode())
			return NLS.bind(PDEUIMessages.ExternalizeResolution_attrib, getNameOfNode());
		if (locationPath.charAt(0) == '(')
			return NLS.bind(PDEUIMessages.ExternalizeResolution_text, getNameOfNode());
		return NLS.bind(PDEUIMessages.ExternalizeResolution_header, locationPath);
	}

	private void addLocalization(IPluginModelBase model, String localizationValue) {
		// should always be IBundlePluginModelBase.  Only time wasn't was when we only passed in plugin.xml to ModelModification contructor.
		// Now that we pass in both the Manifest and plugin.xml if we are externalizing the a plugin.xml string (see run(IMarker)),
		// model should always be IBundlePluginModelBase
		if (model instanceof IBundlePluginModelBase) {
			IBundle bundle = ((IBundlePluginModelBase) model).getBundleModel().getBundle();
			bundle.setHeader(Constants.BUNDLE_LOCALIZATION, localizationValue);
		}
	}

	@Override
	public void run(IMarker marker) {
		fResource = marker.getResource();
		IFile file = ((IFile) marker.getResource());
		ModelModification modification = null;
		// if file we are externalizing is not manifest, try to pass manifest in if it exists
		if (!file.getName().equals(ICoreConstants.MANIFEST_FILENAME)) {
			IFile manifest = PDEProject.getManifest(file.getProject());
			if (manifest.exists()) {
				modification = new ModelModification(manifest, file) {
					@Override
					protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
						createChange(model);
					}
				};
			}
		}
		if (modification == null) {
			modification = new ModelModification(file) {
				@Override
				protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
					createChange(model);
				}
			};
		}
		PDEModelUtility.modifyModel(modification, null);
	}

	@Override
	public IMarker[] findOtherMarkers(IMarker[] markers) {
		return new IMarker[0];
	}

	private void checkPropertiesFile(IFile file) {
		if (!file.exists()) {
			String propertiesFileComment = ExternalizeStringsOperation.getPropertiesFileComment(file);
			try (ByteArrayInputStream pStream = new ByteArrayInputStream(propertiesFileComment.getBytes())) {
				IContainer container = file.getParent();
				if (!container.exists())
					// project will exists, therefore we can assume if !IContainer.exist(), the object is an IFolder
					CoreUtility.createFolder((IFolder) container);
				file.create(pStream, true, new NullProgressMonitor());
			} catch (CoreException | IOException e1) {
			}
		}
	}
}

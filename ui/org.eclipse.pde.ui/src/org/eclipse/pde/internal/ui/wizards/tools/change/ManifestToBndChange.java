/*******************************************************************************
 *  Copyright (c) 2023 Christoph Läubrich and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.tools.change;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.DependencyManager;
import org.eclipse.pde.internal.ui.PDEUIMessages;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.EE;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.osgi.Constants;
import aQute.bnd.properties.Document;

public class ManifestToBndChange extends Change {

	private final IFile manifestFile;
	private final IPluginModelBase model;
	private final boolean keepRequireBundle;
	private final boolean keepImportPackage;
	private final boolean keepBREE;
	private final boolean keepExportPackage;
	private final IFile instructionFile;

	public ManifestToBndChange(IProject project, IFile manifest, IPluginModelBase model, IFile instructionsFile,
			boolean keepRequireBundle,
			boolean keepImportPackage,
			boolean keepBREE,
			boolean keepExportPackage) {
		this.keepRequireBundle = keepRequireBundle;
		this.keepImportPackage = keepImportPackage;
		this.keepBREE = keepBREE;
		this.keepExportPackage = keepExportPackage;
		this.manifestFile = manifest;
		this.model = model;
		this.instructionFile = instructionsFile;
	}

	@Override
	public String getName() {
		return PDEUIMessages.ProjectUpdateChange_convert_manifest_to_bnd;
	}

	@Override
	public void initializeValidationData(IProgressMonitor pm) {
	}

	@Override
	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return RefactoringStatus.create(Status.OK_STATUS);
	}

	@Override
	public Change perform(IProgressMonitor pm) throws CoreException {
		List<VersionedClause> bundleClasspath = DependencyManager.getDependencies(List.of(model)).stream()
				.map(BundleDescription::getSymbolicName).map(bsn -> new VersionedClause(bsn)).toList();
		Document document = new Document(""); //$NON-NLS-1$
		Manifest manifest;
		try (InputStream contents = manifestFile.getContents()) {
			manifest = new Manifest(contents);
		} catch (IOException e) {
			throw new CoreException(Status.error("Reading file content failed", e)); //$NON-NLS-1$
		}
		BndEditModel editModel;
		try {
			editModel = new BndEditModel(document);
		} catch (IOException e) {
			throw new CoreException(Status.error("Reading document failed", e)); //$NON-NLS-1$
		}
		Attributes mainAttributes = manifest.getMainAttributes();
		for (Entry<Object, Object> entry : mainAttributes.entrySet()) {
			String propertyName = entry.getKey().toString();
			if (Constants.BUNDLE_MANIFESTVERSION.equals(propertyName) || "Manifest-Version".equals(propertyName)) { //$NON-NLS-1$
				continue;
			}
			String string = entry.getValue().toString();
			editModel.setGenericString(propertyName, BndEditModel.format(propertyName, string));
		}
		String bree = (String) editModel.genericGet(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
		if (bree != null && !keepBREE) {
			try {
				editModel.setEE(EE.parse(bree));
				editModel.genericSet(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, null);
			} catch (Exception e) {
				// if we cannot parse then keep it!
			}
		}
		if (!keepRequireBundle) {
			editModel.genericSet(Constants.REQUIRE_BUNDLE, null);
		}
		if (!keepImportPackage) {
			editModel.genericSet(Constants.IMPORT_PACKAGE, null);
		}
		if (!keepExportPackage) {
			editModel.genericSet(Constants.EXPORT_PACKAGE, null);
		}
		editModel.setBuildPath(bundleClasspath);
		// see https://github.com/bndtools/bnd/issues/5920
		editModel.setGenericString(Constants.SOURCES, "false"); //$NON-NLS-1$
		editModel.saveChangesTo(document);
		instructionFile.create(new ByteArrayInputStream(document.get().getBytes(StandardCharsets.UTF_8)), true, pm);
		return null;
	}

	@Override
	public Object getModifiedElement() {
		return instructionFile;
	}
}

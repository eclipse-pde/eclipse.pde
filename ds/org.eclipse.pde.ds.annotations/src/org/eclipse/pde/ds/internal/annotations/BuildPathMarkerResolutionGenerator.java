/*******************************************************************************
 * Copyright (c) 2015 Ecliptical Software Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ecliptical Software Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ds.internal.annotations;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageHeader;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageObject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.util.ModelModification;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

@SuppressWarnings("restriction")
public class BuildPathMarkerResolutionGenerator implements IMarkerResolutionGenerator2 {

	private static final String ANNOTATIONS_LIB_BUNDLE = "org.eclipse.pde.ds.lib"; //$NON-NLS-1$

	private static final String ANNOTATIONS_JAR_URL = "platform:/plugin/org.eclipse.pde.ds.lib/annotations.jar"; //$NON-NLS-1$

	private static final String ANNOTATIONS_PACKAGE_VERSION_RANGE = "[1.2.0,2.0.0)"; //$NON-NLS-1$

	@Override
	public boolean hasResolutions(IMarker marker) {
		try {
			return DSAnnotationCompilationParticipant.BUILDPATH_PROBLEM_MARKER.equals(marker.getType());
		} catch (CoreException e) {
			return false;
		}
	}

	@Override
	public IMarkerResolution[] getResolutions(IMarker marker) {
		IMarkerResolution[] resolutions = new IMarkerResolution[3];

		resolutions[0] = new BuildPathMarkerResolution(Messages.BuildPathMarkerResolutionGenerator_additionalBundleResolution_label, NLS.bind(Messages.BuildPathMarkerResolutionGenerator_additionalBundleResolution_description, ANNOTATIONS_LIB_BUNDLE), PDEPlugin.getDefault().getLabelProvider().getImage(PluginRegistry.findModel(ANNOTATIONS_LIB_BUNDLE))) {
			@Override
			protected ModelModification createModification(IMarker marker) {
				IProject project = (IProject) marker.getResource();
				return new ModelModification(PDEProject.getBuildProperties(project)) {
					@Override
					protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
						if (model instanceof IBuildModel)
							addAdditionalBundle((IBuildModel) model);
					}
				};
			}
		};

		resolutions[1] = new BuildPathMarkerResolution(Messages.BuildPathMarkerResolutionGenerator_extraLibraryResolution_label, NLS.bind(Messages.BuildPathMarkerResolutionGenerator_extraLibraryResolution_description,ANNOTATIONS_LIB_BUNDLE), JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_JAR_WITH_SOURCE)) {
			@Override
			protected ModelModification createModification(IMarker marker) {
				IProject project = (IProject) marker.getResource();
				return new ModelModification(PDEProject.getBuildProperties(project)) {
					@Override
					protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
						if (model instanceof IBuildModel)
							addExtraLibrary((IBuildModel) model);
					}
				};
			}
		};

		resolutions[2] = new BuildPathMarkerResolution(Messages.BuildPathMarkerResolutionGenerator_packageImportResolution_label, NLS.bind(Messages.BuildPathMarkerResolutionGenerator_packageImportResolution_description, DSAnnotationCompilationParticipant.ANNOTATIONS_PACKAGE), PDEPluginImages.get(PDEPluginImages.OBJ_DESC_PACKAGE)) {
			@Override
			protected ModelModification createModification(IMarker marker) {
				IProject project = (IProject) marker.getResource();
				return new ModelModification(project) {
					@Override
					protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
						if (model instanceof IBundlePluginModelBase)
							addPackageImport((IBundlePluginModelBase) model);
					}
				};
			}
		};

		return resolutions;
	}

	private void addAdditionalBundle(IBuildModel model) throws CoreException {
		IBuild build = model.getBuild();
		IBuildEntry entry = build.getEntry(IBuildEntry.SECONDARY_DEPENDENCIES);
		if (entry == null) {
			entry = model.getFactory().createEntry(IBuildEntry.SECONDARY_DEPENDENCIES);
			build.add(entry);
		}

		if (!entry.contains(ANNOTATIONS_LIB_BUNDLE)) {
			entry.addToken(ANNOTATIONS_LIB_BUNDLE);
		}
	}

	private void addExtraLibrary(IBuildModel model) throws CoreException {
		IBuild build = model.getBuild();
		IBuildEntry entry = build.getEntry(IBuildEntry.JARS_EXTRA_CLASSPATH);
		if (entry == null) {
			entry = model.getFactory().createEntry(IBuildEntry.JARS_EXTRA_CLASSPATH);
			build.add(entry);
		}

		if (!entry.contains(ANNOTATIONS_JAR_URL)) {
			entry.addToken(ANNOTATIONS_JAR_URL);
		}
	}

	private void addPackageImport(IBundlePluginModelBase model) throws CoreException {
		IBundleModel bundleModel = model.getBundleModel();
		IBundle bundle = bundleModel.getBundle();
		ImportPackageHeader header = (ImportPackageHeader) bundle.getManifestHeader(Constants.IMPORT_PACKAGE);
		boolean changed = false;
		if (header == null) {
			header = (ImportPackageHeader) bundleModel.getFactory().createHeader(Constants.IMPORT_PACKAGE, ""); //$NON-NLS-1$
			changed = true;
		}

		ImportPackageObject pkg = header.getPackage(DSAnnotationCompilationParticipant.ANNOTATIONS_PACKAGE);
		if (pkg == null) {
			pkg = header.addPackage(DSAnnotationCompilationParticipant.ANNOTATIONS_PACKAGE);
			pkg.setVersion(ANNOTATIONS_PACKAGE_VERSION_RANGE);
			pkg.setOptional(true);
			changed = true;
		} else if (pkg.getVersion() != null && !new VersionRange(pkg.getVersion()).includes(new Version(1, 2, 0))) {
			pkg.setVersion(ANNOTATIONS_PACKAGE_VERSION_RANGE);
			changed = true;
		}

		if (changed) {
			header.update();
			bundle.setHeader(Constants.IMPORT_PACKAGE, header.getValue());
		}
	}
}

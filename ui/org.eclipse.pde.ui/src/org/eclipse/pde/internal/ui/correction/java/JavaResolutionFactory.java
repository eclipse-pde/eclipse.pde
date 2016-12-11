/*******************************************************************************
 * Copyright (c) 2008, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction.java;

import com.ibm.icu.text.MessageFormat;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.ui.text.java.ClasspathFixProcessor.ClasspathFixProposal;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.bundle.BundlePluginBase;
import org.eclipse.pde.internal.core.ibundle.*;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.text.bundle.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.util.ModelModification;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.osgi.framework.Constants;

/**
 * A factory class used to create resolutions for JDT problem markers which involve modifying a project's MANIFEST.MF (or possibly plugin.xml)
 * @since 3.4
 */
public class JavaResolutionFactory {

	/**
	 * Type constant for a proposal of type IJavaCompletionProposal
	 */
	public static final int TYPE_JAVA_COMPLETION = 0x01;
	/**
	 * Type constant for a proposal of type ClasspathFixProposal
	 */
	public static final int TYPE_CLASSPATH_FIX = 0x02;

	/**
	 * This class represents a Change which will be applied to a Manifest file.  This change is meant to be
	 * used to create an IJavaCompletionProposal or ClasspathFixProposal.
	 */
	private static abstract class AbstractManifestChange extends Change {

		private Object fChangeObject;
		private IProject fProject;

		private AbstractManifestChange(IProject project, Object obj) {
			fProject = project;
			fChangeObject = obj;
		}

		protected Object getChangeObject() {
			return fChangeObject;
		}

		protected IProject getProject() {
			return fProject;
		}

		/*
		 * Provides an image for the Change
		 */
		public abstract Image getImage();

		/*
		 * Provides a description for the Change
		 */
		public abstract String getDescription();

		/*
		 * Added to allow creation of an "undo" change for each AbstractManifestChange
		 */
		protected boolean isUndo() {
			return false;
		}

		@Override
		public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException, OperationCanceledException {
			return RefactoringStatus.create(Status.OK_STATUS);
		}

		@Override
		public Object getModifiedElement() {
			return getProject();
		}

		@Override
		public void initializeValidationData(IProgressMonitor pm) {
		}
	}

	/*
	 * A Change which will add a Require-Bundle entry to resolve the given dependency
	 */
	private static class RequireBundleManifestChange extends AbstractManifestChange {

		private RequireBundleManifestChange(IProject project, ExportPackageDescription desc) {
			super(project, desc);
		}

		@Override
		public Change perform(IProgressMonitor pm) throws CoreException {
			PDEModelUtility.modifyModel(new ModelModification(getProject()) {
				@Override
				protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
					if (!(model instanceof IPluginModelBase))
						return;
					IPluginModelBase base = (IPluginModelBase) model;
					String pluginId = ((ExportPackageDescription) getChangeObject()).getSupplier().getSymbolicName();
					if (!isUndo()) {
						IPluginImport impt = base.getPluginFactory().createImport();
						impt.setId(pluginId);
						base.getPluginBase().add(impt);
					} else {
						IPluginImport[] imports = base.getPluginBase().getImports();
						for (IPluginImport pluginImport : imports)
							if (pluginImport.getId().equals(pluginId))
								base.getPluginBase().remove(pluginImport);
					}
				}
			}, new NullProgressMonitor());

			if (!isUndo())
				return new RequireBundleManifestChange(getProject(), (ExportPackageDescription) getChangeObject()) {
					@Override
					public boolean isUndo() {
						return true;
					}
				};
			return null;
		}

		@Override
		public Image getImage() {
			return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_REQ_PLUGIN_OBJ);
		}

		@Override
		public String getDescription() {
			return PDEUIMessages.UnresolvedImportFixProcessor_2;
		}

		@Override
		public String getName() {
			if (!isUndo()) {
				return MessageFormat.format(PDEUIMessages.UnresolvedImportFixProcessor_0, ((ExportPackageDescription) getChangeObject()).getExporter().getName());
			}
			return MessageFormat.format(PDEUIMessages.UnresolvedImportFixProcessor_1, ((ExportPackageDescription) getChangeObject()).getExporter().getName());
		}

		@Override
		public Object getModifiedElement() {
			IFile[] files = new IFile[] {PDEProject.getManifest(getProject()), PDEProject.getPluginXml(getProject())};
			for (IFile file : files) {
				if (file.exists())
					return file;
			}
			return super.getModifiedElement();
		}

	}

	/*
	 * A Change which will add an Import-Package entry to resolve the given dependency
	 */
	private static class ImportPackageManifestChange extends AbstractManifestChange {

		private ImportPackageManifestChange(IProject project, ExportPackageDescription desc) {
			super(project, desc);
		}

		@Override
		public Change perform(IProgressMonitor pm) throws CoreException {
			PDEModelUtility.modifyModel(new ModelModification(getProject()) {
				@Override
				protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
					if (!(model instanceof IBundlePluginModelBase))
						return;
					IBundlePluginModelBase base = (IBundlePluginModelBase) model;
					IBundle bundle = base.getBundleModel().getBundle();
					String pkgId = ((ExportPackageDescription) getChangeObject()).getName();
					IManifestHeader header = bundle.getManifestHeader(Constants.IMPORT_PACKAGE);
					if (header == null) {
						bundle.setHeader(Constants.IMPORT_PACKAGE, pkgId);
					} else if (header instanceof ImportPackageHeader) {
						ImportPackageHeader ipHeader = (ImportPackageHeader) header;
						int manifestVersion = BundlePluginBase.getBundleManifestVersion(bundle);
						String versionAttr = (manifestVersion < 2) ? ICoreConstants.PACKAGE_SPECIFICATION_VERSION : Constants.VERSION_ATTRIBUTE;
						ImportPackageObject impObject = new ImportPackageObject((ManifestHeader) header, (ExportPackageDescription) getChangeObject(), versionAttr);
						if (!isUndo()) {
							ipHeader.addPackage(impObject);
						} else {
							ipHeader.removePackage(impObject);
						}
					}
				}
			}, new NullProgressMonitor());

			if (!isUndo())
				return new ImportPackageManifestChange(getProject(), (ExportPackageDescription) getChangeObject()) {
					@Override
					public boolean isUndo() {
						return true;
					}
				};
			return null;
		}

		@Override
		public String getDescription() {
			return PDEUIMessages.UnresolvedImportFixProcessor_5;
		}

		@Override
		public Image getImage() {
			return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_BUNDLE_OBJ);
		}

		@Override
		public String getName() {
			if (!isUndo()) {
				return MessageFormat.format(PDEUIMessages.UnresolvedImportFixProcessor_3, ((ExportPackageDescription) getChangeObject()).getName());
			}
			return MessageFormat.format(PDEUIMessages.UnresolvedImportFixProcessor_4, ((ExportPackageDescription) getChangeObject()).getName());
		}

		@Override
		public Object getModifiedElement() {
			IFile file = PDEProject.getManifest(getProject());
			if (file.exists())
				return file;
			return super.getModifiedElement();
		}

	}

	private static class ExportPackageChange extends AbstractManifestChange {

		public ExportPackageChange(IProject project, IPackageFragment fragment) {
			super(project, fragment);
		}

		@Override
		public Change perform(IProgressMonitor pm) throws CoreException {
			ModelModification mod = new ModelModification(getProject()) {
				@Override
				protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
					if (model instanceof IBundlePluginModelBase) {
						IBundle bundle = ((IBundlePluginModelBase) model).getBundleModel().getBundle();

						ExportPackageHeader header = (ExportPackageHeader) bundle.getManifestHeader(Constants.EXPORT_PACKAGE);
						if (header == null) {
							bundle.setHeader(Constants.EXPORT_PACKAGE, ""); //$NON-NLS-1$
							header = (ExportPackageHeader) bundle.getManifestHeader(Constants.EXPORT_PACKAGE);
						}
						header.addPackage(new ExportPackageObject(header, (IPackageFragment) getChangeObject(), Constants.VERSION_ATTRIBUTE));
					}
				}
			};
			PDEModelUtility.modifyModel(mod, new NullProgressMonitor());
			// No plans to use as ClasspathFixProposal, therefore we don't have to worry about an undo
			return null;
		}

		@Override
		public String getName() {
			return NLS.bind(PDEUIMessages.ForbiddenAccessProposal_quickfixMessage, new String[] {((IPackageFragment) getChangeObject()).getElementName(), getProject().getName()});
		}

		@Override
		public Image getImage() {
			return PDEPluginImages.get(PDEPluginImages.OBJ_DESC_BUNDLE);
		}

		@Override
		public Object getModifiedElement() {
			IFile file = PDEProject.getManifest(getProject());
			if (file.exists())
				return file;
			return super.getModifiedElement();
		}

		@Override
		public String getDescription() {
			// No plans to use as ClasspathFixProposal, therefore we don't have to implement a description
			return null;
		}
	}

	/**
	 * Creates and returns a proposal which create a Require-Bundle entry in the MANIFEST.MF (or corresponding plugin.xml) for the supplier
	 * of desc.  The object will be of the type specified by the type argument.
	 * @param project the project to be updated
	 * @param desc an ExportPackageDescription from the bundle that is to be added as a Require-Bundle dependency
	 * @param type the type of the proposal to be returned
	 * @param relevance the relevance of the new proposal
	 * @see JavaResolutionFactory#TYPE_JAVA_COMPLETION
	 * @see JavaResolutionFactory#TYPE_CLASSPATH_FIX
	 */
	public static final Object createRequireBundleProposal(IProject project, ExportPackageDescription desc, int type, int relevance) {
		if (desc.getSupplier() == null)
			return null;
		AbstractManifestChange change = new RequireBundleManifestChange(project, desc);
		return createWrapper(change, type, relevance);
	}

	/**
	 * Creates and returns a proposal which create an Import-Package entry in the MANIFEST.MF for the package represented by
	 * desc.  The object will be of the type specified by the type argument.
	 * @param project the project to be updated
	 * @param desc an ExportPackageDescription which represents the package to be added
	 * @param type the type of the proposal to be returned
	 * @param relevance the relevance of the new proposal
	 * @see JavaResolutionFactory#TYPE_JAVA_COMPLETION
	 * @see JavaResolutionFactory#TYPE_CLASSPATH_FIX
	 */
	public static final Object createImportPackageProposal(IProject project, ExportPackageDescription desc, int type, int relevance) {
		AbstractManifestChange change = new ImportPackageManifestChange(project, desc);
		return createWrapper(change, type, relevance);
	}

	public static final IJavaCompletionProposal createSearchRepositoriesProposal(String packageName) {
		return new SearchRepositoriesForIUProposal(packageName);
	}

	/**
	 * Creates and returns a proposal which create an Export-Package entry in the MANIFEST.MF for the package represented by
	 * pkg.  The object will be of the type specified by the type argument.
	 * @param project the project to be updated
	 * @param pkg an IPackageFragment which represents the package to be added
	 * @param type the type of the proposal to be returned
	 * @param relevance the relevance of the new proposal
	 * @see JavaResolutionFactory#TYPE_JAVA_COMPLETION
	 * @see JavaResolutionFactory#TYPE_CLASSPATH_FIX
	 */
	public static final Object createExportPackageProposal(IProject project, IPackageFragment pkg, int type, int relevance) {
		AbstractManifestChange change = new ExportPackageChange(project, pkg);
		return createWrapper(change, type, relevance);
	}

	private static final Object createWrapper(AbstractManifestChange change, int type, int relevance) {
		switch (type) {
			case TYPE_JAVA_COMPLETION :
				return createJavaCompletionProposal(change, relevance);
			case TYPE_CLASSPATH_FIX :
				return createClasspathFixProposal(change, relevance);
		}
		return null;
	}

	// Methods to wrap a AbstractMethodChange into a consumable format

	/**
	 * Creates and returns a ClasspathFixProposal for the given AbstractManifestChange
	 * @param change the modification which should be performed by the proposal
	 * @since 3.4
	 * @see AbstractManifestChange
	 */
	public final static ClasspathFixProposal createClasspathFixProposal(final AbstractManifestChange change, final int relevance) {
		return new ClasspathFixProposal() {

			@Override
			public Change createChange(IProgressMonitor monitor) throws CoreException {
				return change;
			}

			@Override
			public String getAdditionalProposalInfo() {
				return change.getDescription();
			}

			@Override
			public String getDisplayString() {
				return change.getName();
			}

			@Override
			public Image getImage() {
				return change.getImage();
			}

			@Override
			public int getRelevance() {
				return relevance;
			}

		};
	}

	/**
	 * Creates and returns an IJavaCompletionProposal for the given AbstractManifestChange with the given relevance.
	 * @param change the modification which should be performed by the proposal
	 * @param relevance the relevance of the IJavaCompletionProposal
	 * @since 3.4
	 * @see AbstractManifestChange
	 */
	public final static IJavaCompletionProposal createJavaCompletionProposal(final AbstractManifestChange change, final int relevance) {
		return new IJavaCompletionProposal() {

			@Override
			public int getRelevance() {
				return relevance;
			}

			@Override
			public void apply(IDocument document) {
				try {
					change.perform(new NullProgressMonitor());
				} catch (CoreException e) {
				}
			}

			@Override
			public String getAdditionalProposalInfo() {
				return change.getDescription();
			}

			@Override
			public IContextInformation getContextInformation() {
				return null;
			}

			@Override
			public String getDisplayString() {
				return change.getName();
			}

			@Override
			public Image getImage() {
				return change.getImage();
			}

			@Override
			public Point getSelection(IDocument document) {
				return null;
			}
		};
	}

}

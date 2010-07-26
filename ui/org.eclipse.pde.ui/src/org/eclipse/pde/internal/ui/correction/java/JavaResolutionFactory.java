/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
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

		/* (non-Javadoc)
		 * @see org.eclipse.ltk.core.refactoring.Change#isValid(org.eclipse.core.runtime.IProgressMonitor)
		 */
		public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException, OperationCanceledException {
			return RefactoringStatus.create(Status.OK_STATUS);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.ltk.core.refactoring.Change#getModifiedElement()
		 */
		public Object getModifiedElement() {
			return getProject();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.ltk.core.refactoring.Change#initializeValidationData(org.eclipse.core.runtime.IProgressMonitor)
		 */
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

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.ltk.core.refactoring.Change#perform(org.eclipse.core.runtime.IProgressMonitor)
		 */
		public Change perform(IProgressMonitor pm) throws CoreException {
			PDEModelUtility.modifyModel(new ModelModification(getProject()) {
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
						for (int i = 0; i < imports.length; i++)
							if (imports[i].getId().equals(pluginId))
								base.getPluginBase().remove(imports[i]);
					}
				}
			}, new NullProgressMonitor());

			if (!isUndo())
				return new RequireBundleManifestChange(getProject(), (ExportPackageDescription) getChangeObject()) {
					public boolean isUndo() {
						return true;
					}
				};
			return null;
		}

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.pde.internal.ui.correction.java.JavaResolutionFactory.AbstractManifestChange#getImage()
		 */
		public Image getImage() {
			return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_REQ_PLUGIN_OBJ);
		}

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.pde.internal.ui.correction.java.JavaResolutionFactory.AbstractManifestChange#getDescription()
		 */
		public String getDescription() {
			return PDEUIMessages.UnresolvedImportFixProcessor_2;
		}

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.ltk.core.refactoring.Change#getName()
		 */
		public String getName() {
			if (!isUndo()) {
				return MessageFormat.format(PDEUIMessages.UnresolvedImportFixProcessor_0, new Object[] {((ExportPackageDescription) getChangeObject()).getExporter().getName()});
			}
			return MessageFormat.format(PDEUIMessages.UnresolvedImportFixProcessor_1, new Object[] {((ExportPackageDescription) getChangeObject()).getExporter().getName()});
		}

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.pde.internal.ui.correction.java.JavaResolutionFactory.AbstractManifestChange#getModifiedElement()
		 */
		public Object getModifiedElement() {
			IFile[] files = new IFile[] {PDEProject.getManifest(getProject()), PDEProject.getPluginXml(getProject())};
			for (int i = 0; i < files.length; i++) {
				if (files[i].exists())
					return files[i];
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

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.ltk.core.refactoring.Change#perform(org.eclipse.core.runtime.IProgressMonitor)
		 */
		public Change perform(IProgressMonitor pm) throws CoreException {
			PDEModelUtility.modifyModel(new ModelModification(getProject()) {
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
					public boolean isUndo() {
						return true;
					}
				};
			return null;
		}

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.pde.internal.ui.correction.java.JavaResolutionFactory.AbstractManifestChange#getDescription()
		 */
		public String getDescription() {
			return PDEUIMessages.UnresolvedImportFixProcessor_5;
		}

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.pde.internal.ui.correction.java.JavaResolutionFactory.AbstractManifestChange#getImage()
		 */
		public Image getImage() {
			return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_BUNDLE_OBJ);
		}

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.ltk.core.refactoring.Change#getName()
		 */
		public String getName() {
			if (!isUndo()) {
				return MessageFormat.format(PDEUIMessages.UnresolvedImportFixProcessor_3, new Object[] {((ExportPackageDescription) getChangeObject()).getName()});
			}
			return MessageFormat.format(PDEUIMessages.UnresolvedImportFixProcessor_4, new Object[] {((ExportPackageDescription) getChangeObject()).getName()});
		}

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.pde.internal.ui.correction.java.JavaResolutionFactory.AbstractManifestChange#getModifiedElement()
		 */
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

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.ltk.core.refactoring.Change#perform(org.eclipse.core.runtime.IProgressMonitor)
		 */
		public Change perform(IProgressMonitor pm) throws CoreException {
			ModelModification mod = new ModelModification(getProject()) {
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

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.ltk.core.refactoring.Change#getName()
		 */
		public String getName() {
			return NLS.bind(PDEUIMessages.ForbiddenAccessProposal_quickfixMessage, new String[] {((IPackageFragment) getChangeObject()).getElementName(), getProject().getName()});
		}

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.pde.internal.ui.correction.java.JavaResolutionFactory.AbstractManifestChange#getImage()
		 */
		public Image getImage() {
			return PDEPluginImages.get(PDEPluginImages.OBJ_DESC_BUNDLE);
		}

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.pde.internal.ui.correction.java.JavaResolutionFactory.AbstractManifestChange#getModifiedElement()
		 */
		public Object getModifiedElement() {
			IFile file = PDEProject.getManifest(getProject());
			if (file.exists())
				return file;
			return super.getModifiedElement();
		}

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.pde.internal.ui.correction.java.JavaResolutionFactory.AbstractManifestChange#getDescription()
		 */
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
	 * @see TYPE_JAVA_COMPLETION , TYPE_CLASSPATH_FIX
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
	 * @see TYPE_JAVA_COMPLETION , TYPE_CLASSPATH_FIX
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
	 * @see TYPE_JAVA_COMPLETION , TYPE_CLASSPATH_FIX
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

			/* (non-Javadoc)
			 * @see org.eclipse.jdt.ui.text.java.ClasspathFixProcessor.ClasspathFixProposal#createChange(org.eclipse.core.runtime.IProgressMonitor)
			 */
			public Change createChange(IProgressMonitor monitor) throws CoreException {
				return change;
			}

			/* (non-Javadoc)
			 * @see org.eclipse.jdt.ui.text.java.ClasspathFixProcessor.ClasspathFixProposal#getAdditionalProposalInfo()
			 */
			public String getAdditionalProposalInfo() {
				return change.getDescription();
			}

			/* (non-Javadoc)
			 * @see org.eclipse.jdt.ui.text.java.ClasspathFixProcessor.ClasspathFixProposal#getDisplayString()
			 */
			public String getDisplayString() {
				return change.getName();
			}

			/* (non-Javadoc)
			 * @see org.eclipse.jdt.ui.text.java.ClasspathFixProcessor.ClasspathFixProposal#getImage()
			 */
			public Image getImage() {
				return change.getImage();
			}

			/* (non-Javadoc)
			 * @see org.eclipse.jdt.ui.text.java.ClasspathFixProcessor.ClasspathFixProposal#getRelevance()
			 */
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

			/*
			 * (non-Javadoc)
			 * @see org.eclipse.jdt.ui.text.java.IJavaCompletionProposal#getRelevance()
			 */
			public int getRelevance() {
				return relevance;
			}

			/*
			 * (non-Javadoc)
			 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#apply(org.eclipse.jface.text.IDocument)
			 */
			public void apply(IDocument document) {
				try {
					change.perform(new NullProgressMonitor());
				} catch (CoreException e) {
				}
			}

			/*
			 * (non-Javadoc)
			 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getAdditionalProposalInfo()
			 */
			public String getAdditionalProposalInfo() {
				return change.getDescription();
			}

			/*
			 * (non-Javadoc)
			 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getContextInformation()
			 */
			public IContextInformation getContextInformation() {
				return null;
			}

			/*
			 * (non-Javadoc)
			 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getDisplayString()
			 */
			public String getDisplayString() {
				return change.getName();
			}

			/*
			 * (non-Javadoc)
			 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getImage()
			 */
			public Image getImage() {
				return change.getImage();
			}

			/*
			 * (non-Javadoc)
			 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getSelection(org.eclipse.jface.text.IDocument)
			 */
			public Point getSelection(IDocument document) {
				return null;
			}
		};
	}

}

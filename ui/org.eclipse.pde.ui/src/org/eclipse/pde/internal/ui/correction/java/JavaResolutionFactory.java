/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction.java;

import com.ibm.icu.text.MessageFormat;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.ui.text.java.ClasspathFixProcessor.ClasspathFixProposal;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.ltk.core.refactoring.*;
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
import org.eclipse.text.edits.TextEdit;
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
		private CompilationUnit fCompilationUnit;
		private String fQualifiedTypeToImport;

		private AbstractManifestChange(IProject project, Object obj) {
			fProject = project;
			fChangeObject = obj;
		}

		public AbstractManifestChange(IProject project, Object changeObj, CompilationUnit cu,
				String qualifiedTypeToImport) {
			this(project, changeObj);
			fCompilationUnit = cu;
			fQualifiedTypeToImport = qualifiedTypeToImport;
		}

		protected Object getChangeObject() {
			return fChangeObject;
		}

		protected IProject getProject() {
			return fProject;
		}

		protected CompilationUnit getCompilationUnit() {
			return fCompilationUnit;
		}

		protected String getQualifiedTypeToImport() {
			return fQualifiedTypeToImport;
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

		protected void insertImport(CompilationUnit compilationUnit, String qualifiedTypeToImport, IProgressMonitor pm)
				throws CoreException {
			if (compilationUnit == null || qualifiedTypeToImport == null) {
				return;
			}
			ImportRewrite rewrite = ImportRewrite.create(compilationUnit, true);
			if (rewrite == null) {
				return;
			}
			if (!isUndo()) {
				rewrite.addImport(qualifiedTypeToImport);
			} else {
				rewrite.removeImport(qualifiedTypeToImport);
			}
			TextEdit rewriteImports = rewrite.rewriteImports(pm);
			ICompilationUnit iCompilationUnit = (ICompilationUnit) compilationUnit.getJavaElement()
					.getAdapter(IOpenable.class);
			performTextEdit(rewriteImports, (IFile) iCompilationUnit.getResource(), pm);
		}

		private void performTextEdit(TextEdit textEdit, IFile file, IProgressMonitor pm)
				throws CoreException {
			TextFileChange textFileChange = new TextFileChange("Add import for " + fQualifiedTypeToImport, file); //$NON-NLS-1$
			textFileChange.setSaveMode(TextFileChange.KEEP_SAVE_STATE);
			textFileChange.setEdit(textEdit);
			textFileChange.perform(pm);
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
	 * A Change which will add a Require-Bundle entry to resolve the given
	 * dependency or add multiple Require-Bundle entries to resolve the dependency
	 * based on description name
	 */
	private static class RequireBundleManifestChange extends AbstractManifestChange {

		private RequireBundleManifestChange(IProject project, ExportPackageDescription desc, CompilationUnit cu,
				String qualifiedTypeToImport) {
			super(project, desc, cu, qualifiedTypeToImport);
		}

		private RequireBundleManifestChange(IProject project, String desc, CompilationUnit cu,
				String qualifiedTypeToImport) {
			super(project, desc, cu, qualifiedTypeToImport);
		}

		@Override
		public Change perform(IProgressMonitor pm) throws CoreException {
			PDEModelUtility.modifyModel(new ModelModification(getProject()) {
				@Override
				protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
					if (!(model instanceof IPluginModelBase))
						return;
					IPluginModelBase base = (IPluginModelBase) model;
					String[] pluginIdStrings = null;
					String[] junit5PluginIDList = getJUnit5Bundles();
					if (getChangeObject() instanceof String && getChangeObject().equals("JUnit 5 bundles")) { //$NON-NLS-1$
						pluginIdStrings = junit5PluginIDList;
					}
					if (getChangeObject() instanceof ExportPackageDescription) {
						pluginIdStrings = new String[1];
						pluginIdStrings[0] = ((ExportPackageDescription) getChangeObject()).getSupplier()
								.getSymbolicName();
					}
					for (int i = 0; i < pluginIdStrings.length; i++) {
						String pluginId = pluginIdStrings[i];
						if (!isUndo()) {
							IPluginImport impt = base.getPluginFactory().createImport();
							impt.setId(pluginId);
							base.getPluginBase().add(impt);
						} else {
							IPluginImport[] imports = base.getPluginBase().getImports();
							for (IPluginImport pluginImport : imports) {
								if (pluginImport.getId().equals(pluginId)) {
									base.getPluginBase().remove(pluginImport);
								}
							}
						}
					}
				}

				private String[] getJUnit5Bundles() {
					String[] junit5PluginIDList = { "org.junit", "org.junit.jupiter.api", "org.junit.jupiter.engine", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							"org.junit.jupiter.migrationsupport", "org.junit.jupiter.params", //$NON-NLS-1$ //$NON-NLS-2$
							"org.junit.platform.commons", "org.junit.platform.engine", //$NON-NLS-1$ //$NON-NLS-2$
							"org.junit.platform.launcher", "org.junit.platform.runner", //$NON-NLS-1$ //$NON-NLS-2$
							 "org.junit.platform.suite.api","org.junit.vintage.engine",  //$NON-NLS-1$ //$NON-NLS-2$
							"org.hamcrest.core", "org.opentest4j", "org.apiguardian" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					return junit5PluginIDList;
				}
			}, new NullProgressMonitor());

			insertImport(getCompilationUnit(), getQualifiedTypeToImport(), pm);

			if (!isUndo()) {
				if (getChangeObject() instanceof ExportPackageDescription) {
					return new RequireBundleManifestChange(getProject(), (ExportPackageDescription) getChangeObject(),
							getCompilationUnit(), getQualifiedTypeToImport()) {
						@Override
						public boolean isUndo() {
							return true;
						}
					};
				}
				if (getChangeObject() instanceof String) {
					return new RequireBundleManifestChange(getProject(),(String) getChangeObject(),
							getCompilationUnit(), getQualifiedTypeToImport()) {
						@Override
						public boolean isUndo() {
							return true;
						}
					};
				}
			}
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
				if(getChangeObject() instanceof String) {
					return MessageFormat.format(PDEUIMessages.UnresolvedImportFixProcessor_0,
							(getChangeObject().toString()));
				}
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

		private ImportPackageManifestChange(IProject project, ExportPackageDescription desc, CompilationUnit cu,
				String qualifiedTypeToImport) {
			super(project, desc, cu, qualifiedTypeToImport);
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

			insertImport(getCompilationUnit(), getQualifiedTypeToImport(), pm);

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
	 * Creates and returns a proposal which create a Require-Bundle entry in the
	 * MANIFEST.MF (or corresponding plugin.xml) for the supplier of desc. The
	 * object will be of the type specified by the type argument.
	 *
	 * @param project
	 *            the project to be updated
	 * @param desc
	 *            an ExportPackageDescription from the bundle that is to be
	 *            added as a Require-Bundle dependency
	 * @param type
	 *            the type of the proposal to be returned
	 * @param relevance
	 *            the relevance of the new proposal
	 * @param qualifiedTypeToImport
	 *            the qualified type name of the type that requires this
	 *            proposal. If this argument and cu are supplied the proposal
	 *            will add an import statement for this type to the source file
	 *            in which the proposal was invoked.
	 * @param cu
	 *            the AST root of the java source file in which this fix was
	 *            invoked
	 * @see JavaResolutionFactory#TYPE_JAVA_COMPLETION
	 * @see JavaResolutionFactory#TYPE_CLASSPATH_FIX
	 */
	public static final Object createRequireBundleProposal(IProject project, ExportPackageDescription desc, int type,
			int relevance, CompilationUnit cu, String qualifiedTypeToImport) {
		if (desc.getSupplier() == null)
			return null;
		AbstractManifestChange change = new RequireBundleManifestChange(project, desc, cu, qualifiedTypeToImport);
		return createWrapper(change, type, relevance);
	}

	/**
	 * Creates and returns a proposal which create multiple Require-Bundle entry in the
	 * MANIFEST.MF (or corresponding plugin.xml) for desc name.
	 *
	 * @param project
	 *            the project to be updated
	 * @param desc
	 *            multiple bundles that is to be  added as a Require-Bundle dependency
	 *            based on description name
	 * @param type
	 *            the type of the proposal to be returned
	 * @param relevance
	 *            the relevance of the new proposal
	 * @param qualifiedTypeToImport
	 *            the qualified type name of the type that requires this
	 *            proposal. If this argument and cu are supplied the proposal
	 *            will add an import statement for this type to the source file
	 *            in which the proposal was invoked.
	 * @param cu
	 *            the AST root of the java source file in which this fix was
	 *            invoked
	 * @see JavaResolutionFactory#TYPE_JAVA_COMPLETION
	 * @see JavaResolutionFactory#TYPE_CLASSPATH_FIX
	 */
	public static final Object createRequireBundleProposal(IProject project, String desc, int type, int relevance,
			CompilationUnit cu, String qualifiedTypeToImport) {
		if (desc == null)
			return null;
		AbstractManifestChange change = new RequireBundleManifestChange(project, desc, cu, qualifiedTypeToImport);
		return createWrapper(change, type, relevance);
	}

	/**
	 * Creates and returns a proposal which create an Import-Package entry in
	 * the MANIFEST.MF for the package represented by desc. The object will be
	 * of the type specified by the type argument.
	 *
	 * @param project
	 *            the project to be updated
	 * @param desc
	 *            an ExportPackageDescription which represents the package to be
	 *            added
	 * @param type
	 *            the type of the proposal to be returned
	 * @param relevance
	 *            the relevance of the new proposal
	 * @param qualifiedTypeToImport
	 *            the qualified type name of the type that requires this
	 *            proposal. If this argument and cu are supplied the proposal
	 *            will add an import statement for this type to the source file
	 *            in which the proposal was invoked.
	 * @param cu
	 *            the AST root of the java source file in which this fix was
	 *            invoked
	 * @see JavaResolutionFactory#TYPE_JAVA_COMPLETION
	 * @see JavaResolutionFactory#TYPE_CLASSPATH_FIX
	 */
	public static final Object createImportPackageProposal(IProject project, ExportPackageDescription desc, int type,
			int relevance, CompilationUnit cu, String qualifiedTypeToImport) {
		AbstractManifestChange change = new ImportPackageManifestChange(project, desc, cu, qualifiedTypeToImport);
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

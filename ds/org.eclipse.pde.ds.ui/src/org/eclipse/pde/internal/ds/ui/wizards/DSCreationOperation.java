/*******************************************************************************
 * Copyright (c) 2008, 2015 Code 9 Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira Nobrega <rafael.oliveira@gmail.com> - bug 242028
 *     IBM - ongoing maintenance
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.StringTokenizer;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.build.IBuildModelFactory;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.ds.core.IDSComponent;
import org.eclipse.pde.internal.ds.core.IDSDocumentFactory;
import org.eclipse.pde.internal.ds.core.IDSImplementation;
import org.eclipse.pde.internal.ds.core.IDSModel;
import org.eclipse.pde.internal.ds.core.text.DSModel;
import org.eclipse.pde.internal.ds.ui.Activator;
import org.eclipse.pde.internal.ds.ui.IConstants;
import org.eclipse.pde.internal.ds.ui.Messages;
import org.eclipse.pde.internal.ui.util.ModelModification;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;
import org.eclipse.pde.internal.ui.util.TextUtil;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ISetSelectionTarget;

public class DSCreationOperation extends WorkspaceModifyOperation {

	protected IFile fFile;
	private final String fComponentName;
	private final String fImplementationClass;

	private static final String DS_MANIFEST_KEY = "Service-Component"; //$NON-NLS-1$

	/**
	 *
	 */
	public DSCreationOperation(IFile file, String componentName,
			String implementationClass) {
		fFile = file;
		fComponentName = componentName;
		fImplementationClass = implementationClass;
	}

	@Override
	protected void execute(IProgressMonitor monitor) throws CoreException,
			InvocationTargetException, InterruptedException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, Messages.DSCreationOperation_title, 3);
		createContent();
		subMonitor.worked(1);
		openFile();
		if (PDE.hasPluginNature(fFile.getProject())) {
			writeManifest(fFile.getProject(), subMonitor.split(1));
			writeBuildProperties(fFile.getProject(), subMonitor.split(1));
		}
		subMonitor.setWorkRemaining(0);
	}

	private void writeManifest(IProject project, IProgressMonitor monitor) {

		PDEModelUtility.modifyModel(new ModelModification(project) {
			@Override
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {

				if (model instanceof IBundlePluginModelBase) {
					updateManifest((IBundlePluginModelBase) model, monitor);
				}
			}
		}, monitor);

	}

	private void writeBuildProperties(final IProject project, IProgressMonitor monitor) {

		PDEModelUtility.modifyModel(new ModelModification(PDEProject.getBuildProperties(project)) {
			@Override
			protected void modifyModel(IBaseModel model,
					IProgressMonitor monitor) throws CoreException {
				if (!(model instanceof IBuildModel))
					return;
				IFile file = PDEProject.getBuildProperties(project);
				if (file.exists()) {
					WorkspaceBuildModel wbm = new WorkspaceBuildModel(file);
					wbm.load();
					if (!wbm.isLoaded())
						return;
					IBuildModelFactory factory = wbm.getFactory();
					String path = fFile.getFullPath().removeFirstSegments(1).toPortableString();
					IBuildEntry entry = wbm.getBuild().getEntry(
							IBuildEntry.BIN_INCLUDES);
					if (entry == null) {
						entry = factory.createEntry(IBuildEntry.BIN_INCLUDES);
						wbm.getBuild().add(entry);
					}
					entry.addToken(path);
					wbm.save();
				}
			}
		}, null);

	}

	private void updateManifest(IBundlePluginModelBase model,
			IProgressMonitor monitor) throws CoreException {
		IBundleModel bundleModel = model.getBundleModel();

		// Create a path from the bundle root to the component file
		IContainer root = PDEProject.getBundleRoot(fFile.getProject());
		String filePath = fFile.getFullPath()
				.makeRelativeTo(root.getFullPath()).toPortableString();

		String header = bundleModel.getBundle().getHeader(DS_MANIFEST_KEY);
		if (header != null) {
			if (containsValue(header, filePath)) {
				return;
			}
			filePath = header + "," + TextUtil.getDefaultLineDelimiter() + " " + filePath; //$NON-NLS-1$ //$NON-NLS-2$
		}
		bundleModel.getBundle().setHeader(DS_MANIFEST_KEY, filePath);
	}

	private boolean containsValue(String header, String value) {
		value = value.trim();
		StringTokenizer st= new StringTokenizer(header, ","); //$NON-NLS-1$
		while (st.hasMoreElements()) {
			String token = st.nextToken();
			if (value.equals(token.trim())) {
				return true;
			}
		}
		return false;
	}

	protected void createContent() throws CoreException {
		IDSModel model = new DSModel(CoreUtility.getTextDocument(fFile
				.getContents()), false);
		model.setUnderlyingResource(fFile);
		initializeDS(model.getDSComponent(), fFile);
		model.save();
		model.dispose();
	}

	/**
	 * @param component
	 * @param file
	 */
	protected void initializeDS(IDSComponent component, IFile file) {
		IDSDocumentFactory factory = component.getModel().getFactory();

		IDSImplementation implementation = factory.createImplementation();
		implementation.setClassName(fImplementationClass);
		component.setImplementation(implementation);
		component.setAttributeName(fComponentName);

		try {
			// Add builder
			IProject project = file.getProject();
			IProjectDescription description = project.getDescription();
			ICommand[] commands = description.getBuildSpec();

			for (ICommand command : commands) {
				if (command.getBuilderName().equals(IConstants.ID_BUILDER)) {
					return;
				}
			}

			ICommand[] newCommands = new ICommand[commands.length + 1];
			System.arraycopy(commands, 0, newCommands, 0, commands.length);
			ICommand command = description.newCommand();
			command.setBuilderName(IConstants.ID_BUILDER);
			newCommands[newCommands.length - 1] = command;
			description.setBuildSpec(newCommands);
			project.setDescription(description, null);

		} catch (CoreException e) {
			Activator.logException(e, null, null);
		}

	}

	/**
	 *
	 */
	private void openFile() {
		Display.getCurrent().asyncExec(() -> {
			IWorkbenchWindow window = Activator.getActiveWorkbenchWindow();
			if (window == null) {
				return;
			}
			IWorkbenchPage page = window.getActivePage();
			if ((page == null) || !fFile.exists()) {
				return;
			}
			IWorkbenchPart focusPart = page.getActivePart();
			if (focusPart instanceof ISetSelectionTarget) {
				ISelection selection = new StructuredSelection(fFile);
				((ISetSelectionTarget) focusPart).selectReveal(selection);
			}
			try {
				IDE.openEditor(page, fFile);
			} catch (PartInitException e) {
				// Ignore
			}
		});
	}

}

/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira N�brega <rafael.oliveira@gmail.com> - bug 223738
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
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
	private String fComponentName;
	private String fImplementationClass;


	/**
	 * 
	 */
	public DSCreationOperation(IFile file, String componentName,
			String implementationClass) {
		fFile = file;
		fComponentName = componentName;
		fImplementationClass = implementationClass;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.actions.WorkspaceModifyOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void execute(IProgressMonitor monitor) throws CoreException,
			InvocationTargetException, InterruptedException {
		monitor.beginTask(Messages.DSCreationOperation_title, 2);
		createContent();
		monitor.worked(1);
		openFile();
		writeDSPropIntoManifest(fFile.getProject(), new SubProgressMonitor(
				monitor, 1));
		monitor.done();
	}

	private void writeDSPropIntoManifest(IProject project,
			SubProgressMonitor monitor) {
		
		PDEModelUtility.modifyModel(new ModelModification(project) {
			
			protected void modifyModel(IBaseModel model,
					IProgressMonitor monitor) throws CoreException {
				
				if (model instanceof IBundlePluginModelBase)
					updateManifest((IBundlePluginModelBase) model, monitor);
			}
		}, monitor);
		monitor.done();

	}


	private void updateManifest(IBundlePluginModelBase model,
			IProgressMonitor monitor) throws CoreException {
		IBundleModel bundleModel = model.getBundleModel();
		String filePath = fFile.getFullPath().toOSString();
		
		// gets the second index of "\" (the first is index=0)
		filePath = filePath.substring(1);
		int index = filePath.indexOf("\\"); //$NON-NLS-1$
		// TODO we need to deal with this header if there are existing entries
		if (index > -1) {
			String dsFilePath = filePath.substring(index + 1);
			bundleModel.getBundle().setHeader(
					"Service-Component", dsFilePath); //$NON-NLS-1$
		}

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

		// Element: implemenation
		IDSImplementation implementation = factory.createImplementation();
		implementation.setClassName(fImplementationClass);
		component.setImplementation(implementation);

		// Component Attributes

		component.setAttributeName(fComponentName);

		try {
			// Add builder
			IProject project = file.getProject();
			IProjectDescription description = project.getDescription();
			ICommand[] commands = description.getBuildSpec();

			for (int i = 0; i < commands.length; ++i) {
				if (commands[i].getBuilderName().equals(IConstants.ID_BUILDER)) {
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
		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {
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
			}
		});
	}

}

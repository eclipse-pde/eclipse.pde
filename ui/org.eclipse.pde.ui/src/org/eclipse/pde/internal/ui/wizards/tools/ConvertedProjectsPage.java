/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *     Les Jones <lesojones@gamil.com> - bug 205361
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.tools;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.lang.reflect.InvocationTargetException;
import java.util.Vector;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

/**
 * Only wizard page in the {@link ConvertedProjectWizard}.  Allows users to convert existing projects
 * to plug-in projects.
 */
public class ConvertedProjectsPage extends WizardPage {
	private CheckboxTableViewer fProjectViewer;
	private Button fSelectButton;
	private Button fDeselectButton;
	private Button fApiAnalysisButton;

	private final static String S_API_ANALYSIS = "apiAnalysis"; //$NON-NLS-1$

	/**
	 * Items to select when the table is created, based off what the user had selected when opening the wizard
	 */
	private IProject[] fInitialSelection;

	/**
	 * All items to put in the table, consists of all unconverted projects in the workspace
	 */
	private IProject[] fAllUnconvertedProjects;

	/**
	 * Label provider for the table
	 */
	public class ProjectLabelProvider extends LabelProvider {
		@Override
		public Image getImage(Object element) {
			return PlatformUI.getWorkbench().getSharedImages().getImage(IDE.SharedImages.IMG_OBJ_PROJECT);
		}

		@Override
		public String getText(Object element) {
			return ((IProject) element).getName();
		}
	}

	public ConvertedProjectsPage(IProject[] projects, Vector<?> initialSelection) {
		super("convertedProjects"); //$NON-NLS-1$
		setTitle(PDEUIMessages.ConvertedProjectWizard_title);
		setDescription(PDEUIMessages.ConvertedProjectWizard_desc);
		this.fAllUnconvertedProjects = projects != null ? projects : new IProject[0];
		this.fInitialSelection = initialSelection.toArray(new IProject[initialSelection.size()]);
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_BOTH);

		SWTFactory.createLabel(container, PDEUIMessages.ConvertedProjectWizard_projectList, 2);

		fProjectViewer = CheckboxTableViewer.newCheckList(container, SWT.BORDER);
		fProjectViewer.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));
		fProjectViewer.setContentProvider(ArrayContentProvider.getInstance());
		fProjectViewer.setLabelProvider(new ProjectLabelProvider());
		fProjectViewer.setInput(fAllUnconvertedProjects);
		fProjectViewer.setCheckedElements(fInitialSelection);
		fProjectViewer.addCheckStateListener(event -> updateButtons());

		Composite buttonContainer = SWTFactory.createComposite(container, 1, 1, GridData.FILL_VERTICAL, 0, 0);

		fSelectButton = SWTFactory.createPushButton(buttonContainer, PDEUIMessages.ConvertedProjectsPage_SelectAll, null);
		fSelectButton.addSelectionListener(widgetSelectedAdapter(e -> {
			fProjectViewer.setAllChecked(true);
			updateButtons();
		}));
		fDeselectButton = SWTFactory.createPushButton(buttonContainer, PDEUIMessages.ConvertedProjectsPage_DeselectAll, null);
		fDeselectButton.addSelectionListener(widgetSelectedAdapter(e -> {
			fProjectViewer.setAllChecked(false);
			updateButtons();
		}));
		updateButtons();

		fApiAnalysisButton = SWTFactory.createCheckButton(container, PDEUIMessages.PluginContentPage_enable_api_analysis, null, false, 2);

		loadSettings();

		setControl(container);
		Dialog.applyDialogFont(container);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.CONVERTED_PROJECTS);
	}

	private void updateButtons() {
		int count = fProjectViewer.getCheckedElements().length;
		fSelectButton.setEnabled(count < fAllUnconvertedProjects.length);
		fDeselectButton.setEnabled(count > 0);
	}

	private void storeSettings() {
		IDialogSettings settings = getDialogSettings();
		if (settings != null) {
			settings.put(S_API_ANALYSIS, Boolean.toString(fApiAnalysisButton.getSelection()));
		}
	}

	private void loadSettings() {
		IDialogSettings settings = getDialogSettings();
		if (settings != null) {
			fApiAnalysisButton.setSelection(settings.getBoolean(S_API_ANALYSIS));
		}
	}

	public boolean finish() {
		storeSettings();

		Object[] selected = fProjectViewer.getCheckedElements();
		final IProject[] projects = new IProject[selected.length];
		for (int i = 0; i < selected.length; i++) {
			projects[i] = (IProject) selected[i];
		}

		try {
			IRunnableWithProgress convertOperation;
			convertOperation = new ConvertProjectToPluginOperation(projects, fApiAnalysisButton.getSelection());
			getContainer().run(false, true, convertOperation);
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
			return false;
		} catch (InterruptedException e) {
			PDEPlugin.logException(e);
			return false;
		}
		return true;
	}

}

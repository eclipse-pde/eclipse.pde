/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.ui.internal.preferences;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.JavaElementComparator;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsHelpContextIds;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionStatusDialog;

/**
 * A dialog for selecting a project to configure project specific settings for
 *
 * @since 1.0.0
 */
public class ProjectSelectionDialog extends SelectionStatusDialog {

	static class ApiJavaElementContentProvider extends StandardJavaElementContentProvider {
		@Override
		public Object[] getChildren(Object element) {
			if (element instanceof IJavaModel) {
				IJavaModel model = (IJavaModel) element;
				HashSet<IJavaProject> set = new HashSet<>();
				try {
					IJavaProject[] projects = model.getJavaProjects();
					for (IJavaProject project : projects) {
						if (project.getProject().hasNature(ApiPlugin.NATURE_ID)) {
							set.add(project);
						}
					}
				} catch (CoreException ce) {
					// ignore
				}
				return set.toArray();
			}
			return super.getChildren(element);
		}
	}

	// the visual selection widget group
	private TableViewer fTableViewer;
	Set<IJavaProject> fProjectsWithSpecifics;

	// sizing constants
	private final static int SIZING_SELECTION_WIDGET_HEIGHT = 250;
	private final static int SIZING_SELECTION_WIDGET_WIDTH = 300;

	private final static String DIALOG_SETTINGS_SHOW_ALL = "ProjectSelectionDialog.show_all"; //$NON-NLS-1$

	/**
	 * The filter for the viewer
	 */
	private ViewerFilter fFilter;

	/**
	 * Constructor
	 *
	 * @param parentShell
	 * @param projectsWithSpecifics
	 */
	public ProjectSelectionDialog(Shell parentShell, Set<IJavaProject> projectsWithSpecifics) {
		super(parentShell);
		setTitle(PreferenceMessages.ProjectSelectionDialog_title);
		setMessage(PreferenceMessages.ProjectSelectionDialog_message);
		fProjectsWithSpecifics = projectsWithSpecifics;

		fFilter = new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				return fProjectsWithSpecifics.contains(element);
			}
		};
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		// page group
		Composite composite = (Composite) super.createDialogArea(parent);

		Font font = parent.getFont();
		composite.setFont(font);

		createMessageArea(composite);

		fTableViewer = new TableViewer(composite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		fTableViewer.addSelectionChangedListener(
				event -> doSelectionChanged(event.getStructuredSelection().toArray()));
		fTableViewer.addDoubleClickListener(event -> okPressed());
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.heightHint = SIZING_SELECTION_WIDGET_HEIGHT;
		data.widthHint = SIZING_SELECTION_WIDGET_WIDTH;
		fTableViewer.getTable().setLayoutData(data);

		fTableViewer.setLabelProvider(new JavaElementLabelProvider());
		fTableViewer.setContentProvider(new ApiJavaElementContentProvider());
		fTableViewer.setComparator(new JavaElementComparator());
		fTableViewer.getControl().setFont(font);

		Button checkbox = SWTFactory.createCheckButton(composite, PreferenceMessages.ProjectSelectionDialog_checkbox_text, null, false, 1);
		checkbox.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateFilter(((Button) e.widget).getSelection());
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				updateFilter(((Button) e.widget).getSelection());
			}
		});
		IDialogSettings dialogSettings = ApiUIPlugin.getDefault().getDialogSettings();
		boolean doFilter = !dialogSettings.getBoolean(DIALOG_SETTINGS_SHOW_ALL) && !fProjectsWithSpecifics.isEmpty();
		checkbox.setSelection(doFilter);
		updateFilter(doFilter);

		IJavaModel input = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
		fTableViewer.setInput(input);

		doSelectionChanged(new Object[0]);
		Dialog.applyDialogFont(composite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IApiToolsHelpContextIds.APITOOLS_PROJECT_SPECIFIC_SETTINGS_SELECTION_DIALOG);
		return composite;
	}

	@Override
	protected void okPressed() {
		// override to avoid synthetic accessor warnings
		super.okPressed();
	}

	/**
	 * Handles the change in selection of the viewer and updates the status of
	 * the dialog at the same time
	 *
	 * @param objects
	 */
	void doSelectionChanged(Object[] objects) {
		if (objects.length != 1) {
			updateStatus(Status.error("")); //$NON-NLS-1$
			setSelectionResult(null);
		} else {
			updateStatus(Status.OK_STATUS);
			setSelectionResult(objects);
		}
	}

	/**
	 * Updates the viewer filter based on the selection of the 'show project
	 * with...' button
	 *
	 * @param selected
	 */
	protected void updateFilter(boolean selected) {
		if (selected) {
			fTableViewer.addFilter(fFilter);
		} else {
			fTableViewer.removeFilter(fFilter);
		}
		ApiUIPlugin.getDefault().getDialogSettings().put(DIALOG_SETTINGS_SHOW_ALL, !selected);
	}

	@Override
	protected void computeResult() {
	}
}

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
 *     Manumitting Technologies Inc - bug 324310
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.wizards;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.api.tools.internal.ApiBaselineManager;
import org.eclipse.pde.api.tools.internal.model.ApiModelFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;
import org.eclipse.pde.api.tools.ui.internal.preferences.ApiBaselinePreferencePage;

public abstract class ApiBaselineWizardPage extends WizardPage {

	/**
	 * an EE entry (child of an api component in the viewer)
	 */
	public static class EEEntry {
		String name = null;

		/**
		 * Constructor
		 */
		public EEEntry(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return this.name;
		}
	}

	/**
	 * Content provider for the viewer
	 */
	static class ContentProvider implements ITreeContentProvider {

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof IApiComponent) {
				try {
					IApiComponent component = (IApiComponent) parentElement;
					String[] ees = component.getExecutionEnvironments();
					ArrayList<EEEntry> entries = new ArrayList<>(ees.length);
					for (String ee : ees) {
						entries.add(new EEEntry(ee));
					}
					return entries.toArray();
				} catch (CoreException e) {
					ApiPlugin.log(e);
				}
			}
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof IApiComponent) {
				try {
					IApiComponent component = (IApiComponent) element;
					return component.getExecutionEnvironments().length > 0;
				} catch (CoreException e) {
					ApiPlugin.log(e);
				}
			}
			return false;
		}

		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof IApiComponent[]) {
				return (Object[]) inputElement;
			}
			return new Object[0];
		}


		@Override
		public Object getParent(Object element) {
			return null;
		}

	}

	/**
	 * Operation that creates a new working copy for an {@link IApiProfile} that
	 * is being edited
	 */
	static class WorkingCopyOperation implements IRunnableWithProgress {

		IApiBaseline original = null, workingcopy = null;

		/**
		 * Constructor
		 *
		 * @param original
		 */
		public WorkingCopyOperation(IApiBaseline original) {
			this.original = original;
		}

		@Override
		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			try {
				IApiComponent[] components = original.getApiComponents();
				IProgressMonitor localmonitor = SubMonitor.convert(monitor, WizardMessages.ApiProfileWizardPage_create_working_copy, components.length + 1);
				localmonitor.subTask(WizardMessages.ApiProfileWizardPage_copy_profile_attribs);
				workingcopy = ApiModelFactory.newApiBaseline(original.getName(), original.getLocation());
				localmonitor.worked(1);
				localmonitor.subTask(WizardMessages.ApiProfileWizardPage_copy_api_components);
				ArrayList<IApiComponent> comps = new ArrayList<>();
				IApiComponent comp = null;
				for (IApiComponent component : components) {
					comp = ApiModelFactory.newApiComponent(workingcopy, component.getLocation());
					if (comp != null) {
						comps.add(comp);
					}
					localmonitor.worked(1);
				}
				workingcopy.addApiComponents(comps.toArray(new IApiComponent[comps.size()]));
			} catch (CoreException ce) {
				ApiUIPlugin.log(ce);
			}
		}

		/**
		 * Returns the newly created {@link IApiProfile} working copy or
		 * <code>null</code>
		 *
		 * @return the working copy or <code>null</code>
		 */
		public IApiBaseline getWorkingCopy() {
			return workingcopy;
		}
	}

	IApiBaseline fProfile = null;
	private String originalname = null;

	/**
	 * Flag to know if the baselines' content has actually changed, or just some
	 * other attribute https://bugs.eclipse.org/bugs/show_bug.cgi?id=267875
	 */
	boolean contentchange = false;

	protected ApiBaselineWizardPage(IApiBaseline profile) {
		super(WizardMessages.ApiProfileWizardPage_1);
		this.fProfile = profile;
		setTitle(WizardMessages.ApiProfileWizardPage_1);
		if (profile == null) {
			setMessage(WizardMessages.ApiProfileWizardPage_3);
		} else {
			originalname = fProfile.getName();
			setMessage(WizardMessages.ApiProfileWizardPage_4);
		}
		setImageDescriptor(ApiUIPlugin.getImageDescriptor(IApiToolsConstants.IMG_WIZBAN_PROFILE));
	}

	/**
	 * Hook up the widgets to the profile, if any. Subclasses should call this
	 * method as part of
	 * {@link #createControl(org.eclipse.swt.widgets.Composite)} as it makes a
	 * working copy of the provided profile, which could take a long time.
	 */
	protected void initialize() {
		if (fProfile == null) {
			return;
		}
		WorkingCopyOperation op = new WorkingCopyOperation(fProfile);
		try {
			getContainer().run(true, false, op);
		} catch (InvocationTargetException e) {
			ApiUIPlugin.log(e);
		} catch (InterruptedException e) {
			ApiUIPlugin.log(e);
		}
		fProfile = op.getWorkingCopy();
	}

	/**
	 * @param name
	 * @return
	 */
	protected boolean isNameValid(String name) {
		if (name.length() < 1) {
			setErrorMessage(WizardMessages.ApiProfileWizardPage_20);
			return false;
		}
		if (!name.equals(originalname) && (((ApiBaselineManager) ApiPlugin.getDefault().getApiBaselineManager()).isExistingProfileName(name) && !ApiBaselinePreferencePage.isRemovedBaseline(name))) {
			setErrorMessage(WizardMessages.ApiProfileWizardPage_profile_with_that_name_exists);
			return false;
		}
		IStatus status = ResourcesPlugin.getWorkspace().validateName(name, IResource.FILE);
		if (!status.isOK()) {
			setErrorMessage(status.getMessage());
			return false;
		}
		return true;
	}

	/**
	 * Returns the current API components in the baseline or an empty collection
	 * if none.
	 *
	 * @return the current API components in the baseline or an empty collection
	 *         if none
	 */
	protected IApiComponent[] getCurrentComponents() {
		if (fProfile != null) {
			return fProfile.getApiComponents();
		}
		return new IApiComponent[0];
	}

	/**
	 * Creates or edits the profile and returns it
	 *
	 * @return a new {@link IApiProfile} or <code>null</code> if an error was
	 *         encountered creating the new profile
	 * @throws IOException
	 * @throws CoreException
	 */
	public abstract IApiBaseline finish() throws IOException, CoreException;

	/**
	 * @return if the actual content of the base line has changed and not just
	 *         some other attribute
	 */
	public boolean contentChanged() {
		return this.contentchange;
	}

	/**
	 * Cleans up the working copy if the page is canceled
	 */
	public void cancel() {
		if (fProfile != null) {
			fProfile.dispose();
		}
	}

}

/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.samples;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.*;
import org.eclipse.ui.activities.IWorkbenchActivitySupport;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.ide.IDE;

public class SampleWizard extends Wizard implements INewWizard, IExecutableExtension {
	private IConfigurationElement[] samples;
	private IConfigurationElement selection;
	private ProjectNamesPage namesPage;
	private ReviewPage lastPage;

	private boolean sampleEditorNeeded = true;
	private boolean switchPerspective = true;
	private boolean selectRevealEnabled = true;
	private boolean activitiesEnabled = true;

	private class ImportOverwriteQuery implements IOverwriteQuery {
		public String queryOverwrite(String file) {
			String[] returnCodes = {YES, NO, ALL, CANCEL};
			int returnVal = openDialog(file);
			return returnVal < 0 ? CANCEL : returnCodes[returnVal];
		}

		private int openDialog(final String file) {
			final int[] result = {IDialogConstants.CANCEL_ID};
			getShell().getDisplay().syncExec(new Runnable() {
				public void run() {
					String title = PDEUIMessages.SampleWizard_title;
					String msg = NLS.bind(PDEUIMessages.SampleWizard_overwrite, file);
					String[] options = {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.YES_TO_ALL_LABEL, IDialogConstants.CANCEL_LABEL};
					MessageDialog dialog = new MessageDialog(getShell(), title, null, msg, MessageDialog.QUESTION, options, 0);
					result[0] = dialog.open();
				}
			});
			return result[0];
		}
	}

	/**
	 * The default constructor.
	 *  
	 */
	public SampleWizard() {
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWEXP_WIZ);
		samples = Platform.getExtensionRegistry().getConfigurationElementsFor("org.eclipse.pde.ui.samples"); //$NON-NLS-1$
		namesPage = new ProjectNamesPage(this);
		lastPage = new ReviewPage(this);
		setNeedsProgressMonitor(true);
		setWindowTitle(PDEUIMessages.ShowSampleAction_title);
	}

	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}

	public IConfigurationElement[] getSamples() {
		return samples;
	}

	/**
	 *  
	 */
	public void addPages() {
		if (selection == null) {
			addPage(new SelectionPage(this));
		}
		addPage(namesPage);
		addPage(lastPage);
	}

	/**
	 *  
	 */
	public boolean performFinish() {
		try {
			String perspId = selection.getAttribute("perspectiveId"); //$NON-NLS-1$
			IWorkbenchPage page = PDEPlugin.getActivePage();
			if (perspId != null && switchPerspective) {
				PlatformUI.getWorkbench().showPerspective(perspId, page.getWorkbenchWindow());
			}
			SampleOperation op = new SampleOperation(selection, namesPage.getProjectNames(), new ImportOverwriteQuery());
			getContainer().run(true, true, op);
			IFile sampleManifest = op.getSampleManifest();
			if (selectRevealEnabled) {
				selectReveal(getShell());
			}
			if (activitiesEnabled)
				enableActivities();
			if (sampleEditorNeeded && sampleManifest != null)
				IDE.openEditor(page, sampleManifest, true);
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
			return false;
		} catch (InterruptedException e) {
			//PDEPlugin.logException(e);
			return false;
		} catch (CoreException e) {
			PDEPlugin.logException(e);
			return false;
		} catch (OperationCanceledException e) {
			return false;
		}
		return true;
	}

	public void selectReveal(Shell shell) {
		/*
		shell.getDisplay().asyncExec(new Runnable() {
			public void run() {
				doSelectReveal();
			}
		});
		*/
	}

	/*private void doSelectReveal() {
		if (selection == null || createdProjects==null)
			return;
		String viewId = selection.getAttribute("targetViewId"); //$NON-NLS-1$
		if (viewId == null)
			return;
		IWorkbenchWindow window = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();
		if (window == null)
			return;
		IWorkbenchPage page = window.getActivePage();
		if (page == null)
			return;
		IViewPart view = page.findView(viewId);
		if (view == null || !(view instanceof ISetSelectionTarget))
			return;
		ISetSelectionTarget target = (ISetSelectionTarget) view;
		IConfigurationElement[] projects = selection.getChildren("project"); //$NON-NLS-1$

		ArrayList items = new ArrayList();
		for (int i = 0; i < projects.length; i++) {
			String path = projects[i].getAttribute("selectReveal"); //$NON-NLS-1$
			if (path == null)
				continue;
			IResource resource = createdProjects[i].findMember(path);
			if (resource.exists())
				items.add(resource);
		}
		if (items.size() > 0)
			target.selectReveal(new StructuredSelection(items));
	}
	*/
	public void enableActivities() {
		IConfigurationElement[] elements = selection.getChildren("activity"); //$NON-NLS-1$
		HashSet activitiesToEnable = new HashSet();
		IWorkbenchActivitySupport workbenchActivitySupport = PlatformUI.getWorkbench().getActivitySupport();

		for (int i = 0; i < elements.length; i++) {
			IConfigurationElement element = elements[i];
			String id = element.getAttribute("id"); //$NON-NLS-1$
			if (id == null)
				continue;
			activitiesToEnable.add(id);
		}
		HashSet set = new HashSet(workbenchActivitySupport.getActivityManager().getEnabledActivityIds());
		set.addAll(activitiesToEnable);
		workbenchActivitySupport.setEnabledActivityIds(set);
	}

	/**
	 *  
	 */
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
		String variable = data != null && data instanceof String ? data.toString() : null;
		if (variable != null) {
			for (int i = 0; i < samples.length; i++) {
				IConfigurationElement element = samples[i];
				String id = element.getAttribute("id"); //$NON-NLS-1$
				if (id != null && id.equals(variable)) {
					setSelection(element);
					break;
				}
			}
		}
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}

	/**
	 * @return Returns the selection.
	 */
	public IConfigurationElement getSelection() {
		return selection;
	}

	/**
	 * @param selection
	 *            The selection to set.
	 */
	public void setSelection(IConfigurationElement selection) {
		this.selection = selection;
	}

	/**
	 * @return Returns the sampleEditorNeeded.
	 */
	public boolean isSampleEditorNeeded() {
		return sampleEditorNeeded;
	}

	/**
	 * @param sampleEditorNeeded
	 *            The sampleEditorNeeded to set.
	 */
	public void setSampleEditorNeeded(boolean sampleEditorNeeded) {
		this.sampleEditorNeeded = sampleEditorNeeded;
	}

	/**
	 * @return Returns the switchPerspective.
	 * @todo Generated comment
	 */
	public boolean isSwitchPerspective() {
		return switchPerspective;
	}

	/**
	 * @param switchPerspective The switchPerspective to set.
	 * @todo Generated comment
	 */
	public void setSwitchPerspective(boolean switchPerspective) {
		this.switchPerspective = switchPerspective;
	}

	/**
	 * @return Returns the selectRevealEnabled.
	 * @todo Generated comment
	 */
	public boolean isSelectRevealEnabled() {
		return selectRevealEnabled;
	}

	/**
	 * @param selectRevealEnabled The selectRevealEnabled to set.
	 * @todo Generated comment
	 */
	public void setSelectRevealEnabled(boolean selectRevealEnabled) {
		this.selectRevealEnabled = selectRevealEnabled;
	}

	/**
	 * @return Returns the activitiesEnabled.
	 * @todo Generated comment
	 */
	public boolean getActivitiesEnabled() {
		return activitiesEnabled;
	}

	/**
	 * @param activitiesEnabled The activitiesEnabled to set.
	 * @todo Generated comment
	 */
	public void setActivitiesEnabled(boolean activitiesEnabled) {
		this.activitiesEnabled = activitiesEnabled;
	}
}

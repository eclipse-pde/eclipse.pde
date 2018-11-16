/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.wizards.exports;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import com.ibm.icu.text.MessageFormat;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.XMLPrintHandler;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.feature.WorkspaceFeatureModel;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModelBase;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.build.BaseBuildAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.w3c.dom.Document;

public abstract class AntGeneratingExportWizard extends BaseExportWizard {

	protected BaseExportWizardPage fPage;

	@Override
	public void addPages() {
		fPage = createPage1();
		addPage(fPage);
	}

	protected abstract BaseExportWizardPage createPage1();

	@Override
	protected boolean performPreliminaryChecks() {
		// Check if we are going to overwrite an existing build.xml file
		if (!MessageDialogWithToggle.ALWAYS.equals(PDEPlugin.getDefault().getPreferenceStore().getString(IPreferenceConstants.OVERWRITE_BUILD_FILES_ON_EXPORT))) {
			Object[] objects = fPage.getSelectedItems();
			List<Object> problemModels = new ArrayList<>();
			for (Object object : objects) {
				String installLocation = null;
				IResource underlyingResource = null;
				if (object instanceof WorkspacePluginModelBase) {
					installLocation = ((WorkspacePluginModelBase) object).getInstallLocation();
					underlyingResource = ((WorkspacePluginModelBase) object).getUnderlyingResource();
				} else if (object instanceof WorkspaceFeatureModel) {
					installLocation = ((WorkspaceFeatureModel) object).getInstallLocation();
					underlyingResource = ((WorkspaceFeatureModel) object).getUnderlyingResource();
				}
				if (installLocation != null && underlyingResource != null) {
					File file = new File(installLocation, "build.xml"); //$NON-NLS-1$
					if (file.exists()) {
						try {
							IFile buildFile = PDEProject.getBuildProperties(underlyingResource.getProject());
							IBuildModel buildModel = new WorkspaceBuildModel(buildFile);
							buildModel.load();
							IBuildEntry entry = buildModel.getBuild().getEntry(IBuildPropertiesConstants.PROPERTY_CUSTOM);
							if (entry == null || !entry.contains(IBuildPropertiesConstants.TRUE)) {
								problemModels.add(object);
							}
						} catch (CoreException e) {
							PDEPlugin.log(e);
						}
					}
				}
			}
			if (!problemModels.isEmpty()) {
				StringBuilder buf = new StringBuilder();
				PDELabelProvider labelProvider = new PDELabelProvider();
				int maxCount = 10;
				for (Object object : problemModels) {
					buf.append(labelProvider.getText(object));
					buf.append('\n');
					maxCount--;
					if (maxCount <= 0) {
						buf.append(Dialog.ELLIPSIS);
						break;
					}
				}

				MessageDialogWithToggle dialog = MessageDialogWithToggle.openOkCancelConfirm(getShell(), PDEUIMessages.AntGeneratingExportWizard_0, MessageFormat.format(PDEUIMessages.AntGeneratingExportWizard_1, buf.toString()), PDEUIMessages.AntGeneratingExportWizard_2, false, PDEPlugin.getDefault().getPreferenceStore(), IPreferenceConstants.OVERWRITE_BUILD_FILES_ON_EXPORT);
				if (dialog.getReturnCode() == Window.CANCEL) {
					return false;
				}
			}
		}
		if (fPage.doGenerateAntFile())
			generateAntBuildFile(fPage.getAntBuildFileName());
		return true;
	}

	@Override
	protected boolean confirmDelete() {
		if (!fPage.doExportToDirectory()) {
			File zipFile = new File(fPage.getDestination(), fPage.getFileName());
			if (zipFile.exists()) {
				if (!MessageDialog.openQuestion(getContainer().getShell(), PDEUIMessages.BaseExportWizard_confirmReplace_title, NLS.bind(PDEUIMessages.BaseExportWizard_confirmReplace_desc, zipFile.getAbsolutePath())))
					return false;
				zipFile.delete();
			}
		}
		return true;
	}

	protected abstract Document generateAntTask();

	protected void generateAntBuildFile(String filename) {
		String parent = new Path(filename).removeLastSegments(1).toOSString();
		String buildFilename = new Path(filename).lastSegment();
		if (!buildFilename.endsWith(".xml")) //$NON-NLS-1$
			buildFilename += ".xml"; //$NON-NLS-1$
		File dir = new File(new File(parent).getAbsolutePath());
		if (!dir.exists())
			dir.mkdirs();

		try {
			Document task = generateAntTask();
			if (task != null) {
				File buildFile = new File(dir, buildFilename);
				XMLPrintHandler.writeFile(task, buildFile);
				generateAntTask();
				setDefaultValues(dir, buildFilename);
			}
		} catch (IOException e) {
		}
	}

	private void setDefaultValues(File dir, String buildFilename) {
		try {
			IContainer container = PDEPlugin.getWorkspace().getRoot().getContainerForLocation(new Path(dir.toString()));
			if (container != null && container.exists()) {
				IProject project = container.getProject();
				if (project != null) {
					project.refreshLocal(IResource.DEPTH_INFINITE, null);
					IFile file = container.getFile(new Path(buildFilename));
					if (file.exists())
						BaseBuildAction.setDefaultValues(file);
				}
			}
		} catch (CoreException e) {
		}
	}

	protected String getExportOperation() {
		return fPage.doExportToDirectory() ? "directory" : "zip"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	protected class AntErrorDialog extends MessageDialog {
		private File fLogLocation;

		public AntErrorDialog(File logLocation) {
			super(PlatformUI.getWorkbench().getDisplay().getActiveShell(), PDECoreMessages.FeatureBasedExportOperation_ProblemDuringExport, null, null, MessageDialog.ERROR, new String[] {IDialogConstants.OK_LABEL}, 0);
			fLogLocation = logLocation;
		}

		@Override
		protected Control createMessageArea(Composite composite) {
			Link link = new Link(composite, SWT.WRAP);
			try {
				link.setText(NLS.bind(PDEUIMessages.PluginExportWizard_Ant_errors_during_export_logs_generated, "<a>" + fLogLocation.getCanonicalPath() + "</a>")); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (IOException e) {
				PDEPlugin.log(e);
			}
			GridData data = new GridData();
			data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
			link.setLayoutData(data);
			link.addSelectionListener(widgetSelectedAdapter(e -> {
				try {
					Program.launch(fLogLocation.getCanonicalPath());
				} catch (IOException ex) {
					PDEPlugin.log(ex);
				}
			}));
			return link;
		}
	}

}

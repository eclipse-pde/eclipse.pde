/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.imports;

import java.util.ArrayList;
import java.util.Arrays;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.shared.CachedCheckboxTreeViewer;
import org.eclipse.pde.internal.ui.shared.FilteredCheckboxTree;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionStatusDialog;
import org.eclipse.ui.ide.IDE;
import org.osgi.framework.Version;

/**
 * This dialog expects a list of plug-in projects. It displays a filtered list to help 
 * select the projects to be deleted during the import process.
 * The returned results are the list of projects that shall be deleted.
 * 
 * @see PluginImportWizardDetailedPage
 * @see PluginImportOperation
 * @since 3.6
 */
public class OverwriteProjectsSelectionDialog extends SelectionStatusDialog {

	private static final String ID = "id"; //$NON-NLS-1$

	private class PluginContentProvider implements ITreeContentProvider {

		public Object[] getChildren(Object parentElement) {
			return new Object[0];
		}

		public Object getParent(Object element) {
			return null;
		}

		public boolean hasChildren(Object element) {
			return false;
		}

		public Object[] getElements(Object inputElement) {
			return (IPluginModelBase[]) inputElement;
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

	}

	private class StyledPluginLabelProvider extends StyledCellLabelProvider implements ILabelProvider {

		public Image getImage(Object element) {
			return PlatformUI.getWorkbench().getSharedImages().getImage(IDE.SharedImages.IMG_OBJ_PROJECT);
		}

		public String getText(Object element) {
			return getStyledText(element).getString();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.StyledCellLabelProvider#update(org.eclipse.jface.viewers.ViewerCell)
		 */
		public void update(ViewerCell cell) {
			StyledString string = getStyledText(cell.getElement());
			cell.setText(string.getString());
			cell.setStyleRanges(string.getStyleRanges());
			cell.setImage(getImage(cell.getElement()));
			super.update(cell);
		}

		private StyledString getStyledText(Object element) {
			StyledString styledString = new StyledString();
			IPluginModelBase plugin = (IPluginModelBase) element;
			String symbolicName = plugin.getBundleDescription().getSymbolicName();
			Version version = plugin.getBundleDescription().getVersion();
			String versionString = String.valueOf(version.getMajor()) + '.' + String.valueOf(version.getMinor()) + '.' + String.valueOf(version.getMicro());
			String projectName = plugin.getUnderlyingResource().getProject().getName();

			styledString.append(projectName);
			styledString.append(' ');
			styledString.append('(', StyledString.DECORATIONS_STYLER);
			styledString.append(symbolicName, StyledString.DECORATIONS_STYLER);
			styledString.append(' ');
			styledString.append(versionString, StyledString.DECORATIONS_STYLER);
			styledString.append(')', StyledString.DECORATIONS_STYLER);

			return styledString;
		}

	}

	/**
	 * Common listener for the Select All and Deselect All buttons
	 */
	private class ButtonSelectionListener extends SelectionAdapter {
		public void widgetSelected(SelectionEvent e) {
			String buttonID = (String) e.widget.getData(ID);
			if (PDEUIMessages.DuplicatePluginResolutionDialog_selectAll.equals(buttonID)) {
				fCheckboxTreeViewer.setAllChecked(true);
			} else if (PDEUIMessages.DuplicatePluginResolutionDialog_deselectAll.equals(buttonID)) {
				fCheckboxTreeViewer.setAllChecked(false);
			}
		}
	}

	private ArrayList fPluginProjectList;
	private FilteredCheckboxTree fFilteredTree;
	private CachedCheckboxTreeViewer fCheckboxTreeViewer;

	/**
	 * Constructor
	 * @param parent shell to create this dialog on top of
	 * @param plugins list of IPluginModelBase objects that have conflicts
	 */
	public OverwriteProjectsSelectionDialog(Shell parent, ArrayList plugins) {
		super(parent);
		setTitle(PDEUIMessages.PluginImportOperation_OverwritePluginProjects);
		Assert.isNotNull(plugins);
		fPluginProjectList = plugins;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.SelectionStatusDialog#configureShell(org.eclipse.swt.widgets.Shell)
	 */
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, IHelpContextIds.PLUGIN_IMPORT_OVERWRITE_DIALOG);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.SelectionStatusDialog#computeResult()
	 */
	protected void computeResult() {
		java.util.List result = Arrays.asList(fCheckboxTreeViewer.getCheckedLeafElements());
		setResult(result);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite tableComposite = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH, 15, 15);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = 400;
		gd.widthHint = 500;
		tableComposite.setLayoutData(gd);

		if (fPluginProjectList != null && fPluginProjectList.size() == 1) {
			setMessage(PDEUIMessages.DuplicatePluginResolutionDialog_messageSingular);
		} else {
			setMessage(PDEUIMessages.DuplicatePluginResolutionDialog_message);
		}
		SWTFactory.createWrapLabel(tableComposite, getMessage(), 1, 400);
		SWTFactory.createVerticalSpacer(tableComposite, 1);
		SWTFactory.createLabel(tableComposite, PDEUIMessages.OverwriteProjectsSelectionDialog_0, 1);

		createTableArea(tableComposite);

		Composite buttonComposite = SWTFactory.createComposite(tableComposite, 2, 1, GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END, 0, 5);
		Button buttonSelectAll = SWTFactory.createPushButton(buttonComposite, PDEUIMessages.DuplicatePluginResolutionDialog_selectAll, null);
		Button buttonDeselectAll = SWTFactory.createPushButton(buttonComposite, PDEUIMessages.DuplicatePluginResolutionDialog_deselectAll, null);
		buttonSelectAll.addSelectionListener(new ButtonSelectionListener());
		buttonSelectAll.setData(ID, PDEUIMessages.DuplicatePluginResolutionDialog_selectAll);
		buttonDeselectAll.addSelectionListener(new ButtonSelectionListener());
		buttonDeselectAll.setData(ID, PDEUIMessages.DuplicatePluginResolutionDialog_deselectAll);

		return tableComposite;
	}

	private void createTableArea(Composite parent) {
		fFilteredTree = new FilteredCheckboxTree(parent, null);
		fFilteredTree.getPatternFilter().setIncludeLeadingWildcard(true);
		fCheckboxTreeViewer = fFilteredTree.getCheckboxTreeViewer();
		fCheckboxTreeViewer.setContentProvider(new PluginContentProvider());
		fCheckboxTreeViewer.setLabelProvider(new StyledPluginLabelProvider());
		fCheckboxTreeViewer.setUseHashlookup(true);
		fCheckboxTreeViewer.setInput(fPluginProjectList.toArray(new IPluginModelBase[fPluginProjectList.size()]));
		for (int i = 0; i < fPluginProjectList.size(); i++) {
			fCheckboxTreeViewer.setChecked(fPluginProjectList.get(i), true);
		}
	}

}

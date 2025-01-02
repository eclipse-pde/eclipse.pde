/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.internal.ui.parts;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ModelEntry;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.target.NameVersionDescriptor;
import org.eclipse.pde.internal.build.Utils;
import org.eclipse.pde.internal.core.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.ManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.PackageObject;
import org.eclipse.pde.internal.core.util.UtilMessages;
import org.eclipse.pde.internal.core.util.VersionUtil;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.PDELabelUtility;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

public class PluginVersionPart {

	private class PluginVersionTablePart extends TablePart {

		public PluginVersionTablePart(String[] buttonLabels) {
			super(buttonLabels);
		}

		@Override
		protected void selectionChanged(IStructuredSelection selection) {
			if (selection.size() < 1) {
				setButtonEnabled(0, false);
			} else {
				setButtonEnabled(0, true);
			}
		}

		@Override
		protected void handleDoubleClick(IStructuredSelection selection) {
			if (selection.size() == 1) {
				IPluginModelBase entry = (IPluginModelBase) selection.getFirstElement();
				String version = VersionUtil.computeInitialPluginVersion(entry.getBundleDescription().getVersion().toString());
				setVersion(version, ""); //$NON-NLS-1$
			}
		}

		@Override
		protected void buttonSelected(Button button, int index) {
			IStructuredSelection selection = getTableViewer().getStructuredSelection();
			if (selection.size() == 1) {
				String version;
				if (isPlugin) {
					IPluginModelBase entry = (IPluginModelBase) selection.getFirstElement();
					version = VersionUtil
							.computeInitialPluginVersion(entry.getBundleDescription().getVersion().toString());
				} else {
					PackageObject po = (PackageObject) selection.getFirstElement();
					version = po.getVersion();
				}
				setVersion(version, ""); //$NON-NLS-1$
			} else {
				// plug-ins come back in a sorted order so we assume min/max
				String minVersion;
				String maxVersion;
				if (isPlugin) {
					Object[] objects = selection.toArray();
					IPluginModelBase min = (IPluginModelBase) objects[0];
					IPluginModelBase max = (IPluginModelBase) objects[objects.length - 1];

					minVersion = VersionUtil
						.computeInitialPluginVersion(min.getBundleDescription().getVersion().toString());
					maxVersion = VersionUtil
						.computeInitialPluginVersion(max.getBundleDescription().getVersion().toString());
				} else {
					Object[] objects = selection.toArray();
					PackageObject poMin = (PackageObject) objects[0];
					PackageObject poMax = (PackageObject) objects[objects.length - 1];

					minVersion = poMin.getVersion();
					maxVersion = poMax.getVersion();

				}
				setVersion(minVersion, maxVersion);
			}
		}

	}

	private static class PluginVersionContentProvider implements IStructuredContentProvider {

		@Override
		public Object[] getElements(Object element) {
			if (element instanceof ModelEntry entry) {
				return entry.getActiveModels();
			}
			return new Object[0];
		}


	}


	private static class ImportPackageVersionContentProvider implements IStructuredContentProvider {

		@Override
		public Object[] getElements(Object inputElement) {
			IPluginModelBase[] models = PluginRegistry.getActiveModels();
			ArrayList<PackageObject> list = new ArrayList<>();
			Set<NameVersionDescriptor> nameVersions = new HashSet<>();
			for (IPluginModelBase pluginModel : models) {
				BundleDescription desc = pluginModel.getBundleDescription();

				String id = desc == null ? null : desc.getSymbolicName();
				if (id == null)
					continue;
				ExportPackageDescription[] exported = desc.getExportPackages();
				for (ExportPackageDescription exportedPackage : exported) {
					String name = exportedPackage.getName();
					ManifestHeader mHeader = new ManifestHeader("Export-Package", "", new Bundle(), "\n"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
					PackageObject po = new PackageObject(mHeader, exportedPackage.getName(),
							exportedPackage.getVersion().toString(), "version"); //$NON-NLS-1$

					NameVersionDescriptor nameVersion = new NameVersionDescriptor(exportedPackage.getName(),
							exportedPackage.getVersion().toString(), NameVersionDescriptor.TYPE_PACKAGE);
					exportedPackage.getExporter().getBundle();

					if (("java".equals(name) || name.startsWith("java."))) //$NON-NLS-1$ //$NON-NLS-2$
						// $NON-NLS-2$
						continue;
					if (nameVersions.add(nameVersion)) {
						if (name.equalsIgnoreCase(inputElement.toString()))
								list.add(po);
					}
				}
			}
			return list.toArray();
		}

	}


	private Text fMinVersionText;
	private Text fMaxVersionText;
	private Combo fMinVersionBound;
	private Combo fMaxVersionBound;

	private VersionRange fVersionRange;
	private boolean fIsRanged;
	private final boolean fRangeAllowed;
	private boolean isPlugin;

	public PluginVersionPart(boolean rangeAllowed) {
		this(rangeAllowed, false);

	}

	public PluginVersionPart(boolean rangeAllowed, boolean isPlugin) {
		this.isPlugin = isPlugin;
		fRangeAllowed = rangeAllowed;
	}

	public void setVersion(String version) {
		try {
			if (version != null && !version.equals("")) { //$NON-NLS-1$
				fVersionRange = new VersionRange(version);
				Version max = fVersionRange.getRight();
				if (max != null && fVersionRange.getLeft().compareTo(max) < 0) {
					fIsRanged = true;
				}
			}
		} catch (IllegalArgumentException e) {
			// illegal version string passed
			fVersionRange = new VersionRange("[1.0.0,1.0.0]"); //$NON-NLS-1$
		}
	}

	private void setVersion(String min, String max) {
		fMinVersionBound.select(0);
		fMinVersionText.setText(min);
		fMaxVersionBound.select(1);
		fMaxVersionText.setText(max);
	}

	public void createVersionFields(Composite comp, boolean createGroup, boolean editable) {
		if (fRangeAllowed)
			createRangeField(comp, createGroup, editable);
		else
			createSingleField(comp, createGroup, editable);
		preloadFields();
	}

	public void createVersionSelectionField(Composite comp, String id) {
		Group group = new Group(comp, SWT.NONE);
		group.setText(PDEUIMessages.PluginVersionPart_groupTitle);
		group.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL));
		group.setLayout(new GridLayout(2, false));

		PluginVersionTablePart part = new PluginVersionTablePart(new String[] {PDEUIMessages.PluginVersionPart_buttonTitle});
		part.createControl(group, SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL, 1, null);
		part.setMinimumSize(0, 75);
		part.getViewer().setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		if (isPlugin) {
			part.getViewer().setContentProvider(new PluginVersionContentProvider());
			part.getViewer().setComparator(new ViewerComparator());
			part.getViewer().setInput(PluginRegistry.findEntry(id));
		} else {
			part.getViewer().setContentProvider(new ImportPackageVersionContentProvider());
			part.getViewer().setComparator(new ViewerComparator());
			part.getViewer().setInput(id);
		}
		part.setButtonEnabled(0, false);
	}

	private void createRangeField(Composite parent, boolean createGroup, boolean editable) {
		if (createGroup) {
			parent = new Group(parent, SWT.NONE);
			((Group) parent).setText(getGroupText());
			parent.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			parent.setLayout(new GridLayout(3, false));
		}
		String[] comboItems = new String[] {PDEUIMessages.DependencyPropertiesDialog_comboInclusive, PDEUIMessages.DependencyPropertiesDialog_comboExclusive};
		Label minlabel = new Label(parent, SWT.NONE);
		minlabel.setText(PDEUIMessages.DependencyPropertiesDialog_minimumVersion);
		fMinVersionText = new Text(parent, SWT.SINGLE | SWT.BORDER);
		fMinVersionText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fMinVersionText.setEnabled(editable);

		fMinVersionBound = new Combo(parent, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
		fMinVersionBound.setEnabled(editable);
		fMinVersionBound.setItems(comboItems);

		Label maxlabel = new Label(parent, SWT.NONE);
		maxlabel.setText(PDEUIMessages.DependencyPropertiesDialog_maximumVersion);
		fMaxVersionText = new Text(parent, SWT.SINGLE | SWT.BORDER);
		fMaxVersionText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fMaxVersionText.setEnabled(editable);

		fMaxVersionBound = new Combo(parent, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
		fMaxVersionBound.setEnabled(editable);
		fMaxVersionBound.setItems(comboItems);
	}

	private void createSingleField(Composite parent, boolean createGroup, boolean editable) {
		if (createGroup) {
			parent = new Group(parent, SWT.NONE);
			((Group) parent).setText(getGroupText());
			parent.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL));
			parent.setLayout(new GridLayout(2, false));
		}
		Label label = new Label(parent, SWT.NONE);
		label.setText(PDEUIMessages.DependencyPropertiesDialog_version);

		fMinVersionText = new Text(parent, SWT.SINGLE | SWT.BORDER);
		fMinVersionText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fMinVersionText.setEnabled(editable);
	}

	public void preloadFields() {
		if (fRangeAllowed) {
			fMinVersionText.setText((fVersionRange != null) ? fVersionRange.getLeft().toString() : ""); //$NON-NLS-1$
			fMaxVersionText.setText((fVersionRange != null && fVersionRange.getRight() != null) ? fVersionRange.getRight().toString() : ""); //$NON-NLS-1$

			if (fVersionRange != null)
				fMinVersionBound.select((fVersionRange.getLeftType() == VersionRange.LEFT_CLOSED) ? 0 : 1);
			else
				fMinVersionBound.select(0);

			if (fVersionRange != null && getMaxVersion().length() > 0)
				fMaxVersionBound.select((fVersionRange.getRightType() == VersionRange.RIGHT_CLOSED) ? 0 : 1);
			else
				fMaxVersionBound.select(1);
		}
		fMinVersionText.setText((fVersionRange != null) ? fVersionRange.getLeft().toString() : ""); //$NON-NLS-1$
	}

	private IStatus validateVersion(String text, Text textWidget, boolean shortErrorMessage) {
		if (text.length() == 0)
			return Status.OK_STATUS;
		if (!VersionUtil.validateVersion(text).isOK()) {
			String errorMessage = null;
			if (shortErrorMessage) {
				// For dialogs
				errorMessage = PDEUIMessages.DependencyPropertiesDialog_invalidFormat;
			} else {
				// For everything else:  Field assist, wizards
				errorMessage = UtilMessages.BundleErrorReporter_InvalidFormatInBundleVersion;
			}
			return Status.error(PDELabelUtility.qualifyMessage(PDELabelUtility.getFieldLabel(textWidget), errorMessage),
					null);
		}

		return Status.OK_STATUS;
	}

	private IStatus validateVersionRange(boolean shortErrorMessage) {
		if ((!fRangeAllowed && getMinVersion().length() == 0) || (fRangeAllowed && (getMinVersion().length() == 0 || getMaxVersion().length() == 0))) {
			fIsRanged = false;
			return Status.OK_STATUS;
		}

		String errorMessage = null;
		if (shortErrorMessage) {
			// For dialogs
			errorMessage = PDEUIMessages.DependencyPropertiesDialog_invalidFormat;
		} else {
			// For everything else:  Field assist, wizards
			errorMessage = UtilMessages.BundleErrorReporter_InvalidFormatInBundleVersion;
		}

		Version v1;
		Version v2;
		try {
			v1 = new Version(getMinVersion());
		} catch (IllegalArgumentException e) {
			return Status.error(
					PDELabelUtility.qualifyMessage(PDELabelUtility.getFieldLabel(fMinVersionText), errorMessage));
		}
		if (!fRangeAllowed) // version created fine
			return Status.OK_STATUS;

		try {
			v2 = new Version(getMaxVersion());
		} catch (IllegalArgumentException e) {
			return Status.error(
					PDELabelUtility.qualifyMessage(PDELabelUtility.getFieldLabel(fMaxVersionText), errorMessage));
		}
		if (v1.compareTo(v2) == 0 || v1.compareTo(v2) < 0) {
			fIsRanged = true;
			return Status.OK_STATUS;
		}
		return Status.error(PDEUIMessages.DependencyPropertiesDialog_versionRangeError);
	}

	/**
	 * Short error messages are required for dialog status lines.  Long error
	 * messages are truncated and are not decorated with a status image.
	 * @param shortErrorMessage if <code>true</code>, a brief error message
	 * will be used.
	 * @return an OK status if all versions are valid, otherwise the status's
	 * message will contain an error message.
	 */
	public IStatus validateFullVersionRangeText(boolean shortErrorMessage) {
		IStatus status = validateVersion(getMinVersion(), fMinVersionText, shortErrorMessage);
		if (status.isOK())
			status = validateVersion(getMaxVersion(), fMaxVersionText, shortErrorMessage);
		if (status.isOK())
			status = validateVersionRange(shortErrorMessage);
		return status;
	}

	private String getMinVersion() {
		return fMinVersionText.getText().trim();
	}

	private String getMaxVersion() {
		if (fMaxVersionText != null)
			return fMaxVersionText.getText().trim();
		return ""; //$NON-NLS-1$
	}

	private boolean getMinInclusive() {
		if (fMinVersionBound != null)
			return fMinVersionBound.getSelectionIndex() == 0;
		return false;
	}

	private boolean getMaxInclusive() {
		if (fMaxVersionBound != null)
			return fMaxVersionBound.getSelectionIndex() == 0;
		return true;
	}

	public String getVersion() {
		if (fIsRanged) {
			// if versions are equal they must be inclusive for a range to be valid
			// blindly set for the user
			String minV = getMinVersion();
			String maxV = getMaxVersion();
			boolean minI = getMinInclusive();
			boolean maxI = getMaxInclusive();
			if (minV.equals(maxV)) {
				minI = maxI = true;
			}
			return Utils.createVersionRange(minV, minI, maxV, maxI).toString();
		}
		if (!fRangeAllowed) {
			if (getMinVersion().length() > 0) {
				return new Version(getMinVersion()).toString();
			}
			return ""; //$NON-NLS-1$
		}
		if (getMinVersion().length() == 0 && getMaxVersion().length() > 0) {
			return Utils.createVersionRange(null, getMinInclusive(), getMaxVersion(), getMaxInclusive()).toString();
		}
		if (getMinVersion().length() > 0) {
			return new Version(getMinVersion()).toString();
		}
		return ""; //$NON-NLS-1$
	}

	public void addListeners(ModifyListener minListener, ModifyListener maxListener) {
		if (fMinVersionText != null && minListener != null)
			fMinVersionText.addModifyListener(minListener);
		if (fRangeAllowed && fMaxVersionText != null && maxListener != null)
			fMaxVersionText.addModifyListener(maxListener);
	}

	protected String getGroupText() {
		return PDEUIMessages.DependencyPropertiesDialog_groupText;
	}
}

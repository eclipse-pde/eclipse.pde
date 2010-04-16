/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 217908
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import com.ibm.icu.text.MessageFormat;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.core.util.VMUtil;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.ComboViewerPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.*;

public class JRESection extends PDESection {

	private final class EELabelProvider extends LabelProvider {
		public String getText(Object element) {
			if (!(element instanceof IExecutionEnvironment))
				return ""; //$NON-NLS-1$
			IExecutionEnvironment env = (IExecutionEnvironment) element;
			IPath path = JavaRuntime.newJREContainerPath(env);
			IVMInstall install = JavaRuntime.getVMInstall(path);
			String eeItem;
			if (install != null) {
				eeItem = MessageFormat.format(PDEUIMessages.JRESection_eeBoundJRE, new String[] {env.getId(), install.getName()});
			} else {
				eeItem = MessageFormat.format(PDEUIMessages.JRESection_eeUnboundJRE, new String[] {env.getId()});
			}
			return eeItem;
		}
	}

	private Button fEEButton;
	private Button fExecutionEnvironmentsButton;
	private ComboViewerPart fEEsCombo;
	private boolean fBlockChanges;

	private static final String[] TAB_LABELS = {"linux", "macosx", "solaris", "win32"}; //$NON-NLS-1$  //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	private static final String[] TAB_OS = {Platform.OS_LINUX, Platform.OS_MACOSX, Platform.OS_SOLARIS, Platform.OS_WIN32};

	private CTabFolder fTabFolder;
	private int fLastTab;

	public JRESection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	protected void createClient(Section section, FormToolkit toolkit) {

		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		section.setLayoutData(data);

		section.setText(PDEUIMessages.ProductJRESection_title);
		section.setDescription(PDEUIMessages.ProductJRESection_desc);

		Composite client = toolkit.createComposite(section);
		client.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 3));
		client.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fTabFolder = new CTabFolder(client, SWT.FLAT | SWT.TOP);
		toolkit.adapt(fTabFolder, true, true);
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		fTabFolder.setLayoutData(gd);
		gd.heightHint = 2;
		gd.horizontalSpan = 3;
		gd.grabExcessHorizontalSpace = true;
		toolkit.getColors().initializeSectionToolBarColors();
		Color selectedColor = toolkit.getColors().getColor(IFormColors.TB_BG);
		fTabFolder.setSelectionBackground(new Color[] {selectedColor, toolkit.getColors().getBackground()}, new int[] {100}, true);

		fTabFolder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				refresh();
			}
		});
		fTabFolder.setUnselectedImageVisible(false);

		FormText text = toolkit.createFormText(client, false);
		text.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
		text.setText(PDEUIMessages.ProductJRESection_eeName, false, false);

		fEEsCombo = new ComboViewerPart();
		fEEsCombo.createControl(client, toolkit, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
		fEEsCombo.getControl().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fEEsCombo.setLabelProvider(new EELabelProvider());
		fEEsCombo.setComparator(new ViewerComparator());
		fEEsCombo.setItems(VMUtil.getExecutionEnvironments());
		fEEsCombo.addItem("", 0); //$NON-NLS-1$
		fEEsCombo.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				if (!fBlockChanges) {
					Object selection = ((IStructuredSelection) event.getSelection()).getFirstElement();
					setEE(selection instanceof IExecutionEnvironment ? (IExecutionEnvironment) selection : null);
					fEEButton.setEnabled(selection instanceof IExecutionEnvironment);
				}
			}
		});
		fEEsCombo.setEnabled(isEditable());

		fExecutionEnvironmentsButton = toolkit.createButton(client, PDEUIMessages.ProductJRESection_browseEEs, SWT.PUSH);
		GridDataFactory.fillDefaults().applyTo(fExecutionEnvironmentsButton);
		fExecutionEnvironmentsButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				PreferencesUtil.createPreferenceDialogOn(getSection().getShell(), "org.eclipse.jdt.debug.ui.jreProfiles", //$NON-NLS-1$
						new String[] {"org.eclipse.jdt.debug.ui.jreProfiles"}, null).open(); //$NON-NLS-1$ 
			}
		});
		fExecutionEnvironmentsButton.setEnabled(isEditable());

		fEEButton = toolkit.createButton(client, PDEUIMessages.ProdctJRESection_bundleJRE, SWT.CHECK);
		GridData buttonLayout = new GridData(GridData.FILL_HORIZONTAL);
		buttonLayout.horizontalSpan = 2;
		fEEButton.setLayoutData(buttonLayout);
		fEEButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getJVMLocations().setIncludeJREWithProduct(getOS(fLastTab), fEEButton.getSelection());
			}
		});
		fEEButton.setEnabled(isEditable());

		createTabs();
		toolkit.paintBordersFor(client);
		section.setClient(client);
		// Register to be notified when the model changes
		getProductModel().addModelChangedListener(this);
	}

	private void setEE(IExecutionEnvironment ee) {
		IPath eePath = null;
		if (ee != null)
			eePath = JavaRuntime.newJREContainerPath(ee);
		getJVMLocations().setJREContainerPath(getOS(fLastTab), eePath);

	}

	private IProductModel getProductModel() {
		return (IProductModel) getPage().getPDEEditor().getAggregateModel();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#dispose()
	 */
	public void dispose() {
		IProductModel model = getProductModel();
		if (model != null) {
			model.removeModelChangedListener(this);
		}
		super.dispose();
	}

	private void createTabs() {
		for (int i = 0; i < TAB_LABELS.length; i++) {
			CTabItem item = new CTabItem(fTabFolder, SWT.NULL);
			item.setText(TAB_LABELS[i]);
			item.setImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_OPERATING_SYSTEM_OBJ));
		}
		fLastTab = 0;
		fTabFolder.setSelection(fLastTab);

		String currentTarget = TargetPlatform.getOS();

		if (Platform.OS_WIN32.equals(currentTarget)) {
			fTabFolder.setSelection(3);
		} else if (Platform.OS_MACOSX.equals(currentTarget)) {
			fTabFolder.setSelection(1);
		} else if (Platform.OS_SOLARIS.equals(currentTarget)) {
			fTabFolder.setSelection(2);
		}
	}

	public void refresh() {
		fBlockChanges = true;
		fLastTab = fTabFolder.getSelectionIndex();
		fEEButton.setSelection(getJVMLocations().includeJREWithProduct(getOS(fLastTab)));
		IPath jrePath = getJVMLocations().getJREContainerPath(getOS(fLastTab));
		if (jrePath != null) {
			String eeID = JavaRuntime.getExecutionEnvironmentId(jrePath);
			IExecutionEnvironment env = VMUtil.getExecutionEnvironment(eeID);
			if (env != null) {
				if (!fEEsCombo.getItems().contains(env))
					fEEsCombo.addItem(env);
				fEEsCombo.select(env);
			} else {
				IVMInstall install = JavaRuntime.getVMInstall(jrePath);
				if (install != null) {
					fEEsCombo.select(null);
				}
			}
		} else {
			fEEsCombo.select(null);
		}
		updateWidgets();
		super.refresh();
		fBlockChanges = false;
	}

	private IJREInfo getJVMLocations() {
		IJREInfo info = getProduct().getJREInfo();
		if (info == null) {
			info = getModel().getFactory().createJVMInfo();
			getProduct().setJREInfo(info);
		}
		return info;
	}

	private String getOS(int tab) {
		if (tab >= 0 && tab < TAB_OS.length) {
			return TAB_OS[tab];
		}
		return null;
	}

	private IProduct getProduct() {
		return getModel().getProduct();
	}

	private IProductModel getModel() {
		return (IProductModel) getPage().getPDEEditor().getAggregateModel();
	}

	public boolean canPaste(Clipboard clipboard) {
		Display d = getSection().getDisplay();
		return d.getFocusControl() instanceof Text;
	}

	protected void updateWidgets() {
		fEEButton.setEnabled(isEditable() && fEEsCombo.getSelection() instanceof IExecutionEnvironment);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#modelChanged(org.eclipse.pde.core.IModelChangedEvent)
	 */
	public void modelChanged(IModelChangedEvent e) {
		// No need to call super, handling world changed event here
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			handleModelEventWorldChanged(e);
		}
	}

	private void handleModelEventWorldChanged(IModelChangedEvent event) {
		refresh();
		// Note:  A deferred selection event is fired from radio buttons when
		// their value is toggled, the user switches to another page, and the
		// user switches back to the same page containing the radio buttons
		// This appears to be a result of a SWT bug.
		// If the radio button is the last widget to have focus when leaving 
		// the page, an event will be fired when entering the page again.
		// An event is not fired if the radio button does not have focus.
		// The solution is to redirect focus to a stable widget.
		getPage().setLastFocusControl(fEEsCombo.getControl());
	}
}

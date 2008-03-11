/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.target;

import java.lang.reflect.InvocationTargetException;
import java.util.TreeSet;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.itarget.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.pde.internal.ui.util.LocaleUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class EnvironmentSection extends PDESection {

	private ComboPart fOSCombo;
	private ComboPart fWSCombo;
	private ComboPart fNLCombo;
	private ComboPart fArchCombo;

	private TreeSet fNLChoices;
	private TreeSet fOSChoices;
	private TreeSet fWSChoices;
	private TreeSet fArchChoices;
	private boolean LOCALES_INITIALIZED = false;

	public EnvironmentSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	protected void createClient(Section section, FormToolkit toolkit) {
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		section.setText(PDEUIMessages.EnvironmentSection_title);
		section.setDescription(PDEUIMessages.EnvironmentSection_description);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.verticalAlignment = SWT.TOP;
		data.horizontalSpan = 2;
		section.setLayoutData(data);

		Composite client = toolkit.createComposite(section);
		client.setLayout(FormLayoutFactory.createSectionClientGridLayout(true, 2));
		client.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite left = toolkit.createComposite(client);
		left.setLayout(new GridLayout(2, false));
		GridLayout layout = FormLayoutFactory.createClearGridLayout(false, 2);
		layout.horizontalSpacing = layout.verticalSpacing = 5;
		left.setLayout(layout);
		left.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		IEnvironmentInfo orgEnv = getEnvironment();
		initializeChoices(orgEnv);

		Label label = toolkit.createLabel(left, PDEUIMessages.EnvironmentSection_operationSystem);
		label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));

		fOSCombo = new ComboPart();
		fOSCombo.createControl(left, toolkit, SWT.SINGLE | SWT.BORDER);
		fOSCombo.getControl().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fOSCombo.setItems((String[]) fOSChoices.toArray(new String[fOSChoices.size()]));

		label = toolkit.createLabel(left, PDEUIMessages.EnvironmentSection_windowingSystem);
		label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));

		fWSCombo = new ComboPart();
		fWSCombo.createControl(left, toolkit, SWT.SINGLE | SWT.BORDER);
		fWSCombo.getControl().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fWSCombo.setItems((String[]) fWSChoices.toArray(new String[fWSChoices.size()]));

		Composite right = toolkit.createComposite(client);
		layout = FormLayoutFactory.createClearGridLayout(false, 2);
		layout.verticalSpacing = layout.horizontalSpacing = 5;
		right.setLayout(layout);
		right.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		label = toolkit.createLabel(right, PDEUIMessages.EnvironmentSection_architecture);
		label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));

		fArchCombo = new ComboPart();
		fArchCombo.createControl(right, toolkit, SWT.SINGLE | SWT.BORDER);
		fArchCombo.getControl().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fArchCombo.setItems((String[]) fArchChoices.toArray(new String[fArchChoices.size()]));

		label = toolkit.createLabel(right, PDEUIMessages.EnvironmentSection_locale);
		label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));

		fNLCombo = new ComboPart();
		fNLCombo.createControl(right, toolkit, SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL);
		fNLCombo.getControl().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fNLCombo.setItems((String[]) fNLChoices.toArray(new String[fNLChoices.size()]));

		refresh();

		fOSCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				getEnvironment().setOS(getText(fOSCombo));
			}
		});
		fWSCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				getEnvironment().setWS(getText(fWSCombo));
			}
		});
		fArchCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				getEnvironment().setArch(getText(fArchCombo));
			}
		});
		fNLCombo.getControl().addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent event) {
				// if we haven't gotten all the values for the NL's, display a busy cursor to the user while we find them.
				if (!LOCALES_INITIALIZED) {
					try {
						PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {
							public void run(IProgressMonitor monitor) {
								initializeAllLocales();
								LOCALES_INITIALIZED = true;
							}
						});
					} catch (InvocationTargetException e) {
						PDEPlugin.log(e);
					} catch (InterruptedException e) {
						PDEPlugin.log(e);
					}
				}

				// first time through, we should have a max item count of 1.
				// On the first time through, we need to set the new values, and also attach the listener
				// If we attached the listener initially, when we call setItems(..), it would make the editor dirty (when the user didn't change anything)
				if (fNLCombo.getItemCount() < 3) {
					String current = fNLCombo.getSelection();
					if (!fNLCombo.getControl().isDisposed()) {
						fNLCombo.setItems((String[]) fNLChoices.toArray(new String[fNLChoices.size()]));
						fNLCombo.setText(current);
					}

					fNLCombo.addModifyListener(new ModifyListener() {
						public void modifyText(ModifyEvent e) {
							String value = getText(fNLCombo);
							int index = value.indexOf("-"); //$NON-NLS-1$
							if (index > 0)
								value = value.substring(0, index);
							getEnvironment().setNL(value.trim());
						}
					});
				}

			}
		});

		toolkit.paintBordersFor(client);
		section.setClient(client);

		// Register to be notified when the model changes
		getModel().addModelChangedListener(this);
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

	/**
	 * @param event
	 */
	private void handleModelEventWorldChanged(IModelChangedEvent event) {
		// Perform the refresh
		refresh();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#dispose()
	 */
	public void dispose() {
		ITargetModel model = getModel();
		if (model != null) {
			model.removeModelChangedListener(this);
		}
		super.dispose();
	}

	private void initializeChoices(IEnvironmentInfo orgEnv) {
		fOSChoices = new TreeSet();
		String[] os = Platform.knownOSValues();
		for (int i = 0; i < os.length; i++)
			fOSChoices.add(os[i]);
		fOSChoices.add(""); //$NON-NLS-1$
		String fileValue = orgEnv.getOS();
		if (fileValue != null)
			fOSChoices.add(fileValue);

		fWSChoices = new TreeSet();
		String[] ws = Platform.knownWSValues();
		for (int i = 0; i < ws.length; i++)
			fWSChoices.add(ws[i]);
		fWSChoices.add(""); //$NON-NLS-1$
		fileValue = orgEnv.getWS();
		if (fileValue != null)
			fWSChoices.add(fileValue);

		fArchChoices = new TreeSet();
		String[] arch = Platform.knownOSArchValues();
		for (int i = 0; i < arch.length; i++)
			fArchChoices.add(arch[i]);
		fArchChoices.add(""); //$NON-NLS-1$
		fileValue = orgEnv.getArch();
		if (fileValue != null)
			fArchChoices.add(fileValue);

		fNLChoices = new TreeSet();
		fNLChoices.add(""); //$NON-NLS-1$
	}

	private void initializeAllLocales() {
		String[] nl = LocaleUtil.getLocales();
		for (int i = 0; i < nl.length; i++)
			fNLChoices.add(nl[i]);
		String fileValue = getEnvironment().getNL();
		if (fileValue != null)
			fNLChoices.add(LocaleUtil.expandLocaleName(fileValue));
		LOCALES_INITIALIZED = true;
	}

	private String getText(ComboPart combo) {
		Control control = combo.getControl();
		if (control instanceof Combo)
			return ((Combo) control).getText();
		return ((CCombo) control).getText();
	}

	private IEnvironmentInfo getEnvironment() {
		IEnvironmentInfo info = getTarget().getEnvironment();
		if (info == null) {
			info = getModel().getFactory().createEnvironment();
			getTarget().setEnvironment(info);
		}
		return info;
	}

	private ITarget getTarget() {
		return getModel().getTarget();
	}

	private ITargetModel getModel() {
		return (ITargetModel) getPage().getPDEEditor().getAggregateModel();
	}

	public void refresh() {
		IEnvironmentInfo orgEnv = getEnvironment();
		String presetValue = (orgEnv.getOS() == null) ? "" : orgEnv.getOS(); //$NON-NLS-1$
		fOSCombo.setText(presetValue);
		presetValue = (orgEnv.getWS() == null) ? "" : orgEnv.getWS(); //$NON-NLS-1$
		fWSCombo.setText(presetValue);
		presetValue = (orgEnv.getArch() == null) ? "" : orgEnv.getArch(); //$NON-NLS-1$
		fArchCombo.setText(presetValue);
		presetValue = (orgEnv.getNL() == null) ? "" : LocaleUtil.expandLocaleName(orgEnv.getNL()); //$NON-NLS-1$
		fNLCombo.setText(presetValue);

		super.refresh();
	}

	protected void updateChoices() {
		if (LOCALES_INITIALIZED)
			return;
		// prevent NPE Mike found, which we can't reproduce.  Somehow we call initializeAllLocales before the ITargetModel exists.
		if (getModel() == null)
			return;
		// kick off thread in backgroud to find the NL values
		new Thread(new Runnable() {
			public void run() {
				initializeAllLocales();
			}
		}).start();
	}

}

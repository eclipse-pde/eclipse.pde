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

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

public class SelectionPage extends WizardPage {
	private TablePart part;
	private Text desc;
	private SampleWizard wizard;

	class SelectionPart extends TablePart {
		public SelectionPart() {
			super(new String[] {"More Info"}); //$NON-NLS-1$
		}

		protected void buttonSelected(Button button, int index) {
			if (index == 0)
				doMoreInfo();
		}

		protected void selectionChanged(IStructuredSelection selection) {
			updateSelection(selection);
		}

		protected void handleDoubleClick(IStructuredSelection selection) {
		}
	}

	class SampleProvider extends DefaultContentProvider implements IStructuredContentProvider {
		public Object[] getElements(Object input) {
			return wizard.getSamples();
		}
	}

	class SampleLabelProvider extends LabelProvider {
		private Image image;

		public SampleLabelProvider() {
			image = PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_NEWEXP_TOOL);
		}

		public String getText(Object obj) {
			IConfigurationElement sample = (IConfigurationElement) obj;
			return sample.getAttribute("name"); //$NON-NLS-1$
		}

		public Image getImage(Object obj) {
			return image;
		}
	}

	/**
	 * @param pageName
	 */
	public SelectionPage(SampleWizard wizard) {
		super("selection"); //$NON-NLS-1$
		this.wizard = wizard;
		setTitle(PDEUIMessages.SelectionPage_title);
		setDescription(PDEUIMessages.SelectionPage_desc);
		part = new SelectionPart();
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 2;
		part.setMinimumSize(300, 300);
		part.createControl(container, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER, 2, null);
		part.getTableViewer().setContentProvider(new SampleProvider());
		part.getTableViewer().setLabelProvider(new SampleLabelProvider());
		desc = new Text(container, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 64;
		desc.setLayoutData(gd);
		part.getTableViewer().setInput(this);
		updateSelection(null);
		setControl(container);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.SELECTION);
	}

	private void doMoreInfo() {
		if (wizard.getSelection() != null) {
			IConfigurationElement desc[] = wizard.getSelection().getChildren("description"); //$NON-NLS-1$
			String helpHref = desc[0].getAttribute("helpHref"); //$NON-NLS-1$
			PlatformUI.getWorkbench().getHelpSystem().displayHelpResource(helpHref);
		}
	}

	private void updateSelection(IStructuredSelection selection) {
		if (selection == null) {
			desc.setText(""); //$NON-NLS-1$
			part.setButtonEnabled(0, false);
			setPageComplete(false);
		} else {
			IConfigurationElement sample = (IConfigurationElement) selection.getFirstElement();
			String text = ""; //$NON-NLS-1$
			String helpHref = null;
			IConfigurationElement[] sampleDesc = sample.getChildren("description"); //$NON-NLS-1$
			if (sampleDesc.length == 1) {
				text = sampleDesc[0].getValue();
				helpHref = sampleDesc[0].getAttribute("helpHref"); //$NON-NLS-1$
			}
			desc.setText(text);
			part.setButtonEnabled(0, helpHref != null);
			wizard.setSelection(sample);
			setPageComplete(true);
		}
	}
}

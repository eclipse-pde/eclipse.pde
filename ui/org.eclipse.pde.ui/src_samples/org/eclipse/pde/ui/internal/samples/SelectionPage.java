/*
 * Created on Mar 13, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.ui.internal.samples;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.newparts.TablePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.WorkbenchHelp;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class SelectionPage extends WizardPage {
	private TablePart part;
	private Text desc;
	private SampleWizard wizard;
	
	class SelectionPart extends TablePart {
		public SelectionPart() {
			super(new String [] { "More Info" });
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
		public Object [] getElements(Object input) {
			return wizard.getSamples();
		}
	}
	
	class SampleLabelProvider extends LabelProvider {
		private Image image;
		public SampleLabelProvider() {
			image = PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_NEWEXP_TOOL); 
		}
		public String getText(Object obj) {
			IConfigurationElement sample = (IConfigurationElement)obj;
			return sample.getAttribute("name");
		}
		public Image getImage(Object obj) {
			return image;
		}
	}
	/**
	 * @param pageName
	 */
	public SelectionPage(SampleWizard wizard) {
		super("selection");
		this.wizard = wizard;
		setTitle("Selection");
		setDescription("Select the sample to create from the provided list.");
		part  = new SelectionPart();
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
		desc = new Text(container, SWT.MULTI|SWT.BORDER|SWT.WRAP|SWT.V_SCROLL);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 64;
		desc.setLayoutData(gd);
		part.getTableViewer().setInput(this);
		updateSelection(null);
		setControl(container);
	}
	private void doMoreInfo() {
		if (wizard.getSelection()!=null) {
			IConfigurationElement desc[] = wizard.getSelection().getChildren("description");
			String helpHref = desc[0].getAttribute("helpHref");
			WorkbenchHelp.displayHelpResource(helpHref);
		}
	}
	private void updateSelection(IStructuredSelection selection) {
		if (selection==null) {
			desc.setText("");
			part.setButtonEnabled(0, false);
			setPageComplete(false);
		}
		else {
			IConfigurationElement sample = (IConfigurationElement)selection.getFirstElement();
			String text = "";
			String helpHref=null;
			IConfigurationElement [] sampleDesc = sample.getChildren("description");
			if (sampleDesc.length==1) {
				text = sampleDesc[0].getValue();
				helpHref = sampleDesc[0].getAttribute("helpHref");
			}
			desc.setText(text);
			part.setButtonEnabled(0, helpHref!=null);
			wizard.setSelection(sample);
			setPageComplete(true);
		}
	}
}
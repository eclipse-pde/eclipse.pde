package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.widgets.*;

public class ArgumentsSection extends PDESection {

	private Text fProgramArgs;
	private Text fVMArgs;
	private boolean fBlockNotification;

	public ArgumentsSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}
	
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(PDEPlugin.getResourceString("ArgumentsSection.title")); //$NON-NLS-1$
		section.setDescription(PDEPlugin.getResourceString("ArgumentsSection.desc")); //$NON-NLS-1$
		section.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Composite client = toolkit.createComposite(section);
		client.setLayout(new GridLayout());
		
		Label label = toolkit.createLabel(client, PDEPlugin.getResourceString("ArgumentsSection.program")); //$NON-NLS-1$
		label.setForeground(toolkit.getColors().getColor(FormColors.TITLE));
		
		fProgramArgs = toolkit.createText(client, "", SWT.MULTI|SWT.BORDER); //$NON-NLS-1$
		fProgramArgs.setLayoutData(new GridData(GridData.FILL_BOTH));
		fProgramArgs.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (!fBlockNotification)
					fireSaveNeeded();
			}
		});
		
		label = toolkit.createLabel(client, PDEPlugin.getResourceString("ArgumentsSection.vm")); //$NON-NLS-1$
		label.setForeground(toolkit.getColors().getColor(FormColors.TITLE));
		
		fVMArgs = toolkit.createText(client, "", SWT.MULTI|SWT.BORDER); //$NON-NLS-1$
		fVMArgs.setLayoutData(new GridData(GridData.FILL_BOTH));		
		fVMArgs.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (!fBlockNotification)
					fireSaveNeeded();
			}
		});
		
		toolkit.paintBordersFor(client);
		section.setClient(client);	
	}
	
	public void refresh() {
		fBlockNotification = true;
		IArgumentsInfo info = getLauncherArguments();
		fProgramArgs.setText(info.getProgramArguments());
		fVMArgs.setText(info.getVMArguments());
		super.refresh();
		fBlockNotification = false;
	}
	
	public void commit(boolean onSave) {
		getLauncherArguments().setProgramArguments(fProgramArgs.getText().trim());
		getLauncherArguments().setVMArguments(fVMArgs.getText().trim());
		super.commit(onSave);
	}
	
	private IArgumentsInfo getLauncherArguments() {
		IArgumentsInfo info = getProduct().getLauncherArguments();
		if (info == null) {
			info = getModel().getFactory().createLauncherArguments();
			getProduct().setLauncherArguments(info);
		}
		return info;
	}

	private IProduct getProduct() {
		return getModel().getProduct();
	}
	
	private IProductModel getModel() {
		return (IProductModel)getPage().getPDEEditor().getAggregateModel();
	}
	

}

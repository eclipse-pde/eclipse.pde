/*
 * Created on Mar 12, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.ui.internal.samples;

import java.util.HashSet;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ProjectNamesPage extends WizardPage {
	private SampleWizard wizard;
	private Composite container;
	/**
	 * @param pageName
	 */
	public ProjectNamesPage(SampleWizard wizard) {
		super("projects");
		this.wizard = wizard;
		setTitle("Project names");
		setDescription("Select project names or accept the defaults.");
	}
	public void setVisible(boolean visible) {
		setPageComplete(wizard.getSelection()!=null);
		if (container!=null) updateEntries();
		super.setVisible(visible);
	}
	
	private void updateEntries() {
		IConfigurationElement selection = wizard.getSelection();
		if (selection!=null) {
			setMessage(null);
			IConfigurationElement [] projects = selection.getChildren("project");
			Control [] children = container.getChildren();
			if (projects.length==1 && children.length==2) {
				Text text = (Text)children[1];
				text.setText(projects[0].getAttribute("name"));
				validateEntries();
				return;
			}
			// dispose all
			for (int i=0; i<children.length; i++) {
				children[i].dispose();
			}
			// create entries
			if (projects.length==1) {
				createEntry("Project name:", projects[0].getAttribute("name"));
			}
			else {
				for (int i=0; i<projects.length; i++) {
					String label = "Project name #"+(i+1)+":";
					createEntry(label, projects[i].getAttribute("name"));
				}
			}
			container.layout();
			validateEntries();
		}
		else {
			setMessage("No sample has been selected.", WizardPage.WARNING);
		}
	}
	public String [] getProjectNames() {
		Control [] children = container.getChildren();
		String [] names = new String[children.length/2];

		int index=0;
		for (int i=0; i<children.length; i++) {
			if (children[i] instanceof Text) {
				String name = ((Text)children[i]).getText();
				names[index++] = name;
			}
		}
		return names;
	}
	private void createEntry(String labelName, String projectName) {
		Label label = new Label(container, SWT.NULL);
		label.setText(labelName);
		label.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_CENTER));
		final Text text = new Text(container, SWT.SINGLE|SWT.BORDER);
		text.setText(projectName);
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validateEntries();
			}
		});
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}
	private void validateEntries() {
		Control [] children = container.getChildren();
		boolean empty=false;
		
		HashSet set = new HashSet();
		for (int i=0; i<children.length; i++) {
			if (children[i] instanceof Text) {
				String name = ((Text)children[i]).getText();
				if (name.length()==0) {
					empty=true;
					break;
				}
				else {
					IStatus nameStatus = PDEPlugin.getWorkspace().validateName(name, IResource.PROJECT);
					if (!nameStatus.isOK()) {
						setErrorMessage(nameStatus.getMessage());
						setPageComplete(false);
						return;
					}
					set.add(name);
				}
			}
		}
		if (empty) {
			setErrorMessage("Project name cannot be empty.");
			setPageComplete(false);
		}
		else {
			int nnames = set.size();
			int nfields = children.length/2;
			if (nfields>nnames) {
				setErrorMessage("Duplicate project names.");
				setPageComplete(false);
			}
			else {
				setPageComplete(true);
				setErrorMessage(null);
			}
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		container.setLayout(layout);
		setControl(container);
		updateEntries();
	}
}
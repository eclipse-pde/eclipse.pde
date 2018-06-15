/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ui.tests.smartimport.plugins;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.core.reference.ReferencedComposite;
import org.eclipse.reddeer.eclipse.ui.wizards.datatransfer.WizardProjectsImportPage;
import org.eclipse.reddeer.jface.wizard.WizardPage;
import org.eclipse.reddeer.swt.api.Combo;
import org.eclipse.reddeer.swt.api.Shell;
import org.eclipse.reddeer.swt.api.TreeItem;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.CheckBox;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.combo.LabeledCombo;
import org.eclipse.reddeer.swt.impl.label.DefaultLabel;
import org.eclipse.reddeer.swt.impl.link.DefaultLink;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.tree.DefaultTree;

public class SmartImportRootWizardPage extends WizardPage {

	private static final Logger log = Logger.getLogger(WizardProjectsImportPage.class);

	public SmartImportRootWizardPage(ReferencedComposite referencedComposite) {
		super(referencedComposite);
	}

	public void selectDirectory(String path){
		log.info("Selecting directory \""+path+"\" in SelectImportRootWizardPage.");
		Combo selectDirText = new LabeledCombo(this, "Import source:");
		selectDirText.setText(path);
	}


	public List<String> getDetectors(){
		List<String> resultList = new ArrayList<>();
		new DefaultLink(this, "installed project configurators").click();
		Shell configuratorsShell = new DefaultShell("Installed project configuratos");
		String labelText = new DefaultLabel(configuratorsShell, 1).getText();
		String[] split = labelText.split("\n");
		for (String row: split) {
			if (row.startsWith("*")){
				resultList.add(row.substring(2));
			}
		}
		new OkButton(configuratorsShell).click();
		new WaitWhile(new ShellIsAvailable(configuratorsShell));
		return resultList;
	}

	public void setSearchForNestedProjects(boolean value){
		new CheckBox(this, "Search for nested projects").toggle(value);
	}

	public void setDetectAndConfigureNatures(boolean value){
		new CheckBox(this, "Detect and configure project natures").toggle(value);
	}

	public void setHideOpenProjects(boolean value){
		new CheckBox(this, "Hide already open projects").toggle(value);
	}

	public List<ProjectProposal> getAllProjectProposals() {

		DefaultTree tree = new DefaultTree(this);
		List<ProjectProposal> returnList = parseTree(tree);
		return returnList;
	}

	private List<ProjectProposal> parseTree(DefaultTree tree) {
		List<ProjectProposal> returnList = new ArrayList<>();
		for (TreeItem treeItem : tree.getAllItems()) {
			ProjectProposal projectProposal = new ProjectProposal(treeItem.getCell(0));
			returnList.add(fillProjectProposal(projectProposal, treeItem));
		}
		return returnList;
	}

	private ProjectProposal fillProjectProposal(ProjectProposal projectProposal, TreeItem treeItem) {
		String cell = treeItem.getCell(1);
		String[] split = cell.split(",");
		for (String type : split) {
			if (!type.equals("")) {
				projectProposal.addImportAs(type);
			}
		}
		return projectProposal;
	}

}

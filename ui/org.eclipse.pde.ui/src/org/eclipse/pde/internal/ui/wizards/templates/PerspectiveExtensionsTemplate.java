package org.eclipse.pde.internal.ui.wizards.templates;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.xerces.validators.schema.identity.ValueStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelFactory;

public class PerspectiveExtensionsTemplate extends PDETemplateSection {
	
	public static final String KEY_TARGET_PERSPECTIVE = "targetPerspective";
	public static final String KEY_PERSPECTIVE_SHORTCUT = "perspectiveShortcut";
	public static final String KEY_VIEW_SHORTCUT = "viewShortcut";
	public static final String KEY_WIZARD_SHORTCUT = "wizardShortcut";
	public static final String KEY_VIEW = "view";
	public static final String KEY_VIEW_RELATIVE = "viewRelative";
	public static final String KEY_VIEW_RELATIONSHIP = "viewRelationship";
	public static final String KEY_ACTION_SET = "actionSet";

	/**
	 * Constructor for PerspectiveExtensionsTemplate.
	 */
	public PerspectiveExtensionsTemplate() {
		super();
	}

	public void addPages(Wizard wizard) {
		lists = new ArrayList[2];
		lists[0] = new ArrayList();
		lists[1] = new ArrayList();
		
		createOptions();
		
		pages = new WizardPage[2];
		createPage1(wizard);
		createPage2(wizard);
		wizard.addPage(pages[0]);
		wizard.addPage(pages[1]);
		
		
	}

	private void createPage1(Wizard wizard) {
		pages[0] = new GenericTemplateWizardPage(this, lists[0]);
		pages[0].setTitle("Target Perspective and Shortcuts");
		pages[0].setDescription("Add an action set and shortcuts to the target perspective");
		
	}
	
	private void createPage2(Wizard wizard) {
		pages[1] = new GenericTemplateWizardPage(this,lists[1]);
		pages[1].setTitle("View");
		pages[1].setDescription("Add a view to the Show submenu of the target perspective");
	}
	
	private void createOptions() {
		// add options to first page
		addOption(KEY_TARGET_PERSPECTIVE, "&Target Perspective ID:", "org.eclipse.ui.resourcePerspective", lists[0]);
		addOption(KEY_ACTION_SET,"&Action Set:", "org.eclipse.jdt.ui.JavaActionSet",lists[0]);
		addOption(KEY_PERSPECTIVE_SHORTCUT, "&Perspective Shortcut ID:", "org.eclipse.debug.ui.DebugPerspective", lists[0]);
		addOption(KEY_VIEW_SHORTCUT, "&View Shortcut ID:","org.eclipse.jdt.ui.wizards.NewProjectCreationWizard", lists[0]);
		addOption(KEY_WIZARD_SHORTCUT,"&Wizard Shortcut ID:","org.eclipse.jdt.ui.TypeHierarchy",lists[0]);

		// add options to second page 
		addOption(KEY_VIEW,"&View ID:","org.eclipse.jdt.ui.PackageExplorer", lists[1]);
		addOption(KEY_VIEW_RELATIVE,"&Relative View:","org.eclipse.ui.views.ResourceNavigator",lists[1]);
		addOption(KEY_VIEW_RELATIONSHIP,"Relative Position: ", new String[][] {
						{"stack","stack"},{"left","left"},{"right","right"},{"top","top"},{"bottom","bottom"}},"stack",lists[1]);
	}
	
	private ArrayList getAllPageOptions(TemplateOption source) {
		for (int i = 0; i < lists.length; i++) {
			if (lists[i].contains(source)) 
				return (ArrayList)lists[i];
		}
		return new ArrayList();
	}
			
	/**
	 * @see PDETemplateSection#getSectionId()
	 */
	public String getSectionId() {
		return "perspectiveExtensions";
	}

	/**
	 * @see GenericTemplateSection#validateOptions(TemplateOption)
	 */
	public void validateOptions(TemplateOption source) {
		if (source.isRequired() && source.isEmpty()) {
			flagMissingRequiredOption(source);
		} else {
			validateContainerPage(source);
		}
	}

	private void validateContainerPage(TemplateOption source) {
		ArrayList allPageOptions = getAllPageOptions(source);
		for (int i = 0; i < allPageOptions.size(); i++) {
			TemplateOption nextOption = (TemplateOption) allPageOptions.get(i);
			if (nextOption.isRequired() && nextOption.isEmpty()) {
				flagMissingRequiredOption(nextOption);
				return;
			}
		}
		resetPageState();
	}
		
	/**
	 * @see AbstractTemplateSection#updateModel(IProgressMonitor)
	 */
	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		IPluginExtension extension = createExtension(getUsedExtensionPoint(), true);
		IPluginModelFactory factory = model.getFactory();

		IPluginElement perspectiveElement = factory.createElement(extension);
		perspectiveElement.setName("perspectiveExtension");
		perspectiveElement.setAttribute("targetID", getStringOption(KEY_TARGET_PERSPECTIVE));
		
		IPluginElement wizardShortcutElement = factory.createElement(perspectiveElement);
		wizardShortcutElement.setName("wizardShorcut");
		wizardShortcutElement.setAttribute("id", getStringOption(KEY_WIZARD_SHORTCUT));
		perspectiveElement.add(wizardShortcutElement);
		
		IPluginElement viewShortcutElement = factory.createElement(perspectiveElement);
		viewShortcutElement.setName("viewShortcut");
		viewShortcutElement.setAttribute("id", getStringOption(KEY_VIEW_SHORTCUT));
		perspectiveElement.add(viewShortcutElement);
		
		IPluginElement perspectiveShortcutElement = factory.createElement(perspectiveElement);
		perspectiveShortcutElement.setName("perspectiveShortcut");
		perspectiveShortcutElement.setAttribute("id", getStringOption(KEY_PERSPECTIVE_SHORTCUT));
		perspectiveElement.add(perspectiveShortcutElement);
		
		IPluginElement actionSetElement = factory.createElement(perspectiveElement);
		actionSetElement.setName("actionSet");
		actionSetElement.setAttribute("id", getStringOption(KEY_ACTION_SET));
		perspectiveElement.add(actionSetElement);
		
		IPluginElement viewElement = factory.createElement(perspectiveElement);
		viewElement.setName("view");
		viewElement.setAttribute("id", getStringOption(KEY_VIEW));
		viewElement.setAttribute("relative", getStringOption(KEY_VIEW_RELATIVE));
		String relationship = getValue(KEY_VIEW_RELATIONSHIP).toString();
		viewElement.setAttribute("relationship",relationship);
		if (!relationship.equals("stack")) {
			viewElement.setAttribute("ratio","0.5");
		}
		perspectiveElement.add(viewElement);
		
		extension.add(perspectiveElement);
		if (!extension.isInTheModel())
			plugin.add(extension);
	}

	/**
	 * @see ITemplateSection#getUsedExtensionPoint()
	 */
	public String getUsedExtensionPoint() {
		return "org.eclipse.ui.perspectiveExtensions";
	}
	

}

package org.eclipse.pde.internal.ui.wizards.templates;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.ArrayList;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.ui.templates.TemplateOption;

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
		setPageCount(2);
		createOptions();
		
		WizardPage page0 = createPage(0);
		page0.setTitle("Target Perspective and Shortcuts");
		page0.setDescription("Add an action set and shortcuts to the target perspective");
		wizard.addPage(page0);
		
		WizardPage page1 = createPage(1);
		page1.setTitle("View");
		page1.setDescription("Add a view to the Show submenu of the target perspective");
		wizard.addPage(page1);
	}

	private void createOptions() {
		// add options to first page
		addOption(KEY_TARGET_PERSPECTIVE, "&Target Perspective ID:", "org.eclipse.ui.resourcePerspective", 0);
		addOption(KEY_ACTION_SET,"&Action Set:", "org.eclipse.jdt.ui.JavaActionSet", 0);
		addOption(KEY_PERSPECTIVE_SHORTCUT, "&Perspective Shortcut ID:", "org.eclipse.debug.ui.DebugPerspective", 0);
		addOption(KEY_VIEW_SHORTCUT, "&View Shortcut ID:","org.eclipse.jdt.ui.wizards.NewProjectCreationWizard", 0);
		addOption(KEY_WIZARD_SHORTCUT,"&Wizard Shortcut ID:","org.eclipse.jdt.ui.TypeHierarchy", 0);

		// add options to second page 
		addOption(KEY_VIEW,"&View ID:","org.eclipse.jdt.ui.PackageExplorer", 1);
		addOption(KEY_VIEW_RELATIVE,"&Relative View:","org.eclipse.ui.views.ResourceNavigator", 1);
		addOption(KEY_VIEW_RELATIONSHIP,"Relative Position: ", new String[][] {
						{"stack","stack"},{"left","left"},{"right","right"},{"top","top"},{"bottom","bottom"}},"stack", 1);
	}
	
	private TemplateOption [] getAllPageOptions(TemplateOption source) {
		int pageIndex = getPageIndex(source);
		if (pageIndex!= -1) return getOptions(pageIndex);
		return new TemplateOption[0];
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
		TemplateOption [] siblings = getAllPageOptions(source);
		for (int i = 0; i < siblings.length; i++) {
			TemplateOption nextOption = siblings[i];
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

package org.eclipse.pde.internal.wizards.extension;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.elements.*;
import java.util.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.wizards.*;



public class NewExtensionRegistryReader {
	public static final String TAG_WIZARD = "wizard";
	public static final String ATT_CATEGORY = "category";
	public static final String ATT_SHORTCUTTABLE = "availableAsShortcut";
	public static final String CATEGORY_SEPARATOR = "/";
	public static final String TAG_CATEGORY = "category";
	public static final String TAG_DESCRIPTION = "description";

	private final static String     UNCATEGORIZED_WIZARD_CATEGORY = "org.eclipse.pde.Other";
	private final static String     UNCATEGORIZED_WIZARD_CATEGORY_LABEL = "Other";
	public NewExtensionRegistryReader() {
		super();
	}
protected WizardCollectionElement createCollectionElement(WizardCollectionElement parent, String id, String label) {
	WizardCollectionElement newElement = new WizardCollectionElement(id, label, parent);

	if (parent != null)
		parent.add(newElement);

	return newElement;
}
protected WizardElement createWizardElement(IConfigurationElement config) {
	String name = config.getAttribute(WizardElement.ATT_NAME);
	String id = config.getAttribute(WizardElement.ATT_ID);
	String className = config.getAttribute(WizardElement.ATT_CLASS);
	if (name==null || id == null || className==null) return null;
	WizardElement element = new WizardElement(config);
	String imageName = config.getAttribute(WizardElement.ATT_ICON);
	if (imageName!=null) {
		IExtension extension = config.getDeclaringExtension();
		IPluginDescriptor pd = extension.getDeclaringPluginDescriptor();
		Image image = PDEPlugin.getDefault().getLabelProvider().getImageFromPlugin(pd, imageName);
		element.setImage(image);
	}
	return element;
}
protected String getCategoryStringFor(IConfigurationElement config) {
	String result = config.getAttribute(ATT_CATEGORY);
	if (result == null)
		result = UNCATEGORIZED_WIZARD_CATEGORY;

	return result;
}
protected WizardCollectionElement getChildWithID(
	WizardCollectionElement parent,
	String id) {
	Object[] children = parent.getChildren();

	if (children != null) {
		for (int i = 0; i < children.length; i++) {
			WizardCollectionElement currentChild = (WizardCollectionElement) children[i];
			if (currentChild.getId().equals(id))
				return currentChild;
		}
	}
	return null;
}
protected void insertUsingCategory(WizardElement element, ElementList result) {
	WizardCollectionElement currentResult = (WizardCollectionElement)result;
	StringTokenizer familyTokenizer = new StringTokenizer(getCategoryStringFor(element.getConfigurationElement()), CATEGORY_SEPARATOR);

	// use the period-separated sections of the current Wizard's category
	// to traverse through the NamedSolution "tree" that was previously created
	WizardCollectionElement currentCollectionElement = currentResult; // ie.- root
	boolean moveToOther = false;
	
	while (familyTokenizer.hasMoreElements()) {
		WizardCollectionElement tempCollectionElement =
			getChildWithID(currentCollectionElement,familyTokenizer.nextToken());
			
		if (tempCollectionElement == null) {    // can't find the path; bump it to uncategorized
			moveToOther = true;
			break;
		}
		else
			currentCollectionElement = tempCollectionElement;
	}
	
	if (moveToOther)
		moveElementToUncategorizedCategory(currentResult, element);
	else
		currentCollectionElement.getWizards().add(element);
}
protected void moveElementToUncategorizedCategory(
	WizardCollectionElement root,
	WizardElement element) {
	WizardCollectionElement otherCategory =
		getChildWithID(root, UNCATEGORIZED_WIZARD_CATEGORY);

	if (otherCategory == null)
		otherCategory =
			createCollectionElement(
				root,
				UNCATEGORIZED_WIZARD_CATEGORY,
				UNCATEGORIZED_WIZARD_CATEGORY_LABEL);

	otherCategory.getWizards().add(element);
}
private void processCategory(IConfigurationElement config, ElementList list) {
	WizardCollectionElement result = (WizardCollectionElement)list;
	Category category = null;
	
	category = new Category(config);
	if (category.getID()==null || category.getLabel()==null) {
		System.out.println("Cannot create category: id or name is missing");
		return;
	}
	
	String[] categoryPath = category.getParentCategoryPath();
	WizardCollectionElement parent = result;        // ie.- root
	
	if (categoryPath != null) {
		for (int i = 0; i < categoryPath.length; i++) {
			WizardCollectionElement tempElement = getChildWithID(parent, categoryPath[i]);
			if (tempElement == null) {
				parent = null;
				break;
			} else
				parent = tempElement;
		}
	}
	
	if (parent != null)
		createCollectionElement(parent, category.getID(), category.getLabel());
}
protected void processElement(
	IConfigurationElement element, 
	ElementList result, 
	boolean shortcutsOnly) {
	String tag = element.getName();
	if (tag.equals(TAG_WIZARD)) {
		WizardElement wizard = createWizardElement(element);
		if (shortcutsOnly) {
			String shortcut = element.getAttribute(ATT_SHORTCUTTABLE);
			if (shortcut != null && shortcut.toLowerCase().equals("true")) {
				result.add(wizard);
			}
		} else
			insertUsingCategory(wizard, result);
	} else
		if (tag.equals(TAG_CATEGORY)) {
			if (shortcutsOnly == false) {
				processCategory(element, result);
			}
		}
}
public ElementList readRegistry(
	String pluginId, 
	String pluginPointId, 
	boolean shortcutsOnly) {
	ElementList result = 
		(shortcutsOnly)
			? (new ElementList("shortcuts"))
			: (new WizardCollectionElement("root", "root", null)); 
	IPluginRegistry registry = Platform.getPluginRegistry();
	IExtensionPoint point = registry.getExtensionPoint(pluginId, pluginPointId);
	if (point == null)
		return null;

	IExtension[] extensions = point.getExtensions();
	for (int i = 0; i < extensions.length; i++) {
		IConfigurationElement[] elements = extensions[i].getConfigurationElements();
		for (int j = 0; j < elements.length; j++) {
			IConfigurationElement config = elements[j];
			processElement(config, result, shortcutsOnly);
		}
	}
	return result;
}
}

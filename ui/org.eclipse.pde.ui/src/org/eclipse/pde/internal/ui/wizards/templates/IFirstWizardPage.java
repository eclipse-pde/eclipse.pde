/*
 * Created on Oct 21, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.wizards.templates;

import org.eclipse.pde.ui.IPluginStructureData;
import org.eclipse.pde.ui.templates.IFieldData;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public interface IFirstWizardPage {
	IFieldData createFieldData();
	IPluginStructureData getStructureData();
}

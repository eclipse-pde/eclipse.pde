package org.eclipse.pde.ui;

import org.eclipse.pde.ui.templates.*;

/**
 * @author melhem
 *
 */
public interface IPluginFieldData extends IFieldData {
	
	String getClassname();
	
	boolean isUIPlugin();
	
	ITemplateSection[] getTemplateSections();
	
	boolean doGenerateClass();
}

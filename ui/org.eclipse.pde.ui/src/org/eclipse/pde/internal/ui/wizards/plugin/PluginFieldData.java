package org.eclipse.pde.internal.ui.wizards.plugin;

import java.util.*;

import org.eclipse.pde.ui.*;
import org.eclipse.pde.ui.templates.*;

/**
 * @author melhem
 *
 */
public class PluginFieldData extends AbstractFieldData implements IPluginFieldData {
	
	private String fClassname;	
	private boolean fIsUIPlugin = true;
	private boolean fDoGenerateClass = true;
	private ArrayList templates = new ArrayList();
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.IPluginFieldData#getClassname()
	 */
	public String getClassname() {
		return fClassname;
	}
	
	public void setClassname(String classname) {
		fClassname = classname;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.IPluginFieldData#isUIPlugin()
	 */
	public boolean isUIPlugin() {
		return fIsUIPlugin;
	}
	
	public void setIsUIPlugin(boolean isUIPlugin) {
		fIsUIPlugin = isUIPlugin;
	}
	
	public void addTemplate(ITemplateSection section) {
		if (!templates.contains(section))
			templates.add(section);
	}
	
	public ITemplateSection[] getTemplateSections() {
		return (ITemplateSection[]) templates.toArray(new ITemplateSection[templates.size()]);
	}
	
	public void setDoGenerateClass(boolean doGenerate) {
		fDoGenerateClass = doGenerate;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.IPluginFieldData#doGenerateClass()
	 */
	public boolean doGenerateClass() {
		return fDoGenerateClass;
	}
}

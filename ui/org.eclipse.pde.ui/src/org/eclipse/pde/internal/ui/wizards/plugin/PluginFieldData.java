/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.plugin;

import java.util.*;

import org.eclipse.pde.ui.*;
import org.eclipse.pde.ui.templates.*;

public class PluginFieldData extends AbstractFieldData implements IPluginFieldData {
	
	private String fClassname;	
	private boolean fIsUIPlugin = true;
	private boolean fDoGenerateClass = true;
	private boolean fRCPAppPlugin = false;
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
	
	public void setUIPlugin(boolean isUIPlugin) {
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
	
	public void setRCPApplicationPlugin(boolean isRCPAppPlugin){
	    fRCPAppPlugin = isRCPAppPlugin;
	}
	
	public boolean isRCPApplicationPlugin(){
	    return fRCPAppPlugin;
	}
	



}

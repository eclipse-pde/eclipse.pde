package org.eclipse.pde.internal.ui.model.bundle;

import java.util.*;


public class EclipseAutoStartHeader extends ManifestHeader {
	
	private ArrayList fExceptions = new ArrayList();
	
	public EclipseAutoStartHeader() {
		setName("Eclipse-AutoStart");
	}
	
	public void addException(String packageName) {
		if (!fExceptions.contains(packageName))
			fExceptions.add(packageName);
	}
	
	public void removeException(String packageName) {
		fExceptions.remove(packageName);
	}
	
	public String[] getExceptions() {
		return (String[])fExceptions.toArray(new String[fExceptions.size()]);
	}
}

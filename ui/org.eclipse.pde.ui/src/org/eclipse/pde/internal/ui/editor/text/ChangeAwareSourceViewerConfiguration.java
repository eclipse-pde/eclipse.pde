package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;

public abstract class ChangeAwareSourceViewerConfiguration extends TextSourceViewerConfiguration {
	
	public ChangeAwareSourceViewerConfiguration(IPreferenceStore store) {
		super(store);
	}
	
	public ChangeAwareSourceViewerConfiguration() {
		super();
	}
	
 	public abstract boolean affectsTextPresentation(PropertyChangeEvent event);
 	
 	public abstract boolean affectsColorPresentation(PropertyChangeEvent event);
 	
	public abstract void adaptToPreferenceChange(PropertyChangeEvent event);

}

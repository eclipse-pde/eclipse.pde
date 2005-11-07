package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;

public abstract class ChangeAwareSourceViewerConfiguration extends TextSourceViewerConfiguration {
	
	public abstract boolean affectsTextPresentation(PropertyChangeEvent event);
	public abstract void adaptToPreferenceChange(PropertyChangeEvent event);

}

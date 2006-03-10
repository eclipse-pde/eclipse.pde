package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.internal.core.builders.PDEMarkerFactory;
import org.eclipse.pde.internal.core.text.AbstractEditingModel;
import org.eclipse.pde.internal.core.text.IModelTextChangeListener;
import org.eclipse.pde.internal.core.text.build.Build;
import org.eclipse.pde.internal.core.text.build.BuildModel;
import org.eclipse.pde.internal.core.text.build.PropertiesTextChangeListener;

public abstract class BuildEntryMarkerResolution extends AbstractPDEMarkerResolution {

	protected String fEntry;
	protected String fToken;
	
	public BuildEntryMarkerResolution(int type, IMarker marker) {
		super(type);
		try {
			fEntry = (String)marker.getAttribute(PDEMarkerFactory.K_BUILD_ENTRY);
			fToken = (String)marker.getAttribute(PDEMarkerFactory.K_BUILD_TOKEN);
		} catch (CoreException e) {
		}
	}

	protected AbstractEditingModel createModel(IDocument doc) {
		return new BuildModel(doc, false);
	}

	protected abstract void createChange(Build build);
	
	protected void createChange(AbstractEditingModel model, IMarker marker) {
		if (model instanceof BuildModel)
			createChange((Build)((BuildModel)model).getBuild());
	}
	
	protected IModelTextChangeListener createListener(IDocument doc) {
		return new PropertiesTextChangeListener(doc);
	}
}

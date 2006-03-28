package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDEManager;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.nls.ExternalizeStringsOperation;
import org.eclipse.pde.internal.ui.nls.ModelChange;
import org.eclipse.pde.internal.ui.nls.ModelChangeElement;
import org.eclipse.text.edits.MalformedTreeException;

public class ExternalizeResolution extends AbstractXMLMarkerResolution {

	public ExternalizeResolution(int resolutionType, IMarker marker) {
		super(resolutionType, marker);
	}

	protected void createChange(IPluginModelBase model) {
		Object node = findNode(model);
		ModelChange change = new ModelChange(model, true);
		ModelChangeElement element = new ModelChangeElement(change, node);
		if (element.updateValue()) {
			String localization = PDEManager.getBundleLocalization(model) + ModelChange.LOCALIZATION_FILE_SUFFIX;
			IProject project = model.getUnderlyingResource().getProject();
			IFile file = project.getFile(localization);
			ExternalizeStringsOperation.checkPropertiesFile(file);
			try {
				ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
				manager.connect(file.getFullPath(), null);
				ITextFileBuffer buffer = manager.getTextFileBuffer(file.getFullPath());
				if (buffer.isDirty())
					buffer.commit(null, true);
				
				IDocument document = buffer.getDocument();
				ExternalizeStringsOperation.getPropertiesInsertEdit(document, element).apply(document);
				buffer.commit(null, true);
			} catch (CoreException e) {
				PDEPlugin.log(e);
			} catch (MalformedTreeException e) {
				PDEPlugin.log(e);
			} catch (BadLocationException e) {
				PDEPlugin.log(e);
			} finally {
				try {
					FileBuffers.getTextFileBufferManager().disconnect(file.getFullPath(), null);
				} catch (CoreException e) {
					PDEPlugin.log(e);
				}
			}
		}
	}

	public String getLabel() {
		if (isAttrNode())
			return NLS.bind(PDEUIMessages.ExternalizeResolution_attrib, getNameOfNode());
		if (fLocationPath.charAt(0) == '(')
			return NLS.bind(PDEUIMessages.ExternalizeResolution_text, getNameOfNode());
		return NLS.bind(PDEUIMessages.ExternalizeResolution_header, fLocationPath);
	}

}

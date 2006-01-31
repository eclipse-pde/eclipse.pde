package org.eclipse.pde.internal.ui.compare;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.pde.internal.ui.editor.text.ColorManager;
import org.eclipse.pde.internal.ui.editor.text.IColorManager;
import org.eclipse.pde.internal.ui.editor.text.XMLConfiguration;
import org.eclipse.pde.internal.ui.editor.text.XMLPartitionScanner;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;

public class XMLContentMergeViewer extends TextMergeViewer {

	private IColorManager fColorManager;

	public XMLContentMergeViewer(Composite parent, CompareConfiguration config) {
		super(parent, config);
	}

	protected void configureTextViewer(TextViewer textViewer) {
		if (textViewer instanceof SourceViewer) {
			if (fColorManager == null)
				fColorManager = ColorManager.getDefault();
			((SourceViewer)textViewer).configure(new XMLConfiguration(fColorManager));
			Font font = JFaceResources.getFont(XMLContentMergeViewer.class.getName());
			if (font != null)
				((SourceViewer)textViewer).getTextWidget().setFont(font);
		}
	}

	protected IDocumentPartitioner getDocumentPartitioner() {
		return new FastPartitioner(new XMLPartitionScanner(), XMLPartitionScanner.PARTITIONS);
	}
	
	public String getTitle() {
		return XMLStructureCreator.DEFAULT_NAME; 
	}

	protected void handleDispose(DisposeEvent event) {
		super.handleDispose(event);
		if (fColorManager != null)
			fColorManager.dispose();
	}
}

package $packageName$;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.ui.editors.text.TextEditor;

public class $editorClass$ extends TextEditor {

	private ColorManager colorManager;
	/**
	 * Constructor for SampleEditor.
	 */
	public $editorClass$() {
		super();
	}
	public void dispose() {
		colorManager.dispose();
		super.dispose();
	}
	
	/* (non-Javadoc)
	 * Method declared on AbstractTextEditor
	 */
	protected void initializeEditor() {
		colorManager = new ColorManager();
		setSourceViewerConfiguration(new XMLConfiguration(colorManager));
		setDocumentProvider(new XMLDocumentProvider());
	}

}

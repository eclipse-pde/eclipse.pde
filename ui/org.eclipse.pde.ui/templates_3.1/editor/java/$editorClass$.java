package $packageName$;

import org.eclipse.ui.editors.text.TextEditor;

public class $editorClass$ extends TextEditor {

	private ColorManager colorManager;

	public $editorClass$() {
		super();
		colorManager = new ColorManager();
		setSourceViewerConfiguration(new XMLConfiguration(colorManager));
		setDocumentProvider(new XMLDocumentProvider());
	}
	public void dispose() {
		colorManager.dispose();
		super.dispose();
	}

}

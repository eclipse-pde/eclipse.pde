package org.eclipse.pde.internal.ui.editor.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.help.ui.browser.IBrowser;
import org.eclipse.help.ui.internal.browser.BrowserManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.IEditable;
import org.eclipse.pde.internal.builders.SchemaTransformer;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.schema.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.program.Program;

public class SchemaEditor extends PDEMultiPageXMLEditor {
	public static final String DEFINITION_PAGE = "definition";
	public static final String DOC_PAGE = "documentation";
	public static final String SOURCE_PAGE = "source";
	public static final String KEY_OLD_EXTENSION = "SchemaEditor.oldExtension";
	private File previewFile;

	public SchemaEditor() {
		super();
	}

	protected Object createModel(Object input) {
		if (!(input instanceof IFile))
			return null;

		IFile file = (IFile) input;
		FileSchemaDescriptor sd = new FileSchemaDescriptor(file);
		ISchema schema = sd.getSchema();
		if (schema.isValid() == false)
			return null;
		warnIfOldExtension(file);
		if (schema instanceof EditableSchema) {
			((EditableSchema) schema).setNotificationEnabled(true);
		}
		return schema;
	}

	private void warnIfOldExtension(IFile file) {
		String name = file.getName();
		int dotLoc = name.lastIndexOf('.');
		if (dotLoc != -1) {
			String ext = name.substring(dotLoc + 1).toLowerCase();
			if (ext.equals("xsd")) {
				String title = getSite().getRegisteredName();
				String message = PDEPlugin.getResourceString(KEY_OLD_EXTENSION);
				MessageDialog.openWarning(
					PDEPlugin.getActiveWorkbenchShell(),
					title,
					message);
			}
		}
	}

	public void dispose() {
		PDECore.getDefault().getTempFileManager().disconnect(this);
		super.dispose();
	}

	private File getPreviewFile() throws CoreException {
		if (previewFile == null) {
			String prefix = "pde";
			String suffix = ".html";
			previewFile =
				PDECore.getDefault().getTempFileManager().createTempFile(
					this,
					prefix,
					suffix);
		}
		return previewFile;
	}

	void previewReferenceDocument() {
		ISchema schema = (ISchema) getModel();

		try {
			File tempFile = getPreviewFile();
			if (tempFile == null)
				return;

			SchemaTransformer transformer = new SchemaTransformer();
			OutputStream os = new FileOutputStream(tempFile);
			PrintWriter printWriter = new PrintWriter(os, true);
			transformer.transform(printWriter, schema);
			os.flush();
			os.close();
			showURL(tempFile.getPath());
		} catch (IOException e) {
			PDEPlugin.logException(e);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	protected void createPages() {
		firstPageId = DEFINITION_PAGE;
		SchemaFormPage form = new SchemaFormPage(this);
		SchemaDocPage doc = new SchemaDocPage(form);
		addPage(DEFINITION_PAGE, form);
		addPage(DOC_PAGE, doc);
		addPage(SOURCE_PAGE, new SchemaSourcePage(this));
	}
	public IPDEEditorPage getHomePage() {
		return getPage(DEFINITION_PAGE);
	}
	protected String getSourcePageId() {
		return SOURCE_PAGE;
	}
	protected boolean isModelCorrect(Object model) {
		if (model == null)
			return false;
		ISchema schema = (ISchema) model;
		return schema.isValid();
	}
	protected boolean isModelDirty(Object model) {
		return model instanceof IEditable && ((IEditable) model).isDirty();
	}
	protected boolean updateModel() {
		Schema schema = (Schema) getModel();
		if (schema == null)
			return false;
		IDocument document =
			getDocumentProvider().getDocument(getEditorInput());
		String text = document.get();
		try {
			InputStream stream =
				new ByteArrayInputStream(text.getBytes("UTF8"));
			schema.reload(stream);
			try {
				stream.close();
			} catch (IOException e) {
			}
		} catch (UnsupportedEncodingException e) {
			PDEPlugin.logException(e);
			return false;
		}
		return true;
	}
	private void showURL(String url) {
		boolean win32 = SWT.getPlatform().equals("win32");
		if (win32) {
			Program.launch(url);
		}
		else {
			IBrowser browser = BrowserManager.getInstance().createBrowser();
			browser.displayURL(url);
		}
	}
}

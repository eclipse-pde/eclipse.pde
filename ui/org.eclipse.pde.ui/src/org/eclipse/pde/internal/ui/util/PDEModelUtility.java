package org.eclipse.pde.internal.ui.util;

import java.util.ArrayList;
import java.util.Hashtable;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.text.AbstractEditingModel;
import org.eclipse.pde.internal.core.text.IModelTextChangeListener;
import org.eclipse.pde.internal.core.text.build.BuildModel;
import org.eclipse.pde.internal.core.text.build.PropertiesTextChangeListener;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.BundleTextChangeListener;
import org.eclipse.pde.internal.core.text.plugin.FragmentModel;
import org.eclipse.pde.internal.core.text.plugin.PluginModel;
import org.eclipse.pde.internal.core.text.plugin.XMLTextChangeListener;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.build.BuildEditor;
import org.eclipse.pde.internal.ui.editor.build.BuildInputContext;
import org.eclipse.pde.internal.ui.editor.build.BuildSourcePage;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.forms.editor.IFormPage;

/**
 * Your one stop shop for preforming changes do your plug-in models.
 *
 */
public class PDEModelUtility {
	
	public static final String F_MANIFEST = "MANIFEST.MF"; //$NON-NLS-1$
	public static final String F_PLUGIN = "plugin.xml"; //$NON-NLS-1$
	public static final String F_FRAGMENT = "fragment.xml"; //$NON-NLS-1$
	public static final String F_BUILD = "build.properties"; //$NON-NLS-1$
	
	private static Hashtable fOpenPDEEditors = new Hashtable();
	
	/**
	 * PDE editors should call this during their creation.
	 * 
	 * Currently the pde editor superclass (PDEFormEditor) 
	 * connects during its createPages method and so this
	 * method does not need to be invoked anywhere else.
	 * @param editor the editor to connect to
	 */
	public static void connect(PDEFormEditor editor) {
		IProject project = editor.getCommonProject();
		if (project == null)
			return;
		if (fOpenPDEEditors.containsKey(project)) {
			ArrayList list = (ArrayList)fOpenPDEEditors.get(project);
			if (!list.contains(editor))
				list.add(editor);
		} else {
			ArrayList list = new ArrayList();
			list.add(editor);
			fOpenPDEEditors.put(project, list);
		}
	}
	
	/**
	 * PDE editors should call this when they are closing down.
	 * @param editor the pde editor to disconnect from
	 */
	public static void disconnect(PDEFormEditor editor) {
		IProject project = editor.getCommonProject();
		if (project == null)
			return;
		if (!fOpenPDEEditors.containsKey(project))
			return;
		ArrayList list = (ArrayList)fOpenPDEEditors.get(project);
		list.remove(editor);
		if (list.size() == 0)
			fOpenPDEEditors.remove(project);
	}
	
	/**
	 * Returns an open ManifestEditor that is associated with this project.
	 * @param project
	 * @return null if no ManifestEditor is open for this project
	 */
	public static ManifestEditor getOpenManifestEditor(IProject project) {
		return (ManifestEditor)getOpenEditor(project, IPDEUIConstants.MANIFEST_EDITOR_ID);
	}
	
	/**
	 * Returns an open BuildEditor that is associated with this project.
	 * @param project
	 * @return null if no BuildEditor is open for this project
	 */
	public static BuildEditor getOpenBuildPropertiesEditor(IProject project) {
		return (BuildEditor)getOpenEditor(project, IPDEUIConstants.BUILD_EDITOR_ID);
	}
	
	private static PDEFormEditor getOpenEditor(IProject project, String editorId) {
		ArrayList list = (ArrayList)fOpenPDEEditors.get(project);
		if (list == null)
			return null;
		for (int i = 0; i < list.size(); i++) {
			PDEFormEditor editor = (PDEFormEditor)list.get(i);
			if (editor.getEditorSite().getId().equals(editorId))
				return editor;
		}
		return null;
	}
	
	/**
	 * Modify a model based on the specifications provided by the ModelModification parameter.
	 * 
	 * A model will be searched for in the open editors, if it is found changes will be applied
	 * and the editor will be saved.
	 * If no model is found one will be created and text edit operations will be generated / applied.
	 * @param modification
	 * @throws CoreException
	 */
	public static void modifyModel(final ModelModification modification) throws CoreException {
		IFile file = modification.getFile();
		
		final PDEFormEditor editor = getOpenEditor(modification);
		final IBaseModel model = getModelFromEditor(editor, modification);
		
		if (model != null) {
			// open editor found, should have underlying text listeners -> apply modification
			Runnable runnable = new Runnable() {
				public void run() {
					try {
						modification.modifyEditorModel(model);
						editor.doSave(null);
					} catch (CoreException e) {
						PDEPlugin.log(e);
					}
				}
			};
			PDEPlugin.getActiveWorkbenchShell().getDisplay().syncExec(runnable);
		} else {
			// create own model, attach listener and grab text edits
			ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
			try {
				manager.connect(file.getFullPath(), null);
				ITextFileBuffer buffer = manager.getTextFileBuffer(file.getFullPath());
				if (buffer.isDirty())
					buffer.commit(null, true);
				IDocument document = buffer.getDocument();	
				AbstractEditingModel editModel = prepareModel(modification, document);
				IModelTextChangeListener listener = createListener(file.getName(), document);
				if (!editModel.isLoaded() || listener == null)
					return;
				editModel.addModelChangedListener(listener);
				modification.modifyModel(editModel);
				TextEdit[] edits = listener.getTextOperations();
				if (edits.length > 0) {
					MultiTextEdit multi = new MultiTextEdit();
					multi.addChildren(edits);
					multi.apply(document);
					buffer.commit(null, true);
				}
			} catch (CoreException e) {
				PDEPlugin.log(e);
			} catch (MalformedTreeException e) {
				PDEPlugin.log(e);
			} catch (BadLocationException e) {
				PDEPlugin.log(e);
			} finally {
				manager.disconnect(file.getFullPath(), null);
			}
		}
	}
	
	private static PDEFormEditor getOpenEditor(ModelModification modification) {
		IProject project = modification.getFile().getProject();
		String name = modification.getFile().getName();
		PDEFormEditor openEditor = null;
		if (name.equals(F_PLUGIN) || name.equals(F_FRAGMENT)) {
			openEditor = getOpenManifestEditor(project);
		} else if (name.equals(F_BUILD)) {
			openEditor = getOpenBuildPropertiesEditor(project);
			if (openEditor == null)
				openEditor = getOpenManifestEditor(project);
		} else if (name.equals(F_MANIFEST)) {
			openEditor = getOpenManifestEditor(project);
		}
		return openEditor;
	}
	
	private static IBaseModel getModelFromEditor(PDEFormEditor openEditor, ModelModification modification) {
		if (openEditor == null)
			return null;
		String name = modification.getFile().getName();
		if (name.equals(F_PLUGIN) || name.equals(F_FRAGMENT)) {
			IBaseModel model = openEditor.getAggregateModel();
			if (model instanceof IBundlePluginModelBase)
				model = ((IBundlePluginModelBase)model).getExtensionsModel();
			if (model instanceof AbstractEditingModel)
				return model;
		} else if (name.equals(F_BUILD)) {
			if (openEditor instanceof BuildEditor) {
				IBaseModel model = openEditor.getAggregateModel();
				if (model instanceof AbstractEditingModel)
					return model;
			} else if (openEditor instanceof ManifestEditor) {
				IFormPage page = openEditor.findPage(BuildInputContext.CONTEXT_ID);
				if (page instanceof BuildSourcePage) {
					IBaseModel model = ((BuildSourcePage)page).getInputContext().getModel();
					if (model instanceof AbstractEditingModel)
						return model;
				}
			}
		} else if (name.equals(F_MANIFEST)) {
			IBaseModel model = openEditor.getAggregateModel();
			if (model instanceof IBundlePluginModel) {
				if (!modification.searchForBundlePlugin())
					return model;
				model = ((IBundlePluginModel)model).getBundleModel();
				if (model instanceof AbstractEditingModel)
					return model;
			}
		}
		return null;
	}
	
	private static IModelTextChangeListener createListener(String filename, IDocument doc) {
		if (filename.equals(F_PLUGIN) || filename.equals(F_FRAGMENT))
			return new XMLTextChangeListener(doc);
		else if (filename.equals(F_MANIFEST))
			return new BundleTextChangeListener(doc);
		else if (filename.equals(F_BUILD))
			return new PropertiesTextChangeListener(doc);
		return null;
	}
	
	private static AbstractEditingModel prepareModel(ModelModification mod, IDocument doc) {
		AbstractEditingModel model;
		String filename = mod.getFile().getName();
		if (filename.equals(F_MANIFEST))
			model = new BundleModel(doc, true);
		else if (filename.equals(F_FRAGMENT))
			model = new FragmentModel(doc, true);
		else if (filename.equals(F_PLUGIN))
			model = new PluginModel(doc, true);
		else if (filename.equals(F_BUILD))
			model = new BuildModel(doc, true);
		else
			return null;
		model.setUnderlyingResource(mod.getFile());
		try {
			model.load();
		} catch (CoreException e) {
		}
		return model;
	}
	
}

package org.eclipse.pde.ui;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.search.dependencies.GatherUnusedDependenciesOperation;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * @since 3.17
 */
public class RemoveUnusedDependenciesHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			// retrieve the current plugin model
			IPluginModelBase model = getCurrentPluginModel();
			if (model == null) {
				return null;
			}


			GatherUnusedDependenciesOperation operation = new GatherUnusedDependenciesOperation(model);

			operation.run(new NullProgressMonitor());

			// gather unused dependencies
			List<Object> unusedDependencies = operation.getList();

			// remove unused dependencies if found
			if (!unusedDependencies.isEmpty()) {
				Object[] unusedArray = unusedDependencies.toArray();
	            GatherUnusedDependenciesOperation.removeDependencies(model, unusedArray);
			}

		} catch (InvocationTargetException e) {
			throw new ExecutionException("Error analyzing dependencies", e.getCause());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ExecutionException("Operation was interrupted", e);
		}

		return null;
	}


	private IPluginModelBase getCurrentPluginModel() {

		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null) {
			IWorkbenchPage page = window.getActivePage();
			if (page != null) {
				IEditorPart editor = page.getActiveEditor();
				if (editor instanceof PDEFormEditor) {
					PDEFormEditor pdeEditor = (PDEFormEditor) editor;
					return (IPluginModelBase) pdeEditor.getAggregateModel();
				}
			}
		}

		return null;
	}
}

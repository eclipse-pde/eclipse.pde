package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.util.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.dialogs.*;
	/**
	 * Implementation of a <code>ISelectionValidator</code> to validate the
	 * type of an element.
	 * Empty selections are not accepted.
	 */
public class JarSelectionValidator implements ISelectionStatusValidator {

		private Class[] fAcceptedTypes;
		private boolean fAllowMultipleSelection;

	
		/**
		 * @param acceptedTypes The types accepted by the validator
		 * @param allowMultipleSelection If set to <code>true</code>, the validator
		 * allows multiple selection.
		 */
		public JarSelectionValidator(Class[] acceptedTypes, boolean allowMultipleSelection) {
			Assert.isNotNull(acceptedTypes);
			fAcceptedTypes= acceptedTypes;
			fAllowMultipleSelection= allowMultipleSelection;
		}
	

		/*
		 * @see org.eclipse.ui.dialogs.ISelectionValidator#isValid(java.lang.Object)
		 */
		public IStatus validate(Object[] elements) {
			if (isValidSelection(elements)) {
				return new Status(
					IStatus.OK,
					PDEPlugin.getPluginId(),
					IStatus.OK,
					"", //$NON-NLS-1$
					null);
			}
			return new Status(
				IStatus.ERROR,
				PDEPlugin.getPluginId(),
				IStatus.ERROR,
				"", //$NON-NLS-1$
				null);
		}	

		private boolean isValidSelection(Object[] selection) {
			if (selection.length == 0) {
				return false;
			}
		
			if (!fAllowMultipleSelection && selection.length != 1) {
				return false;
			}
		
			for (int i= 0; i < selection.length; i++) {
				Object o= selection[i];	
				if (!isValid(o)) {
					return false;
				}
			}
			return true;
		}
		
		public boolean isValid(Object element) {
			for (int i= 0; i < fAcceptedTypes.length; i++) {
				if (fAcceptedTypes[i].isInstance(element)) {
					return true;
				}
			}
			return false;		
		}
	}

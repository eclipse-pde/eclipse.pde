package org.eclipse.pde.internal.ui.shared.target;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.target.impl.FeatureBundleContainer;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.SWTFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class EditFeatureContainerPage extends EditDirectoryContainerPage {

	public EditFeatureContainerPage(ITargetDefinition target, IBundleContainer container) {
		super(target, container);
		setTitle(Messages.EditFeatureContainerPage_0);
		setMessage(Messages.EditFeatureContainerPage_1);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.shared.target.AddDirectoryContainerPage#createLocationArea(org.eclipse.swt.widgets.Composite)
	 */
	protected void createLocationArea(Composite parent) {
		FeatureBundleContainer container = (FeatureBundleContainer) getBundleContainer();
		Composite comp = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_HORIZONTAL);

		SWTFactory.createLabel(comp, Messages.EditFeatureContainerPage_2, 1);
		Text text = SWTFactory.createText(comp, SWT.READ_ONLY | SWT.BORDER, 1);
		text.setText(container.getFeatureId());

		SWTFactory.createLabel(comp, Messages.EditFeatureContainerPage_3, 1);
		text = SWTFactory.createText(comp, SWT.READ_ONLY | SWT.BORDER, 1);
		text.setText(container.getFeatureVersion() != null ? container.getFeatureVersion() : Messages.EditFeatureContainerPage_4);

		SWTFactory.createLabel(comp, Messages.EditFeatureContainerPage_5, 1);
		text = SWTFactory.createText(comp, SWT.READ_ONLY | SWT.BORDER, 1);
		try {
			text.setText(container.getLocation(false));
		} catch (CoreException e) {
			setErrorMessage(e.getMessage());
			PDEPlugin.log(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.shared.target.EditDirectoryContainerPage#initializeInputFields(org.eclipse.pde.internal.core.target.provisional.IBundleContainer)
	 */
	protected void initializeInputFields(IBundleContainer container) {
		containerChanged(0);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.shared.target.EditDirectoryContainerPage#storeSettings()
	 */
	protected void storeSettings() {
		// Do nothing, no settings
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.shared.target.EditDirectoryContainerPage#validateInput()
	 */
	protected boolean validateInput() throws CoreException {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.shared.target.EditDirectoryContainerPage#refreshContainer(org.eclipse.pde.internal.core.target.provisional.IBundleContainer)
	 */
	protected IBundleContainer refreshContainer(IBundleContainer previous) throws CoreException {
		return getBundleContainer();
	}

}

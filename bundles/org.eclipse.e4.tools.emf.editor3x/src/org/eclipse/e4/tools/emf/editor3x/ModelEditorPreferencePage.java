/*******************************************************************************
 *
 * Contributors:
 *     Steven Spungin <steven@spungin.tv> - Bug 431735
 ******************************************************************************/

package org.eclipse.e4.tools.emf.editor3x;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

public class ModelEditorPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	private BooleanFieldEditor fAutoGeneratedId;
	private BooleanFieldEditor fShowXMIId;
	private ColorFieldEditor fColorValueNotRendered;
	private ColorFieldEditor fColorValueNotVisible;
	private ColorFieldEditor fColorValueNotVisibleAndRendered;
	private BooleanFieldEditor fShowListTab;
	private BooleanFieldEditor fShowSearch;

	public ModelEditorPreferencePage() {
	}

	public ModelEditorPreferencePage(String title) {
		super(title);
	}

	public ModelEditorPreferencePage(String title, ImageDescriptor image) {
		super(title, image);
	}

	public void init(IWorkbench workbench) {
		setPreferenceStore(new ScopedPreferenceStore(InstanceScope.INSTANCE, "org.eclipse.e4.tools.emf.ui"));
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite result = new Composite(parent, SWT.NONE);
		result.setLayout(new GridLayout());

		{
			Group group = new Group(result, SWT.NONE);
			group.setText("Color");
			group.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false, 2, 1));
			group.setLayout(new GridLayout(2, false));

			{
				fColorValueNotRendered = new ColorFieldEditor("notRenderedColor", "Not rendered color", group);
				fColorValueNotRendered.setPage(this);
				fColorValueNotRendered.setPreferenceStore(getPreferenceStore());
				fColorValueNotRendered.load();
			}

			{
				fColorValueNotVisible = new ColorFieldEditor("notVisibleColor", "Not visible color", group);
				fColorValueNotVisible.setPage(this);
				fColorValueNotVisible.setPreferenceStore(getPreferenceStore());
				fColorValueNotVisible.load();
			}

			{
				fColorValueNotVisibleAndRendered = new ColorFieldEditor("notVisibleAndRenderedColor", "Not visible and not rendered color", group);
				fColorValueNotVisibleAndRendered.setPage(this);
				fColorValueNotVisibleAndRendered.setPreferenceStore(getPreferenceStore());
				fColorValueNotVisibleAndRendered.load();
			}
		}

		{
			Group group = new Group(result, SWT.NONE);
			group.setText("Form Tab");
			group.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false, 2, 1));
			group.setLayout(new GridLayout(2, false));

			{
				Composite container = new Composite(group, SWT.NONE);
				container.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false, 2, 1));
				fAutoGeneratedId = new BooleanFieldEditor("autoCreateElementId", "Autogenerate Element-Id", container);
				fAutoGeneratedId.setPage(this);
				fAutoGeneratedId.setPreferenceStore(getPreferenceStore());
				fAutoGeneratedId.load();
			}

			{
				Composite container = new Composite(group, SWT.NONE);
				container.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false, 2, 1));
				fShowXMIId = new BooleanFieldEditor("showXMIId", "Show XMI:ID", container);
				fShowXMIId.setPage(this);
				fShowXMIId.setPreferenceStore(getPreferenceStore());
				fShowXMIId.load();
			}

			{
				Composite container = new Composite(group, SWT.NONE);
				container.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false, 2, 1));
				fShowSearch = new BooleanFieldEditor("tab-form-search-show", "Searchable tree on form tab\n" + "(forces read only XMI tab)\n" + "(requires reopening model)", container);
				fShowSearch.setPage(this);
				fShowSearch.setPreferenceStore(getPreferenceStore());
				fShowSearch.load();
			}

		}

		{
			Group group = new Group(result, SWT.NONE);
			group.setText("Tabs");
			group.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false, 2, 1));
			group.setLayout(new GridLayout(2, false));

			{
				Composite container = new Composite(group, SWT.NONE);
				container.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false, 2, 1));
				fShowListTab = new BooleanFieldEditor("tab-list-show", "Show List Tab", container);
				fShowListTab.setPage(this);
				fShowListTab.setPreferenceStore(getPreferenceStore());
				fShowListTab.load();
			}
		}

		return result;
	}

	@Override
	public boolean performOk() {
		fAutoGeneratedId.store();
		fShowXMIId.store();
		fColorValueNotRendered.store();
		fColorValueNotVisible.store();
		fColorValueNotVisibleAndRendered.store();
		fShowListTab.store();
		fShowSearch.store();
		return super.performOk();
	}

	@Override
	protected void performDefaults() {
		fAutoGeneratedId.loadDefault();
		fShowXMIId.loadDefault();
		fColorValueNotRendered.loadDefault();
		fColorValueNotVisible.loadDefault();
		fColorValueNotVisibleAndRendered.loadDefault();
		fShowListTab.loadDefault();
		fShowSearch.loadDefault();
		super.performDefaults();
	}

	@Override
	public void dispose() {
		super.dispose();
	}
}

package org.eclipse.e4.workbench.ui.renderers.swt;

import java.util.List;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.e4.ui.model.application.ApplicationPackage;
import org.eclipse.e4.ui.model.application.ItemPart;
import org.eclipse.e4.ui.model.application.Part;
import org.eclipse.e4.ui.model.application.Stack;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.databinding.EMFObservables;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Widget;

public class StackModelFactory extends PartFactory {

	public StackModelFactory() {
		super();
	}

	public Object createWidget(Part<?> part) {
		Widget newWidget = null;

		if (!(part instanceof Stack))
			return null;

		Widget parentWidget = getParentWidget(part);
		if (parentWidget instanceof Composite) {
			CTabFolder ctf = new CTabFolder((Composite) parentWidget,
					SWT.BORDER);
			ctf.setVisible(true);
			ctf.setSimple(false);
			ctf.setTabHeight(20);
			newWidget = ctf;
		}

		return newWidget;
	}

	public void postProcess(Part<?> part) {
		if (!(part instanceof Stack))
			return;

		CTabFolder ctf = (CTabFolder) part.getWidget();
		CTabItem[] items = ctf.getItems();
		Part<?> selPart = ((Stack) part).getActiveChild();

		// If there's none defined then pick the first
		if (selPart == null && part.getChildren().size() > 0) {
			((Stack) part).setActiveChild((ItemPart<?>) part.getChildren().get(
					0));
			// selPart = (Part) part.getChildren().get(0);
		} else {
			for (int i = 0; i < items.length; i++) {
				Part<?> me = (Part<?>) items[i].getData(OWNING_ME);
				if (selPart == me)
					ctf.setSelection(items[i]);
			}
		}
	}

	@Override
	public void childAdded(Part<?> parentElement, Part<?> element) {
		super.childAdded(parentElement, element);

		if (element instanceof ItemPart<?>) {
			ItemPart<?> itemPart = (ItemPart<?>) element;
			CTabFolder ctf = (CTabFolder) parentElement.getWidget();
			int createFlags = 0;

			// if(element instanceof View && ((View)element).isCloseable())
			createFlags = createFlags | SWT.CLOSE;

			CTabItem cti = findItemForPart(parentElement, element);
			if (cti == null)
				cti = new CTabItem(ctf, createFlags);

			cti.setData(OWNING_ME, element);
			cti.setText(itemPart.getName());
			cti.setImage(getImage(element));

			// Lazy Loading: On the first pass through this method the
			// part's control will be null (we're just creating the tabs
			Control ctrl = (Control) element.getWidget();
			if (ctrl != null) {
				cti.setControl(ctrl);

				// Hook up special logic to synch up the Tab Items
				hookChildControllerLogic(parentElement, element, cti);
			}
		}
	}

	@Override
	public <P extends Part<?>> void processContents(Part<P> me) {
		Widget parentWidget = getParentWidget(me);
		if (parentWidget == null)
			return;

		// Lazy Loading: here we only create the CTabItems, not the parts
		// themselves; they get rendered when the tab gets selected
		List<P> parts = me.getChildren();
		if (parts != null) {
			for (Part<?> childME : parts) {
				if (childME.isVisible())
					childAdded(me, childME);
			}
		}
	}

	private CTabItem findItemForPart(Part<?> folder, Part<?> part) {
		CTabFolder ctf = (CTabFolder) folder.getWidget();
		if (ctf == null)
			return null;

		CTabItem[] items = ctf.getItems();
		for (int i = 0; i < items.length; i++) {
			if (items[i].getData(OWNING_ME) == part)
				return items[i];
		}
		return null;
	}

	private void hookChildControllerLogic(final Part<?> parentElement,
			final Part<?> childElement, CTabItem cti) {
		// Handle label changes
		IObservableValue textObs = EMFObservables.observeValue(
				(EObject) childElement,
				ApplicationPackage.Literals.ITEM__NAME);
		ISWTObservableValue uiObs = SWTObservables.observeText(cti);
		dbc.bindValue(uiObs, textObs, null, null);

		IObservableValue emfTTipObs = EMFObservables.observeValue(
				(EObject) childElement,
				ApplicationPackage.Literals.ITEM__TOOLTIP);
		ISWTObservableValue uiTTipObs = SWTObservables.observeTooltipText(cti);
		dbc.bindValue(uiTTipObs, emfTTipObs, null, null);

		// Handle tab item image changes
		((EObject) childElement).eAdapters().add(new AdapterImpl() {
			@Override
			public void notifyChanged(Notification msg) {
				Part<?> sm = (Part<?>) msg.getNotifier();
				if (ApplicationPackage.Literals.ITEM__ICON_URI.equals(msg
						.getFeature())) {
					CTabItem item = findItemForPart(parentElement, sm);
					if (item != null) {
						Image image = getImage(sm);
						if (image != null)
							item.setImage(image);
					}
				}
			}
		});
	}

	@Override
	public void childRemoved(Part<?> parentElement, Part<?> child) {
		super.childRemoved(parentElement, child);

		CTabFolder ctf = (CTabFolder) parentElement.getWidget();
		CTabItem oldItem = findItemForPart(parentElement, child);
		if (oldItem != null) {
			oldItem.setControl(null); // prevent the widget from being disposed
			oldItem.dispose();
		}
	}

	@Override
	public void hookControllerLogic(final Part<?> me) {
		super.hookControllerLogic(me);

		final Stack sm = (Stack) me;
		// synch up the selection state

		// Match the selected TabItem to its Part
		CTabFolder ctf = (CTabFolder) me.getWidget();
		ctf.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
			}

			public void widgetSelected(SelectionEvent e) {
				ItemPart<?> newPart = (ItemPart<?>) e.item.getData(OWNING_ME);
				if (sm.getActiveChild() != newPart)
					sm.setActiveChild(newPart);
			}
		});

		((EObject) me).eAdapters().add(new AdapterImpl() {
			@Override
			public void notifyChanged(Notification msg) {
				if (ApplicationPackage.Literals.PART__ACTIVE_CHILD.equals(msg
						.getFeature())) {
					Stack sm = (Stack) msg.getNotifier();
					Part<?> selPart = sm.getActiveChild();
					CTabFolder ctf = (CTabFolder) ((Stack) msg.getNotifier())
							.getWidget();
					CTabItem item = findItemForPart(sm, selPart);
					if (item != null) {
						// Lazy Loading: we create the control here if necessary
						// Note that this will result in a second call to
						// 'childAdded' but
						// that logic expects this
						Control ctrl = item.getControl();
						if (ctrl == null) {
							renderer.createGui(selPart);
						}

						ctf.setSelection(item);
					}
				}
			}
		});
	}
}

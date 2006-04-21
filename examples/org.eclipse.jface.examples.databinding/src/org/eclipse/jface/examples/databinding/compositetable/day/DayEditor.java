/*******************************************************************************
 * Copyright (c) 2006 The Pampered Chef and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Pampered Chef - initial API and implementation
 ******************************************************************************/
package org.eclipse.jface.examples.databinding.compositetable.day;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.examples.databinding.compositetable.CompositeTable;
import org.eclipse.jface.examples.databinding.compositetable.IRowContentProvider;
import org.eclipse.jface.examples.databinding.compositetable.RowConstructionListener;
import org.eclipse.jface.examples.databinding.compositetable.ScrollEvent;
import org.eclipse.jface.examples.databinding.compositetable.ScrollListener;
import org.eclipse.jface.examples.databinding.compositetable.day.internal.CalendarableEventControl;
import org.eclipse.jface.examples.databinding.compositetable.day.internal.DayModel;
import org.eclipse.jface.examples.databinding.compositetable.day.internal.TimeSlice;
import org.eclipse.jface.examples.databinding.compositetable.day.internal.TimeSlot;
import org.eclipse.jface.examples.databinding.compositetable.timeeditor.Calendarable;
import org.eclipse.jface.examples.databinding.compositetable.timeeditor.CalendarableModel;
import org.eclipse.jface.examples.databinding.compositetable.timeeditor.EventContentProvider;
import org.eclipse.jface.examples.databinding.compositetable.timeeditor.EventCountProvider;
import org.eclipse.jface.examples.databinding.compositetable.timeeditor.IEventEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Layout;

/**
 * A DayEditor is an SWT control that can display events on a time line that can
 * span one or more days.  This class is not intended to be subclassed.
 * 
 * @since 3.2
 */
public class DayEditor extends Composite implements IEventEditor {
	private CompositeTable compositeTable = null;
	private CalendarableModel model = new CalendarableModel();
	private List spareCalendarableEventControls = new LinkedList();
	protected TimeSlice daysHeader;

	/**
	 * Constructor DayEditor.  Constructs a calendar control that can display
	 * events on one or more days.
	 * 
	 * @param parent
	 * @param style
	 */
	public DayEditor(Composite parent, int style) {
		super(parent, style);
		setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.examples.databinding.compositetable.timeeditor.IEventEditor#setTimeBreakdown(int, int)
	 */
	public void setTimeBreakdown(int numberOfDays, int numberOfDivisionsInHour) {
		model.setTimeBreakdown(numberOfDays, numberOfDivisionsInHour);
		
		if (compositeTable != null) {
			compositeTable.dispose();
		}
		
		createCompositeTable(numberOfDays, numberOfDivisionsInHour);
	}

	/**
	 * This method initializes compositeTable
	 * 
	 * @param numberOfDays
	 *            The number of day columns to display
	 */
	private void createCompositeTable(final int numberOfDays,
			final int numberOfDivisionsInHour) {
		compositeTable = new CompositeTable(this, SWT.NONE);
		new TimeSlice(compositeTable, SWT.BORDER);		// The prototype header
		new TimeSlice(compositeTable, SWT.NONE); // The prototype row
		
		compositeTable.setNumRowsInCollection(computeNumRowsInCollection(numberOfDivisionsInHour));
		
		compositeTable.addRowConstructionListener(new RowConstructionListener() {
			public void headerConstructed(Control newHeader) {
				daysHeader = (TimeSlice) newHeader;
				daysHeader.setHeaderControl(true);
				daysHeader.setNumberOfColumns(numberOfDays);
				if (model.getStartDate() == null) {
					return;
				}
				refreshColumnHeaders(daysHeader.getColumns());
			}
			
			public void rowConstructed(Control newRow) {
				TimeSlice timeSlice = (TimeSlice) newRow;
				timeSlice.setNumberOfColumns(numberOfDays);
				timeSlice.addCellFocusListener(cellFocusListener);
				timeSlice.addKeyListener(keyListener);
			}
		});
		compositeTable.addRowContentProvider(new IRowContentProvider() {
			public void refresh(CompositeTable sender, int currentObjectOffset,
					Control row) {
				TimeSlice timeSlice = (TimeSlice) row;
				refreshRow(currentObjectOffset, timeSlice);
			}
		});
		compositeTable.addScrollListener(new ScrollListener() {
			public void tableScrolled(ScrollEvent scrollEvent) {
				layoutEventControls();
			}
		});
		addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				Rectangle bounds = DayEditor.this.getBounds();
				compositeTable.setBounds(0, 0, bounds.width, bounds.height);
				layoutEventControlsDeferred();
			}
		});
		
		compositeTable.setRunTime(true);
	}
	
	private KeyListener keyListener = new KeyAdapter() {
		public void keyPressed(KeyEvent e) {
			Calendarable selection = selectedCalendarable;
			int selectedRow;
			int selectedDay;
			
			if (selection == null) {
				selectedRow = convertViewportRowToDayRow(compositeTable.getCurrentRow());
				selectedDay = compositeTable.getCurrentColumn();
			} else {
				selectedDay = model.getDay(selection);
				selectedRow = computeRowForCalendarable(selection, selectedDay);
			}
			
			switch (e.character) {
			case SWT.TAB:
				if ((e.stateMask & SWT.SHIFT) != 0) {
					selectNextCalendarable(selectedDay, selectedRow, selection);
				} else {
					selectPreviousCalendarable(selectedDay, selectedRow, selection);
				}
			}
		}
	};
	
	private void selectNextCalendarable(int selectedDay, int selectedRow, Calendarable selection) {
		Object[] newSelection = model.findNextCalendarable(selectedDay, selectedRow, selection);
		setCompositeSelection(newSelection);
	}

	private void selectPreviousCalendarable(int selectedDay, int selectedRow, Calendarable selection) {
		Object[] newSelection = model.findPreviousCalendarable(selectedDay, selectedRow, selection);
		setCompositeSelection(newSelection);
	}

	private void setCompositeSelection(Object[] newSelection) {
		int day = ((Integer)newSelection[0]).intValue();
		int row = ((Integer)newSelection[1]).intValue();
		Calendarable aboutToSelect = (Calendarable) newSelection[2];
		setSelectionByDayAndRow(day, row, aboutToSelect);
	}
	
	private boolean selectCalendarableControlOnSetFocus = true;
	
	private FocusListener cellFocusListener = new FocusAdapter() {
		public void focusGained(FocusEvent e) {
			TimeSlice sendingRow = (TimeSlice) ((Composite) e.widget).getParent();
			int day = sendingRow.getControlColumn(e.widget);
			int row = compositeTable.getControlRow(sendingRow);
			if (selectCalendarableControlOnSetFocus) {
				setSelectionByDayAndRow(day, row, null);
			} else {
				selectCalendarableControlOnSetFocus = true;
			}
		}
	};
	
	private void setSelectionByDayAndRow(int day, int row, Calendarable aboutToSelect) {
		int dayRow = convertViewportRowToDayRow(row);
		if (aboutToSelect == null)
			aboutToSelect = getFirstCalendarableAt(day, dayRow);
		if (aboutToSelect == null) {
			aboutToSelect = getAllDayCalendarableAt(day, row + compositeTable.getTopRow());
		}
		setSelection(aboutToSelect);
		aboutToSelect = null;
	}

	/** (non-API)
	 * Method getFirstCalendarableAt. Finds the calendarable event at the 
	 * specified day/row in DayRow coordinates.  If no calendarable exists
	 * at the specified coordinates, does nothing.
	 * 
	 * @param day The day offset
	 * @param row The row offset in DayRow coordinates
	 * @return the first Calendarable in the specified (day, row) or null if none.
	 */
	protected Calendarable getFirstCalendarableAt(int day, int row) {
		Calendarable[][] eventLayout = model.getEventLayout(day);
		Calendarable selectedCalendarable = null;
		for (int column=0; column < eventLayout.length; ++column) {
			Calendarable calendarable = eventLayout[column][row];
			if (calendarable != null) {
				if (selectedCalendarable == null) {
					selectedCalendarable = calendarable;
				} else if (calendarable.getStartTime().after(selectedCalendarable.getStartTime())) {
					selectedCalendarable = calendarable;
				}
			}
		}
		return selectedCalendarable;
	}
	
	/**
	 * Find the all day event that is positioned at the specified day and row in viewport coordinates
	 * 
	 * @param day
	 * @param row
	 * @return The found Calendarable or null if none
	 */
	protected Calendarable getAllDayCalendarableAt(int day, int row) {
		Calendarable[] allDayEvents = model.getAllDayCalendarables(day);
		for (int allDayEventRow = 0; allDayEventRow < allDayEvents.length; allDayEventRow++) {
			Calendarable candidate = allDayEvents[allDayEventRow];
			if (allDayEventRow == row) {
				return candidate;
			}
		}
//		int allDayEventRow = 0;
//		for (Iterator calendarablesIter = model.getCalendarableEvents(day).iterator(); calendarablesIter.hasNext();) {
//			Calendarable candidate = (Calendarable) calendarablesIter.next();
//			if (candidate.isAllDayEvent()) {
//				if (allDayEventRow == row) {
//					return candidate;
//				}
//				++allDayEventRow;
//			}
//		}
		return null;
	}

	private Calendarable selectedCalendarable = null;

	/**
	 * Method selectCalendarable.  Selects the specified Calendarable event.
	 * 
	 * @param newSelection The Calendarable to select.
	 */
	public void setSelection(Calendarable newSelection) {
		if (selectedCalendarable == newSelection) {
			return;
		}
		if (selectedCalendarable != null) {
			// The control could be null if it just got scrolled off the screen top or bottom
			if (selectedCalendarable.getControl() != null) {
				selectedCalendarable.getControl().setSelected(false);
			}
		}
		
		scrollCalendarableIntoView(newSelection);
		selectedCalendarable = newSelection;
		
		if (newSelection != null) {
			newSelection.getControl().setSelected(true);
		}
		fireSelectionChangeEvent(selectedCalendarable, newSelection);
	}
	
	/**
	 * @param newSelection
	 */
	private void scrollCalendarableIntoView(Calendarable newSelection) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Method getSelection.  Returns the selected Calendarable event or null
	 * if no Calendarable is selected.
	 * 
	 * @return the selected Calendarable or null if nothing is selected.
	 */
	public Calendarable getSelection() {
		return selectedCalendarable;
	}
	
	private List selectionChangeListeners = new LinkedList();

	private void fireSelectionChangeEvent(Calendarable currentSelection, Calendarable newSelection) {
		SelectionChangeEvent sce = new SelectionChangeEvent(currentSelection, newSelection);
		for (Iterator listenersIter = selectionChangeListeners.iterator(); listenersIter.hasNext();) {
			CalendarableSelectionChangeListener listener = (CalendarableSelectionChangeListener) listenersIter.next();
			listener.selectionChanged(sce);
		}
	}
	
	/**
	 * Adds the listener to the collection of listeners who will
	 * be notified when the receiver's selection changes, by sending
	 * it one of the messages defined in the <code>CalendarableSelectionChangeListener</code>
	 * interface.
	 * <p>
	 * <code>selectionChanged</code> is called when the selection changes.
	 * </p>
	 *
	 * @param listener the listener which should be notified
	 *
	 * @exception IllegalArgumentException <ul>
	 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
	 * </ul>
	 * @exception SWTException <ul>
	 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 * </ul>
	 *
	 * @see CalendarableSelectionChangeListener
	 * @see #removeSelectionChangeListener
	 * @see SelectionChangeEvent
	 */
	public void addSelectionChangeListener(CalendarableSelectionChangeListener l) {
		if (l == null) {
			throw new IllegalArgumentException("The argument cannot be null");
		}
		if (isDisposed()) {
			throw new SWTException("Widget is disposed");
		}
		selectionChangeListeners.add(l);
	}
	
	/**
	 * Removes the listener from the collection of listeners who will
	 * be notified when the receiver's selection changes, by sending
	 * it one of the messages defined in the <code>CalendarableSelectionChangeListener</code>
	 * interface.
	 * <p>
	 * <code>selectionChanged</code> is called when the selection changes.
	 * </p>
	 *
	 * @param listener the listener which should no longer be notified
	 *
	 * @exception IllegalArgumentException <ul>
	 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
	 * </ul>
	 * @exception SWTException <ul>
	 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 * </ul>
	 *
	 * @see CalendarableSelectionChangeListener
	 * @see #addSelectionChangeListener
	 * @see SelectionChangeEvent
	 */
	public void removeSelectionChangeListener(CalendarableSelectionChangeListener l) {
		if (l == null) {
			throw new IllegalArgumentException("The argument cannot be null");
		}
		if (isDisposed()) {
			throw new SWTException("Widget is disposed");
		}
		selectionChangeListeners.remove(l);
	}


	private class DayEditorLayout extends Layout {
		/* (non-Javadoc)
		 * @see org.eclipse.swt.widgets.Layout#computeSize(org.eclipse.swt.widgets.Composite, int, int, boolean)
		 */
		protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
			return new Point(wHint, hHint);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.swt.widgets.Layout#layout(org.eclipse.swt.widgets.Composite, boolean)
		 */
		protected void layout(Composite composite, boolean flushCache) {
			Rectangle bounds = DayEditor.this.getBounds();
			compositeTable.setBounds(0, 0, bounds.width, bounds.height);
			layoutEventControls();
		}
	}

	/**
	 * @return Returns the defaultStartHour.
	 */
	public int getDefaultStartHour() {
		return model.getDefaultStartHour();
	}

	/**
	 * @param defaultStartHour The defaultStartHour to set.
	 */
	public void setDefaultStartHour(int defaultStartHour) {
		model.setDefaultStartHour(defaultStartHour);
		updateVisibleRows();
		layoutEventControls();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.examples.databinding.compositetable.timeeditor.IEventEditor#setDayEventCountProvider(org.eclipse.jface.examples.databinding.compositetable.timeeditor.EventCountProvider)
	 */
	public void setDayEventCountProvider(EventCountProvider eventCountProvider) {
		model.setDayEventCountProvider(eventCountProvider);
		updateVisibleRows();
		layoutEventControls();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.examples.databinding.compositetable.timeeditor.IEventEditor#setEventContentProvider(org.eclipse.jface.examples.databinding.compositetable.timeeditor.EventContentProvider)
	 */
	public void setEventContentProvider(EventContentProvider eventContentProvider) {
		model.setEventContentProvider(eventContentProvider);
		updateVisibleRows();
		layoutEventControls();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.examples.databinding.compositetable.timeeditor.IEventEditor#setStartDate(java.util.Date)
	 */
	public void setStartDate(Date startDate) {
		List removedDays = model.setStartDate(startDate);
		findEventRowsForNewDays(startDate);
		refreshColumnHeaders(daysHeader.getColumns());
		updateVisibleRows();
		freeObsoleteCalendarableEventControls(removedDays);
		layoutEventControls();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.examples.databinding.compositetable.timeeditor.IEventEditor#getStartDate()
	 */
	public Date getStartDate() {
		return model.getStartDate();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.examples.databinding.compositetable.timeeditor.IEventEditor#refresh(java.util.Date)
	 */
	public void refresh(Date date) {
		List removedDays = model.refresh(date);
		freeObsoleteCalendarableEventControls(removedDays);
		updateVisibleRows();
		layoutEventControls();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.examples.databinding.compositetable.timeeditor.IEventEditor#getNumberOfDays()
	 */
	public int getNumberOfDays() {
		return model.getNumberOfDays();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.examples.databinding.compositetable.timeeditor.IEventEditor#getNumberOfDivisionsInHour()
	 */
	public int getNumberOfDivisionsInHour() {
		return model.getNumberOfDivisionsInHour();
	}
	
	// Display Refresh logic here ----------------------------------------------
	
	/*
	 * There are four main coordinate systems the refresh algorithm has to
	 * deal with:
	 * 
	 * 1) Rows starting from midnight (the way the DayModel computes the layout).  
	 *    These are called Day Row coordinates.
	 *    
	 * 2) Rows starting from the top visible row, taking into account all-day 
	 *    event rows.  These are called Viewport Row coordinates
	 *    
	 * 3) Pixel coordinates for each TimeSlot, relative to its parent TimeSlice
	 *    (the CompositeTable row object) row.  This is relevant because these 
	 *    are transformed into #4 in order to place CalendarableEventControls.
	 *    
	 * 4) Pixel coordinates relative to the top left (the origin) of the entire
	 *    DayEditor control.
	 */
	
	private int numberOfAllDayEventRows = 0;
	Calendar calendar = new GregorianCalendar();

	private int computeNumRowsInCollection(final int numberOfDivisionsInHour) {
		numberOfAllDayEventRows = model.computeNumberOfAllDayEventRows();
		return (DISPLAYED_HOURS-model.computeStartHour()) * numberOfDivisionsInHour+numberOfAllDayEventRows;
	}
	
	private int convertViewportRowToDayRow(int row) {
		int topRowOffset = compositeTable.getTopRow() - numberOfAllDayEventRows;
		int startOfDayOffset = model.computeStartHour() * model.getNumberOfDivisionsInHour();
		return row + topRowOffset + startOfDayOffset;
	}

	private int convertDayRowToViewportCoordinates(int row) {
		row -= model.computeStartHour() * model.getNumberOfDivisionsInHour()
			- numberOfAllDayEventRows;
		return row;
	}
	

	/**
	 * @param calendarable
	 * @param day
	 * @return The row in DayRow coordinates
	 */
	private int computeRowForCalendarable(Calendarable calendarable, int day) {
		int row = 0;
		if (calendarable.isAllDayEvent()) {
			Calendarable[] allDayEvents = model.getAllDayCalendarables(day);
			for (int allDayEventRow = 0; allDayEventRow < allDayEvents.length; allDayEventRow++) {
				if (allDayEvents[allDayEventRow] == calendarable) {
					row = allDayEventRow - compositeTable.getTopRow();
					break;
				}
			}
//			for (Iterator calendarablesIter = model.getCalendarableEvents(day).iterator(); calendarablesIter.hasNext();) {
//				Calendarable candidate = (Calendarable) calendarablesIter.next();
//				if (candidate.isAllDayEvent()) {
//					if (candidate == calendarable) {
//						row = allDayEventRow - compositeTable.getTopRow();
//						break;
//					}
//					++allDayEventRow;
//				}
//			}
		} else {
			// Convert to viewport coordinates
			Point upperLeft = calendarable.getUpperLeftPositionInDayRowCoordinates();
			int topRowOffset = compositeTable.getTopRow() - numberOfAllDayEventRows;
			int startOfDayOffset = model.computeStartHour() * model.getNumberOfDivisionsInHour();
			row = upperLeft.y - topRowOffset - startOfDayOffset;
			if (row < 0) {
				row = 0;
			}
		}
		return row;
	}
	
	
	/*
	 * Update the number of rows that are displayed inside the CompositeTable control
	 */
	private void updateVisibleRows() {
		compositeTable.setNumRowsInCollection(computeNumRowsInCollection(getNumberOfDivisionsInHour()));
	}
	
	private void refreshRow(int currentObjectOffset, TimeSlice timeSlice) {
		// Decrement currentObjectOffset for each all-day event line we need.
		for (int allDayEventRow = 0; allDayEventRow < numberOfAllDayEventRows; ++allDayEventRow) {
			--currentObjectOffset;
		}
		
		if (currentObjectOffset < 0) {
			timeSlice.setCurrentTime(null);
		} else {
			calendar.set(Calendar.HOUR_OF_DAY, 
					model.computeHourFromRow(currentObjectOffset));
			calendar.set(Calendar.MINUTE,
					model.computeMinuteFromRow(currentObjectOffset));
			timeSlice.setCurrentTime(calendar.getTime());
		}
	}

	/**
	 * (non-API) Method initializeColumnHeaders.  Called internally when the
	 * column header text needs to be updated.
	 * 
	 * @param columns A LinkedList of CLabels representing the column objects
	 */
	protected void refreshColumnHeaders(LinkedList columns) {
		Date startDate = getStartDate();
		GregorianCalendar gc = new GregorianCalendar();
		gc.setTime(startDate);

		SimpleDateFormat formatter = new SimpleDateFormat("EE, MMM d");
		formatter.applyPattern(formatter.toLocalizedPattern());
		
		for (Iterator iter = columns.iterator(); iter.hasNext();) {
			CLabel headerLabel = (CLabel) iter.next();
			headerLabel.setText(formatter.format(gc.getTime()));
			gc.add(Calendar.DAY_OF_MONTH, 1);
		}
	}
	
	private void freeObsoleteCalendarableEventControls(List removedCalendarables) {
		for (Iterator removedCalendarablesIter = removedCalendarables.iterator(); removedCalendarablesIter.hasNext();) {
			Calendarable toRemove = (Calendarable) removedCalendarablesIter.next();
			if (selectedCalendarable == toRemove) {
				setSelection(null);
			}
			freeCalendarableControl(toRemove);
		}
	}
	
	private void findEventRowsForNewDays(Date startDate) {
		DayModel dayModel = new DayModel(model.getNumberOfDivisionsInHour());
		for (int day=0; day < model.getNumberOfDays(); ++day) {
			if (model.getNumberOfColumnsWithinDay(day) == -1) {
				List events = model.getCalendarableEvents(day);
				Calendarable[][] eventLayout = dayModel.computeEventLayout(events);
				model.setEventLayout(day, eventLayout);
			}
		}
	}

	private void layoutEventControlsDeferred() {
		if (getStartDate() == null) {
			return;
		}
		refreshEventControlPositions.run();
		Display.getCurrent().asyncExec(refreshEventControlPositions);
	}
	
	private void layoutEventControls() {
		if (getStartDate() == null) {
			return;
		}
		refreshEventControlPositions.run();
	}
	
	private Runnable refreshEventControlPositions = new Runnable() {
		public void run() {
			Control[] gridRows = compositeTable.getRowControls();
			
			for (int day=0; day < model.getNumberOfDays(); ++day) {
				int columnsWithinDay = model.getNumberOfColumnsWithinDay(day);
				Point[] columnPositions = computeColumns(day, columnsWithinDay, gridRows);
				
				int allDayEventRow = 0;
				
				for (Iterator calendarablesIter = model.getCalendarableEvents(day).iterator(); calendarablesIter.hasNext();) {
					Calendarable calendarable = (Calendarable) calendarablesIter.next();
					if (calendarable.isAllDayEvent()) {
						layoutAllDayEvent(day, allDayEventRow, calendarable, gridRows);
						++allDayEventRow;
					} else {
						layoutTimedEvent(day, columnPositions, calendarable, gridRows);
					}
				}
			}
		}
	};
	
	protected Point[] computeColumns(int day, int numberOfColumns, Control[] gridRows) {
		Point[] columns = new Point[numberOfColumns];
		Rectangle timeSliceBounds = getTimeSliceBounds(day, compositeTable.getTopRow(), gridRows);
		timeSliceBounds.x += TimeSlot.TIME_BAR_WIDTH + 1;
		timeSliceBounds.width -= TimeSlot.TIME_BAR_WIDTH + 2;
		
		int baseWidth = timeSliceBounds.width / numberOfColumns;
		int extraWidth = timeSliceBounds.width % numberOfColumns;
		
		int startingPosition = timeSliceBounds.x;
		for (int column = 0; column < columns.length; column++) {
			int columnStart = startingPosition;
			int columnWidth = baseWidth;
			if (extraWidth > 0) {
				++columnWidth;
				--extraWidth;
			}
			columns[column] = new Point(columnStart, columnWidth);
			startingPosition += columnWidth;
		}
		return columns;
	}

	private void fillControlData(Calendarable calendarable, int clippingStyle) {
		calendarable.getControl().setText(calendarable.getText());
		calendarable.getControl().setClipping(clippingStyle);
	}

	private void layoutAllDayEvent(int day, int allDayEventRow, Calendarable calendarable, Control[] gridRows) {
		if (eventRowIsVisible(allDayEventRow)) {
			createCalendarableControl(calendarable);
			fillControlData(calendarable, SWT.NULL);
			
			Rectangle timeSliceBounds = getTimeSliceBounds(day, allDayEventRow, gridRows);
			int gutterWidth = TimeSlot.TIME_BAR_WIDTH + 1;
			timeSliceBounds.x += gutterWidth;
			timeSliceBounds.width -= gutterWidth;
			calendarable.getControl().setBounds(timeSliceBounds);
			calendarable.getControl().moveAbove(compositeTable);
		} else {
			freeCalendarableControl(calendarable);
		}
	}

	private void layoutTimedEvent(int day, Point[] columnPositions, Calendarable calendarable, Control[] gridRows) {
		int firstVisibleRow = model.computeStartHour() * model.getNumberOfDivisionsInHour();
		
		int scrolledRows = compositeTable.getTopRow() - numberOfAllDayEventRows;
		int visibleAllDayEventRows = 0;
		if (scrolledRows < 0) {
			visibleAllDayEventRows = -1 * scrolledRows;
			scrolledRows = 0;
		}
		firstVisibleRow += scrolledRows;
		int lastVisibleRow = firstVisibleRow + compositeTable.getNumRowsVisible() - visibleAllDayEventRows - 1;
		
		int startRow = calendarable.getUpperLeftPositionInDayRowCoordinates().y;
		int endRow = calendarable.getLowerRightPositionInDayRowCoordinates().y;
		
		if (timedEventIsVisible(calendarable, firstVisibleRow, lastVisibleRow, startRow, endRow)) {
			int clippingStyle = SWT.NULL;
			
			if (startRow < firstVisibleRow) {
				startRow = firstVisibleRow;
				clippingStyle |= SWT.TOP;
			}
			
			if (endRow > lastVisibleRow) {
				endRow = lastVisibleRow;
				clippingStyle |= SWT.BOTTOM;
			}
			
			startRow = convertDayRowToViewportCoordinates(startRow);
			endRow = convertDayRowToViewportCoordinates(endRow);
			
			createCalendarableControl(calendarable);
			fillControlData(calendarable, clippingStyle);
			
			Rectangle startRowBounds = getTimeSliceBounds(day, startRow, gridRows);
			Rectangle endRowBounds = getTimeSliceBounds(day, endRow, gridRows);
			
			int leftmostColumn = calendarable.getUpperLeftPositionInDayRowCoordinates().x;
			int rightmostColumn = calendarable.getLowerRightPositionInDayRowCoordinates().x;
			
			int left = columnPositions[leftmostColumn].x;
			int top = startRowBounds.y + 1;
			int width = columnPositions[rightmostColumn].x - columnPositions[leftmostColumn].x + columnPositions[rightmostColumn].y;
			int height = endRowBounds.y - startRowBounds.y + endRowBounds.height - 1;
			
			Rectangle finalPosition = new Rectangle(left, top, width, height);
			
			calendarable.getControl().setBounds(finalPosition);
			calendarable.getControl().moveAbove(compositeTable);
		} else {
			freeCalendarableControl(calendarable);
		}
	}

	private boolean eventRowIsVisible(int eventRow) {
		if (compositeTable.getTopRow() <= eventRow) {
			return true;
		}
		return false;
	}
	
	private boolean timedEventIsVisible(Calendarable calendarable, int firstVisibleRow, int lastVisibleRow, int startRow, int endRow) {
		if (startRow < firstVisibleRow && endRow < firstVisibleRow)
			return false;
		
		if (startRow > lastVisibleRow && endRow > lastVisibleRow)
			return false;
		
		return true;
	}

	private void createCalendarableControl(Calendarable calendarable) {
		if (calendarable.getControl() == null) {
			calendarable.setControl(newCEC());
			if (calendarable == selectedCalendarable) {
				calendarable.getControl().setSelected(true);
			}
		}
	}
	
	private Rectangle getTimeSliceBounds(int day, int eventRow, Control[] gridRows) {
		TimeSlice rowObject = (TimeSlice) gridRows[eventRow - compositeTable.getTopRow()];
		Control slot = rowObject.getColumnControl(day);
		return getBoundsInDayEditorCoordinates(slot);
	}
	
	private void freeCalendarableControl(Calendarable calendarable) {
		if (calendarable.getControl() != null) {
			freeCEC(calendarable.getControl());
			calendarable.setControl(null);
		}
	}
	
	private Rectangle getBoundsInDayEditorCoordinates(Control slot) {
		return Display.getCurrent().map(slot.getParent(), this, slot.getBounds());
	}

	// CalendarableEventControl construction/destruction here -----------------

	MouseAdapter selectCompositeTableOnMouseDownAdapter = new MouseAdapter() {
		/* (non-Javadoc)
		 * @see org.eclipse.swt.events.MouseAdapter#mouseDown(org.eclipse.swt.events.MouseEvent)
		 */
		public void mouseDown(MouseEvent e) {
			CalendarableEventControl control = (CalendarableEventControl) e.widget;
			Calendarable aboutToSelect = control.getCalendarable();
			int day = model.getDay(aboutToSelect);
			int row = computeRowForCalendarable(aboutToSelect, day);
			selectCalendarableControlOnSetFocus = false;
			compositeTable.setSelection(day, row);
			setSelectionByDayAndRow(day, row, aboutToSelect);
		}

	};

	private CalendarableEventControl newCEC() {
		if (spareCalendarableEventControls.size() > 0) {
			CalendarableEventControl result = (CalendarableEventControl) spareCalendarableEventControls.remove(0);
			result.setVisible(true);
			return result;
		}
		CalendarableEventControl calendarableEventControl = new CalendarableEventControl(this, SWT.NULL);
		calendarableEventControl.addMouseListener(selectCompositeTableOnMouseDownAdapter);
		return calendarableEventControl;
	}
	
	private void freeCEC(CalendarableEventControl control) {
		control.setSelected(false);
		control.setCalendarable(null);
		control.setVisible(false);
		spareCalendarableEventControls.add(control);
	}

} // @jve:decl-index=0:visual-constraint="10,10"



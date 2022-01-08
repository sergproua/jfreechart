/* ===========================================================
 * JFreeChart : a free chart library for the Java(tm) platform
 * ===========================================================
 *
 * (C) Copyright 2000-2022, by David Gilbert and Contributors.
 *
 * Project Info:  http://www.jfree.org/jfreechart/index.html
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * [Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.]
 *
 * ---------------
 * OHLCSeries.java
 * ---------------
 * (C) Copyright 2006-2022, by David Gilbert.
 *
 * Original Author:  David Gilbert;
 * Contributor(s):   -;
 *
 */

package org.jfree.data.time.ohlc;

import org.jfree.chart.internal.Args;
import org.jfree.chart.internal.CloneUtils;
import org.jfree.data.general.Series;
import org.jfree.data.general.SeriesChangeEvent;
import org.jfree.data.general.SeriesException;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.Year;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * A list of ({@link RegularTimePeriod}, open, high, low, close) data items.
 *
 * @see OHLCSeriesCollection
 * @since 1.0.4
 */
public class OHLCSeries<S extends Comparable<S>> extends Series<S>
        implements Cloneable, Serializable {

    /**
     * The type of period for the data.
     */
    protected Class timePeriodClass;

    /**
     * The list of data items in the series.
     */
    protected List<OHLCItem> data;

    /**
     * Creates a new empty series.  By default, items added to the series will
     * be sorted into ascending order by period, and duplicate periods will
     * not be allowed.
     *
     * @param name the series key ({@code null} not permitted).
     */
    public OHLCSeries(S name) {
        super(name);
        this.timePeriodClass = null;
        this.data = new ArrayList<>();
        this.maximumItemCount = Integer.MAX_VALUE;
        this.maximumItemAge = Long.MAX_VALUE;
    }

    /**
     * The maximum number of items for the series.
     */
    private int maximumItemCount;

    /**
     * The maximum age of items for the series, specified as a number of
     * time periods.
     */
    private long maximumItemAge;

    /**
     * Returns the number of items in the series.
     *
     * @return The item count.
     */
    @Override
    public int getItemCount() {
        return this.data.size();
    }

    /**
     * Returns the list of data items for the series (the list contains
     * {@link OHLCItem} objects and is unmodifiable).
     *
     * @return The list of data items.
     */
    public List<OHLCItem> getItems() {
        return CloneUtils.cloneList(this.data);
    }

    /**
     * Returns the maximum number of items that will be retained in the series.
     * The default value is {@code Integer.MAX_VALUE}.
     *
     * @return The maximum item count.
     * @see #setMaximumItemCount(int)
     */
    public int getMaximumItemCount() {
        return this.maximumItemCount;
    }

    /**
     * Sets the maximum number of items that will be retained in the series.
     * If you add a new item to the series such that the number of items will
     * exceed the maximum item count, then the FIRST element in the series is
     * automatically removed, ensuring that the maximum item count is not
     * exceeded.
     *
     * @param maximum the maximum (requires &gt;= 0).
     * @see #getMaximumItemCount()
     */
    public void setMaximumItemCount(int maximum) {
        if (maximum < 0) {
            throw new IllegalArgumentException("Negative 'maximum' argument.");
        }
        this.maximumItemCount = maximum;
        int count = this.data.size();
        if (count > maximum) {
            delete(0, count - maximum - 1);
        }
    }

    /**
     * Returns the maximum item age (in time periods) for the series.
     *
     * @return The maximum item age.
     * @see #setMaximumItemAge(long)
     */
    public long getMaximumItemAge() {
        return this.maximumItemAge;
    }

    /**
     * Sets the number of time units in the 'history' for the series.  This
     * provides one mechanism for automatically dropping old data from the
     * time series. For example, if a series contains daily data, you might set
     * the history count to 30.  Then, when you add a new data item, all data
     * items more than 30 days older than the latest value are automatically
     * dropped from the series.
     *
     * @param periods the number of time periods.
     * @see #getMaximumItemAge()
     */
    public void setMaximumItemAge(long periods) {
        if (periods < 0) {
            throw new IllegalArgumentException("Negative 'periods' argument.");
        }
        this.maximumItemAge = periods;
        removeAgedItems(true);  // remove old items and notify if necessary
    }

    /**
     * Returns the time period for the specified item.
     *
     * @param index the item index.
     * @return The time period.
     */
    public RegularTimePeriod getPeriod(int index) {
        OHLCItem item = (OHLCItem) getDataItem(index);
        return item.getPeriod();
    }

    /**
     * Returns the time period class for this series.
     * <p>
     * Only one time period class can be used within a single series (enforced).
     * If you add a data item with a {@link Year} for the time period, then all
     * subsequent data items must also have a {@link Year} for the time period.
     *
     * @return The time period class (may be {@code null} but only for
     * an empty series).
     */
    public Class getTimePeriodClass() {
        return this.timePeriodClass;
    }

    /**
     * Returns a data item from the dataset.  Note that the returned object
     * is a clone of the item in the series, so modifying it will have no
     * effect on the data series.
     *
     * @param index the item index.
     * @return The data item.
     */
    public OHLCItem getDataItem(int index) {
        OHLCItem item = this.data.get(index);
        return (OHLCItem) item.clone();
    }

    /**
     * Returns the data item for a specific period.  Note that the returned
     * object is a clone of the item in the series, so modifying it will have
     * no effect on the data series.
     *
     * @param period the period of interest ({@code null} not allowed).
     * @return The data item matching the specified period (or
     * {@code null} if there is no match).
     * @see #getDataItem(int)
     */
    public OHLCItem getDataItem(RegularTimePeriod period) {
        int index = getIndex(period);
        if (index >= 0) {
            return getDataItem(index);
        }
        return null;
    }

    /**
     * Returns a data item for the series.  This method returns the object
     * that is used for the underlying storage - you should not modify the
     * contents of the returned value unless you know what you are doing.
     *
     * @param index the item index (zero-based).
     * @return The data item.
     * @see #getDataItem(int)
     * @since 1.0.14
     */
    OHLCItem getRawDataItem(int index) {
        return this.data.get(index);
    }

    /**
     * Returns a data item for the series.  This method returns the object
     * that is used for the underlying storage - you should not modify the
     * contents of the returned value unless you know what you are doing.
     *
     * @param period the item index (zero-based).
     * @return The data item.
     * @see #getDataItem(RegularTimePeriod)
     * @since 1.0.14
     */
    OHLCItem getRawDataItem(RegularTimePeriod period) {
        int index = getIndex(period);
        if (index >= 0) {
            return this.data.get(index);
        }
        return null;
    }

    /**
     * Returns the time period at the specified index.
     *
     * @param index the index of the data item.
     * @return The time period.
     */
    public RegularTimePeriod getTimePeriod(int index) {
        return getRawDataItem(index).getPeriod();
    }

    /**
     * Returns a time period that would be the next in sequence on the end of
     * the time series.
     *
     * @return The next time period.
     */
    public RegularTimePeriod getNextTimePeriod() {
        RegularTimePeriod last = getTimePeriod(getItemCount() - 1);
        return last.next();
    }

    /**
     * Returns a collection of all the time periods in the time series.
     *
     * @return A collection of all the time periods.
     */
    public Collection getTimePeriods() {
        Collection result = new java.util.ArrayList<>();
        for (int i = 0; i < getItemCount(); i++) {
            result.add(getTimePeriod(i));
        }
        return result;
    }

    /**
     * Returns a collection of time periods in the specified series, but not in
     * this series, and therefore unique to the specified series.
     *
     * @param series the series to check against this one.
     * @return The unique time periods.
     */
    public Collection<RegularTimePeriod> getTimePeriodsUniqueToOtherSeries(OHLCSeries<S> series) {
        Collection<RegularTimePeriod> result = new ArrayList<>();
        for (int i = 0; i < series.getItemCount(); i++) {
            RegularTimePeriod period = series.getTimePeriod(i);
            int index = getIndex(period);
            if (index < 0) {
                result.add(period);
            }
        }
        return result;
    }

    /**
     * Returns the index for the item (if any) that corresponds to a time
     * period.
     *
     * @param period the time period ({@code null} not permitted).
     * @return The index.
     */
    public int getIndex(RegularTimePeriod period) {
        Args.nullNotPermitted(period, "period");
        OHLCItem dummy = new OHLCItem(period, 0, 0, 0, 0);
        return Collections.binarySearch(this.data, dummy);
    }

    /**
     * Returns the value at the specified index.
     *
     * @param index index of a value.
     * @return The value (possibly {@code null}).
     */
    public OHLCItem getValue(int index) {
        return getRawDataItem(index);
    }

    /**
     * Returns the value for a time period.  If there is no data item with the
     * specified period, this method will return {@code null}.
     *
     * @param period time period ({@code null} not permitted).
     * @return The value (possibly {@code null}).
     */
    public OHLCItem getValue(RegularTimePeriod period) {
        int index = getIndex(period);
        if (index >= 0) {
            return getValue(index);
        }
        return null;
    }

    /**
     * Adds a data item to the series and sends a {@link SeriesChangeEvent} to
     * all registered listeners.
     *
     * @param item the (timeperiod, value) pair ({@code null} not permitted).
     */
    public void add(OHLCItem item) {
        add(item, true);
    }

    /**
     * Adds a data item to the series and sends a {@link SeriesChangeEvent} to
     * all registered listeners.
     *
     * @param item   the (timeperiod, value) pair ({@code null} not permitted).
     * @param notify notify listeners?
     */
    public void add(OHLCItem item, boolean notify) {
        Args.nullNotPermitted(item, "item");
        item = (OHLCItem) item.clone();
        Class c = item.getPeriod().getClass();
        if (this.timePeriodClass == null) {
            this.timePeriodClass = c;
        } else if (!this.timePeriodClass.equals(c)) {
            StringBuilder b = new StringBuilder();
            b.append("You are trying to add data where the time period class ");
            b.append("is ");
            b.append(item.getPeriod().getClass().getName());
            b.append(", but the TimeSeries is expecting an instance of ");
            b.append(this.timePeriodClass.getName());
            b.append(".");
            throw new SeriesException(b.toString());
        }

        // make the change (if it's not a duplicate time period)...
        boolean added = false;
        int count = getItemCount();
        if (count == 0) {
            this.data.add(item);
            added = true;
        } else {
            RegularTimePeriod last = getTimePeriod(getItemCount() - 1);
            if (item.getPeriod().compareTo(last) > 0) {
                this.data.add(item);
                added = true;
            } else {
                int index = Collections.binarySearch(this.data, item);
                if (index < 0) {
                    this.data.add(-index - 1, item);
                    added = true;
                } else {
                    StringBuilder b = new StringBuilder();
                    b.append("You are attempting to add an observation for ");
                    b.append("the time period ");
                    b.append(item.getPeriod().toString());
                    b.append(" but the series already contains an observation");
                    b.append(" for that time period. Duplicates are not ");
                    b.append("permitted.  Try using the addOrUpdate() method.");
                    throw new SeriesException(b.toString());
                }
            }
        }
        if (added) {
            // check if this addition will exceed the maximum item count...
            if (getItemCount() > this.maximumItemCount) {
                OHLCItem d = this.data.remove(0);
            }

            removeAgedItems(false);  // remove old items if necessary, but
            // don't notify anyone, because that
            // happens next anyway...
            if (notify) {
                fireSeriesChanged();
            }
        }

    }

    /**
     * Adds a new data item to the series and sends a {@link SeriesChangeEvent}
     * to all registered listeners.
     *
     * @param period the time period ({@code null} not permitted).
     * @param value  the value.
     */
    public void add(RegularTimePeriod period, double open, double high, double low, double close) {
        // defer argument checking...
        add(period, open, high, low, close, true);
    }

    /**
     * Adds a new data item to the series and sends a {@link SeriesChangeEvent}
     * to all registered listeners.
     *
     * @param period the time period ({@code null} not permitted).
     * @param value  the value.
     * @param notify notify listeners?
     */
    public void add(RegularTimePeriod period, double open, double high, double low, double close, boolean notify) {
        // defer argument checking...
        OHLCItem item = new OHLCItem(period, open, high, low, close);
        add(item, notify);
    }

    /**
     * Adds a new data item to the series and sends
     * a {@link org.jfree.data.general.SeriesChangeEvent} to all registered
     * listeners.
     *
     * @param period the time period ({@code null} not permitted).
     * @param value  the value ({@code null} permitted).
     */
    public void add(RegularTimePeriod period, OHLC value) {
        // defer argument checking...
        add(period, value, true);
    }

    /**
     * Adds a new data item to the series and sends a {@link SeriesChangeEvent}
     * to all registered listeners.
     *
     * @param period the time period ({@code null} not permitted).
     * @param value  the value ({@code null} permitted).
     * @param notify notify listeners?
     */
    public void add(RegularTimePeriod period, OHLC value, boolean notify) {
        // defer argument checking...
        OHLCItem item = new OHLCItem(period, value.getOpen(), value.getHigh(), value.getLow(), value.getClose());
        add(item, notify);
    }

    /**
     * Updates (changes) the value for a time period.  Throws a
     * {@link SeriesException} if the period does not exist.
     *
     * @param period the period ({@code null} not permitted).
     * @param value  the value.
     * @since 1.0.14
     */
    public void update(RegularTimePeriod period, double open, double high, double low, double close) {
        update(period, new OHLC(open, high, low, close));
    }

    /**
     * Updates (changes) the value for a time period.  Throws a
     * {@link SeriesException} if the period does not exist.
     *
     * @param period the period ({@code null} not permitted).
     * @param value  the value ({@code null} permitted).
     */
    public void update(RegularTimePeriod period, OHLC value) {
        OHLCItem temp = new OHLCItem(period, value.getOpen(), value.getHigh(), value.getLow(), value.getClose());
        int index = Collections.binarySearch(this.data, temp);
        if (index < 0) {
            throw new SeriesException("There is no existing value for the "
                    + "specified 'period'.");
        }
        update(index, value);
    }

    /**
     * Updates (changes) the value of a data item.
     *
     * @param index the index of the data item.
     * @param value the new value ({@code null} permitted).
     */
    public void update(int index, OHLC value) {
        OHLCItem item = this.data.get(index);
        item.setValue(value.getOpen(), value.getHigh(), value.getLow(), value.getClose());
        fireSeriesChanged();
    }

    /**
     * Adds or updates data from one series to another.  Returns another series
     * containing the values that were overwritten.
     *
     * @param series the series to merge with this.
     * @return A series containing the values that were overwritten.
     */
    public OHLCSeries<S> addAndOrUpdate(OHLCSeries<S> series) {
        OHLCSeries<S> overwritten = new OHLCSeries<>(getKey());
        for (int i = 0; i < series.getItemCount(); i++) {
            OHLCItem item = series.getRawDataItem(i);
            OHLCItem oldItem = addOrUpdate(item.getPeriod(),
                    item.getOpenValue(), item.getHighValue(), item.getLowValue(), item.getCloseValue());
            if (oldItem != null) {
                overwritten.add(oldItem);
            }
        }
        return overwritten;
    }

    /**
     * Adds or updates an item in the times series and sends a
     * {@link SeriesChangeEvent} to all registered listeners.
     *
     * @param period the time period to add/update ({@code null} not
     *               permitted).
     * @param value  the new value.
     * @return A copy of the overwritten data item, or {@code null} if no
     * item was overwritten.
     */
    public OHLCItem addOrUpdate(RegularTimePeriod period,
            double open, double high, double low, double close) {
        return addOrUpdate(period, new OHLC(open, high, low, close));
    }

    /**
     * Adds or updates an item in the times series and sends a
     * {@link SeriesChangeEvent} to all registered listeners.
     *
     * @param period the time period to add/update ({@code null} not
     *               permitted).
     * @param value  the new value ({@code null} permitted).
     * @return A copy of the overwritten data item, or {@code null} if no
     * item was overwritten.
     */
    public OHLCItem addOrUpdate(RegularTimePeriod period,
                                OHLC value) {
        return addOrUpdate(new OHLCItem(period, value.getOpen(), value.getHigh(), value.getLow(), value.getClose()));
    }

    /**
     * Adds or updates an item in the times series and sends a
     * {@link SeriesChangeEvent} to all registered listeners.
     *
     * @param item the data item ({@code null} not permitted).
     * @return A copy of the overwritten data item, or {@code null} if no
     * item was overwritten.
     * @since 1.0.14
     */
    public OHLCItem addOrUpdate(OHLCItem item) {

        Args.nullNotPermitted(item, "item");
        Class periodClass = item.getPeriod().getClass();
        if (this.timePeriodClass == null) {
            this.timePeriodClass = periodClass;
        } else if (!this.timePeriodClass.equals(periodClass)) {
            String msg = "You are trying to add data where the time "
                    + "period class is " + periodClass.getName()
                    + ", but the TimeSeries is expecting an instance of "
                    + this.timePeriodClass.getName() + ".";
            throw new SeriesException(msg);
        }
        OHLCItem overwritten = null;
        int index = Collections.binarySearch(this.data, item);
        if (index >= 0) {
            OHLCItem existing = this.data.get(index);
            overwritten = (OHLCItem) existing.clone();
            // figure out if we need to iterate through all the y-values
            // to find the revised minY / maxY
            existing.setValue(item.getOpenValue(), item.getHighValue(), item.getLowValue(), item.getCloseValue());
        } else {
            item = (OHLCItem) item.clone();
            this.data.add(-index - 1, item);

            // check if this addition will exceed the maximum item count...
            if (getItemCount() > this.maximumItemCount) {
                OHLCItem d = this.data.remove(0);
            }
        }
        removeAgedItems(false);  // remove old items if necessary, but
        // don't notify anyone, because that
        // happens next anyway...
        fireSeriesChanged();
        return overwritten;

    }

    /**
     * Age items in the series.  Ensure that the timespan from the youngest to
     * the oldest record in the series does not exceed maximumItemAge time
     * periods.  Oldest items will be removed if required.
     *
     * @param notify controls whether or not a {@link SeriesChangeEvent} is
     *               sent to registered listeners IF any items are removed.
     */
    public void removeAgedItems(boolean notify) {
        // check if there are any values earlier than specified by the history
        // count...
        if (getItemCount() > 1) {
            long latest = getTimePeriod(getItemCount() - 1).getSerialIndex();
            boolean removed = false;
            while ((latest - getTimePeriod(0).getSerialIndex())
                    > this.maximumItemAge) {
                this.data.remove(0);
                removed = true;
            }
            if (removed) {
                if (notify) {
                    fireSeriesChanged();
                }
            }
        }
    }

    /**
     * Age items in the series.  Ensure that the timespan from the supplied
     * time to the oldest record in the series does not exceed history count.
     * oldest items will be removed if required.
     *
     * @param latest the time to be compared against when aging data
     *               (specified in milliseconds).
     * @param notify controls whether or not a {@link SeriesChangeEvent} is
     *               sent to registered listeners IF any items are removed.
     */
    public void removeAgedItems(long latest, boolean notify) {
        if (this.data.isEmpty()) {
            return;  // nothing to do
        }
        // find the serial index of the period specified by 'latest'
        long index = Long.MAX_VALUE;
        try {
            Method m = RegularTimePeriod.class.getDeclaredMethod(
                    "createInstance", Class.class, Date.class,
                    TimeZone.class, Locale.class);
            RegularTimePeriod newest = (RegularTimePeriod) m.invoke(
                    this.timePeriodClass, new Object[]{this.timePeriodClass,
                            new Date(latest), TimeZone.getDefault(), Locale.getDefault()});
            index = newest.getSerialIndex();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        // check if there are any values earlier than specified by the history
        // count...
        boolean removed = false;
        while (getItemCount() > 0 && (index
                - getTimePeriod(0).getSerialIndex()) > this.maximumItemAge) {
            this.data.remove(0);
            removed = true;
        }
        if (removed) {
            if (notify) {
                fireSeriesChanged();
            }
        }
    }

    /**
     * Removes all data items from the series and sends a
     * {@link SeriesChangeEvent} to all registered listeners.
     */
    public void clear() {
        if (this.data.size() > 0) {
            this.data.clear();
            this.timePeriodClass = null;
            fireSeriesChanged();
        }
    }

    /**
     * Deletes the data item for the given time period and sends a
     * {@link SeriesChangeEvent} to all registered listeners.  If there is no
     * item with the specified time period, this method does nothing.
     *
     * @param period the period of the item to delete ({@code null} not
     *               permitted).
     */
    public void delete(RegularTimePeriod period) {
        int index = getIndex(period);
        if (index >= 0) {
            OHLCItem item = this.data.remove(index);
            if (this.data.isEmpty()) {
                this.timePeriodClass = null;
            }
            fireSeriesChanged();
        }
    }

    /**
     * Deletes data from start until end index (end inclusive).
     *
     * @param start the index of the first period to delete.
     * @param end   the index of the last period to delete.
     */
    public void delete(int start, int end) {
        delete(start, end, true);
    }

    /**
     * Deletes data from start until end index (end inclusive).
     *
     * @param start  the index of the first period to delete.
     * @param end    the index of the last period to delete.
     * @param notify notify listeners?
     * @since 1.0.14
     */
    public void delete(int start, int end, boolean notify) {
        if (end < start) {
            throw new IllegalArgumentException("Requires start <= end.");
        }
        for (int i = 0; i <= (end - start); i++) {
            this.data.remove(start);
        }
        if (this.data.isEmpty()) {
            this.timePeriodClass = null;
        }
        if (notify) {
            fireSeriesChanged();
        }
    }

    /**
     * Returns a clone of the time series.
     * <p>
     * Notes:
     * <ul>
     *   <li>no need to clone the domain and range descriptions, since String
     *     object is immutable;</li>
     *   <li>we pass over to the more general method clone(start, end).</li>
     * </ul>
     *
     * @return A clone of the time series.
     * @throws CloneNotSupportedException not thrown by this class, but
     *                                    subclasses may differ.
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        OHLCSeries<S> clone = (OHLCSeries) super.clone();
        clone.data = CloneUtils.cloneList(this.data);
        return clone;
    }

    /**
     * Creates a new timeseries by copying a subset of the data in this time
     * series.
     *
     * @param start the index of the first time period to copy.
     * @param end   the index of the last time period to copy.
     * @return A series containing a copy of this times series from start until
     * end.
     * @throws CloneNotSupportedException if there is a cloning problem.
     */
    public OHLCSeries<S> createCopy(int start, int end)
            throws CloneNotSupportedException {
        if (start < 0) {
            throw new IllegalArgumentException("Requires start >= 0.");
        }
        if (end < start) {
            throw new IllegalArgumentException("Requires start <= end.");
        }
        OHLCSeries<S> copy = (OHLCSeries) super.clone();
        copy.data = new ArrayList<>();
        if (this.data.size() > 0) {
            for (int index = start; index <= end; index++) {
                OHLCItem item = this.data.get(index);
                OHLCItem clone = (OHLCItem) item.clone();
                try {
                    copy.add(clone);
                } catch (SeriesException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return copy;
    }

    /**
     * Creates a new timeseries by copying a subset of the data in this time
     * series.
     *
     * @param start the first time period to copy ({@code null} not
     *              permitted).
     * @param end   the last time period to copy ({@code null} not permitted).
     * @return A time series containing a copy of this time series from start
     * until end.
     * @throws CloneNotSupportedException if there is a cloning problem.
     */
    public OHLCSeries<S> createCopy(RegularTimePeriod start, RegularTimePeriod end)
            throws CloneNotSupportedException {

        Args.nullNotPermitted(start, "start");
        Args.nullNotPermitted(end, "end");
        if (start.compareTo(end) > 0) {
            throw new IllegalArgumentException(
                    "Requires start on or before end.");
        }
        boolean emptyRange = false;
        int startIndex = getIndex(start);
        if (startIndex < 0) {
            startIndex = -(startIndex + 1);
            if (startIndex == this.data.size()) {
                emptyRange = true;  // start is after last data item
            }
        }
        int endIndex = getIndex(end);
        if (endIndex < 0) {             // end period is not in original series
            endIndex = -(endIndex + 1); // this is first item AFTER end period
            endIndex = endIndex - 1;    // so this is last item BEFORE end
        }
        if ((endIndex < 0) || (endIndex < startIndex)) {
            emptyRange = true;
        }
        if (emptyRange) {
            OHLCSeries<S> copy = (OHLCSeries) super.clone();
            copy.data = new ArrayList<>();
            return copy;
        }
        return createCopy(startIndex, endIndex);
    }

    /**
     * Tests the series for equality with an arbitrary object.
     *
     * @param obj the object to test against ({@code null} permitted).
     * @return A boolean.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof OHLCSeries)) {
            return false;
        }
        OHLCSeries<S> that = (OHLCSeries) obj;
        if (!Objects.equals(this.timePeriodClass, that.timePeriodClass)) {
            return false;
        }
        if (getMaximumItemAge() != that.getMaximumItemAge()) {
            return false;
        }
        if (getMaximumItemCount() != that.getMaximumItemCount()) {
            return false;
        }
        int count = getItemCount();
        if (count != that.getItemCount()) {
            return false;
        }
        if (!Objects.equals(this.data, that.data)) {
            return false;
        }
        return super.equals(obj);
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return The hashcode
     */
    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 29 * result + (this.timePeriodClass != null
                ? this.timePeriodClass.hashCode() : 0);
        // it is too slow to look at every data item, so let's just look at
        // the first, middle and last items...
        int count = getItemCount();
        if (count > 0) {
            OHLCItem item = getRawDataItem(0);
            result = 29 * result + item.hashCode();
        }
        if (count > 1) {
            OHLCItem item = getRawDataItem(count - 1);
            result = 29 * result + item.hashCode();
        }
        if (count > 2) {
            OHLCItem item = getRawDataItem(count / 2);
            result = 29 * result + item.hashCode();
        }
        result = 29 * result + this.maximumItemCount;
        result = 29 * result + (int) this.maximumItemAge;
        return result;
    }

}

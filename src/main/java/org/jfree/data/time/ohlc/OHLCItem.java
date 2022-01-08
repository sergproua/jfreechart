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
 * -------------
 * OHLCItem.java
 * -------------
 * (C) Copyright 2006-2022, by David Gilbert.
 *
 * Original Author:  David Gilbert;
 * Contributor(s):   -;
 *
 */

package org.jfree.data.time.ohlc;

import org.jfree.data.ComparableObjectItem;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeriesDataItem;

import java.io.Serializable;
import java.util.Objects;

/**
 * An item representing data in the form {@code (time-period, open, high, low,
 * close)}.
 */
public class OHLCItem implements Cloneable,
        Comparable<OHLCItem>, Serializable {

    /**
     * The time period.
     */
    private RegularTimePeriod period;

    /**
     * The value associated with the time period.
     */
    private OHLC value;

    /**
     * Creates a new instance of {@code OHLCItem}.
     *
     * @param period the time period.
     * @param open   the open-value.
     * @param high   the high-value.
     * @param low    the low-value.
     * @param close  the close-value.
     */
    public OHLCItem(RegularTimePeriod period, double open, double high,
                    double low, double close) {
        this.period = period;
        this.value = new OHLC(open, high, low, close);
    }

    /**
     * Returns the period.
     *
     * @return The period (never {@code null}).
     */
    public RegularTimePeriod getPeriod() {
        return this.period;
    }

    /**
     * Returns the y-value.
     *
     * @return The y-value.
     */
    public double getYValue() {
        return getCloseValue();
    }

    /**
     * Returns the open value.
     *
     * @return The open value.
     */
    public double getOpenValue() {
        return value.getOpen();
    }

    /**
     * Returns the high value.
     *
     * @return The high value.
     */
    public double getHighValue() {
        return value.getHigh();
    }

    /**
     * Returns the low value.
     *
     * @return The low value.
     */
    public double getLowValue() {
        return value.getLow();
    }

    /**
     * Returns the close value.
     *
     * @return The close value.
     */
    public double getCloseValue() {
        return value.getClose();
    }

    /**
     * Sets the value for this data item.
     *
     * @param open   the open-value.
     * @param high   the high-value.
     * @param low    the low-value.
     * @param close  the close-value.
     */
    public void setValue(double open, double high, double low, double close) {
        this.value = new OHLC(open, high, low, close);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof OHLCItem)) {
            return false;
        }
        OHLCItem that = (OHLCItem) obj;
        if (!Objects.equals(this.period, that.period)) {
            return false;
        }
        if (!Objects.equals(this.value, that.value)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = (this.period != null ? this.period.hashCode() : 0);
        result = 29 * result + (this.value != null ? this.value.hashCode() : 0);
        return result;
    }

    @Override
    public int compareTo(OHLCItem other) {
        return getPeriod().compareTo(other.getPeriod());
    }

    @Override
    public Object clone() {
        Object clone = null;
        try {
            clone = super.clone();
        }
        catch (CloneNotSupportedException e) { // won't get here...
            e.printStackTrace();
        }
        return clone;
    }

}

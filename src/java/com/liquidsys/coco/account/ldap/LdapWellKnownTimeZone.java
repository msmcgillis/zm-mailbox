/*
 * Created on 2005. 7. 11.
 */
package com.liquidsys.coco.account.ldap;

import javax.naming.directory.Attributes;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.coco.account.WellKnownTimeZone;
import com.liquidsys.coco.mailbox.calendar.ICalTimeZone;

/**
 * @author jhahm
 *
 * Note: this class has a natural ordering that is inconsistent with equals.
 */
public class LdapWellKnownTimeZone extends LdapNamedEntry implements WellKnownTimeZone {

    private static Log mLog = LogFactory.getLog(LdapWellKnownTimeZone.class);

    LdapWellKnownTimeZone(String dn, Attributes attrs) {
        super(dn, attrs);
    }

    public String getName() {
        return getAttr(Provisioning.A_cn);
    }

    public String getId() {
        return getAttr(Provisioning.A_cn);
    }

    private ICalTimeZone mTimeZone;

    public synchronized ICalTimeZone toTimeZone() {
        if (mTimeZone != null)
            return mTimeZone;

        String tzId = getId();
        try {
        	int standardOffset = getStandardOffsetMins();
            int daylightOffset = getDaylightOffsetMins();
            mTimeZone = new ICalTimeZone(tzId,
                                         standardOffset * 60 * 1000,
                                         getStandardDtStart(),
                                         getStandardRecurrenceRule(),
                                         daylightOffset * 60 * 1000,
                                         getDaylightDtStart(),
                                         getDaylightRecurrenceRule());
        } catch (Exception e) {
            mLog.error("Invalid time zone entry: " + tzId, e);
            mTimeZone = new ICalTimeZone(tzId,
                                         0,
                                         "16010101T000000",
                                         null,
                                         0,
                                         "16010101T000000",
                                         null);
        }
        return mTimeZone;
    }

    public String getStandardDtStart() {
        return getAttr(Provisioning.A_liquidTimeZoneStandardDtStart);
    }

    public String getStandardOffset() {
        return getAttr(Provisioning.A_liquidTimeZoneStandardOffset);
    }

    boolean mStandardOffsetMinsCached = false;
    int mStandardOffsetMins;

    private synchronized int getStandardOffsetMins() {
        if (!mStandardOffsetMinsCached) {
            mStandardOffsetMins = offsetToInt(getStandardOffset());
            mStandardOffsetMinsCached = true;
        }
        return mStandardOffsetMins;
    }

    public String getStandardRecurrenceRule() {
        return getAttr(Provisioning.A_liquidTimeZoneStandardRRule);
    }

    public String getDaylightDtStart() {
        return getAttr(Provisioning.A_liquidTimeZoneDaylightDtStart);
    }

    public String getDaylightOffset() {
        return getAttr(Provisioning.A_liquidTimeZoneDaylightOffset);
    }

    boolean mDaylightOffsetMinsCached = false;
    int mDaylightOffsetMins;

    private synchronized int getDaylightOffsetMins() {
        if (!mDaylightOffsetMinsCached) {
            mDaylightOffsetMins = offsetToInt(getDaylightOffset());
            mDaylightOffsetMinsCached = true;
        }
        return mDaylightOffsetMins;
    }

    public String getDaylightRecurrenceRule() {
        return getAttr(Provisioning.A_liquidTimeZoneDaylightRRule);
    }

    /**
     * First sort by the offset from GMT in minutes, then alphabetically
     * by substring of the time zone name following the "(GMT+/-HHMM) " prefix.
     */
    public int compareTo(Object obj) {
        if (!(obj instanceof LdapWellKnownTimeZone))
            return 0;
        LdapWellKnownTimeZone other = (LdapWellKnownTimeZone) obj;

        int thisOffset = getStandardOffsetMins();
        int otherOffset = other.getStandardOffsetMins();
        if (thisOffset < otherOffset)
            return -1;
        else if (thisOffset > otherOffset)
            return 1;

        String thisId = getId();
        if (thisId.indexOf("(GMT") == 0)
            thisId = thisId.substring(thisId.indexOf(')') + 1);
        String otherId = other.getId();
        if (otherId.indexOf("(GMT") == 0)
            otherId = otherId.substring(otherId.indexOf(')') + 1);
        return thisId.compareTo(otherId);
    }

    private static int offsetToInt(String offset) {
        try {
        	boolean negative = offset.charAt(0) == '-';
            int hour = Integer.parseInt(offset.substring(1, 3));
            int min = Integer.parseInt(offset.substring(3, 5));
            int offsetMins = hour * 60 + min;
            if (negative)
                offsetMins *= -1;
            return offsetMins;
        } catch (StringIndexOutOfBoundsException se) {
        	return 0;
        } catch (NumberFormatException ne) {
        	return 0;
        }
    }
}

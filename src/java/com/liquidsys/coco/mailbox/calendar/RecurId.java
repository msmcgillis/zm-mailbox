package com.liquidsys.coco.mailbox.calendar;

import java.text.ParseException;
import java.util.Date;

import com.liquidsys.coco.account.Account;
import com.liquidsys.coco.mailbox.Metadata;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.service.mail.MailService;

import net.fortuna.ical4j.model.property.RecurrenceId;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.parameter.Range;
import net.fortuna.ical4j.model.parameter.TzId;

public class RecurId 
{
    static final int RANGE_NONE          = 1;
    private static final int RANGE_THISANDFUTURE = 2;
    private static final int RANGE_THISANDPRIOR  = 3;
    
    private int mRange;
    private ParsedDateTime mDateTime;
    
    public RecurrenceId getRecurrenceId(Account acct) throws ServiceException {
        RecurrenceId toRet = new RecurrenceId();
        try {
            toRet.setValue(mDateTime.getDateTimePartString());
            if (mDateTime.isUTC()) {
                toRet.setUtc(true);
            } else {
                String tzName = mDateTime.getTZName();
                if (tzName == null) {
                    toRet.getParameters().add(new TzId(acct.getTimeZone().getID()));
                } else {
                    toRet.getParameters().add(new TzId(tzName));
                }
            }
            
            switch (mRange) {
            case RANGE_THISANDFUTURE:
                toRet.getParameters().add(Range.THISANDFUTURE);
                break;
            case RANGE_THISANDPRIOR:
                toRet.getParameters().add(Range.THISANDPRIOR);
                break;
            }
        } catch (ParseException e) {
            throw ServiceException.FAILURE("Parsing: "+mDateTime.toString(), e);
        }
        
        return toRet;
    }
    
    public static RecurId parse(RecurrenceId rid, TimeZoneMap tzmap) throws ParseException
    {
        ParsedDateTime dt = ParsedDateTime.parse(rid, tzmap);
        
        Range range = (Range)rid.getParameters().getParameter(Parameter.RANGE);
        int rangeVal = RANGE_NONE;
        if (range != null) {
            if (range.getValue().equals(Range.THISANDFUTURE.getValue())) {
                rangeVal = RANGE_THISANDFUTURE;
            } else if (range.getValue().equals(Range.THISANDPRIOR.getValue())) {
                rangeVal = RANGE_THISANDPRIOR;
            }
        } 

        return new RecurId(dt, rangeVal);
    }
    
    public RecurId(ParsedDateTime dt, String rangeStr) {
        if (rangeStr.equals("THISANDFUTURE")) {
            mRange = RANGE_THISANDFUTURE;
        } else if (rangeStr.equals("THISANDPRIOR")) {
            mRange = RANGE_THISANDPRIOR;
        } else {
            mRange = RANGE_NONE;
        }
        mDateTime = dt;
    }
    
    
    private RecurId(ParsedDateTime dt, int range) {
        mRange = range;
        mDateTime = dt;
    }
    
    public String toString() {
        StringBuffer toRet = new StringBuffer(mDateTime.toString());
        switch (mRange) {
        case RANGE_THISANDFUTURE:
            toRet.append("-THISANDFUTURE");
            break;
        case RANGE_THISANDPRIOR:
            toRet.append("-THISANDPRIOR");
            break;
        }
        return toRet.toString();
    }
    
    public boolean equals(Object other) {
        if (other == null) { return false; }
        RecurId rhs = (RecurId)other;
        
        if (mRange == rhs.mRange && mDateTime.equals(rhs.mDateTime)) {
            return true;
        }
        
        return false;
    }
    
    public boolean withinRange(Date d) {
        int comp = mDateTime.compareTo(d);
        if (comp == 0) {
            return true;
        } else if (comp < 0) { // mDt < d
            return mRange == RANGE_THISANDFUTURE;
        } else {
            return mRange == RANGE_THISANDPRIOR;
        }
    }
    
    public boolean withinRange(long d) {
        int comp = mDateTime.compareTo(d);
        if (comp == 0) {
            return true;
        } else if (comp < 0) { // mDt < d
            return mRange == RANGE_THISANDFUTURE;
        } else {
            return mRange == RANGE_THISANDPRIOR;
        }
    }
    
    
    private static final String FN_DT = "dt";
    private static final String FN_RANGE = "r";
    
    public Metadata encodeMetadata() {
        Metadata md = new Metadata();
        md.put(FN_DT, mDateTime.toString());
        md.put(FN_RANGE, mRange);
        return md;
    }
    
    public Element toXml(Element parent) {
        parent.addAttribute(MailService.A_APPT_RECURRENCE_ID, mDateTime.toString());
        parent.addAttribute(MailService.A_APPT_RECURRENCE_RANGE_TYPE, mRange);
        return parent;
    }
    
    public static RecurId decodeMetadata(Metadata md, TimeZoneMap tzmap) throws ServiceException {
        try {
            return new RecurId(ParsedDateTime.parse(md.get(FN_DT), tzmap), (int)md.getLong(FN_RANGE));
        } catch (ParseException e) {
            throw ServiceException.FAILURE("Parsing "+md.get(FN_DT), e);
        }
    }
}

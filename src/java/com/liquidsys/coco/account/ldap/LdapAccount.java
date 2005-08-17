/*
 * Created on Sep 23, 2004
 *
 */
package com.liquidsys.coco.account.ldap;

import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import com.liquidsys.coco.account.Account;
import com.liquidsys.coco.account.Cos;
import com.liquidsys.coco.account.Domain;
import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.coco.account.WellKnownTimeZone;
import com.liquidsys.coco.mailbox.calendar.ICalTimeZone;
import com.liquidsys.coco.service.ServiceException;

/**
 * @author schemers
 *
 */
public class LdapAccount extends LdapNamedEntry implements Account {

    private LdapProvisioning mProv;
    private String mName;
    private String mDomainName;    
    
    LdapAccount(String dn, Attributes attrs, LdapProvisioning prov) {
        super(dn, attrs);
        mProv = prov;
        initNameAndDomain();
    }

    public String getId() {
        return super.getAttr(Provisioning.A_liquidId);
    }

    public String getUid() {
        return super.getAttr(Provisioning.A_uid);
    }
    
    public String getName() {
        return mName;
    }

    /**
     * @return the domain name for this account (foo.com), or null if an admin account. 
     */
    public String getDomainName() {
        return mDomainName;
    }

    /**
     * @return the domain this account, or null if an admin account. 
     * @throws ServiceException
     */
    public Domain getDomain() throws ServiceException {
        return mDomainName == null ? null : mProv.getDomainByName(mDomainName);
    }

    private void initNameAndDomain() {
        String uid = getUid(); 
        mDomainName = LdapUtil.dnToDomain(mDn);
        if (!mDomainName.equals("")) {
            mName =  uid+"@"+mDomainName;
        } else {
            mName = uid;
            mDomainName = null;
        }
    }

    public String getAccountStatus() {
        return super.getAttr(Provisioning.A_liquidAccountStatus);
    }
     
    public String getAttr(String name) {
        String v = super.getAttr(name);
        if (v != null)
            return v;
        try {
            if (!mProv.getConfig().isInheritedAccountAttr(name))
                return null;
            Cos cos = getCOS();
            if (cos != null)
                return cos.getAttr(name);
            else
                return null;            
        } catch (ServiceException e) {
            return null;
        }
    }

    public String[] getMultiAttr(String name) {
        String v[] = super.getMultiAttr(name);
        if (v.length > 0)
            return v;
        try {
            if (!mProv.getConfig().isInheritedAccountAttr(name))
                return sEmptyMulti;

            Cos cos = getCOS();
            if (cos != null)
                return cos.getMultiAttr(name);
            else
                return sEmptyMulti;
        } catch (ServiceException e) {
            return null;
        }
    }

    
    public String[] getAliases() throws ServiceException
    {
        return getMultiAttr(Provisioning.A_liquidMailAlias);
    }

    /* (non-Javadoc)
     * @see com.liquidsys.coco.account.Account#getCOS()
     */
    public Cos getCOS() throws ServiceException {
        // TODO: caching? assume getCOSById does caching?
        String id = super.getAttr(Provisioning.A_liquidCOSId);
        if (id == null)
            return mProv.getCosByName(Provisioning.DEFAULT_COS_NAME);
        else
            return mProv.getCosById(id); 
    }

    /* (non-Javadoc)
     * @see com.liquidsys.coco.account.Account#getPrefs()
     */
    public Map getPrefs() throws ServiceException {
        HashMap prefs = new HashMap();
        try {
            LdapCos cos = (LdapCos) getCOS();
            // get the COS prefs first
            LdapUtil.getAttrs(cos.mAttrs, prefs, "liquidPref");
            // and override with the account ones
            LdapUtil.getAttrs(mAttrs, prefs, "liquidPref");
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to get prefs", e);
        }
        return prefs;
    }
    
    public Map getAttrs() throws ServiceException {
        return getAttrs(false, true);
    }

    /* (non-Javadoc)
     * @see com.liquidsys.coco.account.Account#getPrefs()
     */
    public Map getAttrs(boolean prefsOnly, boolean applyCos) throws ServiceException {
        HashMap attrs = new HashMap();
        try {
            // get all the account attrs
            LdapUtil.getAttrs(mAttrs, attrs, prefsOnly ? "liquidPref" : null);
            
            if (!applyCos)
                return attrs;
            
            // then enumerate through all inheritable attrs and add them if needed
            String[] inheritable = mProv.getConfig().getMultiAttr(Provisioning.A_liquidCOSInheritedAttr);
            for (int i = 0; i < inheritable.length; i++) {
                if (!prefsOnly || inheritable[i].startsWith("liquidPref")) {
                    Object value = attrs.get(inheritable);
                    if (value == null)
                        value = getMultiAttr(inheritable[i]);
                    if (value != null)
                        attrs.put(inheritable[i], value);
                }
            }
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to get prefs", e);
        }
        return attrs;
    }

    /* (non-Javadoc)
     * @see com.liquidsys.coco.account.Account#isCorrectHost()
     */
    public boolean isCorrectHost() throws ServiceException{
        String target    = getAttr(Provisioning.A_liquidMailHost);
        String localhost = mProv.getLocalServer().getAttr(Provisioning.A_liquidServiceHostname);
        return (target != null && target.equalsIgnoreCase(localhost));
    }

    private ICalTimeZone mTimeZone;

    public synchronized ICalTimeZone getTimeZone() throws ServiceException {
        String tzId = getAttr(Provisioning.A_liquidPrefTimeZoneId);
        if (tzId == null) {
        	if (mTimeZone != null)
                return mTimeZone;
            mTimeZone = ICalTimeZone.getUTC();
            return mTimeZone;
        }

        if (mTimeZone != null) {
            if (mTimeZone.getID().equals(tzId))
                return mTimeZone;
            // Else the account's time zone was updated.  Discard the cached
            // ICalTimeZone object.
        }

    	WellKnownTimeZone z = Provisioning.getInstance().getTimeZoneById(tzId);
        if (z != null)
            mTimeZone = z.toTimeZone();
        if (mTimeZone == null)
            mTimeZone = ICalTimeZone.getUTC();
        return mTimeZone;
    }

}

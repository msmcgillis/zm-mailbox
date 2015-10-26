/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ZimbraAuthToken;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.cs.client.LmcSession;
import com.zimbra.cs.client.soap.LmcSearchRequest;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.account.message.AuthRequest;
import com.zimbra.soap.account.message.AuthResponse;
import com.zimbra.soap.type.AccountSelector;

public class TestAuthentication
extends TestCase {
    private static String USER_NAME = "testauthentication";
    private static String PASSWORD = "test123";

    private Provisioning mProv;
    private Integer mMboxId;

    @Override public void setUp()
    throws Exception {
        mProv = Provisioning.getInstance();
        cleanUp();

        // Create temporary account
        String address = TestUtil.getAddress(USER_NAME);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("zimbraMailHost", TestUtil.getDomain());
        attrs.put("cn", "Unit test temporary user");
        attrs.put("displayName", "Unit test temporary user");
        Account account = mProv.createAccount(address, PASSWORD, attrs);
        assertNotNull("Could not create account", account);
        mMboxId = MailboxManager.getInstance().getMailboxByAccount(account).getId();
    }

    @Override protected void tearDown()
    throws Exception {
        cleanUp();
    }

    private Account getAccount()
    throws Exception {
        String address = TestUtil.getAddress(USER_NAME);
        return Provisioning.getInstance().get(AccountBy.name, address);
    }

    /**
     * Attempts to access a deleted account and confirms that the attempt
     * fails with an auth error.
     */
    public void testAccessDeletedAccount()
    throws Exception {
        // Log in and check the inbox
        LmcSession session = TestUtil.getSoapSession(USER_NAME);
        LmcSearchRequest req = new LmcSearchRequest();
        req.setQuery("in:inbox");
        req.setSession(session);
        req.invoke(TestUtil.getSoapUrl());

        // Delete the account
        Account account = getAccount();
        assertNotNull("Account does not exist", account);
        mProv.deleteAccount(account.getId());

        // Submit another request and make sure it fails with an auth error
        try {
            req.invoke(TestUtil.getSoapUrl());
        } catch (SoapFaultException ex) {
            assertTrue("Unexpected error: " + ex.getMessage(),
                ex.getMessage().indexOf("auth credentials have expired") >= 0);
        }
    }

    /**
     * Attempts to access a deleted account and confirms that the attempt
     * fails with an auth error.
     */
    public void testAccessInactiveAccount()
    throws Exception {
        // Log in and check the inbox
        LmcSession session = TestUtil.getSoapSession(USER_NAME);
        LmcSearchRequest req = new LmcSearchRequest();
        req.setQuery("in:inbox");
        req.setSession(session);
        req.invoke(TestUtil.getSoapUrl());

        // Deactivate the account
        Account account = getAccount();
        mProv.modifyAccountStatus(account, Provisioning.ACCOUNT_STATUS_MAINTENANCE);

        // Submit another request and make sure it fails with an auth error
        try {
            req.invoke(TestUtil.getSoapUrl());
        } catch (SoapFaultException ex) {
            String substring = "auth credentials have expired";
            String msg = String.format("Error message '%s' does not contain '%s'", ex.getMessage(), substring);
            assertTrue(msg, ex.getMessage().contains(substring));
        }
    }

    /**
     * test detault auth request with login/password
     * @throws Exception
     */
    public void testSimpleAuth() throws Exception {
        //regular auth request
        Account a = TestUtil.getAccount(USER_NAME);
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getSoapUrl());
        AccountSelector acctSel = new AccountSelector(com.zimbra.soap.type.AccountBy.name, a.getName());
        AuthRequest req = new AuthRequest(acctSel, "test123");
        Element resp = transport.invoke(JaxbUtil.jaxbToElement(req, SoapProtocol.SoapJS.getFactory()));
        AuthResponse authResp = JaxbUtil.elementToJaxb(resp);
        String newAuthToken = authResp.getAuthToken();
        assertNotNull("should have received a new authtoken", newAuthToken);
        AuthToken at = ZimbraAuthToken.getAuthToken(newAuthToken);
        assertTrue("new auth token should be registered", at.isRegistered());
        assertFalse("new auth token should not be expired yet", at.isExpired());
    }

    /**
     * test detault admin auth request with login/password
     * @throws Exception
     */
    public void testAdminAuth() throws Exception {
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getAdminSoapUrl());
        com.zimbra.soap.admin.message.AuthRequest req = new com.zimbra.soap.admin.message.AuthRequest(LC.zimbra_ldap_user.value(), LC.zimbra_ldap_password.value());
        Element resp = transport.invoke(JaxbUtil.jaxbToElement(req, SoapProtocol.SoapJS.getFactory()));
        com.zimbra.soap.admin.message.AuthResponse authResp = JaxbUtil.elementToJaxb(resp);
        String newAuthToken = authResp.getAuthToken();
        assertNotNull("should have received a new authtoken", newAuthToken);
        AuthToken at = ZimbraAuthToken.getAuthToken(newAuthToken);
        assertTrue("new auth token should be registered", at.isRegistered());
        assertFalse("new auth token should not be expired yet", at.isExpired());
    }

    /**
     * test admin auth request with authtoken cookie instead of login/password
     * @throws Exception
     */
    public void testAdminAuthViaCookie() throws Exception {
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getAdminSoapUrl());
        com.zimbra.soap.admin.message.AuthRequest req = new com.zimbra.soap.admin.message.AuthRequest(LC.zimbra_ldap_user.value(), LC.zimbra_ldap_password.value());
        Element resp = transport.invoke(JaxbUtil.jaxbToElement(req, SoapProtocol.SoapJS.getFactory()));
        com.zimbra.soap.admin.message.AuthResponse authResp = JaxbUtil.elementToJaxb(resp);
        String newAuthToken = authResp.getAuthToken();
        assertNotNull("should have received a new authtoken", newAuthToken);
        AuthToken at = ZimbraAuthToken.getAuthToken(newAuthToken);
        assertTrue("new auth token should be registered", at.isRegistered());
        assertFalse("new auth token should not be expired yet", at.isExpired());
    }

    /**
     * test admin auth request with authtoken in SOAP instead of login/password
     * @throws Exception
     */
    public void testAdminAuthViaSOAPToken() throws Exception {
        AuthToken at = AuthProvider.getAdminAuthToken();
        SoapTransport transport = TestUtil.getAdminSoapTransport();
        com.zimbra.soap.admin.message.AuthRequest req = new com.zimbra.soap.admin.message.AuthRequest();
        req.setAuthToken(at.getEncoded());
        Element resp = transport.invoke(JaxbUtil.jaxbToElement(req, SoapProtocol.SoapJS.getFactory()));
        com.zimbra.soap.admin.message.AuthResponse authResp = JaxbUtil.elementToJaxb(resp);
        String newAuthToken = authResp.getAuthToken();
        assertNotNull("should have received a new authtoken", newAuthToken);
        at = ZimbraAuthToken.getAuthToken(newAuthToken);
        assertTrue("new auth token should be registered", at.isRegistered());
        assertFalse("new auth token should not be expired yet", at.isExpired());
    }

    /**
     * Deletes the account and the associated mailbox.
     */
    private void cleanUp()
    throws Exception {
        Account account = getAccount();
        if (account != null) {
            Provisioning.getInstance().deleteAccount(account.getId());
        }
        
        if (mMboxId != null) {
            Mailbox mbox = MailboxManager.getInstance().getMailboxById(mMboxId);
            mbox.deleteMailbox();
        }
    }
}

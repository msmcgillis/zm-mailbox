/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.datasource;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.DataSource.DataImport;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.DataSourceBy;
import com.zimbra.cs.db.DbMailbox;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbScheduledTask;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.gal.GalImport;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.ScheduledTaskManager;


public class DataSourceManager {
    // accountId -> dataSourceId -> ImportStatus
    private static final Map<String, Map<String, ImportStatus>> sImportStatus =
        new HashMap<String, Map<String, ImportStatus>>();

    /*
     * Tests connecting to a data source.  Do not actually create the
     * data source.
     */
    public static void test(DataSource ds) throws ServiceException {
        ZimbraLog.datasource.info("Testing: %s", ds);
        try {
        	DataImport di = newDataImport(ds);
        	di.test();
            ZimbraLog.datasource.info("Test succeeded: %s", ds);
        } catch (ServiceException x) {
        	ZimbraLog.datasource.warn("Test failed: %s", ds, x);
            throw x;
        }        
    }

    private static DataImport newDataImport(DataSource ds)
        throws ServiceException {
    	DataImport di = ds.getDataImport();
    	if (di != null)
    		return di;
        switch (ds.getType()) {
        case imap:
            return new ImapSync(ds);
        case live:
            return new LiveImport(ds);
        case pop3:
            return new Pop3Sync(ds);
        case rss:
            return new RssImport(ds);
        case gal:
            return new GalImport(ds);
        case cal:
            return new RssImport(ds);
        default:
            throw new IllegalArgumentException(
                "Unknown data import type: " + ds.getType());
        }
    }

    public static List<ImportStatus> getImportStatus(Account account)
        throws ServiceException {
        List<DataSource> dsList = Provisioning.getInstance().getAllDataSources(account);
        List<ImportStatus> allStatus = new ArrayList<ImportStatus>();
        for (DataSource ds : dsList) {
            allStatus.add(getImportStatus(account, ds));
        }
        return allStatus;
    }
    
    public static ImportStatus getImportStatus(Account account, DataSource ds) {
        ImportStatus importStatus;
        
        synchronized (sImportStatus) {
            Map<String, ImportStatus> isMap = sImportStatus.get(account.getId());
            if (isMap == null) {
                isMap = new HashMap<String, ImportStatus>();
                sImportStatus.put(account.getId(), isMap);
            }
            importStatus = isMap.get(ds.getId());
            if (importStatus == null) {
                importStatus = new ImportStatus(ds.getId());
                isMap.put(ds.getId(), importStatus);
            }
        }
        
        return importStatus;
    }

    public static void importData(DataSource ds) throws ServiceException {
        importData(ds, null, true);
    }

    public static void importData(DataSource fs, boolean fullSync)
        throws ServiceException {
        importData(fs, null, fullSync);
    }

    /**
     * Executes the data source's <code>MailItemImport</code> implementation
     * to import data in the current thread.
     */
    public static void importData(DataSource ds, List<Integer> folderIds,
                                  boolean fullSync) throws ServiceException {
        ImportStatus importStatus = getImportStatus(ds.getAccount(), ds);
        
        synchronized (importStatus) {
            if (importStatus.isRunning()) {
                ZimbraLog.datasource.info("Attempted to start import while " +
                    " an import process was already running.  Ignoring the second request.");
                return;
            }
            if (ds.getMailbox().getMailboxLock() != null) {
                ZimbraLog.datasource.info("Mailbox is in maintenance mode. Skipping import.");
                return;
            }            
            importStatus.mHasRun = true;
            importStatus.mIsRunning = true;
            importStatus.mSuccess = false;
            importStatus.mError = null;
        }
                
        boolean success = false;
        String error = null;

        try {
            ZimbraLog.datasource.info("Importing data for data source '%s'", ds.getName());
            newDataImport(ds).importData(folderIds, fullSync);
            success = true;
            resetErrorStatusIfNecessary(ds);
        } catch (ServiceException x) {
            error = generateErrorMessage(x);
            setErrorStatus(ds, error);
            throw x;
        } finally {
            ZimbraLog.datasource.info("Import completed for data source '%s'", ds.getName());
            synchronized (importStatus) {
                importStatus.mSuccess = success;
                importStatus.mError = error;
                importStatus.mIsRunning = false;
            }
        }
        
        return;
    }
    
    private static void resetErrorStatusIfNecessary(DataSource ds) {
        if (ds.getAttr(Provisioning.A_zimbraDataSourceFailingSince) != null ||
            ds.getAttr(Provisioning.A_zimbraDataSourceLastError) != null) {
            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put(Provisioning.A_zimbraDataSourceFailingSince, null);
            attrs.put(Provisioning.A_zimbraDataSourceLastError, null);
            try {
                Provisioning.getInstance().modifyAttrs(ds, attrs);
            } catch (ServiceException e) {
                ZimbraLog.datasource.warn("Unable to reset error status for data source %s.", ds.getName());
            }
        }
    }
    
    private static void setErrorStatus(DataSource ds, String error) {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceLastError, error);
        if (ds.getAttr(Provisioning.A_zimbraDataSourceFailingSince) == null) {
            attrs.put(Provisioning.A_zimbraDataSourceFailingSince, DateUtil.toGeneralizedTime(new Date()));
        }
        try {
            Provisioning.getInstance().modifyAttrs(ds, attrs);
        } catch (ServiceException e) {
            ZimbraLog.datasource.warn("Unable to set error status for data source %s.", ds.getName());
        }
    }
    
    private static String generateErrorMessage(Throwable t) {
        StringBuilder buf = new StringBuilder();
        boolean isFirst = true;
        while (t != null) {
			// HACK: go with JavaMail error message
			if (t.getClass().getName().startsWith("javax.mail.")) {
				return t.getMessage();
			}
            if (isFirst) {
                isFirst = false;
            } else {
                buf.append(", ");
            }
            String msg = t.getMessage();
            if (msg == null) {
                msg = t.toString(); 
            }
            buf.append(msg);
            t = t.getCause();
        }
        return buf.toString();
    }

    static void cancelTask(Mailbox mbox, String dsId)
        throws ServiceException {
        ScheduledTaskManager.cancel(DataSourceTask.class.getName(), dsId, mbox.getId(), false);
        DbScheduledTask.deleteTask(DataSourceTask.class.getName(), dsId);
    }
    
    /*
     * Updates scheduling data for this <tt>DataSource</tt> both in memory and in the
     * <tt>data_source_task</tt> database table.
     */
    public static void updateSchedule(String accountId, String dsId)
    throws ServiceException {
        ZimbraLog.datasource.debug("Updating schedule for account %s, data source %s", accountId, dsId);
        
        // Look up account and data source
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.get(AccountBy.id, accountId);
        if (account == null) {
            ZimbraLog.datasource.info(
                "Account %s was deleted for data source %s.  Deleting scheduled task.",
                accountId, dsId);
            DbScheduledTask.deleteTask(DataSourceTask.class.getName(), dsId);
            // Don't have mailbox ID, so we'll have to wait for the task to run and clean itself up.
            return;
        }
        // Get the mailbox without requesting auto-create.  It's important not to auto-create
        // the mailbox when this code is called during restore.
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(account.getId(), false);
        if (mbox == null)
        	return;
        DataSource ds = prov.get(account, DataSourceBy.id, dsId);
        if (ds == null) {
            ZimbraLog.datasource.info(
                "Data source %s was deleted.  Deleting scheduled task.", dsId);
            ScheduledTaskManager.cancel(DataSourceTask.class.getName(), dsId, mbox.getId(), false);
            DbScheduledTask.deleteTask(DataSourceTask.class.getName(), dsId);
            return;
        }
        if (!ds.isEnabled()) {
            ZimbraLog.datasource.info(
                "Data source %s is disabled.  Deleting scheduled task.", dsId);
            ScheduledTaskManager.cancel(DataSourceTask.class.getName(), dsId, mbox.getId(), false);
            DbScheduledTask.deleteTask(DataSourceTask.class.getName(), dsId);
            return;
        }
        
        ZimbraLog.datasource.info("Updating schedule for data source %s", ds.getName());
        synchronized (DbMailbox.getSynchronizer()) {
            Connection conn = null;
            try {
                conn = DbPool.getConnection();
                ScheduledTaskManager.cancel(conn, DataSourceTask.class.getName(), ds.getId(), mbox.getId(), false);
                if (ds.isScheduled()) {
                    DataSourceTask task = new DataSourceTask(mbox.getId(), accountId, dsId, ds.getPollingInterval());
                    ZimbraLog.datasource.debug("Scheduling %s", task);
                    ScheduledTaskManager.schedule(conn, task);
                }
                conn.commit();
            } catch (ServiceException e) {
                ZimbraLog.datasource.warn("Unable to schedule data source %s", ds.getName(), e);
                DbPool.quietRollback(conn);
            } finally {
                DbPool.quietClose(conn);
            }
        }
    }
}

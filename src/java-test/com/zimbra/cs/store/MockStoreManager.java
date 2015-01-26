/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014 Zimbra, Inc.
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
package com.zimbra.cs.store;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.output.ByteArrayOutputStream;

import com.google.common.io.ByteStreams;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * Mock implementation of {@link StoreManager}.
 *
 * @author jylee
 * @author ysasaki
 */
public final class MockStoreManager extends StoreManager {

    private final Map<String, MockMailboxBlob> blobs = new HashMap<String, MockMailboxBlob>();

    public MockStoreManager() {
//        DebugConfig.disableMessageStoreFsync = true;
    }

    @Override
    public void startup() throws IOException {
        purge();

        FileUtil.deleteDir(MockLocalBlob.tmpdir);
        FileUtil.ensureDirExists(MockLocalBlob.tmpdir);
        BlobInputStream.setFileDescriptorCache(new FileDescriptorCache(null));
    }

    @Override
    public void shutdown() {
        purge();

        BlobInputStream.setFileDescriptorCache(null);
    }

    @Override
    public boolean supports(StoreFeature feature) {
        switch (feature) {
            case BULK_DELETE:
                return false;
            case CENTRALIZED:
                return false;
            case SINGLE_INSTANCE_SERVER_CREATE:
                return false;
            default:
                return false;
        }
    }

    public void purge() {
        blobs.clear();
    }

    public int size() {
        return blobs.size();
    }

    @Override
    public BlobBuilder getBlobBuilder() {
        return new MockBlobBuilder();
    }

    @Override
    public Blob storeIncoming(InputStream data, boolean storeAsIs) throws IOException {
        return new MockBlob(ByteStreams.toByteArray(data));
    }

    @Override
    public StagedBlob stage(InputStream data, long actualSize, Mailbox.MailboxData mboxData) throws IOException {
        return new MockStagedBlob(mboxData,  ByteStreams.toByteArray(data));
    }

    @Override
    public StagedBlob stage(Blob blob, Mailbox.MailboxData mboxData) {
        return new MockStagedBlob(mboxData,  ((MockBlob) blob).content);
    }

    private String blobKey(int mailboxId, int itemId, int revision) {
        return mailboxId + "-" + itemId + "-" + revision;
    }

    @Override
    public MailboxBlob copy(MailboxBlob src, Mailbox.MailboxData destMailboxData, int destItemId, int destRevision) {
        MockMailboxBlob blob = new MockMailboxBlob(destMailboxData, destItemId, destRevision, src.getLocator(), ((MockMailboxBlob) src).content);
        blobs.put(blobKey(destMailboxData.id, destItemId, destRevision), blob);
        return blob;
    }

    @Override
    public MailboxBlob link(StagedBlob src, Mailbox.MailboxData destMailboxData, int destItemId, int destRevision) {
        MockMailboxBlob blob = new MockMailboxBlob(destMailboxData, destItemId, destRevision, src.getLocator(), ((MockStagedBlob) src).content);
        blobs.put(blobKey(destMailboxData.id, destItemId, destRevision), blob);
        return blob;
    }

    @Override
    public MailboxBlob renameTo(StagedBlob src, Mailbox.MailboxData destMailboxData, int destItemId, int destRevision) {
        MockMailboxBlob blob = new MockMailboxBlob(destMailboxData, destItemId, destRevision, src.getLocator(), ((MockStagedBlob) src).content);
        blobs.put(blobKey(destMailboxData.id, destItemId, destRevision), blob);
        return blob;
    }

    @Override
    public boolean delete(Blob blob) throws IOException {
        if (blob instanceof MockLocalBlob) {
            File file = blob.getFile();
            if (file != null) {
                ZimbraLog.store.debug("Deleting %s.", file.getPath());
                BlobInputStream.getFileDescriptorCache().remove(file.getPath()); // Prevent stale cache read.
                boolean deleted = file.delete();
                if (deleted) {
                    return true;
                }
                if (!file.exists()) {
                    // File wasn't there to begin with.
                    return false;
                }
                throw new IOException("Unable to delete blob file " + file.getAbsolutePath());
            }
        }
        return true;
    }

    @Override
    public boolean delete(StagedBlob staged) {
        return true;
    }

    @Override
    public boolean delete(MailboxBlob mblob) throws IOException {
        blobs.remove(blobKey(mblob.getMailboxId(), mblob.getItemId(), mblob.getRevision()));
        delete(((MockMailboxBlob) mblob).blob);
        return true;
    }

    @Override
    public MailboxBlob getMailboxBlob(Mailbox.MailboxData mboxData, int itemId, int revision, String locator) {
        return blobs.get(blobKey(mboxData.id, itemId, revision));
    }

    @Override
    public InputStream getContent(MailboxBlob mblob) throws IOException {
        return mblob.getLocalBlob().getInputStream();
    }

    @Override
    public InputStream getContent(Blob blob) throws IOException {
        return blob.getInputStream();
    }

    @Override
    public boolean deleteStore(Mailbox.MailboxData mboxData, Iterable<MailboxBlob.MailboxBlobInfo> mblobs) throws IOException {
        assert mblobs != null : "we require a blob iterator for testing purposes";
        for (MailboxBlob.MailboxBlobInfo mbinfo : mblobs) {
            delete(getMailboxBlob(mboxData,  mbinfo.itemId, mbinfo.revision, mbinfo.locator));
        }
        return true;
    }

    private static final class MockBlob extends Blob {
        byte[] content;

        MockBlob() {
            super(new File("build/test/store"));
            content = new byte[0];
        }

        void setContent(byte[] content) {
            this.content = content;
        }

        MockBlob(byte[] data) {
            super(new File("build/test/store"));
            content = data;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(content);
        }

        @Override
        public long getRawSize() {
            return content.length;
        }
    }

    private static final class MockLocalBlob extends Blob {
        static final File tmpdir = new File("build/test/store");
        private final int length;

        public MockLocalBlob(byte[] content) throws IOException {
            super(writeToTemp(content));
            length = content.length;
        }

        private static File writeToTemp(byte[] content) throws IOException {
            File blob = File.createTempFile("mlblob", ".msg", tmpdir);
            blob.deleteOnExit();

            FileOutputStream fos = new FileOutputStream(blob);
            fos.write(content);
            fos.close();

            return blob;
        }

        @Override
        public long getRawSize() {
            return length;
        }
    }

    private static final class MockStagedBlob extends StagedBlob {
        final byte[] content;

        MockStagedBlob(Mailbox.MailboxData mboxData, byte[] data) {
            super(mboxData,  String.valueOf(data.length), data.length);
            content = data;
        }

        @Override
        public String getLocator() {
            return "0";
        }
    }

    @SuppressWarnings("serial")
    private static final class MockMailboxBlob extends MailboxBlob {
        final byte[] content;
        MockLocalBlob blob = null;

        MockMailboxBlob(Mailbox.MailboxData mboxData, int itemId, int revision, String locator, byte[] data) {
            super(mboxData,  itemId, revision, locator);
            content = data;
        }

        @Override
        public Blob getLocalBlob() throws IOException {
            return blob == null ? blob = new MockLocalBlob(content) : blob;
        }
    }

    private static final class MockBlobBuilder extends BlobBuilder {
        private ByteArrayOutputStream out;

        protected MockBlobBuilder() {
            super(new MockBlob());
        }

        @Override
        protected OutputStream createOutputStream(File file) throws FileNotFoundException {
            assert out == null : "Output stream already created";
            out = new ByteArrayOutputStream();
            return out;
        }

        @Override
        protected FileChannel getFileChannel() {
            return null;
        }

        @Override
        public Blob finish() throws IOException, ServiceException {
            MockBlob mockblob = (MockBlob) super.finish();

            if (out != null) {
                mockblob.setContent(out.toByteArray());
                out = null;
            }

            return mockblob;
        }
    }
}

/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.apache.bookkeeper.bookie;

import io.netty.buffer.ByteBuf;
import java.io.IOException;
import java.util.Arrays;
import java.util.Observable;
import java.util.Observer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements a ledger inside a bookie. In particular, it implements operations
 * to write entries to a ledger and read entries from a ledger.
 *
 */
public class LedgerDescriptorImpl extends LedgerDescriptor {
    private final static Logger LOG = LoggerFactory.getLogger(LedgerDescriptor.class);
    final LedgerStorage ledgerStorage;
    private long ledgerId;

    final byte[] masterKey;

    LedgerDescriptorImpl(byte[] masterKey, long ledgerId, LedgerStorage ledgerStorage) {
        this.masterKey = masterKey;
        this.ledgerId = ledgerId;
        this.ledgerStorage = ledgerStorage;
    }

    @Override
    void checkAccess(byte masterKey[]) throws BookieException, IOException {
        if (!Arrays.equals(this.masterKey, masterKey)) {
            LOG.error("[{}] Requested master key {} does not match the cached master key {}", new Object[] {
                    this.ledgerId, Arrays.toString(masterKey), Arrays.toString(this.masterKey) });
            throw BookieException.create(BookieException.Code.UnauthorizedAccessException);
        }
    }

    @Override
    public long getLedgerId() {
        return ledgerId;
    }

    @Override
    boolean setFenced() throws IOException {
        return ledgerStorage.setFenced(ledgerId);
    }

    @Override
    boolean isFenced() throws IOException {
        return ledgerStorage.isFenced(ledgerId);
    }

    @Override
    void setExplicitLac(ByteBuf lac) throws IOException {
        ledgerStorage.setExplicitlac(ledgerId, lac);
    }

    @Override
    ByteBuf getExplicitLac() {
        return ledgerStorage.getExplicitLac(ledgerId);
    }

    @Override
    long addEntry(ByteBuf entry) throws IOException {
        long ledgerId = entry.getLong(entry.readerIndex());

        if (ledgerId != this.ledgerId) {
            throw new IOException("Entry for ledger " + ledgerId + " was sent to " + this.ledgerId);
        }

        return ledgerStorage.addEntry(entry);
    }

    @Override
    ByteBuf readEntry(long entryId) throws IOException {
        return ledgerStorage.getEntry(ledgerId, entryId);
    }

    @Override
    long getLastAddConfirmed() throws IOException {
        return ledgerStorage.getLastAddConfirmed(ledgerId);
    }

    @Override
    Observable waitForLastAddConfirmedUpdate(long previoisLAC, Observer observer) throws IOException {
        return ledgerStorage.waitForLastAddConfirmedUpdate(ledgerId, previoisLAC, observer);
    }
}

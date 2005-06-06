/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.fs.local;

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.HashSet;
import java.io.InputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * This Class implements a very simple open handle monitor for the local
 * file system. This is usefull, if the list of open handles, referenced by
 * an open FileInputStream() should be tracked. This can cause problems on
 * windows filesystems where open files cannot be deleted.
 */
public class HandleMonitor {

    /**
     * The default logger
     */
    private static Logger log = Logger.getLogger(HandleMonitor.class);

    /**
     * the map of open handles (key=File, value=Handle)
     */
    private HashMap openHandles = new HashMap();

    /**
     * Opens a file and returns an InputStream
     *
     * @param file
     * @return
     * @throws FileNotFoundException
     */
    public InputStream open(File file) throws FileNotFoundException {
        Handle handle = getHandle(file);
        InputStream in = handle.open();
        return in;
    }

    /**
     * Checks, if the file is open
     * @param file
     * @return
     */
    public boolean isOpen(File file) {
        return openHandles.containsKey(file);
    }

    /**
     * Closes a file
     * @param file
     */
    private void close(File file) {
        openHandles.remove(file);
    }

    /**
     * Returns the handle for a file.
     * @param file
     * @return
     */
    private Handle getHandle(File file) {
        Handle handle = (Handle) openHandles.get(file);
        if (handle == null) {
            handle = new Handle(file);
            openHandles.put(file, handle);
        }
        return handle;
    }

    /**
     * Dumps the contents of this monitor
     */
    public void dump() {
        log.info("Number of open files: " + openHandles.size());
        Iterator iter = openHandles.keySet().iterator();
        while (iter.hasNext()) {
            File file = (File) iter.next();
            Handle handle = (Handle) openHandles.get(file);
            handle.dump();
        }
    }

    /**
     * Dumps the information for a file
     * @param file
     */
    public void dump(File file) {
        Handle handle = (Handle) openHandles.get(file);
        if (handle != null) {
            handle.dump(true);
        }
    }

    /**
     * Class representing all open handles to a file
     */
    private class Handle {

        /**
         * the file of this handle
         */
        private File file;

        /**
         * all open streams of this handle
         */
        private HashSet streams = new HashSet();

        /**
         * Creates a new handle for a file
         * @param file
         */
        public Handle(File file) {
            this.file = file;
        }

        /**
         * opens a stream for this handle
         * @return
         * @throws FileNotFoundException
         */
        public InputStream open() throws FileNotFoundException {
            Handle.MonitoredInputStream in = new Handle.MonitoredInputStream(file);
            streams.add(in);
            return in;
        }

        /**
         * Closes a stream
         * @param in
         */
        public void close(MonitoredInputStream in) {
            streams.remove(in);
            if (streams.isEmpty()) {
                HandleMonitor.this.close(file);
            }
        }

        /**
         * Dumps this handle
         */
        public void dump() {
            dump(false);
        }

        /**
         * Dumps this handle
         */
        public void dump(boolean detailed) {
            if (detailed) {
                log.info("- " + file.getPath() + ", " + streams.size());
                Iterator iter = streams.iterator();
                while (iter.hasNext()) {
                    Handle.MonitoredInputStream in = (Handle.MonitoredInputStream) iter.next();
                    in.dump();
                }
            } else {
                log.info("- " + file.getPath() + ", " + streams.size());
            }
        }

        /**
         * Delegating input stream that registers/unregisters itself from the
         * handle.
         */
        private class MonitoredInputStream extends InputStream {

            /**
             * the underlying input stream
             */
            private final FileInputStream in;

            /**
             * throwable of the time, the stream was created
             */
            private final Throwable throwable;

            /**
             * Creates a new stream
             * @param file
             * @throws FileNotFoundException
             */
            public MonitoredInputStream(File file) throws FileNotFoundException {
                in = new FileInputStream(file);
                try {
                    throw new Exception();
                } catch (Exception e) {
                    throwable = e;
                }
            }

            /**
             * dumps this stream
             */
            public void dump() {
                log.info("- opened by : ", throwable);
            }

            /**
             * {@inheritDoc}
             */
            public int available() throws IOException {
                return in.available();
            }

            /**
             * {@inheritDoc}
             */
            public void close() throws IOException {
                in.close();
                Handle.this.close(this);
            }

            /**
             * {@inheritDoc}
             */
            public synchronized void reset() throws IOException {
                in.reset();
            }

            /**
             * {@inheritDoc}
             */
            public boolean markSupported() {
                return in.markSupported();
            }

            /**
             * {@inheritDoc}
             */
            public synchronized void mark(int readlimit) {
                in.mark(readlimit);
            }

            /**
             * {@inheritDoc}
             */
            public long skip(long n) throws IOException {
                return in.skip(n);
            }

            /**
             * {@inheritDoc}
             */
            public int read(byte b[]) throws IOException {
                return in.read(b);
            }

            /**
             * {@inheritDoc}
             */
            public int read(byte b[], int off, int len) throws IOException {
                return in.read(b, off, len);
            }

            /**
             * {@inheritDoc}
             */
            public int read() throws IOException {
                return in.read();
            }
        }
    }

}

/*****************************************************************
Archiver

Copyright (C) 2011-2013 The National Center for Telehealth and 
Technology

Eclipse Public License 1.0 (EPL-1.0)

This library is free software; you can redistribute it and/or
modify it under the terms of the Eclipse Public License as
published by the Free Software Foundation, version 1.0 of the 
License.

The Eclipse Public License is a reciprocal license, under 
Section 3. REQUIREMENTS iv) states that source code for the 
Program is available from such Contributor, and informs licensees 
how to obtain it in a reasonable manner on or through a medium 
customarily used for software exchange.

Post your updates and modifications to our GitHub or email to 
t2@tee2.org.

This library is distributed WITHOUT ANY WARRANTY; without 
the implied warranty of MERCHANTABILITY or FITNESS FOR A 
PARTICULAR PURPOSE.  See the Eclipse Public License 1.0 (EPL-1.0)
for more details.
 
You should have received a copy of the Eclipse Public License
along with this library; if not, 
visit http://www.opensource.org/licenses/EPL-1.0

*****************************************************************/
package com.t2.dataouthandlertest;

import android.content.Context;
import android.text.TextUtils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * @internal
 * @class Archiver Utility class used to archive/unarchive objects.  This implementation serializes the object
 * to a binary format and saves to the protected storage area (disk).
 */
public final class Archiver {
    private static final String TAG = Archiver.class.getSimpleName();
    // token used to separate parts of file name
    private static final String DICTIONARY_FILENAME_SEPARATOR = "~";
    // prefix + dictionary name
    private static final String DICTIONARY_FILENAME_BASE_FORMAT = "dict" + DICTIONARY_FILENAME_SEPARATOR +
            "%s";

    private Archiver() {
    }

    /**
     * Saves (archives) the specified object to the local (protected) file system.
     *
     * @param name    The name the object will be saved as on disk.  This parameter cannot be null.
     * @param object  The object to be saved.
     * @param context A context to access the filesystem from.
     * @throws IllegalArgumentException if the name parameter is null.
     */
    public static void asyncSave(final String name, final Object object, final Context context) {
        if (TextUtils.isEmpty(name)) throw new IllegalArgumentException("name parameter cannot be null");

        ThreadUtils.executeInBg(new Runnable() {
            public void run() {
                String fileName = String.format(DICTIONARY_FILENAME_BASE_FORMAT, name);

                // purge existing file
                context.deleteFile(fileName);

                FileOutputStream fos = null;
                ObjectOutputStream oos = null;
                try {
                    fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
                    oos = new ObjectOutputStream(fos);
                    oos.writeObject(object);
                    oos.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    try {
                        if (oos != null) {
                            oos.close();
                        }
                    } catch (IOException ignore) {
                    }

                    try {
                        if (fos != null) {
                            fos.close();
                        }
                    } catch (IOException ignore) {
                    }
                }
            }
        });
    }

    /**
     * Loads (unarchives) the specified object from the local (protected) file system.
     *
     * @param name    The name of the object to be loaded from disk.  This parameter cannot be null.
     * @param context The Context used to access the file system.
     * @return The object if found and loaded, null otherwise.
     * @throws IllegalArgumentException if the name parameter is null.
     * @throws IllegalStateException    if context is null.
     * @throws LoadException            if there was any problem loading the object. (Including SUID
     *                                  mismatches, IO exceptions, de-serialization exceptions, cosmic rays,
     *                                  et cetera.)
     */
    @SuppressWarnings("unchecked")
    public static <T> T load(String name, Context context) throws LoadException {
        String fileName = String.format(DICTIONARY_FILENAME_BASE_FORMAT, name);

        FileInputStream fis = null;
        ObjectInputStream ois = null;
        try {
            fis = context.openFileInput(fileName);
            ois = new ObjectInputStream(fis);
            return (T) ois.readObject();
        } catch (IOException e) {
            throw new LoadException(e);
        } catch (ClassNotFoundException e) {
            throw new LoadException(e);
        } finally {
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException ignore) {
                }
            }

            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    public static void delete(Context context, String name) {
        String fileName = String.format(DICTIONARY_FILENAME_BASE_FORMAT, name);
        context.deleteFile(fileName);

    }

    public static class LoadException extends Exception {
        public LoadException(String detailMessage) {
            super(detailMessage);
        }

        public LoadException(Throwable throwable) {
            super(throwable);
        }
    }
}
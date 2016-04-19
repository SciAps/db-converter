package com.sciaps.data;


import com.devsmart.IOUtils;
import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.sciaps.common.data.LIBZTest;
import com.sciaps.common.data.Model;
import com.sciaps.common.data.Region;
import com.sciaps.common.data.Standard;
import com.sciaps.common.objtracker.IdRefTypeAdapterFactory;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class SDBFile {

    private static final Logger logger = LoggerFactory.getLogger(SDBFile.class);

    public static final Type MapType = new TypeToken<Map<String, Object>>(){}.getType();

    private static final Pattern DBOBJ_REGEX = Pattern.compile("dbobj/([^/]*)/(.*).json");
    private static final Pattern SPECTRUM_FILE_REGEX = Pattern.compile("spectrum/(.*).gz");
    private static final Pattern FPLIB_FILE_REGEX = Pattern.compile("fplib/(.*).json");

    public static final Gson ZipGson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .serializeSpecialFloatingPointValues()
            .create();

    public static final Gson TypeGson = new GsonBuilder()
            .registerTypeAdapterFactory(new IdRefTypeAdapterFactory())
            .serializeNulls()
            .serializeSpecialFloatingPointValues()
            .create();

    public static final Comparator<DBEntry> KEY_COMPARATOR = new Comparator<DBEntry>() {
        @Override
        public int compare(DBEntry a, DBEntry b) {
            return a.key.compareTo(b.key);
        }
    };


    public static class DBEntry {
        public String key;
        public String type;
        public JsonElement value;

        public DBEntry(String key) {
            this.key = key;
        }

        public DBEntry(DBEntry entry) {
            this.key = entry.key;
            this.type = entry.type;
            this.value = entry.value;
        }

        public void writeToZip(ZipOutputStream zipOut) throws IOException {
            final String fileName = String.format("dbobj/%s/%s.json", type, key);
            ZipEntry entry = new ZipEntry(fileName);
            zipOut.putNextEntry(entry);

            final OutputStreamWriter writer = new OutputStreamWriter(zipOut, Charsets.UTF_8);
            ZipGson.toJson(value, writer);
            writer.flush();
            zipOut.closeEntry();

        }


        @Override
        public String toString() {
            //return Objects.toStringHelper(this)
            //        .toString();
            return this.toString();
        }
    }

    private DB mMemDB = DBMaker.newTempFileDB()
            .transactionDisable()
            .deleteFilesAfterClose()
            .make();
    protected final BTreeMap<String, byte[]> mSpectrumTable;
    private TreeSet<DBEntry> mDB = new TreeSet<DBEntry>(KEY_COMPARATOR);

    List<Standard> mStandards;
    List<Model> mModels;
    List<LIBZTest> mTests;
    List<Region> mRegions;

    public Iterator<DBEntry> getAll() {
        return mDB.iterator();
    }

    private class TypeIterator implements Iterator<DBEntry> {

        private final String mType;
        private final Iterator<DBEntry> mIt;
        private DBEntry mNextEntry;

        public TypeIterator(String type) {
            mType = type;
            mIt = mDB.iterator();
        }

        @Override
        public boolean hasNext() {
            while(mIt.hasNext()) {
                DBEntry entry = mIt.next();
                if(entry.type != null && entry.type.equals(mType)){
                    mNextEntry = entry;
                    return true;
                }

            }
            return false;
        }

        @Override
        public DBEntry next() {
            DBEntry retval = mNextEntry;
            if(retval == null) {
                throw new RuntimeException();
            }
            mNextEntry = null;
            return retval;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public Iterator<DBEntry> getAllOfType(final String type) {
        return new TypeIterator(type);
    }

    public DBEntry get(String id) {
        DBEntry retval = mDB.floor(new DBEntry(id));
        return retval;
    }

    public byte[] getSpectrum(String id) {
        return mSpectrumTable.get(id);
    }


    public SDBFile() {
        mSpectrumTable = mMemDB.createTreeMap("spectrum")
                .makeStringMap();

    }

    public void load(ZipInputStream zipIn) throws IOException {
        ZipEntry entry = null;
        while((entry = zipIn.getNextEntry()) != null) {
            final String name = entry.getName();

            Matcher m = DBOBJ_REGEX.matcher(name);
            if(m.find()) {
                String type = m.group(1);
                String id = m.group(2);

                logger.info("loading dbobj {} : {}", type, id);

                InputStreamReader in = new InputStreamReader(zipIn, Charsets.UTF_8);

                DBEntry dbEntry = new DBEntry(id);
                dbEntry.type = type;
                dbEntry.value = ZipGson.fromJson(in, JsonElement.class);

                if(mDB.contains(dbEntry)) {
                    throw new RuntimeException("db already contains obj with id: " + dbEntry.key);
                }

                mDB.add(dbEntry);

                continue;
            }


            m = SPECTRUM_FILE_REGEX.matcher(name);
            if(m.find()) {
                String id = m.group(1);
                logger.info("loading spectrum file: {}", id);

                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                IOUtils.pump(zipIn, bout, false, true);
                mSpectrumTable.put(id, bout.toByteArray());

                continue;
            }

            logger.warn("unknown file: {}", name);


        }
    }


}

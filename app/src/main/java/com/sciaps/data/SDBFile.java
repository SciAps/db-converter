package com.sciaps.data;


import com.devsmart.IOUtils;
import com.devsmart.microdb.MicroDB;
import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.sciaps.Utils.DBObjectConverter;
import com.sciaps.common.data.LIBZTest;
import com.sciaps.common.data.Model;
import com.sciaps.common.data.Region;
import com.sciaps.common.data.Standard;
import com.sciaps.common.objtracker.IdRefTypeAdapterFactory;
import com.sciaps.common.spectrum.LIBZPixelSpectrum;
import com.sciaps.common.utils.MultiShotSpectrumFileInputStream;
import com.sciaps.common.utils.ShotDataHelper;
import org.apache.commons.lang.ArrayUtils;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
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

    //public byte[] getSpectrum(String id) {
    //   return mSpectrumTable.get(id);
    //}


    private final int MAX_STORE_SPECTRA_SIZE = 1000;
    private ArrayList<String> mProcessedSpectra = new ArrayList<String>();
    private String mSdbFilePath;

    public SDBFile(String sdbFilePath) {
        mSdbFilePath = sdbFilePath;
        mSpectrumTable = mMemDB.createTreeMap("spectrum")
                .makeStringMap();

    }

    // Loading all dbobject in memory
    public void loadDBObjects() throws IOException {

        InputStream theFile = new FileInputStream(mSdbFilePath);
        ZipInputStream zipIn = new ZipInputStream(theFile);

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
            }


            m = SPECTRUM_FILE_REGEX.matcher(name);
            if(m.find()) {
                String id = m.group(1);
                //logger.info("loading spectrum file: {}", id);

                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                IOUtils.pump(zipIn, bout, false, true);
                mSpectrumTable.put(id, bout.toByteArray());

                continue;
            }

            logger.warn("unknown file: {}", name);
        }

        zipIn.close();
    }

    // Read the sdb and return the spectrum data
    public byte[] loadSpectrumByID(final String spectrumFileID) throws IOException {
        InputStream theFile = new FileInputStream(mSdbFilePath);
        ZipInputStream zipIn = new ZipInputStream(theFile);

        ZipEntry entry = null;
        while ((entry = zipIn.getNextEntry()) != null) {
            final String name = entry.getName();

            Matcher m = SPECTRUM_FILE_REGEX.matcher(name);
            if (m.find()) {
                String id = m.group(1);

                if (id.compareTo(spectrumFileID) == 0) {
                    logger.info("loading spectrum file: {}", id);

                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    IOUtils.pump(zipIn, bout, false, true);

                    byte[] data = bout.toByteArray();
                    zipIn.close();

                    mProcessedSpectra.add(spectrumFileID);
                    return data;
                }
            }
        }

        return null;
    }

    // Read the sdb file, reads all test object and save it to the new microDB
    public void loadAndConvertTestSpectra(final MicroDB db, final DBObjectConverter dbObjectConverter) throws IOException {
        InputStream theFile = new FileInputStream(mSdbFilePath);
        ZipInputStream zipIn = new ZipInputStream(theFile);

        final Map<UUID, ArrayList<String>> testShotFileIDMap = dbObjectConverter.getTestShotFileIDMap();

        byte[] data;
        ArrayList<LIBZPixelSpectrum> spectra = new ArrayList<LIBZPixelSpectrum>();
        SpectraData[] newSpectraData;
        SpectraData[] orgSpectraData;
        SpectraData[] concatedData;
        LIBZPixelSpectrum spectrum;
        ByteArrayInputStream bin;
        MultiShotSpectrumFileInputStream multiShotIn;
        ByteArrayOutputStream bout;

        int cnt = 0;
        ZipEntry entry = null;
        while ((entry = zipIn.getNextEntry()) != null) {
            final String name = entry.getName();

            Matcher m = SPECTRUM_FILE_REGEX.matcher(name);
            if (m.find()) {
                cnt ++;

                String id = m.group(1);

                    logger.info("loading spectrum file: {}", id);

                    bout = new ByteArrayOutputStream();
                    IOUtils.pump(zipIn, bout, false, true);

                    data = bout.toByteArray();

                    spectra.clear();

                    // old format
                    try {
                        spectrum = ShotDataHelper.loadCompressed(new ByteArrayInputStream(data));
                        spectra.add(spectrum);

                    } catch (ZipException ze) {
                        // new format

                        bin = new ByteArrayInputStream(data);
                        multiShotIn = new MultiShotSpectrumFileInputStream(bin);

                        multiShotIn.seekTo(0);
                        spectrum = multiShotIn.getNextShot();
                        while (spectrum != null) {
                            spectra.add(spectrum);
                            spectrum = multiShotIn.getNextShot();
                        }
                        multiShotIn.close();
                    }

                if (spectra.size() != 0) {

                    // Lookup the test of this spectrum and add it
                    for (Map.Entry<UUID, ArrayList<String>> testShotFileIDEntry : testShotFileIDMap.entrySet()) {
                        for (String fileID : testShotFileIDEntry.getValue()) {

                            //Found the matching file id for the spectrum
                            if (fileID.compareTo(id) == 0) {

                                // Retrieve the acquisition obj from database and update the spectraData list
                                Acquisition acquisition = db.get(testShotFileIDEntry.getKey());

                                newSpectraData = new SpectraData[spectra.size()];

                                // Convert LIBZPixelSpectrum to SpectraData
                                for (int i = 0; i < spectra.size(); i++) {
                                    SpectraData spectraData = new SpectraData();
                                    dbObjectConverter.convertSpectrumToSpectraData(spectra.get(i), spectraData);
                                    newSpectraData[i] = spectraData;
                                }

                                // setting spectraData
                                orgSpectraData = acquisition.getSpectraData();
                                if (orgSpectraData == null) {
                                    acquisition.setSpectraData(newSpectraData);
                                } else {
                                    concatedData = (SpectraData[]) ArrayUtils.addAll(orgSpectraData, newSpectraData);
                                    acquisition.setSpectraData(concatedData);
                                }

                                if(cnt > 10) {
                                    db.flush();
                                    cnt = 0;
                                }
                            }
                        }
                    }

                }

            }
        }
    }


}

package com.sciaps.data;


import com.google.gson.*;
import com.sciaps.common.objtracker.DBObj;
import com.sciaps.common.objtracker.IdRefTypeAdapterFactory;
import com.sciaps.common.spectrum.LIBZPixelSpectrum;
import com.sciaps.common.spectrum.Spectrum;
import com.sciaps.common.utils.MultiShotSpectrumFileInputStream;
import com.sciaps.common.utils.ShotDataHelper;
import org.mapdb.BTreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class LIBZDB {

    static Logger logger = LoggerFactory.getLogger(LIBZDB.class);

    private final TreeMap<String, JsonElement> mAllObjects = new TreeMap<String, JsonElement>();
    private final TreeMap<String, DBObj> mLiveInstances = new TreeMap<String, DBObj>();
    private final DBDataMarshaller mMarshaller;
    private BTreeMap<String, byte[]> mSpectrumTable;

    private class DBDataMarshaller implements DBObj.IdLookup, DBObj.ObjLoader {

        private final Gson mGson;

        DBDataMarshaller() {
            mGson = new GsonBuilder()
                    .registerTypeAdapterFactory(new IdRefTypeAdapterFactory())
                    .serializeNulls()
                    .serializeSpecialFloatingPointValues()
                    .create();
        }

        @Override
        public String getId(Object obj) {
            if (obj instanceof DBObj) {
                return ((DBObj) obj).mId;
            } else {
                return null;
            }
        }

        @Override
        public Object load(String id, Class<?> type) {
            if (id == null) {
                return null;
            }
            DBObj retval = mLiveInstances.get(id);
            if (retval == null) {
                JsonElement doc = mAllObjects.get(id);
                retval = (DBObj) mGson.fromJson(doc, type);
                retval.mId = id;
                retval.loadFields(this);
                mLiveInstances.put(id, retval);
            }
            return retval;
        }

        public <T extends DBObj> T deserialize(Class<T> objClass, JsonElement data) {
            T retval = mGson.fromJson(data, objClass);
            retval.loadFields(this);
            return retval;
        }
    }

    public LIBZDB() {
        mMarshaller = new DBDataMarshaller();

    }

    public void load(SDBFile file) {
        mAllObjects.clear();

        Iterator<SDBFile.DBEntry> it = file.getAll();
        while (it.hasNext()) {
            SDBFile.DBEntry entry = it.next();
            mAllObjects.put(entry.key, entry.value);

        }

        mSpectrumTable = file.mSpectrumTable;
    }

    public JsonObject getObject(String id) {
        JsonElement e = mAllObjects.get(id);
        if (e != null) {
            return e.getAsJsonObject();
        } else {
            return null;
        }
    }

    public Spectrum getAvgSpectrum(OrgLIBZTest test) throws IOException {
        JsonObject obj = getObject(test.mId);
        JsonObject shotTable = obj.getAsJsonObject("shotTable");

        String allId = shotTable.getAsJsonPrimitive("all").getAsString();
        ByteArrayInputStream bin = new ByteArrayInputStream(mSpectrumTable.get(allId));
        MultiShotSpectrumFileInputStream multiShotIn = new MultiShotSpectrumFileInputStream(bin);
        Spectrum retval = multiShotIn.getShot(-1);
        multiShotIn.close();
        return retval;
    }

    public ArrayList<LIBZPixelSpectrum> getSpectra(OrgLIBZTest test) throws IOException {

        JsonObject obj = getObject(test.mId);
        JsonObject shotTable = obj.getAsJsonObject("shotTable");
        JsonPrimitive all = shotTable.getAsJsonPrimitive("all");

        if (all != null) {
            return getSpectra(shotTable);
        } else {
            return getSpectraOldFormat(shotTable);
        }
    }

    private ArrayList<LIBZPixelSpectrum> getSpectra(JsonObject shotTable) throws IOException {
        ArrayList<LIBZPixelSpectrum> retval = new ArrayList<LIBZPixelSpectrum>();
        String allId = shotTable.getAsJsonPrimitive("all").getAsString();

        ByteArrayInputStream bin = new ByteArrayInputStream(mSpectrumTable.get(allId));
        MultiShotSpectrumFileInputStream multiShotIn = new MultiShotSpectrumFileInputStream(bin);

        multiShotIn.seekTo(0);
        LIBZPixelSpectrum spectrum = multiShotIn.getNextShot();
        while (spectrum != null) {
            retval.add(spectrum);
            spectrum = multiShotIn.getNextShot();
        }
        multiShotIn.close();
        return retval;
    }

    private ArrayList<LIBZPixelSpectrum> getSpectraOldFormat(JsonObject shotTable) throws IOException {
        ArrayList<LIBZPixelSpectrum> retval = new ArrayList<LIBZPixelSpectrum>();

        for (Map.Entry<String, JsonElement> entry : shotTable.entrySet()) {
            String key = entry.getKey();

            if (key.compareTo("shot_avg") != 0) {
                String id = shotTable.getAsJsonPrimitive(key).getAsString();
                LIBZPixelSpectrum spectrum = ShotDataHelper.loadCompressed(new ByteArrayInputStream(mSpectrumTable.get(id)));

                if (spectrum != null) {
                    retval.add(spectrum);
                }
            }
        }

        return retval;
    }

    public Iterable<OrgModel> getAllModels() {
        ArrayList<OrgModel> retval = new ArrayList<OrgModel>();
        for (Map.Entry<String, JsonElement> e : mAllObjects.entrySet()) {
            JsonObject obj = e.getValue().getAsJsonObject();
            if (obj.has("type") && "model".equals(obj.getAsJsonPrimitive("type").getAsString())) {
                OrgModel m = (OrgModel) mMarshaller.load(e.getKey(), OrgModel.class);
                for (OrgIRCurve c : m.irs.values()) {
                    c.loadFields(mMarshaller);
                }
                retval.add(m);
            }
        }

        return retval;
    }

    public Iterable<OrgModel2> getAllModels2() {
        ArrayList<OrgModel2> retval = new ArrayList<OrgModel2>();
        for (Map.Entry<String, JsonElement> e : mAllObjects.entrySet()) {
            JsonObject obj = e.getValue().getAsJsonObject();
            if (obj.has("type") && "model".equals(obj.getAsJsonPrimitive("type").getAsString())) {
                OrgModel2 m = (OrgModel2) mMarshaller.load(e.getKey(), OrgModel2.class);

                for (java.util.List<OrgIRCurve> irCurves : m.irs.values()) {
                    for (OrgIRCurve irCurve : irCurves) {
                        irCurve.loadFields(mMarshaller);
                    }
                }
                retval.add(m);
            }
        }

        return retval;
    }

    public Iterable<OrgLIBZTest> getAllTests() {
        ArrayList<OrgLIBZTest> retval = new ArrayList<OrgLIBZTest>();
        for (Map.Entry<String, JsonElement> e : mAllObjects.entrySet()) {
            JsonObject obj = e.getValue().getAsJsonObject();
            if (obj.has("type") && "test".equals(obj.getAsJsonPrimitive("type").getAsString())) {
                OrgLIBZTest t = (OrgLIBZTest) mMarshaller.load(e.getKey(), OrgLIBZTest.class);
                retval.add(t);
            }
        }
        return retval;
    }

    public Iterable<OrgStandard> getAllStandards() {
        ArrayList<OrgStandard> retval = new ArrayList<OrgStandard>();
        for (Map.Entry<String, JsonElement> e : mAllObjects.entrySet()) {
            JsonObject obj = e.getValue().getAsJsonObject();
            if (obj.has("type") && "standard".equals(obj.getAsJsonPrimitive("type").getAsString())) {
                OrgStandard t = (OrgStandard) mMarshaller.load(e.getKey(), OrgStandard.class);
                retval.add(t);
            }
        }
        return retval;
    }

    public Iterable<OrgFingerprintLibraryTemplate> getAllFingerprints() {
        ArrayList<OrgFingerprintLibraryTemplate> retval = new ArrayList<OrgFingerprintLibraryTemplate>();
        for (Map.Entry<String, JsonElement> e : mAllObjects.entrySet()) {
            JsonObject obj = e.getValue().getAsJsonObject();
            if (obj.has("type") && "fplib".equals(obj.getAsJsonPrimitive("type").getAsString())) {
                OrgFingerprintLibraryTemplate t = (OrgFingerprintLibraryTemplate) mMarshaller.load(e.getKey(), OrgFingerprintLibraryTemplate.class);
                retval.add(t);
            }
        }
        return retval;
    }

    public float getDatabaseVersion() {
        float version = -1;

        for (Map.Entry<String, JsonElement> e : mAllObjects.entrySet()) {
            JsonObject obj = e.getValue().getAsJsonObject();
            if (obj.has("schemaVersion") && obj.getAsJsonPrimitive("schemaVersion").getAsFloat() >= 0) {
                version = obj.getAsJsonPrimitive("schemaVersion").getAsFloat();
            }
        }
        return version;
    }

}

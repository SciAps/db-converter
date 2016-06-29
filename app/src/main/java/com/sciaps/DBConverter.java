package com.sciaps;

import com.devsmart.microdb.DBBuilder;
import com.devsmart.microdb.MicroDB;
import com.google.common.collect.Iterables;
import com.sciaps.Utils.DBObjectConverter;
import com.sciaps.data.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

/**
 * Created by jchen on 1/7/16.
 */
public class DBConverter {

    public interface DBConverterStatusInterface {
        public void processingState(String string);
    }

    static Logger logger = LoggerFactory.getLogger(DBConverter.class);
    public String mSummary;
    private DBConverterStatusInterface mCallback;

    public DBConverter(DBConverterStatusInterface callback) {
        mCallback = callback;
    }

    public DBConverter() {
        mCallback = null;
    }

    private void notifyCallback(String msg) {

        if (mCallback != null) {
            mCallback.processingState(msg);
        }
    }

    public void setCallback(DBConverterStatusInterface callback) {
        mCallback = callback;
    }

    public void doDBConvert(File dbFile) throws Exception {
        mSummary = "";

        logger.info(dbFile.getParent());
        logger.info("DB to convert: " + dbFile.getCanonicalPath());

        String defaultDBFileName = MainFrame.mFilePath + File.separator + "MicroDB";
        try {
            File dbfile = new File(defaultDBFileName);
            if (dbfile.exists()) {
                dbfile.delete();
                logger.info("deleted existing: " + defaultDBFileName);
            }

            File progressFile = new File(defaultDBFileName + ".p");
            if (progressFile.exists()) {
                progressFile.delete();
                logger.info("deleted existing: " + defaultDBFileName + ".p");
            }

            File transactionFile = new File(defaultDBFileName + ".t");
            if (transactionFile.exists()) {
                transactionFile.delete();
                logger.info("deleted existing: " + defaultDBFileName + ".t");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Failed to delete existing " + defaultDBFileName);
            logger.error("Error deleting existing database.");
            return;
        }

        notifyCallback("Loading DB Objects...");

        InputStream theFile = new FileInputStream(dbFile.getAbsoluteFile());
        ZipInputStream zipInputStream = new ZipInputStream(theFile);

        SDBFile sdbFile = new SDBFile();
        sdbFile.load(zipInputStream);

        LIBZDB libzdb = new LIBZDB();
        libzdb.load(sdbFile);

        File dbfile = new File(defaultDBFileName);
        MicroDB db = DBBuilder.builder().cacheSize(128).build(dbfile);
        DBObjectConverter dbObjectConverter = new DBObjectConverter();

        float dbVersion = libzdb.getDatabaseVersion();
        logger.info("Converting DB Version: " + dbVersion);

        // Order of db object reading
        // 1. Standards
        // 2. Tests
        // 3. Models
        // 4. Fingerprints
        // 5. Grade Library

        notifyCallback("Reading Standards...");
        logger.info("Reading Standards");
        Iterable<OrgStandard> standards = libzdb.getAllStandards();

        notifyCallback("Processing Standards...");
        logger.info("Processing Standards");
        int totalStandards = Iterables.size(standards);
        int cnt = 0;
        for (OrgStandard orgStandard : standards) {
            logger.info("   StanardName: " + orgStandard.name);
            Standard dbStandard = db.insert(Standard.class);
            dbObjectConverter.convertStandardToDBStandard(orgStandard, dbStandard);
            notifyCallback("Processing Standards..." + ++cnt + "/" + totalStandards);
        }

        notifyCallback("Reading Tests...");
        logger.info("Reading Tests");
        Iterable<OrgLIBZTest> tests = libzdb.getAllTests();

        notifyCallback("Processing Tests...");
        logger.info("Processing Tests");
        int totalTests = Iterables.size(tests);
        int totalTestFailed = 0;
        cnt = 0;
        for (OrgLIBZTest test : tests) {
            logger.info("    TestID: " + test.mId);
            Acquisition acquisition = db.insert(Acquisition.class);

            if (dbObjectConverter.convertLIBZTestToAcquisition(test, acquisition, libzdb) == false) {
                totalTestFailed++;
            }
            notifyCallback("Processing Tests..." + ++cnt + "/" + totalTests);
        }


        // dbVersion == -1 is older DB(no multicurves)
        int totalModels = 0;
        cnt = 0;
        if (dbVersion < 0) {
            notifyCallback("Reading Models...");
            logger.info("Reading Models");
            Iterable<OrgModel> models = libzdb.getAllModels();

            totalModels = Iterables.size(models);
            notifyCallback("Processing Models...");
            logger.info("Processing Models");
            for (OrgModel orgModel : models) {
                logger.info("    Models: " + orgModel.name);
                EmpiricalModel empiricalModel = db.insert(EmpiricalModel.class);
                dbObjectConverter.convertModelToEmpiricalModel(orgModel, empiricalModel);

                notifyCallback("Processing Models..." + ++cnt + "/" + totalModels);
            }
        } else {
            notifyCallback("Reading Models...");
            logger.info("Reading Models");
            Iterable<OrgModel2> models2 = libzdb.getAllModels2();

            totalModels = Iterables.size(models2);
            notifyCallback("Processing Models...");
            logger.info("Processing Models");
            for (OrgModel2 orgModel2 : models2) {
                logger.info("    Models: " + orgModel2.name);
                EmpiricalModel empiricalModel = db.insert(EmpiricalModel.class);
                dbObjectConverter.convertModel2ToEmpiricalModel(orgModel2, empiricalModel);

                notifyCallback("Processing Models..." + ++cnt + "/" + totalModels);
            }
        }

        // Because Template has a dependency on the type of Analyzer it is created from. It is not a good idea
        // to convert them, to prevent the template is used in a wrong type of analyzer
        //
        //
        //notifyCallback("Reading Fingerprints...");
        //logger.info("Reading Fingerprints");
        //Iterable<OrgFingerprintLibraryTemplate> fingerprints = libzdb.getAllFingerprints();
        //
        //notifyCallback("Processing Fingerprints...");
        //logger.info("Processing Fingerprints");
        //for (OrgFingerprintLibraryTemplate orgFingerprintLibraryTemplate : fingerprints) {
        //   logger.info("    FP: " + orgFingerprintLibraryTemplate.name);
        //    FingerprintLibTemplate dbFingerprintLibTemplate = db.insert(FingerprintLibTemplate.class);
        //    dbObjectConverter.convertFingerprintToDBFingerprint(orgFingerprintLibraryTemplate, dbFingerprintLibTemplate);
        //}

        notifyCallback("Writing to database...");
        db.flush();

        db.close();
        db.shutdown();

        logger.info("DBFile: " + dbfile.getName());
        File dbStorageFile = new File(defaultDBFileName + ".p");
        long bytes = dbStorageFile.length();

        int bytesPerMB = 1000000;
        if (bytes >= bytesPerMB) {
            logger.info("DB Size: " + (bytes / bytesPerMB) + " MB");
        } else {
            logger.info("DB Size: " + bytes + " bytes");
        }

        mSummary = String.format("Models: %d  Standards: %d   Tests: %d/%d",
                totalModels, totalStandards, totalTests - totalTestFailed, totalTests);
    }
}

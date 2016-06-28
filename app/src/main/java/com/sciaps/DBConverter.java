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
import java.io.IOException;

/**
 * Created by jchen on 1/7/16.
 */
public class DBConverter {

    public interface DBConverterStatusInterface {
        public void processingState(String string);
    }

    static Logger logger = LoggerFactory.getLogger(DBConverter.class);
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

        SDBFile sdbFile = new SDBFile(dbFile.getAbsolutePath());
        sdbFile.loadDBObjects();

        LIBZDB libzdb = new LIBZDB(sdbFile);
        libzdb.load();

        File dbfile = new File(defaultDBFileName);
        MicroDB db = DBBuilder.builder()
                .cacheSize(128)
                .build(dbfile);
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
        for (OrgStandard orgStandard : standards) {
            logger.info("   StanardName: " + orgStandard.name);
            Standard dbStandard = db.insert(Standard.class);
            dbObjectConverter.convertStandardToDBStandard(orgStandard, dbStandard);
        }

        notifyCallback("Reading Tests...");
        logger.info("Reading Tests");
        Iterable<OrgLIBZTest> tests = libzdb.getAllTests();

        notifyCallback("Processing Tests...");
        logger.info("Processing Tests");

        // Step 1
        // This step convert everything in a test except spectrum data
        dbObjectConverter.setTestShotFileIDMapSize(Iterables.size(tests));
        for (OrgLIBZTest test : tests) {
            logger.info("    TestID: " + test.mId);
            Acquisition acquisition = db.insert(Acquisition.class);
            if (dbObjectConverter.convertLIBZTestToAcquisition(test, acquisition, libzdb) == false) {
                //status = false;
            }
        }

        // Step 2
        // Convert spectrum data and assign to the test.
        //sdbFile.loadAndConvertTestSpectra(db, dbObjectConverter);


        // dbVersion == -1 is older DB(no multicurves)
        int totalModels = 0;
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

        db.flush();

        //doTestModel(db);
        //doTestAcquisition(db);
        //doStandardTest(db);

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

        System.out.println("TestCnt: " + dbObjectConverter.testCnt);
        System.out.println("Test Failed: " + dbObjectConverter.testFailed);

        System.out.println("Total Model:     " + totalModels);
        System.out.println("Total Standards: " + Iterables.size(standards));
        System.out.println("Total Tests:     " + Iterables.size(tests));

    }

    private void doTestModel(MicroDB db) {
        try {
            System.out.println("Retrieving Model");
            Iterable<EmpiricalModel> models = db.getAllOfType(EmpiricalModel.class);
            for (EmpiricalModel model : models) {

                System.out.println("************" + model.getName() + "\t Standard: " + model.getStandards().length + "\t curves: " + model.getIrCurves().length);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void doTestAcquisition(MicroDB db) {
        try {
            System.out.println("Retrieving test");
            Iterable<Acquisition> tests = db.getAllOfType(Acquisition.class);
            for (Acquisition test : tests) {

                if (test.getSpectraData() != null) {
                    System.out.println("************" + test.getSpectraData().length);
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void doStandardTest(MicroDB db) {
        try {
            System.out.println("Retrieving test");
            Iterable<Standard> standards = db.getAllOfType(Standard.class);

            System.out.println("# Standards = " + Iterables.size(standards));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}

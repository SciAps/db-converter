package com.sciaps.Utils;

import com.devsmart.StringUtils;
import com.devsmart.ubjson.UBArray;
import com.devsmart.ubjson.UBObject;
import com.devsmart.ubjson.UBValue;
import com.devsmart.ubjson.UBValueFactory;
import com.sciaps.common.AtomicElement;
import com.sciaps.common.algorithms.SGolayIntensity;
import com.sciaps.common.data.ChemValue;
import com.sciaps.common.spectrum.LIBZPixelSpectrum;
import com.sciaps.data.*;
import com.sciaps.datastructures.ArrayTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by jchen on 1/8/16.
 */
public class DBObjectConverter {

    static Logger logger = LoggerFactory.getLogger(DBObjectConverter.class);

    private DBObjectInventory mDBObjectInventory = new DBObjectInventory();

    public void convertSpectrumToSpectraData(LIBZPixelSpectrum spectrum, SpectraData spectraData) {
        DataHelper.LIBZSpectraDataBuilder builder = DataHelper.LIBZSpectraDataBuilder.builder();
        builder.knots(spectrum.knots, 0, spectrum.knots.length);

        for (int i = 0; i < 4; i++) {
            double[] coeff = spectrum.wlCalibrations[i].pixToNm.getCoefficients();
            builder.setPixToNmCoeff(i, coeff, 0, coeff.length);
            builder.setRawPixels(i, spectrum.pixels[i], 0, spectrum.pixels[i].length);
        }

        builder.build(spectraData);
    }

    public boolean convertLIBZTestToAcquisition(OrgLIBZTest test, Acquisition acquisition, LIBZDB libzdb) {
        boolean status = true;

        //Time
        acquisition.setTime(test.unixTime * 1000);

        //standard
        if (test.standard != null) {
            Standard dbStandard = mDBObjectInventory.getDBStandardByStandard(test.standard);
            if (dbStandard == null) {
                dbStandard = acquisition.getDB().insert(Standard.class);
                convertStandardToDBStandard(test.standard, dbStandard);
            }
            acquisition.setStandard(dbStandard);
        } else {
            acquisition.setStandard(null);
        }

        //Acquisition params
        LIBZAcquisitionParams params = new LIBZAcquisitionParams();
        params.setAcquisitionParams(test.config);
        acquisition.setAcquisitionParams(params.getParamUBObject());

        //Metadata
        UBObject metadata = UBValueFactory.createObject((TreeMap) test.metadata.clone());
        acquisition.setMetadata(metadata);

        // display name
        if (!StringUtils.isEmptyString(test.displayName)) {
            metadata.put("displayName", UBValueFactory.createValue(test.displayName));
        }
        //acquisition.setDisplayName(test.displayName);

        // Only Avg Save flag
        //acquisition.setOnlyAvgSaved(test.onlyAvgSaved);

        // get spectra data
        try {
            // Avg and each shot
            SpectraData[] spectraDatas = new SpectraData[libzdb.getSpectra(test).size()];

            //Avg
            //LIBZPixelSpectrum avgSpectrum = (LIBZPixelSpectrum) libzdb.getAvgSpectrum(test);
            //SpectraData avgSpectraData = acquisition.getDB().insert(SpectraData.class);
            //convertSpectrumToSpectraData(avgSpectrum, avgSpectraData);
            //spectraDatas[0] = avgSpectraData;
            //avgSpectrum.release();

            //Shot data
            int i = 0;
            ArrayList<LIBZPixelSpectrum> spectra = libzdb.getSpectra(test);
            for (LIBZPixelSpectrum spectrum : spectra) {
                SpectraData spectraData = acquisition.getDB().insert(SpectraData.class);
                convertSpectrumToSpectraData(spectrum, spectraData);
                spectraDatas[i] = spectraData;
                spectrum.release();
                i++;
            }

            acquisition.setSpectraData(spectraDatas);

        } catch (Exception e) {
            status = false;
            logger.error("Failed to get spectrum for test: " + test.mId);
            logger.error("This exception can be ok. ", e);
        }


        return status;
    }

    // Old single curve per element model
    public boolean convertModelToEmpiricalModel(OrgModel orgModel, EmpiricalModel empiricalModel) {
        boolean status = true;

        //Name
        empiricalModel.setName(orgModel.name);

        //int ModelType;
        empiricalModel.setModeType(orgModel.modelType);

        //AcquisitionParams
        LIBZAcquisitionParams acquisitionParams = new LIBZAcquisitionParams();
        UBObject libzAcquisitionParams = acquisitionParams.getParamUBObject();
        UBValue[] ubValues = new UBValue[1];
        ubValues[0] = libzAcquisitionParams;
        UBArray paramsArray = UBValueFactory.createArray(ubValues);
        empiricalModel.setAcquisitionParams(paramsArray);

        //Standard[]
        Standard[] standards = new Standard[orgModel.standardList.size()];
        int i = 0;
        for (OrgStandard orgStandard : orgModel.standardList) {
            Standard dbStandard = mDBObjectInventory.getDBStandardByStandard(orgStandard);
            if (dbStandard == null) {
                dbStandard = empiricalModel.getDB().insert(Standard.class);
                convertStandardToDBStandard(orgStandard, dbStandard);
            }

            standards[i] = dbStandard;
            i++;
        }
        empiricalModel.setStandards(standards);

        // IRCurve[] curves;
        int size = orgModel.irs.size();
        IRCurve[] irCurves = new IRCurve[size];
        int index = 0;
        for (Map.Entry<AtomicElement, OrgIRCurve> irCurve : orgModel.irs.entrySet()) {

            IRCurve dbirCurve = new IRCurve();
            dbirCurve.setPriority(0); // not multiple curves, not need to set priority
            convertIRCurveToDBIRCurve(irCurve.getValue(), dbirCurve);

            irCurves[index] = dbirCurve;
            index++;
        }
        empiricalModel.setIrCurves(irCurves);

        //Metadata
        UBObject metadata = UBValueFactory.createObject(new HashMap<String, UBValue>());
        empiricalModel.setMetadata(metadata);

        return status;
    }

    // Multi curves model
    public boolean convertModel2ToEmpiricalModel(OrgModel2 orgModel, EmpiricalModel empiricalModel) {
        boolean status = true;

        //Name
        empiricalModel.setName(orgModel.name);

        // Mode Type
        empiricalModel.setModeType(orgModel.modelType);

        //AcquisitionParams
        if (orgModel.rasterParams != null) {
            LIBZAcquisitionParams acquisitionParams = new LIBZAcquisitionParams();
            acquisitionParams.setAcquisitionParams(orgModel.rasterParams);
            UBObject ubObjAcquisitionParams = acquisitionParams.getParamUBObject();

            UBValue[] ubValues = new UBValue[1];
            ubValues[0] = ubObjAcquisitionParams;

            UBArray ubArray = UBValueFactory.createArray(ubValues);

            empiricalModel.setAcquisitionParams(ubArray);
        }

        //ModelType;
        empiricalModel.setModeType(orgModel.modelType);

        //LinkList<standard>
        Standard[] standards = new Standard[orgModel.standardList.size()];
        int i = 0;
        for (OrgStandard orgStandard : orgModel.standardList) {
            Standard dbStandard = mDBObjectInventory.getDBStandardByStandard(orgStandard);
            if (dbStandard == null) {
                dbStandard = empiricalModel.getDB().insert(Standard.class);
                convertStandardToDBStandard(orgStandard, dbStandard);
            }

            standards[i] = dbStandard;
            i++;
        }
        empiricalModel.setStandards(standards);

        // DBIRCurve[] curves;
        int priority = 0;
        int size = 0;
        for (List<OrgIRCurve> orgIRCurves : orgModel.irs.values()) {
            size += orgIRCurves.size();
        }
        IRCurve[] irCurves = new IRCurve[size];
        int index = 0;
        for (List<OrgIRCurve> orgIRCurves : orgModel.irs.values()) {

            priority = 0; // reset the curve priority within each element
            for (OrgIRCurve orgIRCurve : orgIRCurves) {

                IRCurve dbirCurve = new IRCurve();
                convertIRCurveToDBIRCurve(orgIRCurve, dbirCurve);
                dbirCurve.setPriority(priority);

                irCurves[index] = dbirCurve;
                index++;
                priority++;
            }
        }
        empiricalModel.setIrCurves(irCurves);

        // Metadata
        UBObject metadata = UBValueFactory.createObject(new HashMap<String, UBValue>());
        empiricalModel.setMetadata(metadata);

        return status;
    }

    // Convert old standard type to the new DB standard type
    public boolean convertStandardToDBStandard(OrgStandard orgStandard, Standard dbStandard) {

        boolean status = true;

        // String name
        dbStandard.setName(orgStandard.name);

        ArrayTable table = ArrayTable.createWithColumnTypes(int.class, double.class, double.class);

        for (ChemValue chemValue : orgStandard.spec) {
            table.addRow(chemValue.element.atomicNumber, chemValue.percent, chemValue.error);
        }
        dbStandard.setAssayTable(table);

        // int standardType
        dbStandard.setModeType(orgStandard.standardType);

        mDBObjectInventory.addStandardToDBStandardingMapping(orgStandard, dbStandard);

        return status;
    }

    public OrgIRRatio convertIRRatio2To1(OrgIRRatio2 orgIrRatio2) {
        OrgIRRatio irRatio = new OrgIRRatio();
        irRatio.name = orgIrRatio2.name;
        irRatio.element = orgIrRatio2.element;
        irRatio.numerator = new ArrayList<OrgRegion>(orgIrRatio2.numerator.size());
        irRatio.numerator.addAll(orgIrRatio2.numerator);
        irRatio.denominator = new ArrayList<OrgRegion>(orgIrRatio2.denominator.size());
        irRatio.denominator.addAll(orgIrRatio2.denominator);

        return irRatio;
    }

    public boolean convertIRRatio2ToDBIRRatio(OrgIRRatio2 orgIrRatio2, IRRatio dbIRRatio) {
        OrgIRRatio irRatio = convertIRRatio2To1(orgIrRatio2);

        return convertIRRatioToDBIRRatio(irRatio, dbIRRatio);
    }

    public boolean convertIRRatioToDBIRRatio(OrgIRRatio irRatio, IRRatio dbIRRatio) {

        boolean status = true;

        // String name
        //dbIRRatio.setName(irRatio.name);

        // Atomic #
        dbIRRatio.setAtomicNum(irRatio.element.atomicNumber);

        // Region[] numerator
        Region[] numeratorRegions = new Region[irRatio.numerator.size()];
        int index = 0;
        for (OrgRegion orgRegion : irRatio.numerator) {
            Region dbRegion = new Region();
            convertRegionToDBRegion(orgRegion, dbRegion);

            numeratorRegions[index] = dbRegion;
            index++;
        }
        dbIRRatio.setNumerator(numeratorRegions);

        // Region[] numerator
        Region[] denominatorRegions = new Region[irRatio.denominator.size()];
        index = 0;
        for (OrgRegion orgRegion : irRatio.denominator) {
            Region dbRegion = new Region();
            convertRegionToDBRegion(orgRegion, dbRegion);

            denominatorRegions[index] = dbRegion;
            index++;
        }
        dbIRRatio.setDenominator(denominatorRegions);
        dbIRRatio.setMetadata(UBValueFactory.createObject());

        return status;
    }

    public boolean convertRegionToDBRegion(OrgRegion orgRegion, Region dbRegion) {
        boolean status = true;

        // String name;
        dbRegion.setName(orgRegion.name);

        dbRegion.setDisabled(false);

        // WaveLengthRange wavelengthRange;
        dbRegion.setMin(orgRegion.wavelengthRange.getMinimumDouble());
        dbRegion.setMax(orgRegion.wavelengthRange.getMaximumDouble());

        // Params
        String algorithmName = orgRegion.params.get("name");
        String valueStr = orgRegion.params.get(SGolayIntensity.KEY_SGOLAYEASY);

        if (StringUtils.isEmptyString(algorithmName) || StringUtils.isEmptyString(valueStr)) {
            algorithmName = "com.sciaps.common.algorithms.SimpleIntensityValue";
            valueStr = "[3, 2, 0]";
        }

        UBObject params = UBValueFactory.createObject();
        params.put("name", UBValueFactory.createValue(algorithmName));
        params.put(SGolayIntensity.KEY_SGOLAYEASY, UBValueFactory.createValue(valueStr));

        dbRegion.setParams(params);

        // int mode type;
        //dbRegion.setModeType(orgRegion.regionType);

        return status;
    }

    public boolean convertIRCurveToDBIRCurve(OrgIRCurve irCurve, IRCurve dbirCurve) {

        boolean status = true;
        int index = 0;

        // Priority
        dbirCurve.setPriority(0);

        // Standard[]
        Standard[] excludedStandards = new Standard[irCurve.excludedOrgStandards.size()];
        for (OrgStandard orgStandard : irCurve.excludedOrgStandards) {
            Standard dbStandard = mDBObjectInventory.getDBStandardByStandard(orgStandard);
            if (dbStandard == null) {
                dbStandard = dbirCurve.getDB().insert(Standard.class);
                convertStandardToDBStandard(orgStandard, dbStandard);
            }

            excludedStandards[index] = dbStandard;
            index++;
        }
        dbirCurve.setExcludedStandards(excludedStandards);

        // int degree
        dbirCurve.setDegree(irCurve.degree);

        // boolean forceZero
        dbirCurve.setForceZero(irCurve.forceZero);

        // double[] coefficients
        if (irCurve.coefficients != null) {
            dbirCurve.setCoefficients(irCurve.coefficients.clone());
        }

        // double r2
        dbirCurve.setR2(irCurve.r2);

        // WaveLengthRange irRange
        if (irCurve.irRange != null) {
            dbirCurve.setMin(irCurve.irRange.getMinimumDouble());
            dbirCurve.setMax(irCurve.irRange.getMaximumDouble());
        }

        // double calRangeFactor
        dbirCurve.setCalRangeFactor(irCurve.calRangeFactor);

        IRRatio irRatio = new IRRatio();

        dbirCurve.setIrRatio(irRatio);

        // String name
        //irRatio.setName(irCurve.name);

        // AtomicElementDatum atomicElementDatum
        irRatio.setAtomicNum(irCurve.element.atomicNumber);

        // Region[] numerator
        Region[] numeratorRegions = new Region[irCurve.numerator.size()];
        index = 0;
        for (OrgRegion orgRegion : irCurve.numerator) {

            Region dbRegion = new Region();
            convertRegionToDBRegion(orgRegion, dbRegion);

            numeratorRegions[index] = dbRegion;
            index++;
        }
        irRatio.setNumerator(numeratorRegions);

        // Region[] Denominator
        Region[] denominatorRegions = new Region[irCurve.denominator.size()];
        index = 0;
        for (OrgRegion orgRegion : irCurve.denominator) {
            Region dbRegion = new Region();
            convertRegionToDBRegion(orgRegion, dbRegion);

            denominatorRegions[index] = dbRegion;
            index++;
        }
        irRatio.setDenominator(denominatorRegions);
        irRatio.setMetadata(UBValueFactory.createObject());

        dbirCurve.setMetadata(UBValueFactory.createObject());

        return status;
    }

    public boolean convertFingerprintToDBFingerprint(OrgFingerprintLibraryTemplate orgFingerprintLibraryTemplate, FingerprintLibTemplate dbFingerprintLibTemplate) {
        boolean status = true;

        // String name
        dbFingerprintLibTemplate.setName(orgFingerprintLibraryTemplate.name);

        //Empirical model
        if (orgFingerprintLibraryTemplate.orgModel != null) {
            EmpiricalModel empiricalModel = mDBObjectInventory.getEmpiricalModelByModel(orgFingerprintLibraryTemplate.orgModel);

            if (empiricalModel == null) {
                empiricalModel = dbFingerprintLibTemplate.getDB().insert(EmpiricalModel.class);
                convertModelToEmpiricalModel(orgFingerprintLibraryTemplate.orgModel, empiricalModel);
                mDBObjectInventory.addModelByEmpiricalModelMapping(orgFingerprintLibraryTemplate.orgModel, empiricalModel);
            }
            dbFingerprintLibTemplate.setModel(empiricalModel);
        }

        //Standard[]
        Standard[] standards = new Standard[orgFingerprintLibraryTemplate.orgStandardList.size()];
        int index = 0;
        for (OrgStandard orgStandard : orgFingerprintLibraryTemplate.orgStandardList) {
            Standard dbStandard = mDBObjectInventory.getDBStandardByStandard(orgStandard);
            if (dbStandard == null) {
                dbStandard = dbFingerprintLibTemplate.getDB().insert(Standard.class);
                convertStandardToDBStandard(orgStandard, dbStandard);
            }

            standards[index] = dbStandard;
            index++;
        }
        dbFingerprintLibTemplate.setStandards(standards);

        //IRRatio[]
        index = 0;
        IRRatio[] featureRegions = new IRRatio[orgFingerprintLibraryTemplate.featureRegions.size()];
        for (OrgIRRatio2 orgIrRatio2 : orgFingerprintLibraryTemplate.featureRegions) {
            IRRatio dbirRatio = new IRRatio();
            convertIRRatio2ToDBIRRatio(orgIrRatio2, dbirRatio);

            featureRegions[index] = dbirRatio;
            index++;
        }
        dbFingerprintLibTemplate.setFeatureRegions(featureRegions);

        //double[] weights
        if (orgFingerprintLibraryTemplate.weights != null) {
            double[] weights = orgFingerprintLibraryTemplate.weights.clone();
            dbFingerprintLibTemplate.setWeights(weights);
        }

        return status;
    }
}

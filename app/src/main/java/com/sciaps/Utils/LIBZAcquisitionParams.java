package com.sciaps.Utils;

import com.devsmart.ubjson.UBObject;
import com.devsmart.ubjson.UBValue;
import com.devsmart.ubjson.UBValueFactory;
import com.sciaps.common.webserver.ILaserController;
import com.sciaps.data.OrgLIBZTest;

import java.util.TreeMap;

/**
 * Created by jchen on 1/7/16.
 */
public class LIBZAcquisitionParams {

    public static final String START_LOCATIONS = "startLocations";
    public static final String END_LOCATIONS = "endLocations";
    public static final String NUM_LOCATIONS = "numLocations";
    public static final String NUM_CLEANING_SHOTS_PER_LOC = "numCleaningShotsPerLoc";
    public static final String NUM_SHOTS_PER_LOC = "numShotsPerLoc";
    public static final String ARGON_PRE_FLUSH = "argonPreflush";
    public static final String RESET_STAGE = "resetStage";
    public static final String USE_GATING = "useGating";
    public static final String PULSE_PERIOD = "pulsePeriod";
    public static final String INTG_DELAY = "integrationDelay";
    public static final String INTG_PERIOD = "integrationPeriod";
    public static final String STEP_SIZE = "stepSize";
    public static final String NUM_SHOTS_TO_AVG = "numShotsToAvg";
    public static final String SHOT_GROUP_TYPE = "shotGroupType";


    ILaserController.RasterParams rasterParams;

    public LIBZAcquisitionParams() {
        rasterParams = new ILaserController.RasterParams();
        rasterParams.startLocation = new int[3];
        rasterParams.startLocation[0] = 0;
        rasterParams.startLocation[1] = 0;
        rasterParams.startLocation[2] = 0;
        rasterParams.endLocation = new int[2];
        rasterParams.endLocation[0] = 0;
        rasterParams.endLocation[1] = 0;
        rasterParams.numlocations = 60;
        rasterParams.numCleaningShotsPerLocation = 0;
        rasterParams.numShotsPerLocation = 1;
        rasterParams.argonpreflush = 1000;
        rasterParams.resetStage = true;
        rasterParams.useGating = true;
        rasterParams.pulsePeriod = 100;
        rasterParams.intergrationDelay = 19;
        rasterParams.intergrationPeriod = 0;
        rasterParams.stepSize = 20;
        rasterParams.numShotsToAvg = 10;
        rasterParams.shotGroupType = ILaserController.RasterParams.ShotGrouper.Random;
    }

    public LIBZAcquisitionParams(UBObject params) {
        setAcquisitionParams(params);
    }

    public LIBZAcquisitionParams(ILaserController.RasterParams params) {
        setAcquisitionParams(params);
    }

    public ILaserController.RasterParams getAcquisitionParams() {
        return rasterParams;
    }

    public UBObject getParamUBObject() {
        TreeMap<String, UBValue> values = new TreeMap<String, UBValue>();

        values.put(START_LOCATIONS, UBValueFactory.createArray(rasterParams.startLocation));
        values.put(END_LOCATIONS, UBValueFactory.createArray(rasterParams.endLocation));
        values.put(NUM_LOCATIONS, UBValueFactory.createValue(rasterParams.numlocations));
        values.put(NUM_SHOTS_PER_LOC, UBValueFactory.createValue(rasterParams.numShotsPerLocation));
        values.put(NUM_CLEANING_SHOTS_PER_LOC, UBValueFactory.createValue(rasterParams.numCleaningShotsPerLocation));
        values.put(ARGON_PRE_FLUSH, UBValueFactory.createValue(rasterParams.argonpreflush));
        values.put(RESET_STAGE, UBValueFactory.createBool(rasterParams.resetStage));
        values.put(USE_GATING, UBValueFactory.createBool(rasterParams.useGating));
        values.put(PULSE_PERIOD, UBValueFactory.createValue(rasterParams.pulsePeriod));
        values.put(INTG_DELAY, UBValueFactory.createValue(rasterParams.intergrationDelay));
        values.put(INTG_PERIOD, UBValueFactory.createValue(rasterParams.intergrationPeriod));
        values.put(STEP_SIZE, UBValueFactory.createValue(rasterParams.stepSize));
        values.put(NUM_SHOTS_TO_AVG, UBValueFactory.createValue(rasterParams.numShotsToAvg));
        if (rasterParams.shotGroupType.equals(ILaserController.RasterParams.ShotGrouper.Linear)) {
            values.put(SHOT_GROUP_TYPE, UBValueFactory.createValue(0));
        } else {
            values.put(SHOT_GROUP_TYPE, UBValueFactory.createValue(1));
        }

        return UBValueFactory.createObject(values);
    }

    public void setAcquisitionParams(UBObject config) {
        rasterParams = new ILaserController.RasterParams();
        rasterParams.startLocation = config.get(START_LOCATIONS).asInt32Array();
        rasterParams.endLocation = config.get(END_LOCATIONS).asInt32Array();
        rasterParams.numlocations = config.get(NUM_LOCATIONS).asInt();
        rasterParams.numCleaningShotsPerLocation = config.get(NUM_CLEANING_SHOTS_PER_LOC).asInt();
        rasterParams.numShotsPerLocation = config.get(NUM_SHOTS_PER_LOC).asInt();
        rasterParams.argonpreflush = config.get(ARGON_PRE_FLUSH).asInt();
        rasterParams.resetStage = config.get(RESET_STAGE).asBool();
        rasterParams.useGating = config.get(USE_GATING).asBool();
        rasterParams.pulsePeriod = config.get(PULSE_PERIOD).asInt();
        rasterParams.intergrationDelay = config.get(INTG_DELAY).asInt();
        rasterParams.intergrationPeriod = config.get(INTG_PERIOD).asInt();
        rasterParams.stepSize = config.get(STEP_SIZE).asInt();
        rasterParams.numShotsToAvg = config.get(NUM_SHOTS_TO_AVG).asInt();
        int shotGroupType = config.get(SHOT_GROUP_TYPE).asInt();
        if (shotGroupType == 0) {
            rasterParams.shotGroupType = ILaserController.RasterParams.ShotGrouper.Linear;
        } else {
            rasterParams.shotGroupType = ILaserController.RasterParams.ShotGrouper.Random;
        }
    }

    public void setAcquisitionParams(ILaserController.RasterParams params) {
        rasterParams = new ILaserController.RasterParams();
        rasterParams.startLocation = new int[3];
        rasterParams.startLocation[0] = params.startLocation[0];
        rasterParams.startLocation[1] = params.startLocation[1];
        rasterParams.startLocation[2] = params.startLocation[2];
        rasterParams.endLocation = new int[2];
        rasterParams.endLocation[0] = params.endLocation[0];
        rasterParams.endLocation[1] = params.endLocation[1];
        rasterParams.numlocations = params.numlocations;
        rasterParams.numCleaningShotsPerLocation = params.numCleaningShotsPerLocation;
        rasterParams.numShotsPerLocation = params.numShotsPerLocation;
        rasterParams.argonpreflush = params.argonpreflush;
        rasterParams.resetStage = params.resetStage;
        rasterParams.useGating = params.useGating;
        rasterParams.pulsePeriod = params.pulsePeriod;
        rasterParams.intergrationDelay = params.intergrationDelay;
        rasterParams.intergrationPeriod = params.intergrationPeriod;
        rasterParams.stepSize = params.stepSize;
        rasterParams.numShotsToAvg = params.numShotsToAvg;
        rasterParams.shotGroupType = params.shotGroupType;
    }

    public void setAcquisitionParams(OrgLIBZTest.Config params) {
        rasterParams = new ILaserController.RasterParams();
        rasterParams.startLocation = new int[3];
        rasterParams.startLocation[0] = params.rasterStart[0];
        rasterParams.startLocation[1] = params.rasterStart[1];
        rasterParams.startLocation[2] = params.rasterStart[2];
        rasterParams.endLocation = new int[2];
        rasterParams.endLocation[0] = 0; // field not defined in old test config
        rasterParams.endLocation[1] = 0; // field not defined in old test config
        rasterParams.numlocations = params.rasterNumLocations;
        rasterParams.numCleaningShotsPerLocation = params.numCleaningShotsPerLocation;
        rasterParams.numShotsPerLocation = params.numShotsPerLocation;
        rasterParams.argonpreflush = params.argonPreflush;
        rasterParams.resetStage = true; // field not defined in old test config
        rasterParams.useGating = params.gating;
        rasterParams.pulsePeriod = 100; // field not defined in old test config
        rasterParams.intergrationDelay = params.intergrationDelay;
        rasterParams.intergrationPeriod = params.intergrationPeriod;
        rasterParams.stepSize = 20; // field not defined in old test config
        rasterParams.numShotsToAvg = params.numShotsToAvg;
        rasterParams.shotGroupType = ILaserController.RasterParams.ShotGrouper.Random; // TODO, maybe set to unknown
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        //TODO

        return true;
    }
}
package com.sciaps.data;

import com.devsmart.StringUtils;
import com.sciaps.common.objtracker.DBObj;
import com.sciaps.common.objtracker.IdReference;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.TreeMap;

public final class OrgLIBZTest extends DBObj {
    public static class Config {
        public int intergrationDelay;
        public int intergrationPeriod;
        public boolean gating;
        public int argonPreflush;
        public int[] rasterStart = new int[3];
        public int rasterNumLocations = 60;
        public int numShotsPerLocation = 1;
        public int numCleaningShotsPerLocation = 0;
        public int numShotsToAvg = 10;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Config config = (Config) o;

            if (intergrationDelay != config.intergrationDelay) {
                return false;
            }

            if (intergrationPeriod != config.intergrationPeriod) {
                return false;
            }

            if (gating != config.gating) {
                return false;
            }

            if (argonPreflush != config.argonPreflush) {
                return false;
            }

            if (rasterNumLocations != config.rasterNumLocations) {
                return false;
            }


            if (numShotsPerLocation != config.numShotsPerLocation) {
                return false;
            }

            if (numCleaningShotsPerLocation != config.numCleaningShotsPerLocation) {
                return false;
            }

            if (numShotsToAvg != config.numShotsToAvg) {
                return false;
            }

            return Arrays.equals(rasterStart, config.rasterStart);
        }

        @Override
        public int hashCode() {
            int result = intergrationDelay;
            result = 31 * result + intergrationPeriod;
            result = 31 * result + (gating ? 1 : 0);
            result = 31 * result + argonPreflush;
            result = 31 * result + Arrays.hashCode(rasterStart);
            result = 31 * result + rasterNumLocations;
            result = 31 * result + numShotsPerLocation;
            result = 31 * result + numCleaningShotsPerLocation;
            result = 31 * result + numShotsToAvg;
            return result;
        }
    }

    public long unixTime;
    public Config config = new Config();

    @IdReference
    public OrgStandard standard;

    public String displayName = "";
    public TreeMap<String, String> metadata = new TreeMap<String, String>();

    public boolean onlyAvgSaved = false;

    public int getNumShots() {
        if (onlyAvgSaved) {
            return 1;
        }

        double numSavedSpectrums = (config.rasterNumLocations * (config.numShotsPerLocation - config.numCleaningShotsPerLocation)) / (double) config.numShotsToAvg;

        return (int) Math.ceil(numSavedSpectrums);
    }

    public String getFriendlyDisplayName() {
        if (StringUtils.isEmptyString(displayName)) {
            if (mId == null) {
                return "Unsaved Test";
            }

            DateFormat simpleDateFormat = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT);

            return String.format("Test: %s %s", mId.substring(0, 5), simpleDateFormat.format(new Date(unixTime * 1000)));
        }

        return displayName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        OrgLIBZTest orgLibzTest = (OrgLIBZTest) o;

        if (unixTime != orgLibzTest.unixTime) {
            return false;
        }

        if (onlyAvgSaved != orgLibzTest.onlyAvgSaved) {
            return false;
        }

        if (!config.equals(orgLibzTest.config)) {
            return false;
        }

        if (standard != null ? !standard.equals(orgLibzTest.standard) : orgLibzTest.standard != null) {
            return false;
        }

        if (!displayName.equals(orgLibzTest.displayName)) {
            return false;
        }

        return metadata.equals(orgLibzTest.metadata);
    }

    @Override
    public int hashCode() {
        int result = (int) (unixTime ^ (unixTime >>> 32));
        result = 31 * result + config.hashCode();
        result = 31 * result + (standard != null ? standard.hashCode() : 0);
        result = 31 * result + displayName.hashCode();
        result = 31 * result + metadata.hashCode();
        result = 31 * result + (onlyAvgSaved ? 1 : 0);
        return result;
    }
}
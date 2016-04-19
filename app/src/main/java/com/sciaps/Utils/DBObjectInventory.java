package com.sciaps.Utils;


import com.sciaps.data.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jchen on 1/8/16.
 */
public class DBObjectInventory {

    private Map<String, Acquisition> testMapping = new HashMap<String, Acquisition>();
    private Map<String, Standard> standardMapping = new HashMap<String, Standard>();
    private Map<String, Region> regionMapping = new HashMap<String, Region>();
    private Map<String, EmpiricalModel> modelMapping = new HashMap<String, EmpiricalModel>();

    public Acquisition getAcquisitionByTest(OrgLIBZTest test) {
        return testMapping.get(test.mId);
    }

    public void addTestToAcquisitionMapping(OrgLIBZTest test, Acquisition acquisition) {
        testMapping.put(test.mId, acquisition);
    }

    public void removeTestToAcquisitionMapping(OrgLIBZTest test) {
        testMapping.remove(test.mId);
    }

    public Standard getDBStandardByStandard(OrgStandard orgStandard) {
        return standardMapping.get(orgStandard.mId);
    }

    public void addStandardToDBStandardingMapping(OrgStandard orgStandard, Standard dbStandard) {
        standardMapping.put(orgStandard.mId, dbStandard);
    }

    public void removeStandardToDBStandardMapping(OrgStandard orgStandard) {
        standardMapping.remove(orgStandard.mId);
    }

    public Region getDBRegionByRegion(OrgRegion orgRegion) {
        return regionMapping.get(orgRegion.mId);
    }

    public void addRegionToDBRegionMapping(OrgRegion orgRegion, Region dbRegion) {
        regionMapping.put(orgRegion.mId, dbRegion);
    }

    public void removeRegionToDBRegionMapping(OrgRegion orgRegion) {
        regionMapping.remove(orgRegion.mId);
    }

    public EmpiricalModel getEmpiricalModelByModel(OrgModel orgModel) {
        return modelMapping.get(orgModel.mId);
    }

    public EmpiricalModel addModelByEmpiricalModelMapping(OrgModel orgModel, EmpiricalModel empiricalModel) {
        return modelMapping.put(orgModel.mId, empiricalModel);
    }

    public void addRegionToDBRegionMapping(OrgModel orgModel, EmpiricalModel empiricalModel) {
        modelMapping.put(orgModel.mId, empiricalModel);
    }

    public void removeRegionToDBRegionMapping(OrgModel orgModel) {
        modelMapping.remove(orgModel.mId);
    }
}

package com.sciaps.data;

import com.google.common.base.Objects;
import com.sciaps.common.objtracker.DBObj;
import com.sciaps.common.objtracker.IdReference;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class OrgFingerprintLibraryTemplate extends DBObj {

    public String name;
    public String base;

    @IdReference
    public OrgModel orgModel;

    @IdReference(type = OrgStandard.class)
    public List<OrgStandard> orgStandardList = new LinkedList<OrgStandard>();

    public List<OrgIRRatio2> featureRegions = new ArrayList<OrgIRRatio2>();
    public double[] weights;

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .addValue(name)
                .toString();
    }

}

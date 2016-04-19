package com.sciaps.data;


import com.sciaps.common.objtracker.IdReference;
import org.apache.commons.lang.math.DoubleRange;

import java.util.ArrayList;
import java.util.List;

public class OrgIRCurve extends OrgIRRatio {


    @IdReference(type = OrgStandard.class)
    public List<OrgStandard> excludedOrgStandards = new ArrayList<OrgStandard>();
    public int degree = 2;
    public boolean forceZero = false;
    public double[] coefficients;
    public double r2;
    public DoubleRange irRange;
    public double calRangeFactor = 0.3; // 30%
}

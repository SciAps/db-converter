package com.sciaps.data;


import com.google.common.base.Objects;
import com.sciaps.common.AtomicElement;
import com.sciaps.common.constant.ModelType;
import com.sciaps.common.objtracker.DBObj;
import com.sciaps.common.objtracker.IdReference;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

// Single curve model
public class OrgModel extends DBObj {

    public String name;
    public int modelType = ModelType.MODEL_TYPE_ALLOY | ModelType.MODEL_TYPE_SCIAPS;

    @IdReference(type = OrgStandard.class)
    public List<OrgStandard> orgStandardList = new LinkedList<OrgStandard>();

    public Map<AtomicElement, OrgIRCurve> irs = new HashMap<AtomicElement, OrgIRCurve>();

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .addValue(name)
                .toString();
    }
}

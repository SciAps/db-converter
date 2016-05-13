package com.sciaps.data;


import com.google.common.base.Objects;
import com.sciaps.common.AtomicElement;
import com.sciaps.common.constant.ModelType;
import com.sciaps.common.objtracker.DBObj;
import com.sciaps.common.objtracker.IdReference;
import com.sciaps.common.webserver.ILaserController;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

// Multi curves
public class OrgModel2 extends DBObj {

    public String name;
    public int modelType = ModelType.MODEL_TYPE_ALLOY | ModelType.MODEL_TYPE_SCIAPS;
    public ILaserController.RasterParams rasterParams;

    @IdReference(type = OrgStandard.class)
    public List<OrgStandard> standardList = new LinkedList<OrgStandard>();

    public Map<AtomicElement, List<OrgIRCurve>> irs = new HashMap<AtomicElement, List<OrgIRCurve>>();

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .addValue(name)
                .toString();
    }
}

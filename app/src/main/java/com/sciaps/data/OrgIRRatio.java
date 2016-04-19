package com.sciaps.data;


import com.google.common.base.Objects;
import com.sciaps.common.AtomicElement;
import com.sciaps.common.objtracker.DBObj;
import com.sciaps.common.objtracker.IdReference;

import java.util.ArrayList;
import java.util.List;

public class OrgIRRatio extends DBObj {

    public String name;
    public AtomicElement element;

    @IdReference(type = OrgRegion.class)
    public List<OrgRegion> numerator = new ArrayList<OrgRegion>();

    @IdReference(type = OrgRegion.class)
    public List<OrgRegion> denominator = new ArrayList<OrgRegion>();

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .addValue(name)
                .toString();
    }
}

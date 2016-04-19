package com.sciaps.data;


import com.google.common.base.Objects;
import com.sciaps.common.AtomicElement;
import com.sciaps.common.objtracker.DBObj;

import java.util.ArrayList;
import java.util.List;

public class OrgIRRatio2 extends DBObj {

    public String name;
    public AtomicElement element;

    public List<OrgRegion> numerator = new ArrayList<OrgRegion>();

    public List<OrgRegion> denominator = new ArrayList<OrgRegion>();

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .addValue(name)
                .toString();
    }
}

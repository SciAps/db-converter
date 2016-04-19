package com.sciaps.data;

import com.sciaps.common.AtomicElement;
import com.sciaps.common.constant.ModelType;
import com.sciaps.common.data.ChemValue;
import com.sciaps.common.objtracker.DBObj;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;

public class OrgStandard extends DBObj {

    public String name;
    public HashSet<ChemValue> spec = new HashSet<ChemValue>();
    public int standardType = ModelType.MODEL_TYPE_ALLOY | ModelType.MODEL_TYPE_SCIAPS;

    @Override
    public String toString() {
        return name;
    }

    public ChemValue getGradeFor(AtomicElement e) {
        ChemValue retval = null;
        for (ChemValue g : spec) {
            if (g.element == e) {
                retval = g;
                break;
            }
        }
        return retval;
    }

    public void removeGradeFor(AtomicElement e) {
        Iterator<ChemValue> it = spec.iterator();
        while (it.hasNext()) {
            ChemValue value = it.next();
            if (value.element == e) {
                it.remove();
                return;
            }
        }
    }

    /**
     * Get the AlloyBase by the naming convention. This assumes that the name of the standard
     * follows the naming convention [AlloyBase name]_[standard name]
     * @return
     */
    public String getBase() {
        return getBaseFromName(name);
    }


    public static String getBaseFromName(String name) {
        String retval = null;

        int i = name.indexOf("_");
        if (i >= 1) {
            retval = name.substring(0, i);
        } else {
            retval = name;
        }
        return retval;
    }

    public static String getName(String name) {
        String retval = name;
        int i = name.indexOf("_");
        if (i > 0) {
            retval = name.substring(i + 1, name.length());
        }
        return retval;
    }

    public String getName() {
        return getName(name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj.getClass() != OrgStandard.class) {
            return false;
        }
        OrgStandard other = (OrgStandard)obj;
        boolean retval = name.equals(other.name) && spec.equals(other.spec);
        return retval;
    }

    public static final Comparator<OrgStandard> ALPHA_COMPARATOR = new Comparator<OrgStandard>() {
        @Override
        public int compare(OrgStandard a, OrgStandard b) {
            return a.name.compareTo(b.name);
        }
    };
}

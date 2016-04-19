package com.sciaps.data;

import com.google.common.base.Objects;
import com.sciaps.common.AtomicElement;
import com.sciaps.common.algorithms.IntensityValue;
import com.sciaps.common.algorithms.SGolayIntensity;
import com.sciaps.common.algorithms.SimpleIntensityValue;
import com.sciaps.common.constant.ModelType;
import com.sciaps.common.data.EmissionLine;
import com.sciaps.common.objtracker.DBObj;
import org.apache.commons.lang.math.DoubleRange;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrgRegion extends DBObj {

    public DoubleRange wavelengthRange;
    public String name;
    public Map<String, String> params = new HashMap<String, String>();
    public int regionType = ModelType.MODEL_TYPE_ALLOY | ModelType.MODEL_TYPE_SCIAPS;

    private static final Pattern ATOMIC_REGEX = Pattern.compile("[A-Z][a-z]*");
    private static final Pattern FLOAT_REGEX = Pattern.compile("\\d+(\\.\\d+)?");

    public static OrgRegion parse(String str) {
        AtomicElement element = null;
        Matcher symbolMatch = ATOMIC_REGEX.matcher(str);
        if (symbolMatch.find()) {
            String symbol = symbolMatch.group(0);
            element = AtomicElement.getElementBySymbol(symbol);
        }

        double a = Double.NaN;
        double b = Double.NaN;

        Matcher floatMatch = FLOAT_REGEX.matcher(str);
        if (floatMatch.find()) {
            a = Double.parseDouble(floatMatch.group(0));
        }

        if (floatMatch.find()) {
            b = Double.parseDouble(floatMatch.group(0));
        }

        if (Double.isNaN(a) || Double.isNaN(b)) {
            throw new RuntimeException("could not parse");
        }

        double midWL = (a + b) / 2;

        OrgRegion retval = new OrgRegion();
        retval.wavelengthRange = new DoubleRange(a, b);
        if (element != null) {
            retval.name = String.format("%s %d", element.symbol,
                    Math.round(midWL));
        } else {
            retval.name = String.format("%d", Math.round(midWL));
        }

        return retval;
    }


    /**
     * get the Atomic element from the naming convention.
     * @return AtomicElement or null
     */
    public AtomicElement getElement() {
        EmissionLine line = null;
        try {
            line = EmissionLine.parse(name);
            return line.element;
        } catch (Exception e) {
            return null;
        }

    }

    private transient IntensityValue mCachedIV;
    public IntensityValue getIntensityValue() {
        if (mCachedIV == null) {
            String className = params.get("name");
            if (className == null) {
                className = SimpleIntensityValue.class.getName();
            }
            try {
                className = className.trim();
                Class<?> classType = Class.forName(className);
                mCachedIV = (IntensityValue)classType.newInstance();
                mCachedIV.configure(params);
            } catch (Exception e ) {
                throw new RuntimeException("cannot create instance of class: " + className, e);
            }
        }
        return mCachedIV;
    }

    public void invalidateIVCache () {
        mCachedIV = null;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
        mCachedIV = null;
    }

    @Override
    public int hashCode() {
        return wavelengthRange.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        boolean retval = false;
        if (obj instanceof OrgRegion) {
            OrgRegion other = (OrgRegion)obj;
            retval = Objects.equal(wavelengthRange, other.wavelengthRange)
                    && Objects.equal(getIntensityValue(), other.getIntensityValue());
        }
        return retval;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .addValue(name)
                .addValue(wavelengthRange)
                .toString();
    }

    public String getShortName() {
        String algorithm = getIntensityValue().getClass().getSimpleName();

        if (algorithm.equals("OneIntensityValue")) {
            return "1";
        }

        double midValue = (wavelengthRange.getMinimumDouble() + wavelengthRange.getMaximumDouble()) / 2;

        String name;
        AtomicElement element = getElement();
        if (element != null) {
            name = String.format("%s %.1f", element.symbol, midValue);
        } else {
            name = String.format("%.1f", midValue);
        }
        return name.trim();
    }

    public String getDetailedName() {
        String algorithm = getIntensityValue().getClass().getSimpleName();

        if (algorithm.equals("OneIntensityValue")) {
            return "1";
        }

        double midValue = (wavelengthRange.getMinimumDouble() + wavelengthRange.getMaximumDouble()) / 2;

        String algorithmnShortHand = "";
        if (algorithm.equals("SGolayIntensity")) {
            algorithmnShortHand = "SG " + params.get(SGolayIntensity.KEY_SGOLAYEASY);
        } else if (algorithm.equals("SGolayBaseLine")) {
            algorithmnShortHand = "SG-B " + params.get(SGolayIntensity.KEY_SGOLAYEASY);
        } else if (algorithm.equals("SimpleIntensityValue")) {
            algorithmnShortHand = "PA-B";
        } else if (algorithm.equals("SimpleBaseLine")) {
            algorithmnShortHand = "LSN";
        }

        String name;
        AtomicElement element = getElement();
        if (element != null) {
            name = String.format("%s %.1f %s", element.symbol, midValue, algorithmnShortHand);
        } else {
            name = String.format("%.1f %s", midValue, algorithmnShortHand);
        }
        return name.trim();
    }

}

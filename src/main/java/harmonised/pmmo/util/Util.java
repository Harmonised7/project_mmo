package harmonised.pmmo.util;

import net.minecraft.util.math.vector.Vector3d;

public class Util
{
    public static double mapCapped( double input, double inLow, double inHigh, double outLow, double outHigh )
    {
        if( input < inLow )
            input = inLow;
        if( input > inHigh )
            input = inHigh;

        return map( input, inLow, inHigh, outLow, outHigh );
    }

    public static double map( double input, double inLow, double inHigh, double outLow, double outHigh )
    {
        return ( (input - inLow) / (inHigh - inLow) ) * (outHigh - outLow) + outLow;
    }

    public static Vector3d getMidVec( Vector3d v1, Vector3d v2 )
    {
        return new Vector3d( (v1.x + v2.x)/2, (v1.y + v2.y)/2, (v1.z + v2.z)/2 );
    }
}
CREATE OR REPLACE FUNCTION TangentialProperPart(x Geometry, y Geometry) RETURNS boolean AS $$
DECLARE
    numPoints int;
    point Geometry;
    geomToCheck Geometry;
    typeX varchar;
    typeY varchar;
BEGIN
	IF ST_Equals(x, y) THEN
	   RETURN true;
    END IF;

    typeX := GeometryType(x);
    typeY := GeometryType(y);

    IF typeX = 'POLYGON' THEN
        IF typeY = 'POLYGON' THEN
            IF NOT ST_CoveredBy(x, y) THEN
                RETURN false;
            END IF;
        END IF;

        geomToCheck := ST_ExteriorRing(x);
        numPoints := ST_NPoints(geomToCheck);
	    FOR i IN 1..numPoints LOOP
	       point := ST_PointN(geomToCheck, i);
	      -- RAISE NOTICE '%, %', AsText(point), AsText(y);
	       IF (ST_Touches(point, y)) THEN
	           RETURN true;
	       END IF;
	    END LOOP;
    ELSIF typeX = 'POINT' THEN
        IF (ST_Intersects(x, y) AND ST_Touches(x, y)) THEN
            RETURN true;
        END IF;
    END IF;

	RETURN false;

END;
$$ LANGUAGE plpgsql;

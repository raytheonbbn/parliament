CREATE OR REPLACE FUNCTION ProperPart(x Geometry, y Geometry) RETURNS boolean AS $$
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

    IF (ST_CoveredBy(x, y)) THEN
        RETURN true;
    END IF;

    RETURN TangentialProperPart(x, y);
END;
$$ LANGUAGE plpgsql;

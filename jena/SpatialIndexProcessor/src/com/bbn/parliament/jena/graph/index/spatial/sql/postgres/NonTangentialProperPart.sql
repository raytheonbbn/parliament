CREATE OR REPLACE FUNCTION NonTangentialProperPart(x Geometry, y Geometry) RETURNS boolean AS $$
DECLARE
	numPoints int;
	point Geometry;
	geomToCheck Geometry;
	typeX varchar;
	typeY varchar;
BEGIN
	IF ST_Equals(x, y) THEN
		RAISE NOTICE '%, %', AsText(x), AsText(y);
		RETURN false;
	END IF;

	RETURN ST_CoveredBy(x, y) AND NOT TangentialProperPart(x, y);
END;
$$ LANGUAGE plpgsql;

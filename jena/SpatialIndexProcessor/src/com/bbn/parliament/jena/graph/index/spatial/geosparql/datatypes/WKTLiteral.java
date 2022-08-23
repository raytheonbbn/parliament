package com.bbn.parliament.jena.graph.index.spatial.geosparql.datatypes;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.CoordinateSequenceFactory;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.impl.CoordinateArraySequenceFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.graph.index.spatial.geosparql.vocabulary.WKT;
import com.hp.hpl.jena.datatypes.DatatypeFormatException;
import com.hp.hpl.jena.graph.impl.LiteralLabel;

/** @author rbattle */
public class WKTLiteral extends GeoSPARQLLiteral {
	protected static final Logger LOG = LoggerFactory.getLogger(WKTLiteral.class);

	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
		return super.equals(obj);
	}

	@Override
	public boolean isEqual(LiteralLabel value1, LiteralLabel value2) {
		// TODO Auto-generated method stub
		return super.isEqual(value1, value2);
	}

	private static final Pattern pattern = Pattern.compile(
		"(<([^<>\"\\{\\}|^`\\\\]*)>)?[\\r\\n\\s]*([\\w\\(\\)\\d\\s\\r\\n.\\-,]*)");

	public WKTLiteral() {
		super(WKT.WKTLiteral.getURI());
	}

	/** {@inheritDoc} */
	@Override
	public Geometry doParse(String lexForm) throws DatatypeFormatException {
		String lexicalForm = lexForm.strip();
		Matcher m = pattern.matcher(lexicalForm);
		if (!m.matches()) {
			throw new DatatypeFormatException(lexicalForm, this,
				"Invalid or missing IRI/WKT");
		}
		if (3 != m.groupCount()) {
			throw new DatatypeFormatException(lexicalForm, this,
				"Invalid or missing IRI/WKT");
		}

		String iri = m.group(2);

		Integer srid = null;
		String auth = null;
		CoordinateReferenceSystem crs = DEFAULT_CRS;

		if (null != iri) {
			auth = getCoordinateReferenceSystemCode(iri);
			if (null == auth) {
				throw new DatatypeFormatException(
					lexicalForm,
					this,
					"Could not find coordinate reference system code.  Only CRS and EPSG are supported");
			}
			try {
				crs = CRS.decode(auth, false);
				srid = CRS.lookupEpsgCode(crs, false);
			} catch (NoSuchAuthorityCodeException e) {
				throw new DatatypeFormatException(lexicalForm, this,
					"Could not create geographic coordinate reference system");
			} catch (FactoryException e) {
				throw new DatatypeFormatException(lexicalForm, this,
					"Could not create geographic coordinate reference system");
			}
			if (null == crs) {
				throw new DatatypeFormatException(lexicalForm, this,
					"Unsupported IRI.  Could not find coordinate reference system");
			}

			if (null == srid) {
				srid = 0;
			}
		} else {
			srid = com.bbn.parliament.jena.graph.index.spatial.Constants.DEFAULT_SRID;
			auth = com.bbn.parliament.jena.graph.index.spatial.Constants.DEFAULT_CRS;
		}

		String wkt = m.group(3).toUpperCase();

		CoordinateSequenceFactory csf = CoordinateArraySequenceFactory.instance();
		GeometryFactory factory = new GeometryFactory(new PrecisionModel(
			PrecisionModel.FLOATING), srid, csf);

		try {
			WKTReader reader = new WKTReader(factory);
			Geometry g = reader.read(wkt);
			g.setUserData(auth);
			return g;
		} catch (ParseException e) {
			throw new DatatypeFormatException(lexicalForm, this, "Invalid WKT");
		}
	}

	/** {@inheritDoc} */
	@Override
	protected String doUnparse(Geometry geometry) {
		String code = geometry.getUserData().toString();
		String coordRefUri = getCoordinateReferenceSystemURI(code);
		String text = geometry.toText();
		return (null == coordRefUri)
			? text
			: "<%1$s> %2$s".formatted(coordRefUri, text);
	}

	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return super.hashCode();
	}

	public static class WKTConstructor extends ConstructorFunction<WKTLiteral> {
		public WKTConstructor() {
			super(new WKTLiteral());
		}
	}

	protected WKTLiteral(String uri) {
		super(uri);
	}

	public static class WKTLiteralSFNamespace extends WKTLiteral {
		public WKTLiteralSFNamespace() {
			super(WKT.DATATYPE_URI + "wktLiteral");
		}

		@Override
		public Geometry doParse(String value) {
			LOG.warn("Accepting WKT geometry with incorrect datatype sf:wktLiteral");
			return super.doParse(value);
		}
	}
}

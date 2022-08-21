package com.bbn.parliament.jena.graph.index.spatial.geosparql.datatypes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import javax.xml.namespace.QName;

import org.geotools.gml3.GMLConfiguration;
import org.geotools.metadata.iso.citation.Citations;
import org.geotools.referencing.crs.AbstractCRS;
import org.geotools.xsd.Encoder;
import org.geotools.xsd.Parser;
import org.locationtech.jts.geom.Geometry;
import org.opengis.referencing.ReferenceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.graph.index.spatial.Constants;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.vocabulary.GML;
import com.hp.hpl.jena.datatypes.DatatypeFormatException;

/** @author rbattle */
public class GMLLiteral extends GeoSPARQLLiteral {
	protected static final Logger LOG = LoggerFactory.getLogger(GMLLiteral.class);

	public static void main(String[] args) {
		GMLLiteral l = new GMLLiteral();
		l.parse("<gml:MultiSurface srsName=\"urn:x-ogc:def:crs:EPSG:4326\" xmlns:gml=\"http://www.opengis.net/gml\">     <gml:surfaceMember>       <gml:Polygon>        <gml:exterior>         <gml:LinearRing>          <gml:posList>38.966556999999995 -77.008232 38.88998799999999 -76.911209 38.78811999999999 -77.045448 38.81391500000001 -77.035248 38.829364999999996 -77.045189 38.838413 -77.040405 38.86243099999999 -77.039078 38.886101 -77.067886 38.91560000000001 -77.078949 38.93206000000001 -77.122627 38.99343099999999 -77.042389 38.966556999999995 -77.008232</gml:posList>         </gml:LinearRing>        </gml:exterior>       </gml:Polygon>      </gml:surfaceMember>     </gml:MultiSurface>");
		l.parse("<gml:Point        srsName=\"urn:x-ogc:def:crs:EPSG:4326\" xmlns:gml=\"http://www.opengis.net/gml\">  <gml:pos sDimension=\"2\">-83.38 33.95</gml:pos></gml:Point>");
		// System.out.println(g);
	}
	// urn:x-ogc:def:crs:EPSG:4326
	// http://www.opengis.net/def/crs/OGC/1.3/CRS84
	// http://www.opengis.net/def/crs/EPSG/0/4326

	public GMLLiteral() {
		super(GML.GMLLiteral.getURI());
	}

	/** {@inheritDoc} */
	@Override
	protected Geometry doParse(String lexicalForm) throws DatatypeFormatException {
		try (Reader input = new StringReader(lexicalForm)) {
			Parser parser = new Parser(new GMLConfiguration());
			Geometry g = (Geometry) parser.parse(input);
			if (g.getUserData() instanceof AbstractCRS crs) {
				Set<ReferenceIdentifier> identifiers = crs.getIdentifiers();
				ReferenceIdentifier found = identifiers.stream()
					.filter(id -> Citations.identifierMatches(Citations.EPSG, id.getAuthority())
						|| Citations.identifierMatches(Citations.CRS, id.getAuthority()))
					.findFirst()
					.orElse(null);
				if (null == found && identifiers.size() > 0) {
					found = identifiers.iterator().next();
				}
				if (null != found) {
					g.setUserData(found.toString());
				} else {
					g.setUserData(Constants.DEFAULT_CRS);
				}
			} else if (null == g.getUserData()) {
				g.setUserData(Constants.DEFAULT_CRS);
			}
			return g;
		} catch (Exception ex) {
			throw new DatatypeFormatException(lexicalForm, this, ex.getMessage());
		}
	}

	/** {@inheritDoc} */
	@Override
	protected String doUnparse(Geometry geometry) {
		Encoder encoder = new Encoder(new GMLConfiguration());
		QName qname = new QName("http://www.opengis.net/gml", geometry.getGeometryType());
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			encoder.encode(geometry, qname, bos);
			return new String(bos.toByteArray(), StandardCharsets.UTF_8);
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	public static class GMLConstructor extends ConstructorFunction<GMLLiteral> {
		public GMLConstructor() {
			super(new GMLLiteral());
		}
	}

	protected GMLLiteral(String uri){
		super(uri);
	}

	public static class GMLLiteralGMLNamespace extends GMLLiteral {
		public GMLLiteralGMLNamespace(){
			super(GML.DATATYPE_URI + "gmlLiteral");
		}

		@Override
		public Geometry doParse(String value){
			LOG.warn("Accepting GML geometry with incorrect datatype gml:gmlLiteral");
			return super.doParse(value);
		}
	}
}

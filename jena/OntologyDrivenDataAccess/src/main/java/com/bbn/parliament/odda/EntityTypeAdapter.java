package com.bbn.parliament.odda;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.TreeSet;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.OWL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class EntityTypeAdapter extends TypeAdapter<Entity> {
	private static final String IRI_JSON_FIELD_NAME = "iri";
	private static final String TYPE_JSON_FIELD_NAME = "typeIri";
	public static final String LAT_JSON_FIELD_NAME = "latitude";
	public static final String LON_JSON_FIELD_NAME = "longitude";

	private static final Logger LOG = LoggerFactory.getLogger(EntityTypeAdapter.class);

	private final EntityFactory entFact;
	private long orderIndexForNextRead;
	private final Deque<Entity> entitiesInProgress;	// contains LIFO stack of entities as
																	// they are being written
	private final Set<Entity> profilesAlreadyWritten;

	public EntityTypeAdapter(EntityFactory entityFactory) {
		super();
		entFact = ArgCheck.throwIfNull(entityFactory, "entityFactory");
		orderIndexForNextRead = 0;
		entitiesInProgress = new ArrayDeque<>();
		profilesAlreadyWritten = new TreeSet<>(Entity.COMPARATOR);
	}

	// Only for use by ObjectPropertyTypeAdapter:
	public void orderIndexForNextRead(long newValue) {
		orderIndexForNextRead = newValue;
	}

	@Override
	public Entity read(JsonReader rdr) throws IOException {
		EntityImpl result = new EntityImpl(null, entFact);
		if (rdr.peek() == JsonToken.BEGIN_OBJECT) {
			DatatypePropertyTypeAdapter wktAdapter = new DatatypePropertyTypeAdapter(result, Geo.asWKT);
			DatatypeProperty lat = null;
			DatatypeProperty lon = null;

			rdr.beginObject();
			while (rdr.peek() != JsonToken.END_OBJECT) {
				String fieldName = rdr.nextName();
				if (LAT_JSON_FIELD_NAME.equals(fieldName)) {
					lat = wktAdapter.read(rdr);
				} else if (LON_JSON_FIELD_NAME.equals(fieldName)) {
					lon = wktAdapter.read(rdr);
				} else {
					readObjectField(rdr, result, fieldName);
				}
			}
			rdr.endObject();

			// Set the orderIndex property, if applicable:
			orderIndex(result);

			// Special case for the latitude and longitude of Geometry objects:
			latLon(result, lat, lon);
		} else if (rdr.peek() == JsonToken.STRING) {
			result.iri(ResourceFactory.createResource(rdr.nextString()));
		} else {
			throw new IllegalStateException(String.format(
				"Expected object or IRI in JSON stream at %1$s", rdr.getPath()));
		}
		return result;
	}

	private void readObjectField(JsonReader rdr, EntityImpl result, String fieldName) throws IOException {
		if (IRI_JSON_FIELD_NAME.equals(fieldName)) {
			result.iri(ResourceFactory.createResource(rdr.nextString()));
		} else if (TYPE_JSON_FIELD_NAME.equals(fieldName)) {
			result.type(new RdfTypeTypeAdapter(result).read(rdr));
		} else {
			Resource prop = rdfPropTools().mapJsonFieldToPropIri(fieldName);
			if (prop == null) {
				throw new IllegalStateException(String.format(
					"Unrecognized field name '%1$s' at %2$s", fieldName, rdr.getPath()));
			}
			if (rdfPropTools().isDTProp(prop)) {
				result.addDTProp(new DatatypePropertyTypeAdapter(result, prop).read(rdr));
			} else if (rdfPropTools().isObjProp(prop)) {
				result.addObjProp(new ObjectPropertyTypeAdapter(result, prop, this).read(rdr));
			} else {
				throw new IllegalStateException(String.format("Property %1$s is neither an %2$s nor an %3$s",
					qnameFor(prop), qnameFor(OWL.DatatypeProperty), qnameFor(OWL.ObjectProperty)));
			}
		}
	}

	private void orderIndex(EntityImpl ent) {
		PropInfo orderIdxPI = rdfPropTools().propertyInfo(entFact.orderIndexPredicate());
		// Test whether ent is declared to have the orderIndex property:
		if (orderIdxPI.domainTypes().stream().anyMatch(ent::isOfType)) {
			DatatypeProperty orderIdxProp = ent.dtProp(entFact.orderIndexPredicate());
			orderIdxProp.clear();
			orderIdxProp.addValue(new RdfLiteral(orderIndexForNextRead));
		}
	}

	private void latLon(EntityImpl ent, DatatypeProperty lat, DatatypeProperty lon) {
		if (ent.isOfType(Geo.Geometry) && (lat != null || lon != null)) {
			WktLiteralPoint pt = new WktLiteralPoint(
				angleFromDTProp(ent, lat, LAT_JSON_FIELD_NAME),
				angleFromDTProp(ent, lon, LON_JSON_FIELD_NAME));
			ent.dtProp(Geo.asWKT).addValue(new RdfLiteral(pt));
		}
	}

	private double angleFromDTProp(EntityImpl ent, DatatypeProperty angleProp, String fieldName) {
		if (angleProp == null) {
			throw new IllegalStateException(String.format("Missing %1$s on %2$s",
				fieldName, qnameFor(ent.iri())));
		} else if (angleProp.values().size() != 1) { // NOPMD
			throw new IllegalStateException(String.format("More than one %1$s on %2$s",
				fieldName, qnameFor(ent.iri())));
		} else {
			String angleStr = angleProp.firstValue();
			try {
				return Double.parseDouble(angleStr);
			} catch (NumberFormatException ex) {
				throw new IllegalStateException(String.format("Bad %1$s on %2$s: '%3$s'",
					fieldName, qnameFor(ent.iri()), angleStr), ex);
			}
		}
	}

	@Override
	public void write(JsonWriter wtr, Entity ent) throws IOException {
		if (!ent.isFetched()) { // NOPMD
			@SuppressWarnings({ "resource", "unused" })
			JsonWriter tmp1 = wtr.value("notfetched:" + ent.iri().getURI());
		} else if (entitiesInProgress.contains(ent)) {
			LOG.warn("Writing nested entity to JSON as IRI-only: {}", qnameFor(ent.iri()));
			@SuppressWarnings({ "resource", "unused" })
			JsonWriter tmp1 = wtr.value("avoidingrecursion:" + ent.iri().getURI());
		} else if (profilesAlreadyWritten.contains(ent)) {
			LOG.warn("Writing previously-written profile to JSON as IRI-only: <{}>", qnameFor(ent.iri()));
			@SuppressWarnings({ "resource", "unused" })
			JsonWriter tmp1 = wtr.value("avoidingredundancy:" + ent.iri().getURI());
		} else {
			entitiesInProgress.addFirst(ent);
			if (ent.isOfType(entFact.rootEntityType())) {	//TODO: Do we need this test?  Why is this collection named after "profiles"?
				profilesAlreadyWritten.add(ent);
			}
			writeFullEntity(wtr, ent);
			entitiesInProgress.removeFirst();
		}
	}

	// Splitting this method out of the write() method above to keep the
	// cyclomatic complexity down
	private void writeFullEntity(JsonWriter wtr, Entity ent) throws IOException {
		RdfTypeTypeAdapter rdfTypeTA = new RdfTypeTypeAdapter();
		DatatypePropertyTypeAdapter dtPropTA = new DatatypePropertyTypeAdapter();
		ObjectPropertyTypeAdapter objPropTA = new ObjectPropertyTypeAdapter(this);

		@SuppressWarnings({ "resource", "unused" })
		JsonWriter tmp1 = wtr.beginObject();

		for (DatatypeProperty prop : ent.dtProps()) {
			if (Geo.asWKT.equals(prop.propIri())) {
				dtPropTA.write(wtr, prop); // Special case for lat-lon on Geometry objects
			} else if (!entFact.orderIndexPredicate().equals(prop.propIri())) {
				@SuppressWarnings({ "resource", "unused" })
				JsonWriter tmp2 = wtr.name(rdfPropTools().mapPropIriToJsonField(prop.propIri(), ent.type().values()));
				dtPropTA.write(wtr, prop);
			}
		}
		for (ObjectProperty prop : ent.objProps()) {
			@SuppressWarnings({ "resource", "unused" })
			JsonWriter tmp2 = wtr.name(rdfPropTools().mapPropIriToJsonField(prop.propIri(), ent.type().values()));
			objPropTA.write(wtr, prop);
		}
		@SuppressWarnings({ "resource", "unused" })
		JsonWriter tmp3 = wtr.name(TYPE_JSON_FIELD_NAME);
		rdfTypeTA.write(wtr, ent.type());

		@SuppressWarnings({ "resource", "unused" })
		JsonWriter tmp4 = wtr.name(IRI_JSON_FIELD_NAME);
		@SuppressWarnings({ "resource", "unused" })
		JsonWriter tmp5 = wtr.value(ent.iri().getURI());

		@SuppressWarnings({ "resource", "unused" })
		JsonWriter tmp6 = wtr.endObject();
	}

	private RdfPropertyTools rdfPropTools() {
		return entFact.rdfPropertyTools();
	}

	private String qnameFor(Resource iri) {
		return entFact.qName().qnameFor(iri);
	}
}

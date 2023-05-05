package com.bbn.parliament.kb_graph.index.spatial;

import org.apache.jena.graph.Node;
import org.locationtech.jts.geom.Geometry;

import com.bbn.parliament.kb_graph.index.Record;

/**
 * This class exists as something of a hack work-around for a shortcoming of the
 * JTS Geometry class. Normally, this class would not exist and we would simply
 * use {@code Record<Geometry>} instead. However, the JTS Geometry class does
 * not properly implement {@code hashCode()} and {@code equals()} methods. The
 * documentation for that class says this:
 *
 * <blockquote>#equals and #hashCode are not overridden, so that when two
 * topologically equal Geometries are added to HashMaps and HashSets, they
 * remain distinct. This behavior is desired in many cases.</blockquote>
 *
 * Why this is so is a bit of a mystery to me, but unfortunately we sometimes
 * need to test equality of these things. (See for example the {@code testIterator}
 * method in class {@code com.bbn.parliament.jena.query.index.IndexTestMethods}.)
 * As a partial work-around, we derive this class, override {@code equals}, and use the
 * {@code compareTo} method on Geometry to test for equality. I say partial, because
 * this does not help implement {@code hashCode}. So, I've disabled {@code hashCode} by throwing
 * {@code UnsupportedOperationException} from it. This means that {@code GeometryRecord} instances can
 * never be stored in a {@code HashMap} (as keys) or in a {@code HashSet}.
 *
 * @author rbattle
 */
public class GeometryRecord extends Record<Geometry> {
	/**
	 * Create a new instance.
	 *
	 * @param key a key
	 * @param value a value
	 * @return a new record.
	 */
	public static GeometryRecord create(Node key, Geometry value) {
		return new GeometryRecord(key, value);
	}

	/**
	 * Construct a new instance.
	 *
	 * @param key a key
	 * @param value a value
	 */
	protected GeometryRecord(Node key, Geometry value) {
		super(key, value);
	}

	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		throw new UnsupportedOperationException();
	}

	/** {@inheritDoc} */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		GeometryRecord other = (GeometryRecord) obj;
		if (getKey() == null) {
			if (other.getKey() != null) {
				return false;
			}
		} else if (!getKey().equals(other.getKey())) {
			return false;
		}
		if (getValue() == null) {
			if (other.getValue() != null) {
				return false;
			}
		} else if (getValue().compareTo(other.getValue()) != 0) {
			return false;
		}
		return true;
	}
}

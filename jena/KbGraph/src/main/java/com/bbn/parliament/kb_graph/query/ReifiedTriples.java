package com.bbn.parliament.kb_graph.query;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.shared.PrefixMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// A registry of managed triple reifications. {@code ReifiedTriples} maintains a
/// registry of Triple instances and their associated names. I.e., an entry in
/// this registry represents the following RDF:
///
/// {@snippet :
/// :s :p :o .
/// :name a rdf:Statement ;
///    rdf:subject :s ;
///    rdf:predicate :p ;
///    rdf:object :o .
/// }
///
/// Prior to Jena 4, Parliament did this in a simpler way: It sub-classed the
/// {@code Triple} class with the class {@code ReifiedTriple}, which added a
/// "name" field. Then anywhere we encountered a {@code Triple} instance, we
/// could test to see if it is a {@code ReifiedTriple} and retrieve the name if
/// so.
///
/// Starting with Jena 4, the {@code Triple} constructor is deprecated (and made
/// private in later versions), and so sub-classing is no longer possible. Thus we
/// create this registry that associates the name with a {@code Triple}
/// externally.
///
/// The problem with this approach is cases where {@code Triple} is used to
/// represent a triple pattern, i.e., it contains variables. For instance, you
/// might query for the subject, predicate, and object of a reified triple with a
/// particular name. This would result in the {@code Triple} (?s, ?p, ?o) being
/// inserted into this registry under a given IRI. Later on, a query looking for
/// all triples, meaning (?s, ?p, ?o) with no reification, might discover the
/// reification from the earlier query because then garbage collector hasn't
/// removed (?s, ?p, ?o) from the registry yet.'
///
/// @author iemmons
public class ReifiedTriples {
	private static final Logger LOG = LoggerFactory.getLogger(ReifiedTriples.class);

	private static class ReifiedTripleRegistryHolder {
		private static final ReifiedTriples INSTANCE = new ReifiedTriples();
	}

	/// Get the singleton instance of the index manager. This follows the "lazy
	/// initialization holder class" idiom for lazy initialization of a static field.
	/// See Item 83 of Effective Java, Third Edition, by Joshua Bloch for details.
	///
	/// @return the instance
	public static ReifiedTriples getInstance() {
		return ReifiedTripleRegistryHolder.INSTANCE;
	}

	private final Map<Triple, Node> registry;

	private ReifiedTriples() {
		registry = Collections.synchronizedMap(new WeakHashMap<>());
	}

	public Triple createReifiedTriple(Node name, Node s, Node p, Node o) {
		Objects.requireNonNull(s, "s");
		Objects.requireNonNull(p, "p");
		Objects.requireNonNull(o, "o");

		var triple = Triple.create(s, p, o);
		registry.put(triple, Objects.requireNonNull(name, "name"));
		LOG.debug("Reified triple {} -> {}", name, triple);
		return triple;
	}

	public Optional<Node> getName(Triple triple) {
		var name = registry.get(triple);
		if (name == null) {
			// This handles the case in which triple is a copy of the
			// Triple instance that was used as a key in registry. Ideally this should not
			// be necessary, so id the other problems with this class detailed above are
			// fixed, then this bit should be deleted.
			var result = registry.entrySet().stream()
				.filter(e -> e.getKey().equals(triple))
				.map(Map.Entry::getValue)
				.findFirst();
			result.ifPresent(n -> LOG.debug("Found reified triple by value: {} -> {}", n, triple));
			return result;
		} else {
			LOG.debug("Found reified triple by reference: {} -> {}", name, triple);
			return Optional.of(name);
		}
	}

	public String toString(Triple triple, PrefixMapping pm) {
		var name = getName(triple);
		var nameStr = (name.isEmpty()) ? "" : name.get().toString(pm);
		var fmt = (name.isEmpty()) ? "[not-reified] %2$s" : "[%1$s] %2$s";
		return fmt.formatted(nameStr, triple.toString(pm));
	}
}

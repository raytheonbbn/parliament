// FILE ADDED BY CODEX CODING AGENT
import React, { useEffect, useMemo, useState } from "https://esm.sh/react@18.3.1";
import { createRoot } from "https://esm.sh/react-dom@18.3.1/client";

const h = React.createElement;

const routes = [
	["/", "Home"],
	["/query", "Query"],
	["/explore", "Explore"],
	["/update", "SPARQL/Update"],
	["/insert", "Insert Data"],
	["/export", "Export"],
	["/indexes", "Indexes"],
	["/admin", "Admin"],
];

function api(path) {
	return fetch(`api/${path}`, { headers: { Accept: "application/json" } })
		.then((response) => {
			if (!response.ok) {
				throw new Error(`${response.status} ${response.statusText}`);
			}
			return response.json();
		});
}

function useHashRoute() {
	const read = () => {
		const raw = window.location.hash.replace(/^#/, "") || "/";
		const [path, query = ""] = raw.split("?");
		return { path, params: new URLSearchParams(query) };
	};
	const [route, setRoute] = useState(read);
	useEffect(() => {
		const onHashChange = () => setRoute(read());
		window.addEventListener("hashchange", onHashChange);
		return () => window.removeEventListener("hashchange", onHashChange);
	}, []);
	return route;
}

function App() {
	const route = useHashRoute();
	const [metadata, setMetadata] = useState(null);
	const [error, setError] = useState("");

	useEffect(() => {
		api("metadata").then(setMetadata).catch((ex) => setError(ex.message));
	}, []);

	const page = (() => {
		if (route.path === "/query") return h(QueryPage, { metadata });
		if (route.path === "/explore") return h(ExplorePage, { route });
		if (route.path === "/update") return h(UpdatePage, { metadata });
		if (route.path === "/insert") return h(InsertPage, { metadata });
		if (route.path === "/export") return h(ExportPage, { metadata });
		if (route.path === "/indexes") return h(IndexesPage);
		if (route.path === "/admin") return h(AdminPage);
		return h(HomePage, { metadata, loadError: error });
	})();

	return h("div", { className: "app-shell" },
		h("aside", { className: "sidebar" },
			h("div", { className: "brand" }, "Parliament Query Server"),
			h("nav", { className: "nav" },
				routes.map(([path, label]) => h("a", {
					key: path,
					className: route.path === path ? "active" : "",
					href: `#${path}`,
				}, label)),
			),
		),
		h("main", { className: "content" }, page),
	);
}

function PageHeader({ title, action }) {
	return h("div", { className: "page-header" },
		h("h1", null, title),
		action || null,
	);
}

function Loading({ label = "Loading..." }) {
	return h("p", { className: "status" }, label);
}

function ErrorMessage({ message }) {
	return message ? h("p", { className: "status error" }, message) : null;
}

function HomePage({ metadata, loadError }) {
	return h(React.Fragment, null,
		h(PageHeader, { title: "Parliament Query Server" }),
		h(ErrorMessage, { message: loadError }),
		h("div", { className: "grid" },
			h("section", { className: "panel" },
				h("h2", null, "Operations"),
				h("ul", { className: "link-list" },
					routes.slice(1).map(([path, label]) => h("li", { key: path },
						h("a", { href: `#${path}` }, label),
					)),
				),
			),
			h(MemoryPanel, { initialMemory: metadata?.memory }),
		),
		h(GraphsPanel, { graphs: metadata?.graphs }),
		h("section", { className: "panel" },
			h("h2", null, "SPARQL"),
			h("p", null, "SPARQL is defined by 3 documents:"),
			h("ul", { className: "link-list" },
				h("li", null, h("a", { href: "http://www.w3.org/TR/rdf-sparql-query/" }, "SPARQL query language")),
				h("li", null, h("a", { href: "http://www.w3.org/TR/rdf-sparql-protocol/" }, "SPARQL protocol")),
				h("li", null, h("a", { href: "http://www.w3.org/TR/rdf-sparql-XMLres/" }, "SPARQL XML results format")),
			),
		),
	);
}

function MemoryPanel({ initialMemory }) {
	const [memory, setMemory] = useState(initialMemory);
	const [error, setError] = useState("");

	useEffect(() => {
		setMemory(initialMemory);
	}, [initialMemory]);

	useEffect(() => {
		let stopped = false;
		const load = () => api("memory")
			.then((data) => {
				if (!stopped) {
					setMemory(data);
					setError("");
				}
			})
			.catch((ex) => !stopped && setError(ex.message));
		const id = setInterval(load, 3000);
		load();
		return () => {
			stopped = true;
			clearInterval(id);
		};
	}, []);

	return h("section", { className: "panel" },
		h("h2", null, "Java Memory Usage (KB)"),
		h(ErrorMessage, { message: error }),
		memory ? h("table", null,
			h("thead", null, h("tr", null, h("th", null, "Type"), h("th", null, "Heap"), h("th", null, "Non-Heap"))),
			h("tbody", null,
				h("tr", null, h("td", null, "Used"), h("td", { className: "count" }, memory.heapUsedKb), h("td", { className: "count" }, memory.nonHeapUsedKb)),
				h("tr", null, h("td", null, "Max"), h("td", { className: "count" }, memory.heapMaxKb), h("td", { className: "count" }, memory.nonHeapMaxKb)),
			),
		) : h(Loading, null),
	);
}

function GraphsPanel({ graphs }) {
	return h("section", { className: "panel" },
		h("h2", null, "Graphs"),
		graphs ? h(GraphTable, { graphs }) : h(Loading, null),
	);
}

function GraphTable({ graphs }) {
	return h("div", { className: "table-wrap" },
		h("table", null,
			h("tbody", null, graphs.map((graph, index) => h("tr", {
				key: graph.uri,
				className: index % 2 ? "" : "alternate",
			},
				h("td", null, h(ValueLink, { value: graph })),
				h("td", null, !graph.defaultGraph && graph.label ? graph.label : ""),
			))),
		),
	);
}

function QueryPage({ metadata }) {
	const [display, setDisplay] = useState("html");
	const query = useMemo(() => `${prefixLines(metadata, "sparql")}

SELECT DISTINCT
?class
WHERE {
   ?class a owl:Class .
   FILTER (!isblank(?class))
}
`, [metadata]);

	return h(React.Fragment, null,
		h(PageHeader, { title: "Query" }),
		h("section", { className: "panel" },
			h("h2", null, "SELECT or CONSTRUCT query"),
			h("form", { action: "sparql", method: "post", onSubmit: (event) => setDisplayHiddenFields(event.currentTarget, display) },
				h("textarea", { name: "query", id: "code", spellCheck: "false", defaultValue: query }),
				h("div", { className: "option-list" },
					displayOptions.map(([value, label]) => h("label", { key: value },
						h("input", {
							type: "radio",
							name: "display",
							value,
							checked: display === value,
							onChange: () => setDisplay(value),
						}),
						label,
						value === "custom" ? h("input", { type: "text", name: "custom", defaultValue: "" }) : null,
					)),
				),
				h("input", { type: "hidden", name: "stylesheet", defaultValue: "" }),
				h("input", { type: "hidden", name: "output", defaultValue: "" }),
				h("input", { type: "submit", value: "Get Results" }),
			),
		),
		h(GraphsPanel, { graphs: metadata?.graphs }),
	);
}

const displayOptions = [
	["html", "HTML table"],
	["count", "Count only"],
	["csv", "CSV"],
	["xml", "SPARQL result set"],
	["custom", "Custom XSLT"],
	["json", "JSON"],
];

function setDisplayHiddenFields(form, display) {
	form.stylesheet.value = "";
	form.output.value = "";
	if (display === "html") form.stylesheet.value = "/xml-to-html.xsl";
	if (display === "count") form.stylesheet.value = "/xml-to-count.xsl";
	if (display === "csv") form.stylesheet.value = "/xml-to-csv.xsl";
	if (display === "xml") form.output.value = "xml";
	if (display === "custom") form.stylesheet.value = form.custom.value;
	if (display === "json") form.output.value = "json";
}

function UpdatePage({ metadata }) {
	const update = useMemo(() => `${prefixLines(metadata, "sparql")}


INSERT DATA
{
#triples
}

DELETE DATA
{
#triples
}
`, [metadata]);

	return h(React.Fragment, null,
		h(PageHeader, { title: "SPARQL/Update Query" }),
		h("section", { className: "panel" },
			h("h2", null, "SPARQL/Update Query"),
			h("form", { action: "sparql", method: "post" },
				h("textarea", { name: "update", spellCheck: "false", defaultValue: update }),
				h("div", { className: "form-row" }, h("input", { type: "submit", value: "Execute Update" })),
			),
		),
	);
}

function InsertPage({ metadata }) {
	const statements = useMemo(() => `${prefixLines(metadata, "turtle")}

`, [metadata]);
	return h(React.Fragment, null,
		h(PageHeader, { title: "Insert Data" }),
		h("section", { className: "panel" },
			h("h2", null, "Import Repository"),
			h("form", {
				action: "bulk/insert",
				method: "post",
				encType: "multipart/form-data",
				acceptCharset: "UTF-8",
				onSubmit: () => confirm("This will replace all existing data in the repository. Are you sure?"),
			},
				h("p", null, "Import an entire repository from a previous Parliament export (ZIP file)"),
				h("input", { type: "file", name: "statements", size: "70" }),
				h("input", { type: "hidden", name: "import", value: "yes" }),
				h("div", { className: "form-row" }, h("input", { type: "submit", value: "Import Repository" })),
			),
		),
		h("section", { className: "panel" },
			h("h2", null, "File Insert"),
			h("form", { action: "bulk/insert", method: "post", encType: "multipart/form-data", acceptCharset: "UTF-8" },
				h(DataFormatField, { auto: true }),
				h("input", { type: "file", name: "statements", size: "70" }),
				h(GraphSelect, { graphs: metadata?.graphs }),
				h("div", { className: "form-row" }, h("input", { type: "submit", value: "Insert File" })),
			),
		),
		h("section", { className: "panel" },
			h("h2", null, "Text Insert"),
			h("form", { action: "bulk/insert", method: "post" },
				h(DataFormatField, null),
				h("textarea", { name: "statements", spellCheck: "false", defaultValue: statements }),
				h(GraphSelect, { graphs: metadata?.graphs }),
				h("div", { className: "form-row" }, h("input", { type: "submit", value: "Insert Data" })),
			),
		),
	);
}

function DataFormatField({ auto = false }) {
	const options = auto
		? [["AUTO", "Auto Detect"], ...dataFormats]
		: dataFormats;
	return h("div", { className: "form-row" },
		h("label", null, "Data to Insert"),
		h("select", { name: "dataFormat" }, options.map(([value, label]) => h("option", { key: value, value }, label))),
		h("span", { className: "meta" }, "Format"),
	);
}

const dataFormats = [
	["TURTLE", "Turtle"],
	["N3", "N3"],
	["N-TRIPLES", "N-TRIPLES"],
	["RDF/XML", "RDF/XML"],
];

function GraphSelect({ graphs }) {
	return h("div", { className: "form-row" },
		h("label", null, "Named graph for insertion:"),
		h("select", { name: "graph" },
			(graphs || [{ name: "", display: "Default Graph" }]).map((graph) => h("option", {
				key: graph.uri || "default",
				value: graph.name || "",
			}, graph.defaultGraph ? "Default Graph" : graph.name)),
		),
	);
}

function ExportPage({ metadata }) {
	const graphs = metadata?.graphs || [];
	return h(React.Fragment, null,
		h(PageHeader, { title: "Export Data" }),
		h("section", { className: "panel" },
			h("h2", null, "Export Repository"),
			h("form", { action: "bulk/export", method: "post" },
				h("label", null, h("input", { type: "radio", name: "exportAll", value: "yes", defaultChecked: true }), " Entire Repository (ZIP file)"),
				h(ExportFormatField, null),
				h("input", { type: "submit", value: "Export Repository" }),
			),
		),
		h("section", { className: "panel" },
			h("h2", null, "Export Graph"),
			h("form", { action: "bulk/export", method: "post" },
				h("div", { className: "option-list" }, graphs.map((graph) => h("label", { key: graph.uri },
					h("input", {
						type: "radio",
						name: "graph",
						value: graph.defaultGraph ? "" : graph.uri,
						defaultChecked: graph.defaultGraph,
					}),
					graph.defaultGraph ? "Default Graph" : graph.display,
				))),
				h(ExportFormatField, null),
				h("input", { type: "submit", value: "Export Graph" }),
			),
		),
	);
}

function ExportFormatField() {
	return h("div", { className: "form-row" },
		h("label", null, "Data Format:"),
		h("select", { name: "dataFormat" },
			["N-TRIPLES", "N3", "RDF/XML", "TURTLE"].map((format) => h("option", { key: format, value: format }, format === "TURTLE" ? "Turtle" : format)),
		),
	);
}

function ExplorePage({ route }) {
	const value = route.params.get("value");
	return value ? h(ExplorerDetail, { value, useLabels: route.params.get("useLabels") === "yes" }) : h(ExplorerIndex);
}

function ExplorerIndex() {
	const [data, setData] = useState(null);
	const [error, setError] = useState("");
	const [value, setValue] = useState("");

	useEffect(() => {
		api("explorer-index").then(setData).catch((ex) => setError(ex.message));
	}, []);

	const submit = (event) => {
		event.preventDefault();
		const normalized = normalizeExplorerValue(value);
		window.location.hash = `/explore?value=${encodeURIComponent(normalized)}`;
	};

	return h(React.Fragment, null,
		h(PageHeader, { title: "Explore Repository" }),
		h(ErrorMessage, { message: error }),
		h("section", { className: "panel" },
			h("form", { onSubmit: submit, acceptCharset: "UTF-8" },
				h("label", null, "Enter a URI to start the exploration with:"),
				h("div", { className: "form-row" },
					h("input", { type: "text", size: "70", value, onChange: (event) => setValue(event.target.value) }),
					h("input", { type: "submit", value: "Explore" }),
				),
			),
		),
		data ? h(GraphsPanel, { graphs: data.graphs }) : h(Loading, null),
		data && (data.classes.length || data.properties.length) ? h("section", { className: "panel" },
			h("h2", null, "Classes and Properties"),
			h("div", { className: "split" },
				h(ResourceList, { title: "Classes", values: data.classes }),
				h(ResourceList, { title: "Properties", values: data.properties }),
			),
		) : null,
	);
}

function ExplorerDetail({ value, useLabels }) {
	const [data, setData] = useState(null);
	const [error, setError] = useState("");

	useEffect(() => {
		const params = new URLSearchParams({ value });
		if (useLabels) params.set("useLabels", "yes");
		api(`explorer?${params}`).then(setData).catch((ex) => setError(ex.message));
	}, [value, useLabels]);

	const toggleLabels = () => {
		const params = new URLSearchParams({ value });
		if (!useLabels) params.set("useLabels", "yes");
		window.location.hash = `/explore?${params}`;
	};

	return h(React.Fragment, null,
		h(PageHeader, {
			title: "Explore Repository",
			action: h("a", { className: "button secondary", href: "#/explore" }, "New Search"),
		}),
		h(ErrorMessage, { message: error }),
		data ? h(React.Fragment, null,
			h("section", { className: "panel" },
				h("p", null, "Showing statements for: ", h("b", null, data.value?.display || "")),
				h("label", null,
					h("input", { type: "checkbox", checked: useLabels, onChange: toggleLabels }),
					" Use resource labels in overview",
				),
			),
			h(StatementPanel, { title: "Statements with this value as subject", statements: data.asSubject, graph: true }),
			h(StatementPanel, { title: "Statements with this value as predicate", statements: data.asPredicate, graph: true }),
			h(StatementPanel, { title: "Statements with this value as object", statements: data.asObject, graph: true }),
			data.inGraph?.length ? h(StatementPanel, { title: "Statements in this graph", statements: data.inGraph, graph: false }) : null,
		) : h(Loading, null),
	);
}

function ResourceList({ title, values }) {
	return h("div", null,
		h("h3", null, title),
		values.length ? values.map((value) => h("div", { key: value.value }, h(ValueLink, { value }))) : h("p", { className: "status" }, "None"),
	);
}

function StatementPanel({ title, statements, graph }) {
	return h("section", { className: "panel" },
		h("h2", null, title),
		statements?.length ? h("div", { className: "table-wrap" },
			h("table", null,
				h("thead", null, h("tr", null,
					graph ? h("th", null, "graph") : null,
					h("th", null, "subject"),
					h("th", null, "predicate"),
					h("th", null, "object"),
				)),
				h("tbody", null, statements.map((statement, index) => h("tr", { key: index },
					graph ? h("td", { className: "value" }, h(ValueLink, { value: statement.graph })) : null,
					h("td", { className: "value" }, h(ValueLink, { value: statement.subject })),
					h("td", { className: "value" }, h(ValueLink, { value: statement.predicate })),
					h("td", { className: "value" }, h(ValueLink, { value: statement.object })),
				))),
			),
		) : h("div", { className: "empty" }, "-- no statements found --"),
	);
}

function ValueLink({ value }) {
	if (!value) return null;
	return h("a", { href: `#/explore?value=${encodeURIComponent(value.value)}` },
		h("span", { className: "value" }, value.display),
	);
}

function IndexesPage() {
	const [data, setData] = useState(null);
	const [error, setError] = useState("");
	const [busyGraph, setBusyGraph] = useState("");

	const load = () => api("indexes").then(setData).catch((ex) => setError(ex.message));
	useEffect(load, []);

	const updateIndexing = (graphUri, enabled) => {
		setBusyGraph(graphUri);
		const update = `INSERT {} WHERE {<${graphUri}> <${data.propertyFunctionNamespace}enableIndexing> "${enabled}"^^<http://www.w3.org/2001/XMLSchema#boolean> }`;
		postSparqlUpdate(update)
			.then(() => setTimeout(load, 3000))
			.catch((ex) => setError(ex.message))
			.finally(() => setBusyGraph(""));
	};

	return h(React.Fragment, null,
		h(PageHeader, { title: "Indexes" }),
		h(ErrorMessage, { message: error }),
		data ? data.factories.length ? h("section", { className: "panel" },
			h("div", { className: "table-wrap" },
				h("table", { id: "indexes" },
					h("thead", null, h("tr", null,
						h("th", null, "Graph"),
						data.factories.map((factory) => h("th", { key: factory }, factory)),
						h("th", null, ""),
					)),
					h("tbody", null, data.graphs.map((graph) => h("tr", { key: graph.uri },
						h("td", null, h(ValueLink, { value: graph })),
						data.factories.map((factory) => h("td", { key: factory, className: "count" },
							graph.counts[factory] == null ? "-" : graph.counts[factory],
						)),
						h("td", null,
							h("button", {
								className: "secondary",
								disabled: graph.enabled || busyGraph === graph.uri,
								onClick: () => updateIndexing(graph.uri, true),
							}, "Create All"),
							" ",
							h("button", {
								className: "secondary",
								disabled: !graph.enabled || busyGraph === graph.uri,
								onClick: () => updateIndexing(graph.uri, false),
							}, "Delete All"),
						),
					))),
				),
			),
		) : h("p", { className: "status" }, "None loaded") : h(Loading, null),
	);
}

function AdminPage() {
	const [tracked, setTracked] = useState([]);
	const [paused, setPaused] = useState(false);
	const [error, setError] = useState("");

	const loadTrackers = () => fetch("tracker", { headers: { Accept: "application/json" } })
		.then((response) => response.json())
		.then((data) => {
			setTracked(data || []);
			setError("");
		})
		.catch((ex) => setError(ex.message));

	useEffect(() => {
		if (paused) return undefined;
		loadTrackers();
		const id = setInterval(loadTrackers, 1000);
		return () => clearInterval(id);
	}, [paused]);

	const stopQuery = (id) => fetch("tracker", {
		method: "POST",
		headers: { "Content-Type": "application/x-www-form-urlencoded" },
		body: new URLSearchParams({ id }),
	}).then(loadTrackers);

	return h(React.Fragment, null,
		h(PageHeader, { title: "Administrator Console" }),
		h("section", { className: "panel" },
			h("h2", null, "Flush Data"),
			h("form", { action: "bulk/flush", method: "post" },
				h("span", null, "Flush in-memory statements to disk: "),
				h("input", { type: "submit", value: "Flush Data" }),
			),
		),
		h("section", { className: "panel" },
			h("div", { className: "page-header" },
				h("h2", null, tracked.length ? `${tracked.length} Queries` : "No Queries"),
				h("div", null,
					h("button", { className: "secondary", onClick: () => setPaused(!paused) }, paused ? "Resume Updates" : "Pause Updates"),
					" ",
					h("button", {
						className: "danger",
						disabled: tracked.length === 0,
						onClick: () => Promise.all(tracked.map((item) => stopQuery(item.id))),
					}, "Cancel All"),
				),
			),
			h(ErrorMessage, { message: error }),
			tracked.length ? h("div", { className: "tracker-list" },
				tracked.map((tracker) => h("div", { className: "tracker", key: tracker.id },
					h("div", null,
						h("b", null, `ID: ${tracker.id}`),
						` (${Math.round((tracker.currentTime - tracker.created) / 1000)} seconds old) `,
						tracker.cancellable ? h("button", { className: "secondary", onClick: () => stopQuery(tracker.id) }, "Stop") : null,
					),
					h("div", { className: "meta" }, `From: ${tracker.creator}`),
					h("div", { className: "meta" }, `Created: ${new Date(tracker.created)}`),
					tracker.started ? h("div", { className: "meta" }, `Started: ${new Date(tracker.started)}`) : null,
					h("div", { className: "meta" }, `Status: ${tracker.status}`),
					h("pre", null, tracker.display),
				)),
			) : h("div", { className: "empty" }, "No active queries"),
		),
	);
}

function prefixLines(metadata, syntax) {
	const prefixes = metadata?.prefixes || {};
	return Object.keys(prefixes).sort().map((key) => {
		if (syntax === "turtle") {
			return `@prefix ${key}: <${prefixes[key]}> .`;
		}
		return `PREFIX ${key}: <${prefixes[key]}>`;
	}).join("\n");
}

function normalizeExplorerValue(value) {
	const trimmed = value.trim();
	if (!trimmed || trimmed.startsWith("<") || trimmed.startsWith("_:") || trimmed.startsWith("\"")) {
		return trimmed;
	}
	return `<${trimmed}>`;
}

function postSparqlUpdate(update) {
	return fetch("sparql", {
		method: "POST",
		headers: { "Content-Type": "application/x-www-form-urlencoded" },
		body: new URLSearchParams({ update }),
	}).then((response) => {
		if (!response.ok) throw new Error(`${response.status} ${response.statusText}`);
	});
}

createRoot(document.getElementById("root")).render(h(App));

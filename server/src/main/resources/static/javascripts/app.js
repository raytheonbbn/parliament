// FILE ADDED BY CODEX CODING AGENT
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

const state = {
	route: readRoute(),
	metadata: null,
	loadError: "",
	memory: null,
	memoryError: "",
	explorerIndex: null,
	explorerData: null,
	explorerError: "",
	indexes: null,
	indexesError: "",
	indexesBusyGraph: "",
	trackers: [],
	trackersError: "",
	trackersPaused: false,
	modal: null,
	timers: [],
	loadedExplorerKey: "",
	indexesLoaded: false,
	trackersLoaded: false,
};

function api(path) {
	return fetch(`api/${path}`, { headers: { Accept: "application/json" } })
		.then((response) => {
			if (!response.ok) {
				throw new Error(`${response.status} ${response.statusText}`);
			}
			return response.json();
		});
}

function readRoute() {
	const raw = window.location.hash.replace(/^#/, "") || "/";
	const [path, query = ""] = raw.split("?");
	return { path, params: new URLSearchParams(query) };
}

function h(tag, attrs, ...children) {
	const node = document.createElement(tag);
	for (const [key, value] of Object.entries(attrs || {})) {
		if (value == null || value === false) {
			continue;
		}
		if (key === "className") {
			node.className = value;
		} else if (key === "htmlFor") {
			node.htmlFor = value;
		} else if (key.startsWith("on") && typeof value === "function") {
			node.addEventListener(key.slice(2).toLowerCase(), value);
		} else if (key === "checked" || key === "disabled" || key === "defaultChecked") {
			node[key === "defaultChecked" ? "checked" : key] = Boolean(value);
		} else if (key === "encType") {
			node.enctype = value;
		} else if (key === "acceptCharset") {
			node.acceptCharset = value;
		} else if (key === "ariaLabel") {
			node.setAttribute("aria-label", value);
		} else if (key === "defaultValue") {
			node.value = value;
		} else {
			node.setAttribute(key, value);
		}
	}
	append(node, children);
	return node;
}

function append(parent, children) {
	for (const child of children.flat(Infinity)) {
		if (child == null || child === false) {
			continue;
		}
		parent.append(child instanceof Node ? child : document.createTextNode(String(child)));
	}
}

function clearTimers() {
	state.timers.forEach((id) => clearInterval(id));
	state.timers = [];
}

function render() {
	clearTimers();
	const root = document.getElementById("root");
	root.replaceChildren(appShell());
	startPageEffects();
}

function appShell() {
	return h("div", { className: "app-shell" },
		h("aside", { className: "sidebar" },
			h("div", { className: "brand" }, "Parliament Query Server"),
			h("nav", { className: "nav" }, routes.map(([path, label]) => h("a", {
				className: state.route.path === path ? "active" : "",
				href: `#${path}`,
			}, label))),
		),
		h("main", { className: "content" }, page()),
		modalView(),
	);
}

function page() {
	switch (state.route.path) {
		case "/query": return queryPage();
		case "/explore": return explorePage();
		case "/update": return updatePage();
		case "/insert": return insertPage();
		case "/export": return exportPage();
		case "/indexes": return indexesPage();
		case "/admin": return adminPage();
		default: return homePage();
	}
}

function pageHeader(title, action) {
	return h("div", { className: "page-header" }, h("h1", null, title), action || null);
}

function loading(label = "Loading...") {
	return h("p", { className: "status" }, label);
}

function errorMessage(message) {
	return message ? h("p", { className: "status error" }, message) : null;
}

function showModal({ title, body, kind = "info", actions = [] }) {
	state.modal = { title, body, kind, actions };
	render();
}

function closeModal() {
	state.modal = null;
	render();
}

function showMessage(title, body, kind = "info") {
	showModal({ title, body, kind });
}

function showConfirm({ title, body, confirmLabel, onConfirm, danger = false }) {
	showModal({
		title,
		body,
		kind: danger ? "warning" : "info",
		actions: [
			{ label: "Cancel", className: "secondary", onClick: closeModal },
			{ label: confirmLabel, className: danger ? "danger" : "", onClick: () => { closeModal(); onConfirm(); } },
		],
	});
}

function modalView() {
	if (!state.modal) return null;
	const modal = state.modal;
	const actions = modal.actions?.length ? modal.actions : [{ label: "Close", className: "", onClick: closeModal }];
	return h("div", { className: "modal-backdrop", role: "presentation" },
		h("section", {
			className: `modal ${modal.kind}`,
			role: "dialog",
			"aria-modal": "true",
			"aria-labelledby": "modal-title",
		},
			h("div", { className: "modal-header" },
				h("h2", { id: "modal-title" }, modal.title),
				h("button", { className: "icon-button secondary", type: "button", ariaLabel: "Close", onClick: closeModal }, "x"),
			),
			h("div", { className: "modal-body" }, messageBody(modal.body)),
			h("div", { className: "modal-actions" }, actions.map((action) => h("button", {
				type: "button",
				className: action.className || "",
				onClick: action.onClick,
			}, action.label))),
		),
	);
}

function messageBody(body) {
	if (body instanceof Node) return body;
	const text = String(body || "").trim();
	return text.includes("\n") ? h("pre", null, text) : h("p", null, text || "Done.");
}

function homePage() {
	return h("div", null,
		pageHeader("Parliament Query Server"),
		errorMessage(state.loadError),
		h("div", { className: "grid" },
			h("section", { className: "panel" },
				h("h2", null, "Operations"),
				h("ul", { className: "link-list" },
					routes.slice(1).map(([path, label]) => h("li", null, h("a", { href: `#${path}` }, label))),
				),
			),
			memoryPanel(),
		),
		graphsPanel(state.metadata?.graphs),
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

function memoryPanel() {
	const memory = state.memory || state.metadata?.memory;
	return h("section", { className: "panel" },
		h("h2", null, "Java Memory Usage (KB)"),
		errorMessage(state.memoryError),
		memory ? h("table", null,
			h("thead", null, h("tr", null, h("th", null, "Type"), h("th", null, "Heap"), h("th", null, "Non-Heap"))),
			h("tbody", null,
				h("tr", null, h("td", null, "Used"), h("td", { className: "count" }, memory.heapUsedKb), h("td", { className: "count" }, memory.nonHeapUsedKb)),
				h("tr", null, h("td", null, "Max"), h("td", { className: "count" }, memory.heapMaxKb), h("td", { className: "count" }, memory.nonHeapMaxKb)),
			),
		) : loading(),
	);
}

function graphsPanel(graphs) {
	return h("section", { className: "panel" },
		h("h2", null, "Graphs"),
		graphs ? graphTable(graphs) : loading(),
	);
}

function graphTable(graphs) {
	return h("div", { className: "table-wrap" },
		h("table", null,
			h("tbody", null, graphs.map((graph, index) => h("tr", { className: index % 2 ? "" : "alternate" },
				h("td", null, valueLink(graph)),
				h("td", null, !graph.defaultGraph && graph.label ? graph.label : ""),
			))),
		),
	);
}

function queryPage() {
	const query = `${prefixLines(state.metadata, "sparql")}

SELECT DISTINCT
?class
WHERE {
   ?class a owl:Class .
   FILTER (!isblank(?class))
}
`;
	const form = h("form", { action: "sparql", method: "post" },
		h("textarea", { name: "query", id: "code", spellCheck: "false", defaultValue: query }),
		h("div", { className: "option-list" }, displayOptions.map(([value, label], index) => {
			const custom = value === "custom" ? h("input", { type: "text", name: "custom", defaultValue: "" }) : null;
			return h("label", null,
				h("input", { type: "radio", name: "display", value, checked: index === 0 }),
				label,
				custom,
			);
		})),
		h("input", { type: "hidden", name: "stylesheet", value: "" }),
		h("input", { type: "hidden", name: "output", value: "" }),
		h("input", { type: "submit", value: "Get Results" }),
	);
	form.addEventListener("submit", (event) => submitQuery(event, form));
	return h("div", null,
		pageHeader("Query"),
		h("section", { className: "panel" }, h("h2", null, "SELECT or CONSTRUCT query"), form),
		graphsPanel(state.metadata?.graphs),
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

function updatePage() {
	const update = `${prefixLines(state.metadata, "sparql")}


INSERT DATA
{
#triples
}

DELETE DATA
{
#triples
}
`;
	const form = h("form", { action: "sparql", method: "post" },
		h("textarea", { name: "update", spellCheck: "false", defaultValue: update }),
		h("div", { className: "form-row" }, h("input", { type: "submit", value: "Execute Update" })),
	);
	form.addEventListener("submit", (event) => submitFormAction(event, form, {
		successTitle: "Update Complete",
		successMessage: "The SPARQL update was executed.",
		errorTitle: "Update Failed",
		onSuccess: refreshRepositoryState,
	}));
	return h("div", null,
		pageHeader("SPARQL/Update Query"),
		h("section", { className: "panel" },
			h("h2", null, "SPARQL/Update Query"),
			form,
		),
	);
}

function insertPage() {
	const statements = `${prefixLines(state.metadata, "turtle")}

`;
	const importForm = h("form", {
		action: "bulk/insert",
		method: "post",
		encType: "multipart/form-data",
		acceptCharset: "UTF-8",
	},
		h("p", null, "Import an entire repository from a previous Parliament export (ZIP file)"),
		h("input", { type: "file", name: "statements", size: "70" }),
		h("input", { type: "hidden", name: "import", value: "yes" }),
		h("div", { className: "form-row" }, h("input", { type: "submit", value: "Import Repository" })),
	);
	importForm.addEventListener("submit", (event) => {
		event.preventDefault();
		const request = buildFormRequest(importForm);
		showConfirm({
			title: "Import Repository",
			body: "This will replace all existing data in the repository.",
			confirmLabel: "Import Repository",
			danger: true,
			onConfirm: () => runPreparedAction(request, {
				successTitle: "Import Complete",
				successMessage: "The repository import finished.",
				errorTitle: "Import Failed",
				onSuccess: refreshRepositoryState,
			}),
		});
	});

	const fileForm = h("form", { action: "bulk/insert", method: "post", encType: "multipart/form-data", acceptCharset: "UTF-8" },
		dataFormatField(true),
		h("input", { type: "file", name: "statements", size: "70" }),
		graphSelect(state.metadata?.graphs),
		h("div", { className: "form-row" }, h("input", { type: "submit", value: "Insert File" })),
	);
	fileForm.addEventListener("submit", (event) => submitFormAction(event, fileForm, {
		successTitle: "Insert Complete",
		successMessage: "The uploaded data was inserted.",
		errorTitle: "Insert Failed",
		onSuccess: refreshRepositoryState,
	}));

	const textForm = h("form", { action: "bulk/insert", method: "post" },
		dataFormatField(false),
		h("textarea", { name: "statements", spellCheck: "false", defaultValue: statements }),
		graphSelect(state.metadata?.graphs),
		h("div", { className: "form-row" }, h("input", { type: "submit", value: "Insert Data" })),
	);
	textForm.addEventListener("submit", (event) => submitFormAction(event, textForm, {
		successTitle: "Insert Complete",
		successMessage: "The data was inserted.",
		errorTitle: "Insert Failed",
		onSuccess: refreshRepositoryState,
	}));

	return h("div", null,
		pageHeader("Insert Data"),
		h("section", { className: "panel" }, h("h2", null, "Import Repository"), importForm),
		h("section", { className: "panel" }, h("h2", null, "File Insert"), fileForm),
		h("section", { className: "panel" }, h("h2", null, "Text Insert"), textForm),
	);
}

function dataFormatField(auto) {
	const options = auto ? [["AUTO", "Auto Detect"], ...dataFormats] : dataFormats;
	return h("div", { className: "form-row" },
		h("label", null, "Data to Insert"),
		h("select", { name: "dataFormat" }, options.map(([value, label]) => h("option", { value }, label))),
		h("span", { className: "meta" }, "Format"),
	);
}

const dataFormats = [
	["TURTLE", "Turtle"],
	["N3", "N3"],
	["N-TRIPLES", "N-TRIPLES"],
	["RDF/XML", "RDF/XML"],
];

function graphSelect(graphs) {
	return h("div", { className: "form-row" },
		h("label", null, "Named graph for insertion:"),
		h("select", { name: "graph" },
			(graphs || [{ name: "", display: "Default Graph" }]).map((graph) => h("option", {
				value: graph.name || "",
			}, graph.defaultGraph ? "Default Graph" : graph.name)),
		),
	);
}

function exportPage() {
	const graphs = state.metadata?.graphs || [];
	const repoForm = h("form", { action: "bulk/export", method: "post" },
		h("label", null, h("input", { type: "radio", name: "exportAll", value: "yes", checked: true }), " Entire Repository (ZIP file)"),
		exportFormatField(),
		h("input", { type: "submit", value: "Export Repository" }),
	);
	repoForm.addEventListener("submit", (event) => downloadExport(event, repoForm, "repository-export.zip"));

	const graphForm = h("form", { action: "bulk/export", method: "post" },
		h("div", { className: "option-list" }, graphs.map((graph) => h("label", null,
			h("input", {
				type: "radio",
				name: "graph",
				value: graph.defaultGraph ? "" : graph.uri,
				checked: graph.defaultGraph,
			}),
			graph.defaultGraph ? "Default Graph" : graph.display,
		))),
		exportFormatField(),
		h("input", { type: "submit", value: "Export Graph" }),
	);
	graphForm.addEventListener("submit", (event) => downloadExport(event, graphForm, "graph-export.rdf"));

	return h("div", null,
		pageHeader("Export Data"),
		h("section", { className: "panel" }, h("h2", null, "Export Repository"), repoForm),
		h("section", { className: "panel" }, h("h2", null, "Export Graph"), graphForm),
	);
}

function exportFormatField() {
	return h("div", { className: "form-row" },
		h("label", null, "Data Format:"),
		h("select", { name: "dataFormat" },
			["N-TRIPLES", "N3", "RDF/XML", "TURTLE"].map((format) => h("option", { value: format }, format === "TURTLE" ? "Turtle" : format)),
		),
	);
}

function explorePage() {
	const value = state.route.params.get("value");
	return value ? explorerDetail(value, state.route.params.get("useLabels") === "yes") : explorerIndex();
}

function explorerIndex() {
	const form = h("form", { acceptCharset: "UTF-8" },
		h("label", null, "Enter a URI to start the exploration with:"),
		h("div", { className: "form-row" },
			h("input", { type: "text", size: "70", name: "value" }),
			h("input", { type: "submit", value: "Explore" }),
		),
	);
	form.addEventListener("submit", (event) => {
		event.preventDefault();
		const normalized = normalizeExplorerValue(new FormData(form).get("value") || "");
		window.location.hash = `/explore?value=${encodeURIComponent(normalized)}`;
	});
	return h("div", null,
		pageHeader("Explore Repository"),
		errorMessage(state.explorerError),
		h("section", { className: "panel" }, form),
		state.explorerIndex ? graphsPanel(state.explorerIndex.graphs) : loading(),
		state.explorerIndex && (state.explorerIndex.classes.length || state.explorerIndex.properties.length)
			? h("section", { className: "panel" },
				h("h2", null, "Classes and Properties"),
				h("div", { className: "split" },
					resourceList("Classes", state.explorerIndex.classes),
					resourceList("Properties", state.explorerIndex.properties),
				),
			)
			: null,
	);
}

function explorerDetail(value, useLabels) {
	const data = state.explorerData;
	const toggleLabels = () => {
		const params = new URLSearchParams({ value });
		if (!useLabels) params.set("useLabels", "yes");
		window.location.hash = `/explore?${params}`;
	};
	return h("div", null,
		pageHeader("Explore Repository", h("a", { className: "button secondary", href: "#/explore" }, "New Search")),
		errorMessage(state.explorerError),
		data ? h("div", null,
			h("section", { className: "panel" },
				h("p", null, "Showing statements for: ", h("b", null, data.value?.display || "")),
				h("label", null,
					h("input", { type: "checkbox", checked: useLabels, onChange: toggleLabels }),
					" Use resource labels in overview",
				),
			),
			statementPanel("Statements with this value as subject", data.asSubject, true),
			statementPanel("Statements with this value as predicate", data.asPredicate, true),
			statementPanel("Statements with this value as object", data.asObject, true),
			data.inGraph?.length ? statementPanel("Statements in this graph", data.inGraph, false) : null,
		) : loading(),
	);
}

function resourceList(title, values) {
	return h("div", null,
		h("h3", null, title),
		values.length ? values.map((value) => h("div", null, valueLink(value))) : h("p", { className: "status" }, "None"),
	);
}

function statementPanel(title, statements, graph) {
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
				h("tbody", null, statements.map((statement) => h("tr", null,
					graph ? h("td", { className: "value" }, valueLink(statement.graph)) : null,
					h("td", { className: "value" }, valueLink(statement.subject)),
					h("td", { className: "value" }, valueLink(statement.predicate)),
					h("td", { className: "value" }, valueLink(statement.object)),
				))),
			),
		) : h("div", { className: "empty" }, "-- no statements found --"),
	);
}

function valueLink(value) {
	if (!value) return null;
	return h("a", { href: `#/explore?value=${encodeURIComponent(value.value)}` },
		h("span", { className: "value" }, value.display),
	);
}

function indexesPage() {
	const data = state.indexes;
	return h("div", null,
		pageHeader("Indexes"),
		errorMessage(state.indexesError),
		data ? data.factories.length ? h("section", { className: "panel" },
			h("div", { className: "table-wrap" },
				h("table", { id: "indexes" },
					h("thead", null, h("tr", null,
						h("th", null, "Graph"),
						data.factories.map((factory) => h("th", null, factory)),
						h("th", null, ""),
					)),
					h("tbody", null, data.graphs.map((graph) => h("tr", null,
						h("td", null, valueLink(graph)),
						data.factories.map((factory) => h("td", { className: "count" }, graph.counts[factory] == null ? "-" : graph.counts[factory])),
						h("td", null,
							h("button", {
								className: "secondary",
								disabled: graph.enabled || state.indexesBusyGraph === graph.uri,
								onClick: () => updateIndexing(graph.uri, true),
							}, "Create All"),
							" ",
							h("button", {
								className: "secondary",
								disabled: !graph.enabled || state.indexesBusyGraph === graph.uri,
								onClick: () => updateIndexing(graph.uri, false),
							}, "Delete All"),
						),
					))),
				),
			),
		) : h("p", { className: "status" }, "None loaded") : loading(),
	);
}

function updateIndexing(graphUri, enabled) {
	state.indexesBusyGraph = graphUri;
	render();
	const update = `INSERT {} WHERE {<${graphUri}> <${state.indexes.propertyFunctionNamespace}enableIndexing> "${enabled}"^^<http://www.w3.org/2001/XMLSchema#boolean> }`;
	postSparqlUpdate(update)
		.then(() => setTimeout(loadIndexes, 3000))
		.catch((ex) => {
			state.indexesError = ex.message;
			state.indexesBusyGraph = "";
			render();
			showMessage("Index Update Failed", ex.message, "error");
		});
}

function adminPage() {
	return h("div", null,
		pageHeader("Administrator Console"),
		flushPanel(),
		h("section", { className: "panel" },
			h("div", { className: "page-header" },
				h("h2", null, state.trackers.length ? `${state.trackers.length} Queries` : "No Queries"),
				h("div", null,
					h("button", { className: "secondary", onClick: () => setTrackersPaused(!state.trackersPaused) }, state.trackersPaused ? "Resume Updates" : "Pause Updates"),
					" ",
					h("button", {
						className: "danger",
						disabled: state.trackers.length === 0,
						onClick: cancelAllQueries,
					}, "Cancel All"),
				),
			),
			errorMessage(state.trackersError),
			state.trackers.length ? h("div", { className: "tracker-list" },
				state.trackers.map((tracker) => h("div", { className: "tracker" },
					h("div", null,
						h("b", null, `ID: ${tracker.id}`),
						` (${Math.round((tracker.currentTime - tracker.created) / 1000)} seconds old) `,
						tracker.cancellable ? h("button", { className: "secondary", onClick: () => stopQueryWithDialog(tracker.id) }, "Stop") : null,
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

function flushPanel() {
	const form = h("form", { action: "bulk/flush", method: "post" },
		h("span", null, "Flush in-memory statements to disk: "),
		h("input", { type: "submit", value: "Flush Data" }),
	);
	form.addEventListener("submit", (event) => submitFormAction(event, form, {
		successTitle: "Flush Complete",
		successMessage: "In-memory statements were flushed to disk.",
		errorTitle: "Flush Failed",
	}));
	return h("section", { className: "panel" }, h("h2", null, "Flush Data"), form);
}

function setTrackersPaused(paused) {

	state.trackersPaused = paused;
	render();
}

function loadMetadata() {
	api("metadata")
		.then((data) => {
			state.metadata = data;
			state.memory = data.memory;
			state.loadError = "";
			render();
		})
		.catch((ex) => {
			state.loadError = ex.message;
			render();
		});
}

function loadMemory() {
	api("memory")
		.then((data) => {
			state.memory = data;
			state.memoryError = "";
			if (state.route.path === "/") render();
		})
		.catch((ex) => {
			state.memoryError = ex.message;
			if (state.route.path === "/") render();
		});
}

function loadExplorerIndex() {
	api("explorer-index")
		.then((data) => {
			state.explorerIndex = data;
			state.explorerError = "";
			render();
		})
		.catch((ex) => {
			state.explorerError = ex.message;
			render();
		});
}

function loadExplorerDetail(value, useLabels) {
	const params = new URLSearchParams({ value });
	if (useLabels) params.set("useLabels", "yes");
	api(`explorer?${params}`)
		.then((data) => {
			state.explorerData = data;
			state.explorerError = "";
			render();
		})
		.catch((ex) => {
			state.explorerError = ex.message;
			render();
		});
}

function loadIndexes() {
	api("indexes")
		.then((data) => {
			state.indexes = data;
			state.indexesError = "";
			state.indexesBusyGraph = "";
			state.indexesLoaded = true;
			render();
		})
		.catch((ex) => {
			state.indexesError = ex.message;
			state.indexesBusyGraph = "";
			state.indexesLoaded = true;
			render();
		});
}

function loadTrackers() {
	fetch("tracker", { headers: { Accept: "application/json" } })
		.then((response) => response.json())
		.then((data) => {
			state.trackers = data || [];
			state.trackersError = "";
			state.trackersLoaded = true;
			if (state.route.path === "/admin") render();
		})
		.catch((ex) => {
			state.trackersError = ex.message;
			state.trackersLoaded = true;
			if (state.route.path === "/admin") render();
		});
}

function stopQuery(id) {
	return requestText("tracker", {
		method: "POST",
		headers: { "Content-Type": "application/x-www-form-urlencoded" },
		body: new URLSearchParams({ id }),
	}).then((message) => loadTrackers().then(() => message));
}

function stopQueryWithDialog(id) {
	stopQuery(id)
		.then((message) => showMessage("Query Cancelled", message || `Cancelled query ${id}.`, "success"))
		.catch((ex) => showMessage("Cancellation Failed", ex.message, "error"));
}

function cancelAllQueries() {
	showConfirm({
		title: "Cancel All Queries",
		body: `Cancel ${state.trackers.length} active queries?`,
		confirmLabel: "Cancel All",
		danger: true,
		onConfirm: () => Promise.all(state.trackers.map((item) => stopQuery(item.id)))
			.then(() => showMessage("Queries Cancelled", "All active queries were cancelled.", "success"))
			.catch((ex) => showMessage("Cancellation Failed", ex.message, "error")),
	});
}

function startPageEffects() {
	if (state.route.path === "/") {
		if (!state.memory) {
			loadMemory();
		}
		state.timers.push(setInterval(loadMemory, 3000));
	}
	if (state.route.path === "/explore") {
		const value = state.route.params.get("value");
		const key = value
			? `detail:${value}:${state.route.params.get("useLabels") === "yes"}`
			: "index";
		if (state.loadedExplorerKey !== key) {
			state.loadedExplorerKey = key;
			if (value) {
				loadExplorerDetail(value, state.route.params.get("useLabels") === "yes");
			} else {
				loadExplorerIndex();
			}
		}
	}
	if (state.route.path === "/indexes" && !state.indexesLoaded) {
		loadIndexes();
	}
	if (state.route.path === "/admin" && !state.trackersPaused) {
		if (!state.trackersLoaded) {
			loadTrackers();
		}
		state.timers.push(setInterval(loadTrackers, 1000));
	}
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



function submitQuery(event, form) {
	event.preventDefault();
	const display = new FormData(form).get("display");
	setDisplayHiddenFields(form, display);
	const submitter = event.submitter || form.querySelector("input[type='submit'], button[type='submit']");
	setSubmitBusy(submitter, true);
	const request = buildFormRequest(form);
	fetch(request.url, request.init)
		.then(async (response) => {
			if (!response.ok) throw new Error(await responseMessage(response) || `${response.status} ${response.statusText}`);
			const blob = await response.blob();
			showResultModal(blob, filenameFromResponse(response) || queryFilename(display));
		})
		.catch((ex) => showMessage("Query Failed", ex.message, "error"))
		.finally(() => setSubmitBusy(submitter, false));
}

function showResultModal(blob, filename) {
	const url = URL.createObjectURL(blob);
	showModal({
		title: "Query Results",
		body: h("iframe", { className: "result-frame", src: url, title: "Query Results" }),
		kind: "result",
		actions: [
			{ label: "Download", className: "secondary", onClick: () => saveBlob(blob, filename) },
			{ label: "Close", className: "", onClick: () => { URL.revokeObjectURL(url); closeModal(); } },
		],
	});
}

function queryFilename(display) {
	if (display === "csv") return "query-results.csv";
	if (display === "json") return "query-results.json";
	return "query-results.xml";
}

function refreshRepositoryState() {
	state.explorerIndex = null;
	state.explorerData = null;
	state.loadedExplorerKey = "";
	state.indexesLoaded = false;
	loadMetadata();
}

function submitFormAction(event, form, options) {
	event.preventDefault();
	const submitter = event.submitter || form.querySelector("input[type='submit'], button[type='submit']");
	setSubmitBusy(submitter, true);
	return runPreparedAction(buildFormRequest(form), options)
		.finally(() => setSubmitBusy(submitter, false));
}

function buildFormRequest(form) {
	const enctype = (form.enctype || "").toLowerCase();
	const body = enctype.includes("multipart/form-data")
		? new FormData(form)
		: new URLSearchParams(new FormData(form));
	const headers = enctype.includes("multipart/form-data")
		? { Accept: "text/html,application/json,text/plain" }
		: { Accept: "text/html,application/json,text/plain", "Content-Type": "application/x-www-form-urlencoded" };
	return {
		url: form.getAttribute("action") || window.location.href,
		init: { method: form.method || "POST", headers, body },
	};
}

function runPreparedAction(request, options) {
	return requestText(request.url, request.init)
		.then((message) => {
			showMessage(options.successTitle || "Complete", message || options.successMessage || "The action completed.", "success");
			if (options.onSuccess) options.onSuccess();
		})
		.catch((ex) => showMessage(options.errorTitle || "Request Failed", ex.message, "error"));
}

function setSubmitBusy(submitter, busy) {
	if (!submitter) return;
	if (busy) {
		submitter.dataset.originalValue = submitter.value || submitter.textContent || "";
		submitter.disabled = true;
		if ("value" in submitter) submitter.value = "Working...";
		else submitter.textContent = "Working...";
	} else {
		submitter.disabled = false;
		const original = submitter.dataset.originalValue;
		if (original) {
			if ("value" in submitter) submitter.value = original;
			else submitter.textContent = original;
		}
	}
}

function requestText(url, init) {
	return fetch(url, init).then(async (response) => {
		const message = await responseMessage(response);
		if (!response.ok) throw new Error(message || `${response.status} ${response.statusText}`);
		return message;
	});
}

async function responseMessage(response) {
	const contentType = response.headers.get("content-type") || "";
	const text = await response.text();
	if (contentType.includes("application/json")) {
		try {
			const json = JSON.parse(text);
			return json.message || json.error || json.title || JSON.stringify(json, null, 2);
		} catch (_ex) {
			return text.trim();
		}
	}
	if (contentType.includes("text/html")) {
		const doc = new DOMParser().parseFromString(text, "text/html");
		return (doc.body?.textContent || text).replace(/\s+/g, " " ).trim();
	}
	return text.trim();
}

function downloadExport(event, form, defaultFilename) {
	event.preventDefault();
	const submitter = event.submitter || form.querySelector("input[type='submit'], button[type='submit']");
	setSubmitBusy(submitter, true);
	const request = buildFormRequest(form);
	fetch(request.url, request.init)
		.then(async (response) => {
			if (!response.ok) throw new Error(await responseMessage(response) || `${response.status} ${response.statusText}`);
			const blob = await response.blob();
			saveBlob(blob, filenameFromResponse(response) || defaultFilename);
			showMessage("Export Ready", "The export download has started.", "success");
		})
		.catch((ex) => showMessage("Export Failed", ex.message, "error"))
		.finally(() => setSubmitBusy(submitter, false));
}

function filenameFromResponse(response) {
	const disposition = response.headers.get("content-disposition") || "";
	const utfMatch = disposition.match(/filename\*=UTF-8''([^;]+)/i);
	if (utfMatch) return decodeURIComponent(utfMatch[1]);
	const match = disposition.match(/filename="?([^";]+)"?/i);
	return match ? match[1] : "";
}

function saveBlob(blob, filename) {
	const url = URL.createObjectURL(blob);
	const link = h("a", { href: url, download: filename });
	document.body.append(link);
	link.click();
	link.remove();
	setTimeout(() => URL.revokeObjectURL(url), 1000);
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

window.addEventListener("hashchange", () => {
	state.route = readRoute();
	if (state.route.path !== "/explore") {
		state.loadedExplorerKey = "";
	}
	if (state.route.path !== "/indexes") {
		state.indexesLoaded = false;
	}
	if (state.route.path !== "/admin") {
		state.trackersLoaded = false;
	}
	state.explorerData = null;
	render();
});

loadMetadata();
render();

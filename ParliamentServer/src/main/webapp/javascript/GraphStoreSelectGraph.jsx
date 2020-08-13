import React, { Component } from "react";

class GraphStoreSelectGraph extends Component {
    constructor(props) {
        super(props);
        this.state = {
            click: false,
            result: "",
            graph: ""
        }

        this.handleGraphList = this.handleGraphList.bind(this);
        this.handleGraphChoice = this.handleGraphChoice.bind(this);
    }

    render() {
        if (this.state.result.head) {
            var graphList = this.state.result;
            var colNames = graphList.head.vars;
            return (
                <div>
                    <h3>Graph Choice: {this.state.graph}</h3>
                    <button onClick={this.handleGraphList}>Click to Select Graph</button>
                    <ul>
                        {graphList.results.bindings.map(row => (
                            <li><button onClick={() => this.handleGraphChoice(row[colNames[0]].value)}>{row[colNames[0]].value}</button></li>
                        ))}
                    </ul>
                </div>
            );
        }
        else {
            return (
                <div>
                    <button onClick={this.handleGraphList}>Click to Select Graph</button>
                </div>
            );
        }
    }
    async handleGraphList() {
        const response = await fetch(encodeURI("/parliament/sparql"), {
            method: "POST",
            body: "prefix par: <http://parliament.semwebcentral.org/parliament#>\nselect distinct ?g where { graph par:MasterGraph { ?g a par:NamedGraph . }}",
            headers: {
                "Content-Type": "application/sparql-query"
            }
            });
        const res = await response.json();   
        this.setState({result: res});
    }

    handleGraphChoice(graphName) {
        this.setState({graph: graphName});
    }
}

export default GraphStoreSelectGraph;
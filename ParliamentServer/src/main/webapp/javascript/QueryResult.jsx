import React, { Component } from "react";

class QueryResult extends Component {
    render() {
        if (!this.props.result) {
            return (<div>No Results yet...</div>)
        }
        var colNames = this.props.result.head.vars;
        return (<ul id="col1-list">
            {this.props.result.results.bindings.map(result => (
                <li>
                    {result[colNames[0]].value}
                </li>
            ))}
        </ul>);
    }
}

export default QueryResult;
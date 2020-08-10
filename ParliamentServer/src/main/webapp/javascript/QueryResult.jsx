import React, { Component } from "react";

class QueryResult extends Component {
    render() {
        if (!this.props.result) {
            return (<div>No Results yet...</div>)
        }
        var colNames = this.props.result.head.vars;
        return (<table id="query_table">
            <tr>
                {colNames.map(name => (
                    <th>{name}</th>
                ))}
            </tr>

            {this.props.result.results.bindings.map(row => (
                <tr>
                {Object.keys(row).map(entry => (
                    <td>{row[entry].value}</td>
                ))}
                </tr>
                //<td>{rows[colNames[0]].value}</td>
                //<td>{rows[colNames[1]].value}</td>
                //<td>{rows[colNames[2]].value}</td>
            ))}
        </table>);
    }
}

export default QueryResult;
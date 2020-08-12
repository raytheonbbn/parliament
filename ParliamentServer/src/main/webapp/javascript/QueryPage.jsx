import React, { Component } from 'react';
import QueryResult from './QueryResult';
import QueryForm from './QueryForm';

class QueryPage extends Component {
    constructor(props) {
        super(props)
        this.state = {
            result: ""
        }
    }

    parentFunction(data) {
        this.setState({result:data})
    }

    render() {
        return (
            <div id="main">
                <h1>Parliament</h1>
                <QueryForm getStateFromParent={this.parentFunction.bind(this)}/>
                <QueryResult result={this.state.result}/>
            </div>
        );
    } 

}

export default QueryPage;
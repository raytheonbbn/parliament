import React, { Component } from 'react';

import GraphStoreSelectGraph from './GraphStoreSelectGraph';

class GraphStorePage extends Component {
    constructor(props) {
        super(props)
        this.state = {
            result: ""
        }
    }


    render() {
        return (
            <div id="main">
                <h1>Parliament</h1>
                <GraphStoreSelectGraph/>
            </div>
        );
    } 

}

export default GraphStorePage;
import React, { Component } from 'react';
import '../css/main.css';

class Home extends Component {


    render() {
        return (
            <div id="main">
                <h1>Parliament</h1>
                <h2>Operations</h2>
                <ul>
                    <li><a href="/query">Query</a></li>
                    <li><a href="/update">Update</a></li>
                    <li><a href="/graphstore">Graph Store Protocol</a></li>
                </ul>
                
            </div>
        );
    } 

}

export default Home;
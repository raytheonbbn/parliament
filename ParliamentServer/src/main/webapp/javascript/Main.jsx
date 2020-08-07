import React, { Component } from 'react';
import ReactDOM from 'react-dom' ;
import '../css/main.css';
import QueryResult from './QueryResult'

class Main extends Component {
    constructor(props) {
        super(props)
        this.state = {
            result: ""
        }
    }

    componentDidMount() {
        fetch(encodeURI("/parliament/sparql?query=select ?x ?y ?z where { ?x ?y ?z }"))
            .then(res => res.json())
            .then((response) => {
                this.setState({
                    result: response
                });
            },
            (error) => {
                alert(error);
            }
            )
    }

    render() {
        return (
            <div>
                <h1>Demo Component</h1>
                <QueryResult result={this.state.result}/>
                <img src="https://upload.wikimedia.org/wikipedia/commons/a/a7/React-icon.svg"/>
            </div>
        );
    }
}

ReactDOM.render(
    <Main />,
    document.getElementById('react-mountpoint')
);
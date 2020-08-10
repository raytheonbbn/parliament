import React, { Component } from 'react';
import ReactDOM from 'react-dom' ;
import '../css/main.css';
import QueryResult from './QueryResult'
import QueryForm from './QueryForm'

class Main extends Component {
    constructor(props) {
        super(props)
        this.state = {
            result: ""
        }
    }

    componentDidMount() {
    }

    parentFunction(data) {
        this.setState({result:data})
    }

    render() {
        return (
            <div>
                <h1>Parliament</h1>
                <QueryForm getStateFromParent={this.parentFunction.bind(this)}/>
                <QueryResult result={this.state.result}/>
            </div>
        );
    }
}

ReactDOM.render(
    <Main />,
    document.getElementById('react-mountpoint')
);
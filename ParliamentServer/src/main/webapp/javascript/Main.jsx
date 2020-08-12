import React, { Component } from 'react';
import ReactDOM from 'react-dom' ;
import { BrowserRouter, Route, Switch } from 'react-router-dom';


import QueryPage from './QueryPage';
import UpdatePage from './UpdatePage';
import Home from './Home';
import Error from './Error';

class Main extends Component {
    constructor(props) {
        super(props)
        this.state = {
            result: ""
        }
    }

    componentDidMount() {
    }


    render() {
        return (
            <Switch>
                <Route exact path="/" component={Home}/>
                <Route path="/query" component={QueryPage}/>
                <Route path="/update" component={UpdatePage}/>
                <Route component={Error}/>
            </Switch>
        );
    }
}

//

ReactDOM.render(
    <BrowserRouter>
        <Main />
    </BrowserRouter>,
    document.getElementById('react-mountpoint')
);
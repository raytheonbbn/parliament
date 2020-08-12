import React, { Component } from 'react';
import UpdateForm from './UpdateForm';

class UpdatePage extends Component {
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
                <UpdateForm getStateFromParent={this.parentFunction.bind(this)}/>
            </div>
        );
    } 

}

export default UpdatePage;
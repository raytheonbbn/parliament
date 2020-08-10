import React, { Component } from "react";
import '../css/QueryForm.css';

class QueryForm extends Component {
    constructor(props) {
        super(props);
        this.state = {
            value: "SELECT ?x ?y ?z WHERE { ?x ?y ?z }"
        }

        this.handleSubmit = this.handleSubmit.bind(this);
        this.handleChange = this.handleChange.bind(this);
    }

    childFunction(event) {
        event.preventDefault();
        this.props.getStateFromParent(this.state.result);
    }

    handleChange(event) {
        this.setState({value: event.target.value})
    }

    handleSubmit(event) {
        fetch(encodeURI("/parliament/sparql?query=" + this.state.value))
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
        event.preventDefault();
        this.childFunction(event);
    }

    render() {
        return (
            <form onSubmit={this.handleSubmit}>
                <label>
                    Query:
                    <br></br>
                    <textarea id ="queryBox" rows="10" cols="200" value={this.state.value} onChange={this.handleChange} />
                </label>
                <br></br>
                <input type="submit" value="submit" />
            </form>
        );
    }
}

export default QueryForm;
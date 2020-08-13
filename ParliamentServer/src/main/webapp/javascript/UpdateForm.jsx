import React, { Component } from "react";
import '../css/QueryForm.css';

class UpdateForm extends Component {
    constructor(props) {
        super(props);
        this.state = {
            value: `
PREFIX afn: <http://jena.hpl.hp.com/ARQ/function#>
PREFIX dc: <http://purl.org/dc/elements/1.1/>
PREFIX dul: <http://www.loa-cnr.it/ontologies/DUL.owl#>
PREFIX fn: <http://www.w3.org/2005/xpath-functions#>
PREFIX geo: <http://www.opengis.net/ont/geosparql#>
PREFIX geof: <http://www.opengis.net/def/function/geosparql/>
PREFIX geor: <http://www.opengis.net/def/rule/geosparql/>
PREFIX gml: <http://www.opengis.net/ont/gml#>
PREFIX ja: <http://jena.hpl.hp.com/2005/11/Assembler#>
PREFIX ogc: <http://www.opengis.net/>
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX par: <http://parliament.semwebcentral.org/parliament#>
PREFIX pt: <http://bbn.com/ParliamentTime#>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX rss: <http://purl.org/rss/1.0/>
PREFIX sf: <http://www.opengis.net/ont/sf#>
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
PREFIX ssn: <http://purl.oclc.org/NET/ssnx/ssn#>
PREFIX time: <http://www.w3.org/2006/time#>
PREFIX vcard: <http://www.w3.org/2001/vcard-rdf/3.0#>
PREFIX xml: <http://www.w3.org/XML/1998/namespace>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>


INSERT DATA
{
#triples
}

DELETE DATA
{
#triples
}`       
        }

        this.handleSubmit = this.handleSubmit.bind(this);
        this.handleChange = this.handleChange.bind(this);
    }

    childFunction() {
        this.props.getStateFromParent(this.state.result);
    }

    handleChange(event) {
        this.setState({value: event.target.value})
    }

    async handleSubmit(event) {
        event.preventDefault();

        const response = await fetch(encodeURI("/parliament/update"), {
            method: "POST",
            body: this.state.value,
            headers: {
                "Content-Type": "application/sparql-update"
            }
            });
        const res = await response.json();    
        this.setState({result: res})
        
        this.childFunction();
    }

    render() {
        return (
            <form onSubmit={this.handleSubmit}>
                <label>
                    Query:
                    <br></br>
                    <textarea id ="queryBox" rows="30 " cols="90" spellcheck="false" value={this.state.value} onChange={this.handleChange} />
                </label>
                <br></br>
                <input type="submit" value="submit" />
            </form>
        );
    }
}

export default UpdateForm;
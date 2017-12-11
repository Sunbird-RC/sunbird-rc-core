require 'json'
require 'pp'
require 'json/ld'
require 'rdf/turtle'
require 'rdf/reasoner'

file = File.read "teacher.json"
input = JSON.parse(file)
graph = RDF::Graph.new << JSON::LD::API.toRdf(input)
pp graph.dump(:ttl, prefixes: {foaf: "http://xmlns.com/foaf/0.1/"})

RDF::Reasoner.apply(:rdfs)

obj = RDF::Literal(Date.new)
p RDF::Vocab::DOAP.created.range_compatible?(obj, graph)
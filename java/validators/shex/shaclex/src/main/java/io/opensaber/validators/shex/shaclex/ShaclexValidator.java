package io.opensaber.validators.shex.shaclex;

import java.io.File;

import org.apache.jena.rdf.model.Model;

import es.weso.rdf.jena.RDFAsJenaModel;
import scala.Option;
import scala.util.Try;

public class ShaclexValidator {

	public void validate(File arg0, String arg1, Option<String> arg2){
		// TODO Auto-generated method stub
		Try<RDFAsJenaModel> model = RDFAsJenaModel.fromFile(arg0, arg1, arg2);
	}

}

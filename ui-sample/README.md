This is a sample utility to demonstrate the OpenSABER API's.

This sample UI is auto-generated from the schema using [alpacajs](http://www.alpacajs.org/). The schema is placed under "schema" folder. Presently, this works with the Person schema provided in the src/main/resources.

### Features
* UI validation
  Field required, any regex validations can be enforced at the presentation layer
* Dynamic form generation
  There is no code written for the form. The alpacajs library generates the form based on the schema. You might notice that the datepicker fields are automatically rendered for the date fields. You could read more about schema types and UI element types in the alpacajs [documentation](http://www.alpacajs.org/documentation.html).
* Layout customization
  You might like to customize and re-position the elements in the screen estate. This could be achieved by 'view' in the alpacajs world. Read about styles [here](http://www.alpacajs.org/docs/api/views.html)
  

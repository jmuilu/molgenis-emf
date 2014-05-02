Molgenis-EMF
===========

Tools for converting [Molgenis](http://molgenis.org) models to UML/Ecore XMI and back using the [EMF framework](https://www.eclipse.org/modeling/emf/). Conversion between the Ecore and UML is done using the utility method available in EMF. Only modelling details available in the Ecore language are used.

## MolgenisToUML/Ecore
### Notes
- Many-to-many relationships are implemented without intermediating class
- All one-to-many associations are directional. Many-to-many relationships are bi-directional leading to two associations in Ecore.
- Role name in associations is taken from the field's label. In the many-to-many case, the role name of opposite association is derived from  getMrefLocalid and label. 
- Models can be visualized using e.g. [EMF to Graphviz](http://marketplace.eclipse.org/content/emf-graphviz-emf2gv) and [TextUML](http://marketplace.eclipse.org/content/textuml-toolkit) Eclipse add-ons. 
- XML and JSON serializations are supported. Examples can be generated using using Maven's test task (mvn test)

## Molgenis - Ecore mappings
|Molgenis| Ecore|
|--------|-------|
|**Entity**|**eClass**|
|name| name |
|description|EAnnotations|
|abstract (bool)|abstract (bool)|
|xref_label| -|
|**Field (attrib)**|**EAttribute**|
|name|name|
|description| eClass.EAnnotations|
|label|-|
|unique|TBD|
|default|TBD|
|autoid|-|
|nillable|!isRequired|
|type|type|
|**Field (xref)**|**EReference**|
|name|name|
|description|EAnnotations|
|label|-|
|xref_label|-|
|xref_field|-|
|entity|end1Class|
|xref_entity|end2Class|
|nillable|!isRequired|
|**Field (mref)**|**EReference (with eOpposite)**|
|name|name|
|description|EAnnotations|
|label|-|
|xref_label|-|
|xref_field|-|
|entity|end1Class|
|xref_entity|end2Class|
|nillable|!isRequired|
|cardinality|TBD|
|-|eOpposite|


## Ecore example
[Molgenis DSL](molgenis-emf/src/test/resources/simple_model/molgenis_db.xml):
![Example-Ecore](molgenis-emf/out_dir/example.ecore.jpg)

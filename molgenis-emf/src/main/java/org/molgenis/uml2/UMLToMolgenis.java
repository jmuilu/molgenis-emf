package org.molgenis.uml2;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.EMap;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.uml2.uml.DataType;
import org.eclipse.uml2.uml.Package;
import org.molgenis.fieldtypes.FieldType;
import org.molgenis.model.MolgenisModelException;
import org.molgenis.model.elements.DBSchema;
import org.molgenis.model.elements.Entity;
import org.molgenis.model.elements.Field;
import org.molgenis.model.elements.Model;

public class UMLToMolgenis
{
	/*
	 * The conversion is based on the Ecore specification. UML model can provide
	 * optional information.
	 */
	protected Package umlPackage = null;
	protected EPackage ecorePackage = null;
	protected UMLEcoreUtil uml = new UMLEcoreUtil();

	static Logger logger = Logger.getLogger(UMLToMolgenis.class.getSimpleName());

	private UMLToMolgenis(final Package pkg)
	{
		umlPackage = pkg;
		Collection<EPackage> coll = uml.convertToEcorePackageCollection(pkg);
		if (coll.isEmpty())
		{
			String message = "Cannot convert UML package to Ecore. Looks like the root package or model is missing ";
			logger.fatal(message);
			throw new RuntimeException(message);
		}
		if (coll.size()>1)
		{
			Iterator <EPackage> iter = coll.iterator();
			EPackage tmp1 = iter.next();
			EPackage tmp2 = iter.next();
			logger.warn("UML to Ecore conversion is ambiquous. More than one packages returned ("+coll.size()+"). E.g. : "
					+ tmp1.getName() + " and " + tmp2.getName() + ". Only " + tmp1.getName()
					+ " will be used"); // todo: check possible cases for this
		}
		ecorePackage = coll.iterator().next();
	}

	public UMLToMolgenis createInstance(final Package pkg)
	{
		return new UMLToMolgenis(pkg);
	}

	public Model toMolgenisModel()
	{
		Model model = new Model("molgenis");
		model.setName(ecorePackage.getName()); // todo: check ???
		DBSchema schema = new DBSchema( ecorePackage.getName(), null, model); //todo: check
		Vector<Entity> entities = new Vector<Entity>();
		EList<EClassifier> classifiers = ecorePackage.getEClassifiers();

		Collection<EClass> classes = EcoreUtil.getObjectsByType(classifiers, EcorePackage.Literals.ECLASS);
		for (EClass eClass : classes)
		{
			System.out.println(eClass.getName());	
			entities.add( toMolgenisEntity(eClass, schema) );
		}

		ecorePackage.getEClassifiers();
		return null;
	}

	public String toMolgenisDescription(EList<EAnnotation> annotations) {
		
		String desc="";
		for (Iterator iteratorAnnotations = annotations.iterator(); iteratorAnnotations.hasNext();)
		{
			EAnnotation eAnnotation = (EAnnotation) iteratorAnnotations.next();

			EMap<String, String> d=eAnnotation.getDetails();
			for (Iterator iterator = d.iterator(); iterator.hasNext();)
			{
				Entry<String, String> entry = (Entry<String, String>) iterator.next();
				desc = desc + entry.getValue()+"\n"; //todo: improve annotations
			}			
		}
	    return desc;
	}

	public Field  toMolgenisField( EStructuralFeature eFeature) {
		Field field = new Field(eFeature.getName());
		field.setDescription(toMolgenisDescription(eFeature.getEAnnotations()));
		return field;
	}

	
	public FieldType toMolgenisType( DataType dataType) {
		FieldType mtype = null;
		return mtype;
	}
	public Field  toMolgenisField( EAttribute eAttribute) {
		Field field = toMolgenisField((EStructuralFeature)eAttribute);
		
		return field;
	}
	
	public Field  toMolgenisField( EReference eReference) {
		Field field = toMolgenisField((EStructuralFeature)eReference);
		return field;
	}
	
	public Entity toMolgenisEntity( EClass eClass, DBSchema schema) { 
		Entity e= new Entity(eClass.getName(), schema);
		e.setAbstract( eClass.isAbstract());
		Collection<EAttribute> attrs = eClass.getEAllAttributes();
		try
		{
			for (Iterator iterator = attrs.iterator(); iterator.hasNext();)
			{
				EAttribute eAttribute = (EAttribute) iterator.next();
				Field field = toMolgenisField(eAttribute) ;
				e.addField(field);
			}
			Collection<EReference> xrefs = eClass.getEAllReferences();
			for (Iterator iterator = xrefs.iterator(); iterator.hasNext();)
			{
				EReference eReference = (EReference) iterator.next();
				Field field = toMolgenisField(eReference) ;
				e.addField(field);
			}
		} catch (MolgenisModelException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}		
		
		return e;
	}
	public static void main(String[] args) throws Exception
	{
		
		UMLEcoreUtil uml2 = new UMLEcoreUtil();
		Package pkg = uml2.loadUMLModel("out_dir/example.uml");
		UMLToMolgenis mol = new UMLToMolgenis(pkg);
		mol.toMolgenisModel();

	}
}

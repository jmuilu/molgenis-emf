package org.molgenis.uml2;

import java.io.File;
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
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.ETypedElement;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.uml2.uml.DataType;
import org.eclipse.uml2.uml.Package;
import org.molgenis.MolgenisOptions;
import org.molgenis.model.MolgenisModel;
import org.molgenis.model.jaxb.Entity;
import org.molgenis.model.jaxb.Field;
import org.molgenis.model.jaxb.Field.Type;
import org.molgenis.model.jaxb.Model;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

public class UMLToMolgenis
{
	/*
	 * The conversion is based on the Ecore specification. UML model can provide
	 * optional information.
	 */
	protected Package umlPackage = null;
	protected EPackage ecorePackage = null;
	protected UMLEcoreUtil uml = new UMLEcoreUtil();
	protected ConfigUtil cfg = new ConfigUtil("molgenis-emf.properties"); // todo:
																			// add
																			// into
																			// constructor
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
		if (coll.size() > 1)
		{
			Iterator<EPackage> iter = coll.iterator();
			EPackage tmp1 = iter.next();
			EPackage tmp2 = iter.next();
			logger.warn("UML to Ecore conversion is ambiquous. More than one packages returned (" + coll.size()
					+ "). E.g. : " + tmp1.getName() + " and " + tmp2.getName() + ". Only " + tmp1.getName()
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
		Model model = new Model();
		model.setName(ecorePackage.getName());
		// Vector<Entity> entities = new Vector<Entity>();
		EList<EClassifier> classifiers = ecorePackage.getEClassifiers();

		Collection<EClass> classes = EcoreUtil.getObjectsByType(classifiers, EcorePackage.Literals.ECLASS);
		for (EClass eClass : classes)
		{
			// entities.add(toMolgenisEntity(eClass));
			model.addEntity(toMolgenisEntity(eClass));
		}
		return model;
	}

	public String toMolgenisDescription(EList<EAnnotation> annotations)
	{

		String desc = "";
		for (Iterator iteratorAnnotations = annotations.iterator(); iteratorAnnotations.hasNext();)
		{
			EAnnotation eAnnotation = (EAnnotation) iteratorAnnotations.next();

			EMap<String, String> d = eAnnotation.getDetails();
			for (Iterator iterator = d.iterator(); iterator.hasNext();)
			{
				Entry<String, String> entry = (Entry<String, String>) iterator.next();
				desc = desc + entry.getValue() + "\n"; // todo: improve
														// annotations
			}
		}
		return desc.replaceAll("\\s+$", "").replaceAll("^\\s+", "");
	}

	public Field toMolgenisField(EStructuralFeature eFeature)
	{
		Field field = new Field();
		field.setName(eFeature.getName());
		String anno=toMolgenisDescription(eFeature.getEAnnotations());
		if ( anno!= "") field.setDescription(anno);
		field.setNillable(  !eFeature.isRequired());
		return field;
	}

	public Type typeMapper(EDataType t)
	{
		if (t.getName().equals("bool"))
		{
			return Type.BOOL;
		} else
		{
			return Type.STRING;
		}
	}

	public Field toMolgenisField(EAttribute eAttribute)
	{
		Field field = toMolgenisField((EStructuralFeature) eAttribute);
		EDataType t = eAttribute.getEAttributeType();		
		Type molgenisType = null;
		if (t instanceof EEnum)
		{
			molgenisType = Type.ENUM;
			Object literals[] = ((EEnum) t).getELiterals().toArray();
			String str = literals.length > 1 ? literals[0].toString() : "";
			for (int i = 1; i < literals.length; i++)
			{
				str = str + "," + literals[i].toString();
			}
			field.setEnumoptions("["+str+"]");
		} else
		{
			molgenisType = cfg.toMolgenisType(t.getName());
		}
		field.setType(molgenisType);
		field.setUnique( eAttribute.isUnique());
		return field;
	}

	public Field toMolgenisField(EReference eReference)
	{
		Field field = toMolgenisField((EStructuralFeature) eReference) ;
		EReference opp = eReference.getEOpposite();	
		Type mReftype = null;
		if ( opp != null ) { 
			mReftype = Type.XREF_MULTIPLE ;	
			field.setMrefLocalid(opp.getName());
		} else { 
			mReftype = Type.XREF_SINGLE;
		}
		field.setType( mReftype);
		field.setXrefEntity(eReference.getEType().getName());
		return field;
	}

	public String implementsAsStrList(  EClass eClass ) {   
		EList<EClass> supers = eClass.getESuperTypes();
		String str="";
		for (Iterator iterator = supers.iterator(); iterator.hasNext();)
		{
			EClass cls = (EClass) iterator.next();
			if ( cls.isAbstract()) { 
				str = str + ","+cls.getName();
			}
		}
		return str.replaceAll("^,","");
	}
	
	public String extendsAsStrList(  EClass eClass ) { 
		EList<EClass> supers = eClass.getESuperTypes();
		String str="";
		for (Iterator iterator = supers.iterator(); iterator.hasNext();)
		{
			EClass cls = (EClass) iterator.next();
			if ( !cls.isAbstract()) { 
				str = str + "," + cls.getName();
			}
		}
		return str.replaceAll("^,","");
	}

	public Entity toMolgenisEntity(EClass eClass)
	{
		Entity e = new Entity();
		e.setName(eClass.getName());
		String ext = extendsAsStrList(eClass);
		if ( ext != "" ) {
			e.setExtends(ext);
		}
		String impl = implementsAsStrList(eClass);
		if ( impl != "" ) {
			if ( eClass.isAbstract()) {
				logger.warn("Check. Abstract class implements other abstract class(es). Class/Interface: "+e.getName()+" implements: "+impl);
			}
			e.setImplements(impl);
		}
		String anno = toMolgenisDescription(eClass.getEAnnotations());
		if ( anno != "") e.setDescription( anno);
		e.setAbstract(eClass.isAbstract());
		Collection<EAttribute> attrs = eClass.getEAllAttributes();
		for (Iterator iterator = attrs.iterator(); iterator.hasNext();)
		{
			EAttribute eAttribute = (EAttribute) iterator.next();
			Field field = toMolgenisField(eAttribute);
			e.addField(field);
		}
		Collection<EReference> xrefs = eClass.getEAllReferences();
		for (Iterator iterator = xrefs.iterator(); iterator.hasNext();)
		{
			EReference eReference = (EReference) iterator.next();
			Field field = toMolgenisField(eReference);
			e.addField(field);
		}
		return e;
	}

	public static void writeModel(String fileName, Model model) throws JAXBException
	{
		writeModel(new File(fileName), model);
	}

	public static void writeModel(File file, Model model) throws JAXBException
	{
		JAXBContext jaxbContext = JAXBContext.newInstance(Model.class);
		Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		jaxbMarshaller.marshal(model, file);
	}

	public static void main(String[] args) throws Exception
	{

		UMLEcoreUtil uml2 = new UMLEcoreUtil();
		Package pkg = uml2.loadUMLModel("out_dir/example.uml");
		UMLToMolgenis mol = new UMLToMolgenis(pkg);
		Model m = mol.toMolgenisModel();
		writeModel("out_dir/example.xml", m);
		
		//org.molgenis.model.elements.Model model = MolgenisModel.parse(new MolgenisOptions("out_dir/molgenis.properties"));

	}
}
